package com.campuspilot.rag.vector;

import com.campuspilot.rag.embedding.LocalEmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内存向量存储实现（轻量级，无外部依赖）
 * 采用线性扫描 + 余弦相似度 + 元数据过滤。
 * 适用于演示与小型政策库（条目数量有限）。
 */
@Component
@Slf4j
public class InMemoryVectorStore implements VectorStore {

    private final List<VectorDocument> documents = new ArrayList<>();

    @Override
    public synchronized void addDocument(VectorDocument document) {
        if (document == null || document.getEmbedding() == null) {
            return;
        }
        documents.removeIf(d -> d.getDocId() != null && d.getDocId().equals(document.getDocId()));
        documents.add(document);
    }

    @Override
    public synchronized void addDocuments(List<VectorDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        for (VectorDocument doc : docs) {
            addDocument(doc);
        }
    }

    @Override
    public synchronized List<ScoredDocument> search(float[] queryEmbedding, int topK, Map<String, Object> filters) {
        if (queryEmbedding == null || topK <= 0) {
            return new ArrayList<>();
        }
        List<ScoredDocument> scored = new ArrayList<>();
        for (VectorDocument doc : documents) {
            if (!matchesFilters(doc, filters)) {
                continue;
            }
            double score = LocalEmbeddingProvider.cosineSimilarity(queryEmbedding, doc.getEmbedding());
            scored.add(ScoredDocument.builder().document(doc).score(score).build());
        }
        return scored.stream()
            .sorted(Comparator.comparingDouble(ScoredDocument::getScore).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized int size() {
        return documents.size();
    }

    @Override
    public synchronized void clear() {
        documents.clear();
    }

    private boolean matchesFilters(VectorDocument doc, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty() || doc.getMetadata() == null) {
            return true;
        }
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            Object docValue = doc.getMetadata().get(entry.getKey());
            if (docValue == null) {
                // 显式空值过滤匹配 null
                if (entry.getValue() != null) {
                    return false;
                }
                continue;
            }
            if (!docValue.toString().equals(String.valueOf(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }
}