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
public class ExecutionTrace {
    
    private String executionId;
    private String sessionId;
    private Long userId;
    private String intent;
    private String workflow;
    private List<ExecutionStep> steps;
    private long startTime;
    private long endTime;
    private long duration;
    private String status;
    private String error;
}