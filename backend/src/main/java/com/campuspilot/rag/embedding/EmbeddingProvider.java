package com.campuspilot.rag.embedding;

import java.util.List;

/**
 * 文本向量化提供者抽象接口
 * 将自然语言文本转换为稠密向量，用于语义相似度检索。
 *
 * 实现：
 * - {@link LocalEmbeddingProvider}：本地 TF-IDF/字符 n-gram 向量化(无需外部服务)
 * - {@link FastGPTEmbeddingProvider}：FastGPT 嵌入接口(待环境就绪)
 */
public interface EmbeddingProvider {

    /**
     * 将单条文本转换为向量
     * @param text 输入文本
     * @return 浮点向量（维度由实现决定）
     */
    float[] embed(String text);

    /**
     * 批量向量化
     * @param texts 输入文本列表
     * @return 对应向量列表，顺序与输入一致
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 向量维度
     */
    int dimension();

    /**
     * 提供者名称
     */
    String providerName();

    /**
     * 是否可用（真实服务已连接）
     */
    boolean isAvailable();
}
