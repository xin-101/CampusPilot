package com.campuspilot.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Policy;
import com.campuspilot.entity.PolicyVersion;
import com.campuspilot.mapper.PolicyMapper;
import com.campuspilot.mapper.PolicyVersionMapper;
import com.campuspilot.policy.PolicyVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 本地关键词检索器(默认RAG提供方)
 * 检索范围：状态为ACTIVE且未删除的政策 + 在查询日期处于生效期的政策版本。
 * 评分：关键词命中标题/政策关键词/版本关键词/描述/正文，结合分类匹配加分。
 * 过期/未生效政策被过滤，不返回给Agent。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeywordPolicyRetriever implements PolicyRetriever {

    private final PolicyMapper policyMapper;
    private final PolicyVersionMapper versionMapper;
    private final PolicyVersionService policyVersionService;

    @Override
    public RetrievalResult retrieve(RetrievalQuery query) {
        LocalDate effectiveDate = query.resolveEffectiveDate();

        List<Policy> policies = policyMapper.selectList(
            new LambdaQueryWrapper<Policy>()
                .eq(Policy::getStatus, "ACTIVE")
                .eq(Policy::getIsDeleted, 0)
        );

        List<RetrievedDocument> candidates = new ArrayList<>();
        int totalCandidates = 0;
        int filteredExpired = 0;

        for (Policy policy : policies) {
            PolicyVersion version = policyVersionService.getCurrentVersion(policy.getId());
            // 关键过滤：过期/未生效政策直接剔除
            if (version == null || !policyVersionService.isEffective(version, effectiveDate)) {
                filteredExpired++;
                continue;
            }
            totalCandidates++;
            double score = scoreDocument(query, policy, version);
            if (score <= 0) {
                // 仍有极小匹配余地的保留低分，避免完全无结果显示；否则跳过
                continue;
            }
            candidates.add(toDocument(policy, version, score, query));
        }

        // 按相关度降序，取topK
        List<RetrievedDocument> ranked = candidates.stream()
            .sorted(Comparator.comparingDouble(RetrievedDocument::getScore).reversed())
            .limit(query.getTopK())
            .collect(Collectors.toList());

        String note = "LOCAL_KEYWORD: 匹配 " + ranked.size() + "/" + totalCandidates
            + " 条，过滤过期/未生效政策 " + filteredExpired + " 条";
        log.info("KeywordPolicyRetriever: query={}, topK={}, result={}, filteredExpired={}",
            query.getQuery(), query.getTopK(), ranked.size(), filteredExpired);

        return RetrievalResult.of("LOCAL_KEYWORD", query, ranked, totalCandidates);
    }

    /**
     * 关键评分函数（标题/关键词/描述/正文命中 + 分类加分），分值 0~1.2 区间，取前topK
     */
    private double scoreDocument(RetrievalQuery query, Policy policy, PolicyVersion version) {
        String rawQuery = query.getQuery() == null ? "" : query.getQuery().trim();
        String category = query.getCategory() == null ? "" : query.getCategory().trim();

        List<String> terms = tokenize(rawQuery);
        if (terms.isEmpty()) {
            // 无关键词时：仅按分类/状态返回
            if (!category.isEmpty() && category.equalsIgnoreCase(policy.getCategory())) {
                return 0.5;
            }
            return 0.3;
        }

        String title = safe(policy.getTitle());
        String policyKeywords = safe(policy.getKeywords());
        String versionKeywords = safe(version.getKeywords());
        String description = safe(policy.getDescription());
        String content = safe(version.getContent());

        double maxTermScore = 0;
        for (String term : terms) {
            double termScore = 0;
            if (title.equalsIgnoreCase(term)) {
                termScore = 1.0;
            } else if (contains(title, term)) {
                termScore = 0.9;
            } else if (contains(policyKeywords, term) || contains(versionKeywords, term)) {
                termScore = 0.7;
            } else if (contains(content, term)) {
                termScore = 0.6;
            } else if (contains(description, term)) {
                termScore = 0.5;
            }
            maxTermScore = Math.max(maxTermScore, termScore);
        }

        double score = maxTermScore;
        if (!category.isEmpty() && category.equalsIgnoreCase(policy.getCategory())) {
            score += 0.3;
        }
        return Math.min(score, 1.5);
    }

    private RetrievedDocument toDocument(Policy policy, PolicyVersion version, double score, RetrievalQuery query) {
        return RetrievedDocument.builder()
            .policyId(policy.getId())
            .policyName(policy.getTitle())
            .category(policy.getCategory())
            .department(policy.getDepartment())
            .version(version.getVersion())
            .content(version.getContent())
            .excerpt(buildExcerpt(version.getContent()))
            .source(version.getSource())
            .effectiveDate(version.getEffectiveDate())
            .expiryDate(version.getExpiryDate())
            .keywords(isBlank(version.getKeywords()) ? policy.getKeywords() : version.getKeywords())
            .score(score)
            .metadata(metadataOf(policy, version))
            .build();
    }

    private Map<String, Object> metadataOf(Policy policy, PolicyVersion version) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("policyId", policy.getId());
        metadata.put("category", policy.getCategory());
        metadata.put("version", version.getVersion());
        metadata.put("effectiveDate", version.getEffectiveDate() != null ? version.getEffectiveDate().toString() : null);
        metadata.put("expiryDate", version.getExpiryDate() != null ? version.getExpiryDate().toString() : null);
        metadata.put("active", "ACTIVE".equals(version.getStatus()));
        return metadata;
    }

    private String buildExcerpt(String content) {
        if (isBlank(content)) {
            return "";
        }
        String plain = content.replace("\n", " ").replace("\r", " ");
        return plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
    }

    private List<String> tokenize(String query) {
        List<String> terms = new ArrayList<>();
        if (isBlank(query)) {
            return terms;
        }
        for (String t : query.split("[,，\\s]+")) {
            String term = t.trim();
            if (!term.isEmpty()) {
                terms.add(term.toLowerCase(Locale.ROOT));
            }
        }
        return terms;
    }

    private boolean contains(String text, String term) {
        return !isBlank(text) && text.toLowerCase(Locale.ROOT).contains(term);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public String providerName() {
        return "LOCAL_KEYWORD";
    }
}