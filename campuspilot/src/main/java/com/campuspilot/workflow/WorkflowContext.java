package com.campuspilot.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow执行上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext {

    private String sessionId;
    private Long userId;
    private String username;
    private String role;
    private String message;
    private String intent;
    private String complexity;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }
}