package com.campuspilot.rag.embedding;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * FastGPT 嵌入提供者（环境待接入）
 * 当 FastGPT API 未配置时，返回 NOT_CONFIGURED，绝不伪造向量。
 */
@Component
public class FastGPTEmbeddingProvider implements EmbeddingProvider {

    private static final int WAITING_DIMENSION = 1536;

    @Override
    public float[] embed(String text) {
        throw new UnsupportedOperationException(
            "FastGPT 嵌入服务尚未配置（WAITING_FOR_FASTGPT_ENVIRONMENT），无法生成真实向量。");
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        throw new UnsupportedOperationException(
            "FastGPT 嵌入服务尚未配置（WAITING_FOR_FASTGPT_ENVIRONMENT），无法生成真实向量。");
    }

    @Override
    public int dimension() {
        return WAITING_DIMENSION;
    }

    @Override
    public String providerName() {
        return "FASTGPT_EMBEDDING";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
