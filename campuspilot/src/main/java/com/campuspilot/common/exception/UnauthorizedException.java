package com.campuspilot.common.exception;

public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message);
    }
    
    public UnauthorizedException() {
        super(401, "UNAUTHORIZED", "未授权");
    }
}