package com.campuspilot.agent.service;

import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;
import com.campuspilot.agent.provider.AgentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {
    
    private final AgentProvider agentProvider;
    
    @Override
    public AgentResponse chat(AgentRequest request) {
        String msg = request.getMessage();
        log.info("Agent chat request: sessionId={}, userId={}, messageLen={}, head={}",
            request.getSessionId(), request.getUserId(), msg == null ? 0 : msg.length(),
            msg == null ? "" : msg.substring(0, Math.min(50, msg.length())));

        try {
            AgentResponse response = agentProvider.chat(request);
            log.info("Agent chat response: sessionId={}, intent={}, complexity={}",
                response.getSessionId(), response.getIntent(), response.getComplexity());
            return response;
        } catch (Exception e) {
            log.error("Agent chat failed: sessionId={}, cause={}", request.getSessionId(), e.getMessage(), e);
            throw new com.campuspilot.common.exception.BusinessException(500, "智能体服务暂不可用，请稍后重试。");
        }
    }
}