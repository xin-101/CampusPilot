package com.campuspilot.workflow;

import com.campuspilot.agent.model.Citation;
import com.campuspilot.agent.model.ExecutionStep;
import com.campuspilot.agent.model.ToolCall;
import com.campuspilot.eligibility.EligibilityResult;
import com.campuspilot.rag.RetrievalResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Workflow执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult {

    /** 执行状态: SUCCESS/FAILED/SKIPPED */
    private String status;

    /** 工作流ID */
    private String workflowId;

    /** 工作流名称 */
    private String workflowName;

    /** 最终回答(Agent响应文本) */
    private String message;

    /** 引用来源 */
    private List<Citation> citations;

    /** 工具调用记录 */
    private List<ToolCall> toolCalls;

    /** 执行步骤 */
    private List<ExecutionStep> steps;

    /** 决策信息(路由/判断等) */
    private Map<String, Object> decisions;

    /** RAG检索结果 */
    private RetrievalResult retrieval;

    /** 资格判断结果(如适用) */
    private EligibilityResult eligibility;

    /** 附加数据(如创建的任务ID) */
    private Map<String, Object> data;

    private String error;

    public static WorkflowResult failed(String workflowId, String message, String error) {
        return WorkflowResult.builder()
            .status("FAILED")
            .workflowId(workflowId)
            .message(message)
            .error(error)
            .steps(new ArrayList<>())
            .citations(new ArrayList<>())
            .toolCalls(new ArrayList<>())
            .build();
    }
}