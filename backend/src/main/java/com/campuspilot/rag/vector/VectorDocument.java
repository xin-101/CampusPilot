package com.campuspilot.rag.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 向量文档：存储嵌入向量及其元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument {

    /** 文档唯一标识 */
    private String docId;

    /** 原始文本内容 */
    private String text;

    /** 嵌入向量 */
    private float[] embedding;

    /** 元数据（用于过滤：policyId, category, effectiveDate, expiryDate 等） */
    private Map<String, Object> metadata;
}
