package com.campuspilot.agent.controller;

import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;
import com.campuspilot.agent.service.AgentService;
import com.campuspilot.common.result.ApiResponse;
import com.campuspilot.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    
    @PostMapping("/chat")
    public ApiResponse<AgentResponse> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.get("sessionId");
        
        if (message == null || message.isEmpty()) {
            return ApiResponse.error(400, "消息不能为空");
        }
        
        // 获取当前用户信息
        String username = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.getCurrentRole();
        
        AgentRequest agentRequest = AgentRequest.builder()
            .sessionId(sessionId)
            .message(message)
            .username(username)
            .role(role)
            .build();
        
        AgentResponse response = agentService.chat(agentRequest);
        return ApiResponse.success(response);
    }
}