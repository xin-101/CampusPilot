package com.campuspilot.agent.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * OpenAI-compatible Chat Completions HTTP 客户端。
 * <p>
 * 支持：OpenAI / DeepSeek / Qwen / 任何兼容接口。
 * 仅需 base-url + api-key + model 即可切换。
 * <p>
 * 不依赖 Spring AI / LangChain4j / 任何外部 Agent 框架。
 */
@Component
@Slf4j
public class LLMGateway {

    private final LLMProperties properties;
    private final RestTemplate restTemplate;

    public LLMGateway(LLMProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 调用 Chat Completions API。
     *
     * @param messages 对话消息列表
     * @return LLM 响应（含 choices / usage / error）
     * @throws LLMGatewayException 网络/解析/认证异常
     */
    public LLMResponse chatCompletions(List<LLMRequest.Message> messages) {
        String url = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";

        LLMRequest body = LLMRequest.builder()
            .model(properties.getModel())
            .messages(messages)
            .temperature(properties.getTemperature())
            .maxTokensAlias(2048)
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        HttpEntity<LLMRequest> entity = new HttpEntity<>(body, headers);

        log.info("LLM request: url={}, model={}, messages={}", url, properties.getModel(), messages.size());

        try {
            LLMResponse response = restTemplate.postForObject(url, entity, LLMResponse.class);

            if (response == null) {
                throw new LLMGatewayException("LLM 返回空响应");
            }

            if (response.getError() != null) {
                throw new LLMGatewayException("LLM API 错误: " + response.getError().getMessage());
            }

            String content = response.getAssistantContent();
            log.info("LLM response: model={}, tokens={}, contentLen={}",
                response.getModel(),
                response.getUsage() != null ? response.getUsage().getTotalTokens() : "?",
                content != null ? content.length() : 0);

            return response;

        } catch (ResourceAccessException e) {
            log.error("LLM 连接超时或不可达: url={}", url, e);
            throw new LLMGatewayException("LLM 服务连接超时或不可达: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("LLM HTTP 错误: url={}", url, e);
            throw new LLMGatewayException("LLM HTTP 错误: " + e.getMessage());
        }
    }

    /**
     * 快捷方法：发送单条用户消息并获取回复文本。
     */
    public String chat(String systemPrompt, String userMessage) {
        List<LLMRequest.Message> messages = List.of(
            LLMRequest.Message.builder().role("system").content(systemPrompt).build(),
            LLMRequest.Message.builder().role("user").content(userMessage).build()
        );
        LLMResponse response = chatCompletions(messages);
        return response.getAssistantContent();
    }
}
