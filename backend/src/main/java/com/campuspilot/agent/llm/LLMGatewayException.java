package com.campuspilot.agent.llm;

/**
 * LLM 网关异常。不会暴露给前端，仅用于内部错误处理。
 */
public class LLMGatewayException extends RuntimeException {

    public LLMGatewayException(String message) {
        super(message);
    }

    public LLMGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
