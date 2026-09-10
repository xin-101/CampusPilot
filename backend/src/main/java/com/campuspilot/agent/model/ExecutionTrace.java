package com.campuspilot.agent.model;

import com.campuspilot.rag.RetrievalResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrace {
    
    private String executionId;
    private String sessionId;
    private Long userId;
    private String intent;
    private String workflow;
    private String workflowId;
    private String workflowName;
    private List<ExecutionStep> steps;
    private List<ToolCall> toolCalls;
    private List<Citation> citations;
    private Map<String, Object> decisions;
    private RetrievalResult retrieval;
    private long startTime;
    private long endTime;
    private long duration;
    private String status;
    private String error;
}