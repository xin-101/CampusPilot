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
        log.info("Agent chat request: sessionId={}, userId={}, message={}", 
            request.getSessionId(), request.getUserId(), 
            request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "...");
        
        try {
            AgentResponse response = agentProvider.chat(request);
            log.info("Agent chat response: sessionId={}, intent={}, complexity={}", 
                response.getSessionId(), response.getIntent(), response.getComplexity());
            return response;
        } catch (Exception e) {
            log.error("Agent chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Agent调用失败: " + e.getMessage());
        }
    }
}