package com.campuspilot.agent.fastgpt;

/**
 * FastGPT集成异常
 */
public class FastGPTException extends RuntimeException {

    public FastGPTException(String message) {
        super(message);
    }

    public FastGPTException(String message, Throwable cause) {
        super(message, cause);
    }
}