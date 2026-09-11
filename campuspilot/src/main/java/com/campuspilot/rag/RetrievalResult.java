package com.campuspilot.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG检索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /** 检索提供方: LOCAL_KEYWORD / FASTGPT */
    private String provider;

    /** 检索查询 */
    private RetrievalQuery query;

    /** 命中文档 */
    private List<RetrievedDocument> documents;

    /** 命中总数(过滤前) */
    private int totalCount;

    /** 说明(如过滤过期政策数量、未配置等信息) */
    private String note;

    public static RetrievalResult of(String provider, RetrievalQuery query, List<RetrievedDocument> documents, int totalCount) {
        return RetrievalResult.builder()
            .provider(provider)
            .query(query)
            .documents(documents != null ? documents : new ArrayList<>())
            .totalCount(totalCount)
            .build();
    }

    public boolean isEmpty() {
        return documents == null || documents.isEmpty();
    }
}