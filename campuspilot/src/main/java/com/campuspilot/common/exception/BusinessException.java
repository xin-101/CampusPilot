package com.campuspilot.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }
}