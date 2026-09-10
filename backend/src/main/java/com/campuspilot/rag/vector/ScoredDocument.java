package com.campuspilot.rag.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 带相似度分数的向量文档（检索结果）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredDocument {

    private VectorDocument document;

    /** 余弦相似度分数 (-1 ~ 1，越高越相似) */
    private double score;
}
