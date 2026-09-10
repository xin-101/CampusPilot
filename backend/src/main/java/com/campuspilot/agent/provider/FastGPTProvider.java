package com.campuspilot.agent.provider;

import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@Slf4j
public class FastGPTProvider implements AgentProvider {
    
    @Value("${agent.fastgpt.url}")
    private String baseUrl;
    
    @Value("${agent.fastgpt.apiKey}")
    private String apiKey;
    
    @Override
    public AgentResponse chat(AgentRequest request) {
        log.info("FastGPT Agent收到消息: {}", request.getMessage());
        
        // TODO: 实现真实的FastGPT API调用
        // 1. 构建FastGPT请求
        // 2. 调用FastGPT Chat API
        // 3. 解析响应
        // 4. 返回AgentResponse
        
        throw new UnsupportedOperationException("FastGPT Provider暂未实现，请使用Mock Provider");
    }
    
    @Override
    public boolean healthCheck() {
        try {
            // TODO: 调用FastGPT健康检查接口
            return true;
        } catch (Exception e) {
            log.error("FastGPT健康检查失败", e);
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return "FastGPT";
    }
}