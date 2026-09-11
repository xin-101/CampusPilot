package com.campuspilot.agent.provider;

import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;

public interface AgentProvider {
    
    /**
     * 发送消息到Agent
     * @param request Agent请求
     * @return Agent响应
     */
    AgentResponse chat(AgentRequest request);
    
    /**
     * 健康检查
     * @return 是否健康
     */
    boolean healthCheck();
    
    /**
     * 获取Provider名称
     * @return Provider名称
     */
    String getProviderName();
}