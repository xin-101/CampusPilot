package com.campuspilot.rag;

import com.campuspilot.agent.model.Citation;

import java.util.List;

/**
 * RAG 服务接口
 * 上层(Workflow/Agent)统一通过本服务获取政策知识，屏蔽具体提供方(本地/FastGPT)差异。
 */
public interface RAGService {

    /**
     * 执行知识库检索
     */
    RetrievalResult retrieve(RetrievalQuery query);

    /**
     * 将检索结果转换为引用来源(用于Agent响应)
     */
    List<Citation> toCitations(RetrievalResult result);

    /**
     * 当前检索提供方名称
     */
    String getProviderName();

    /**
     * 检索器接入状态(如 LOCAL_KEYWORD / WAITING_FOR_FASTGPT_ENVIRONMENT)
     */
    String getStatus();
}