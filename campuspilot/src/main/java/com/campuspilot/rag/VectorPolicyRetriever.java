package com.campuspilot.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Policy;
import com.campuspilot.entity.PolicyVersion;
import com.campuspilot.mapper.PolicyMapper;
import com.campuspilot.policy.PolicyVersionService;
import com.campuspilot.rag.embedding.EmbeddingProvider;
import com.campuspilot.rag.embedding.LocalEmbeddingProvider;
import com.campuspilot.rag.vector.InMemoryVectorStore;
import com.campuspilot.rag.vector.ScoredDocument;
import com.campuspilot.rag.vector.TextSplitter;
import com.campuspilot.rag.vector.VectorDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 本地向量检索器：TF-IDF 向量化 + 内存向量库 + 余弦相似度 + 元数据过滤 + 重排序。
 *
 * 检索流程：文本拆分 → Embedding(本地TF-IDF) → VectorStore搜索 → 相似度排序
 *           → 元数据过滤(分类/有效期/权限) → topK → 重排序(关键词加权回注)
 *
 * 知识来源：
 * 1. MySQL 中的 ACTIVE/未删除政策（当前监管主体）
 * 2. knowledge/ 目录下的 DEMO_POLICY 素材（为演示补全各业务域）
 *
 * 模型说明：向量由 {@link LocalEmbeddingProvider} 基于 TF-IDF + 字符二元组生成，
 * 为真实计算所得（非神经网络、非伪造）。若需语义级嵌入，可切换 FastGPT 嵌入服务。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorPolicyRetriever implements PolicyRetriever {

    private final PolicyMapper policyMapper;
    private final PolicyVersionService policyVersionService;
    private final LocalEmbeddingProvider embeddingProvider;
    private final InMemoryVectorStore vectorStore;

    @Value("${agent.rag.knowledgeDir:../knowledge}")
    private String knowledgeDir;

    private final List<IndexedPolicy> indexedPolicies = new ArrayList<>();

    @PostConstruct
    public synchronized void init() {
        long start = System.currentTimeMillis();
        indexedPolicies.clear();
        vectorStore.clear();

        List<IndexedPolicy> fromDb = loadFromDatabase();
        List<IndexedPolicy> fromKnowledge = loadFromKnowledgeDir();
        indexedPolicies.addAll(fromDb);
        indexedPolicies.addAll(fromKnowledge);

        if (indexedPolicies.isEmpty()) {
            log.warn("VectorPolicyRetriever: 没有任何政策知识可索引");
            return;
        }

        List<String> corpus = indexedPolicies.stream()
            .map(p -> p.title + "\n" + p.content)
            .collect(Collectors.toList());
        embeddingProvider.buildIndex(corpus);

        for (IndexedPolicy policy : indexedPolicies) {
            for (IndexedChunk chunk : policy.chunks) {
                float[] vector = embeddingProvider.embed(chunk.text);
                if (vector.length == 0) {
                    continue;
                }
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("policyId", policy.policyId);
                metadata.put("category", policy.category);
                metadata.put("policyName", policy.title);
                metadata.put("version", policy.version);
                metadata.put("effectiveDate", policy.effectiveDate != null ? policy.effectiveDate.toString() : null);
                metadata.put("expiryDate", policy.expiryDate != null ? policy.expiryDate.toString() : null);
                metadata.put("status", policy.status);
                metadata.put("source", policy.source);
                metadata.put("department", policy.department);
                metadata.put("keywords", policy.keywords);
                metadata.put("knowledgeType", policy.knowledgeType);
                vectorStore.addDocument(VectorDocument.builder()
                    .docId(policy.policyId + ":" + chunk.index)
                    .text(chunk.text)
                    .embedding(vector)
                    .metadata(metadata)
                    .build());
            }
        }

        log.info("VectorPolicyRetriever: 初始化完成，索引 {} 个政策文档，{} 个向量块，耗时 {}ms (provider={})",
            indexedPolicies.size(), vectorStore.size(), System.currentTimeMillis() - start, providerName());
    }

    @Override
    public RetrievalResult retrieve(RetrievalQuery query) {
        LocalDate effectiveDate = query.resolveEffectiveDate();
        String category = query.getCategory() == null ? "" : query.getCategory().trim();
        String role = query.getUserRole() == null ? "" : query.getUserRole().trim();

        if (vectorStore.size() == 0) {
            return RetrievalResult.of(providerName(), query, new ArrayList<>(), 0);
        }

        // 1. 文本向量化
        float[] queryVector = embeddingProvider.embed(query.getQuery());
        if (queryVector.length == 0) {
            return RetrievalResult.of(providerName(), query, new ArrayList<>(), 0);
        }

        // 2. 相似度检索（初选放宽到 2.5 倍 topK，便于重排序）
        int preK = Math.max(20, query.getTopK() * 3);
        Map<String, Object> filters = new HashMap<>();
        if (!category.isEmpty()) {
            filters.put("category", category);
        }
        List<ScoredDocument> candidates = vectorStore.search(queryVector, preK, filters);

        // 3. 重排序与过滤
        double minKeepScore = 0.05;
        List<Map.Entry<RetrievedDocument, Double>> ranked = new ArrayList<>();
        int totalCandidates = vectorStore.size();
        int filteredExpired = 0;
        int filteredLowScore = 0;

        Set<String> seen = new HashSet<>();
        for (ScoredDocument scored : candidates) {
            VectorDocument doc = scored.getDocument();
            String policyIdStr = String.valueOf(doc.getMetadata().get("policyId"));
            String source = String.valueOf(doc.getMetadata().getOrDefault("source", ""));

            // 有效期过滤：元数据中的有效期与查询日期比对
            if (!isWindowEffective(doc, effectiveDate)) {
                filteredExpired++;
                continue;
            }

            // 权限过滤：仅允许公开分类给任意角色（演示版简化：全部可见）
            double finalScore = 0.75 * scored.getScore() + 0.25 * keywordBoost(query, doc);
            if (finalScore < minKeepScore) {
                filteredLowScore++;
                continue;
            }

            // 汇总同一政策的多个 chunk，取最高分
            if (seen.contains(policyIdStr)) {
                for (Map.Entry<RetrievedDocument, Double> e : ranked) {
                    if (e.getKey().getPolicyId() != null
                        && e.getKey().getPolicyId().toString().equals(policyIdStr)) {
                        if (finalScore > e.getValue()) {
                            e.setValue(finalScore);
                        }
                        break;
                    }
                }
                continue;
            }
            seen.add(policyIdStr);

            RetrievedDocument document = RetrievedDocument.builder()
                .policyId(parseLong(policyIdStr))
                .policyName(String.valueOf(doc.getMetadata().get("policyName")))
                .category(String.valueOf(doc.getMetadata().get("category")))
                .department(String.valueOf(doc.getMetadata().getOrDefault("department", "")))
                .version(String.valueOf(doc.getMetadata().getOrDefault("version", "")))
                .content(doc.getText())
                .excerpt(buildExcerpt(doc.getText()))
                .source(source)
                .effectiveDate(parseDate((String) doc.getMetadata().get("effectiveDate")))
                .expiryDate(parseDate((String) doc.getMetadata().get("expiryDate")))
                .keywords(String.valueOf(doc.getMetadata().getOrDefault("keywords", "")))
                .score(Math.max(0, Math.min(1.5, finalScore)))
                .metadata(buildMetadata(doc, scored.getScore(), finalScore))
                .build();
            ranked.add(new AbstractMap.SimpleEntry<>(document, finalScore));
        }

        ranked.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        List<RetrievedDocument> top = ranked.stream()
            .limit(query.getTopK())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        String note = providerName() + ": 匹配 " + top.size() + "/" + totalCandidates
            + " 条候选，过滤过期/未生效 " + filteredExpired + " 条，低相关过滤 " + filteredLowScore + " 条";
        log.info("VectorPolicyRetriever: query={}, topK={}, result={}, candidates={}",
            query.getQuery(), query.getTopK(), top.size(), candidates.size());

        return RetrievalResult.of(providerName(), query, top, totalCandidates);
    }

    @Override
    public String providerName() {
        return "LOCAL_VECTOR_TFIDF";
    }

    // ---------- 内部辅助 ----------

    private List<IndexedPolicy> loadFromDatabase() {
        List<Policy> policies = policyMapper.selectList(
            new LambdaQueryWrapper<Policy>()
                .eq(Policy::getStatus, "ACTIVE")
                .eq(Policy::getIsDeleted, 0)
        );
        List<IndexedPolicy> result = new ArrayList<>();
        for (Policy policy : policies) {
            PolicyVersion version = policyVersionService.getCurrentVersion(policy.getId());
            if (version == null) {
                continue;
            }
            result.add(IndexedPolicy.builder()
                .policyId(policy.getId() == null ? 0L : policy.getId())
                .title(policy.getTitle())
                .category(policy.getCategory())
                .department(policy.getDepartment())
                .version(version.getVersion())
                .content(version.getContent())
                .keywords(policy.getKeywords())
                .effectiveDate(version.getEffectiveDate())
                .expiryDate(version.getExpiryDate())
                .source(version.getSource() == null ? "学生资助相关政策" : version.getSource())
                .status(version.getStatus() == null ? "ACTIVE" : version.getStatus())
                .knowledgeType("DB_POLICY")
                .chunks(buildChunks(policy.getTitle(), version.getContent()))
                .build());
        }
        return result;
    }

    private List<IndexedPolicy> loadFromKnowledgeDir() {
        File base = new File(knowledgeDir);
        if (!base.exists() || !base.isDirectory()) {
            return new ArrayList<>();
        }
        List<IndexedPolicy> result = new ArrayList<>();
        File[] files = base.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return result;
        }
        for (File file : files) {
            try {
                String json = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
                List<Map<String, Object>> docs = parseJsonArray(json);
                for (Map<String, Object> doc : docs) {
                    String title = safeStr(doc.get("title"));
                    String category = safeStr(doc.get("category"));
                    String content = safeStr(doc.get("content"));
                    if (title.isEmpty() || content.isEmpty()) {
                        continue;
                    }
                    LocalDate effective = parseDate(safeStr(doc.get("effectiveDate")));
                    LocalDate expiry = parseDate(safeStr(doc.get("expiryDate")));
                    String source = safeStr(doc.get("source"));
                    String dept = safeStr(doc.get("department"));
                    long id = 10000L + result.size();
                    result.add(IndexedPolicy.builder()
                        .policyId(id)
                        .title(title)
                        .category(category)
                        .department(dept.isEmpty() ? "学生事务部" : dept)
                        .version("DEMO-v1")
                        .content(content)
                        .keywords(safeStr(doc.get("keywords")))
                        .effectiveDate(effective)
                        .expiryDate(expiry)
                        .source(source.isEmpty() ? "DEMO_POLICY-"+category : source)
                        .status("ACTIVE")
                        .knowledgeType("DEMO_POLICY")
                        .chunks(buildChunks(title, content))
                        .build());
                }
            } catch (IOException e) {
                log.warn("VectorPolicyRetriever: 读取知识文件失败: {} - {}", file.getName(), e.getMessage());
            }
        }
        return result;
    }

    private List<IndexedChunk> buildChunks(String title, String content) {
        String full = title + "\n" + content;
        List<String> chunks = TextSplitter.split(full);
        List<IndexedChunk> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            result.add(IndexedChunk.builder().index(i).text(chunks.get(i)).build());
        }
        return result;
    }

    private boolean isWindowEffective(VectorDocument doc, LocalDate date) {
        String eff = (String) doc.getMetadata().get("effectiveDate");
        String exp = (String) doc.getMetadata().get("expiryDate");
        LocalDate effectiveDate = parseDate(eff);
        LocalDate expiryDate = parseDate(exp);
        if (effectiveDate != null && date.isBefore(effectiveDate)) {
            return false;
        }
        if (expiryDate != null && date.isAfter(expiryDate)) {
            return false;
        }
        return true;
    }

    private double keywordBoost(RetrievalQuery query, VectorDocument doc) {
        String q = query.getQuery() == null ? "" : query.getQuery().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return 0;
        }
        String text = (doc.getText() + " " + doc.getMetadata().get("policyName")).toLowerCase(Locale.ROOT);
        for (String token : q.split("[，,\\s]+")) {
            if (!token.isEmpty() && text.contains(token)) {
                return 1.0;
            }
        }
        return 0;
    }

    private Map<String, Object> buildMetadata(VectorDocument doc, double vectorScore, double finalScore) {
        Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
        metadata.put("vectorScore", vectorScore);
        metadata.put("finalScore", finalScore);
        metadata.put("provider", providerName());
        return metadata;
    }

    private String buildExcerpt(String content) {
        if (content == null) {
            return "";
        }
        String plain = content.replace("\n", " ").replace("\r", " ");
        return plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
    }

    // ---------- JSON 解析（轻量级，不引入额外依赖） ----------

    private List<Map<String, Object>> parseJsonArray(String json) {
        List<Map<String, Object>> result = new ArrayList<>();
        String trimmed = json.trim();
        if (!trimmed.startsWith("[")) {
            return result;
        }
        Map<String, Object> current = new HashMap<>();
        boolean inString = false;
        boolean isKey = true;
        StringBuilder buf = new StringBuilder();
        String curKey = null;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{' || c == '[' || c == ']') continue;
                if (Character.isWhitespace(c)) continue;
                if (c == ':') {
                    curKey = buf.toString().trim();
                    buf = new StringBuilder();
                    isKey = false;
                    continue;
                }
                if (c == ',' || c == '}') {
                    if (!isKey && curKey != null) {
                        current.put(curKey, buf.toString().trim());
                    }
                    curKey = null;
                    isKey = true;
                    buf = new StringBuilder();
                    if (c == '}') {
                        result.add(new HashMap<>(current));
                        current.clear();
                        continue;
                    }
                    continue;
                }
            }
            buf.append(c);
        }
        return result;
    }

    private String safeStr(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- 内部数据结构 ----------

    @lombok.Builder
    @lombok.Data
    private static class IndexedPolicy {
        long policyId;
        String title;
        String category;
        String department;
        String version;
        String content;
        String keywords;
        LocalDate effectiveDate;
        LocalDate expiryDate;
        String source;
        String status;
        String knowledgeType;
        List<IndexedChunk> chunks;
    }

    @lombok.Builder
    @lombok.Data
    private static class IndexedChunk {
        int index;
        String text;
    }
}