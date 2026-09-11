package com.campuspilot.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    
    private boolean success;
    private Object data;
    private String message;
    private String code;
    
    public static ToolResult success(Object data) {
        return ToolResult.builder()
            .success(true)
            .data(data)
            .message("success")
            .build();
    }
    
    public static ToolResult error(String message, String code) {
        return ToolResult.builder()
            .success(false)
            .message(message)
            .code(code)
            .build();
    }
}