package com.campuspilot.agent.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置属性。
 * 只要 base-url 和 api-key 已配置，LLM 模式即可启用。
 * 所有值均通过环境变量注入，绝不写入代码或 Git。
 */
@Data
@Component
@ConfigurationProperties(prefix = "campuspilot.llm")
public class LLMProperties {

    /** 是否启用 LLM Provider（需要 base-url + api-key 同时存在） */
    private boolean enabled = false;

    /** OpenAI-compatible Chat Completions API 地址（如 https://api.openai.com/v1） */
    private String baseUrl = "";

    /** API Key（从环境变量 LLM_API_KEY 注入） */
    private String apiKey = "";

    /** 模型名称（如 gpt-4o、deepseek-chat、qwen-turbo） */
    private String model = "gpt-4o";

    /** 生成温度 */
    private double temperature = 0.2;

    /** HTTP 超时（毫秒） */
    private int timeoutMs = 30000;

    /** LLM 失败时是否回退到 Mock（默认 false，不允许静默切换） */
    private boolean fallbackToMock = false;

    /**
     * 判断 LLM 是否真正可用（enabled + baseUrl + apiKey 全部非空）。
     */
    public boolean isReallyConfigured() {
        return enabled
            && baseUrl != null && !baseUrl.isBlank()
            && apiKey != null && !apiKey.isBlank();
    }
}
