package com.campuspilot.agent.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible Chat Completions 响应体。
 */
@Data
public class LLMResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private Error error;

    @Data
    public static class Choice {
        private int index;
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    @Data
    public static class Error {
        private String message;
        private String type;
        private String code;
    }

    /**
     * 提取助手回复文本。若解析异常返回 null。
     */
    public String getAssistantContent() {
        if (choices == null || choices.isEmpty()) return null;
        Choice first = choices.get(0);
        if (first == null || first.getMessage() == null) return null;
        return first.getMessage().getContent();
    }
}
