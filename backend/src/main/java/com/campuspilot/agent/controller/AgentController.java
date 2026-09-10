package com.campuspilot.agent.controller;

import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;
import com.campuspilot.agent.service.AgentService;
import com.campuspilot.common.result.ApiResponse;
import com.campuspilot.security.SecurityUtils;
import com.campuspilot.student.StudentService;
import com.campuspilot.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    private final StudentService studentService;
    
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
        Long userId = null;
        if ("ROLE_STUDENT".equals(role) || "STUDENT".equals(role)) {
            StudentVO student = studentService.getCurrentStudent();
            if (student != null) {
                userId = student.getUserId();
            }
        }
        
        AgentRequest agentRequest = AgentRequest.builder()
            .sessionId(sessionId)
            .message(message)
            .username(username)
            .role(role)
            .userId(userId)
            .build();
        
        AgentResponse response = agentService.chat(agentRequest);
        return ApiResponse.success(response);
    }
}