package com.campuspilot.common.exception;

public class ForbiddenException extends BusinessException {
    
    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message);
    }
    
    public ForbiddenException() {
        super(403, "FORBIDDEN", "权限不足");
    }
}