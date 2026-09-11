package com.campuspilot.rag;

/**
 * 政策检索器抽象接口
 * 实现包括：
 * - {@link KeywordPolicyRetriever}：本地关键词检索(默认)
 * - {@link FastGptPolicyRetriever}：FastGPT 知识库检索(待联调环境)
 */
public interface PolicyRetriever {

    /**
     * 检索政策知识库
     * @param query 检索参数(含关键词/分类/有效日期/权限角色)
     * @return 结构化检索结果
     */
    RetrievalResult retrieve(RetrievalQuery query);

    /**
     * 检索器名称
     */
    String providerName();
}