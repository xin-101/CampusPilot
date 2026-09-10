package com.campuspilot.agent.service;

import com.campuspilot.agent.llm.LLMProperties;
import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;
import com.campuspilot.agent.provider.AgentProvider;
import com.campuspilot.agent.provider.MockAgentProvider;
import com.campuspilot.agent.provider.RealLLMAgentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {
    
    private final MockAgentProvider mockProvider;
    private final Optional<RealLLMAgentProvider> llmProvider;
    private final LLMProperties llmProperties;
    
    public AgentServiceImpl(MockAgentProvider mockProvider,
                            Optional<RealLLMAgentProvider> llmProvider,
                            LLMProperties llmProperties) {
        this.mockProvider = mockProvider;
        this.llmProvider = llmProvider;
        this.llmProperties = llmProperties;
    }
    
    @Override
    public AgentResponse chat(AgentRequest request) {
        String msg = request.getMessage();
        log.info("Agent chat request: sessionId={}, userId={}, messageLen={}, head={}",
            request.getSessionId(), request.getUserId(), msg == null ? 0 : msg.length(),
            msg == null ? "" : msg.substring(0, Math.min(50, msg.length())));

        try {
            AgentProvider provider = selectProvider();
            log.info("Using provider: {}", provider.getProviderName());
            AgentResponse response = provider.chat(request);
            log.info("Agent chat response: sessionId={}, intent={}, complexity={}",
                response.getSessionId(), response.getIntent(), response.getComplexity());
            return response;
        } catch (Exception e) {
            log.error("Agent chat failed: sessionId={}, cause={}", request.getSessionId(), e.getMessage(), e);
            throw new com.campuspilot.common.exception.BusinessException(500, "智能体服务暂不可用，请稍后重试。");
        }
    }
    
    /**
     * 运行时选择 Provider：
     * - LLM 已启用且配置完整 → RealLLMAgentProvider
     * - 否则 → MockAgentProvider
     */
    private AgentProvider selectProvider() {
        if (llmProperties.isReallyConfigured() && llmProvider.isPresent()) {
            return llmProvider.get();
        }
        return mockProvider;
    }
}