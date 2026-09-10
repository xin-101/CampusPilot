package com.campuspilot.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * RAG检索命中文档
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedDocument {

    private Long policyId;

    private String policyName;

    private String category;

    private String department;

    /** 版本号 */
    private String version;

    /** 政策正文 */
    private String content;

    /** 摘要 */
    private String excerpt;

    /** 来源 */
    private String source;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    /** 本体关键词(逗号分隔) */
    private String keywords;

    /** 相关度分数(0-1，越高越相关) */
    private double score;

    /** 附加元数据 */
    private Map<String, Object> metadata;
}