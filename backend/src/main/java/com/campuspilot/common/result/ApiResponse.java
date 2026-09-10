package com.campuspilot.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private int code;
    private String message;
    private T data;
    private String requestId;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .code(200)
            .message("success")
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .code(200)
            .message(message)
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(code)
            .message(message)
            .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(500)
            .message(message)
            .build();
    }
}