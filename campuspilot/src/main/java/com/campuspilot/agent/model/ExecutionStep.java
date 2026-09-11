package com.campuspilot.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStep {
    
    private int step;
    private String type;
    private String name;
    private String status;
    private long duration;
    private Object input;
    private Object output;
    private String error;
}