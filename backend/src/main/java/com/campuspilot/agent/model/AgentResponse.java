package com.campuspilot.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    
    private String sessionId;
    private String messageId;
    private String response;
    private String intent;
    private String complexity;
    private List<Citation> sources;
    private List<ExecutionStep> executionSteps;
    private List<ToolCall> toolCalls;
    private ExecutionTrace executionTrace;
}