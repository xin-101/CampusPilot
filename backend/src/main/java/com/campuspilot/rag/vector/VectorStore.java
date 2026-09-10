package com.campuspilot.rag.vector;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口：存储向量文档，支持相似度检索与元数据过滤
 */
public interface VectorStore {

    /**
     * 添加文档到向量库
     */
    void addDocument(VectorDocument document);

    /**
     * 批量添加文档
     */
    void addDocuments(List<VectorDocument> documents);

    /**
     * 相似度检索
     * @param queryEmbedding 查询向量
     * @param topK 返回条数
     * @param filters 元数据过滤条件（key=value 匹配）
     * @return 按相似度降序排列的文档
     */
    List<ScoredDocument> search(float[] queryEmbedding, int topK, Map<String, Object> filters);

    /**
     * 向量库中的文档总数
     */
    int size();

    /**
     * 清空向量库
     */
    void clear();
}
