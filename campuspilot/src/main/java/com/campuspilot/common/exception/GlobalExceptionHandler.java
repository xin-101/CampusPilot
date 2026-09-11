package com.campuspilot.common.exception;

import com.campuspilot.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        String requestId = UUID.randomUUID().toString();
        log.error("Business exception: {}, requestId: {}", e.getMessage(), requestId);
        
        return ResponseEntity.status(e.getCode())
            .body(ApiResponse.<Void>builder()
                .success(false)
                .code(e.getCode())
                .message(e.getMessage())
                .requestId(requestId)
                .build());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String requestId = UUID.randomUUID().toString();
        Map<String, String> errors = new HashMap<>();
        
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation exception: {}, requestId: {}", errors, requestId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .code(400)
                .message("参数校验失败")
                .requestId(requestId)
                .build());
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        String requestId = UUID.randomUUID().toString();
        log.error("Bad credentials exception: {}, requestId: {}", e.getMessage(), requestId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .code(401)
                .message("用户名或密码错误")
                .requestId(requestId)
                .build());
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        String requestId = UUID.randomUUID().toString();
        log.error("Access denied exception: {}, requestId: {}", e.getMessage(), requestId);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .code(403)
                .message("权限不足")
                .requestId(requestId)
                .build());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        String requestId = UUID.randomUUID().toString();
        log.error("Internal server error: {}, requestId: {}", e.getMessage(), requestId, e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .code(500)
                .message("系统内部错误")
                .requestId(requestId)
                .build());
    }
}