package com.campuspilot.agent.model;

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

    /** 附加数据(如创建的任务ID/是否创建成功) */
    private Map<String, Object> data;
}