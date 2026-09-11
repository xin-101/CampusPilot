package com.campuspilot.workflow;

import com.campuspilot.agent.model.ExecutionStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow执行引擎
 * 确定性路由：
 * - TASK_CREATE      -> taskCreation
 * - ELIGIBILITY_CHECK -> eligibilityCheck (COMPLEX)
 * - POLICY_QUERY     -> policyConsultation (SIMPLE/COMPLEX)
 * - UNKNOWN/其他     -> 无匹配，由上层回退处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowRegistry registry;

    /**
     * 根据意图路由到对应工作流并执行
     * @return 匹配则执行并返回结果；无匹配返回 null
     */
    public WorkflowResult routeAndExecute(WorkflowContext context) {
        AgentWorkflow workflow = registry.getByIntent(context.getIntent());
        if (workflow == null) {
            log.info("WorkflowEngine: no workflow for intent={}", context.getIntent());
            return null;
        }

        log.info("WorkflowEngine: routing intent={} -> workflow={}",
            context.getIntent(), workflow.id());

        List<ExecutionStep> rootSteps = new ArrayList<>();
        rootSteps.add(ExecutionStep.builder()
            .step(1)
            .type("ROUTER")
            .name("工作流路由")
            .status("SUCCESS")
            .duration(0)
            .output(java.util.Map.of("workflowId", workflow.id(), "workflowName", workflow.name()))
            .build());

        try {
            WorkflowResult result = workflow.execute(context);
            if (result.getSteps() == null) {
                result.setSteps(new ArrayList<>());
            }
            // 将路由步骤置于最前
            List<ExecutionStep> all = new ArrayList<>(rootSteps);
            all.addAll(result.getSteps());
            result.setSteps(all);
            return result;
        } catch (Exception e) {
            log.error("Workflow执行异常: workflow={}", workflow.id(), e);
            WorkflowResult failed = WorkflowResult.failed(workflow.id(), "工作流执行失败，请稍后重试", e.getMessage());
            List<ExecutionStep> all = new ArrayList<>(rootSteps);
            all.add(ExecutionStep.builder()
                .step(2)
                .type("ERROR")
                .name("执行失败")
                .status("FAILED")
                .error(e.getMessage())
                .build());
            failed.setSteps(all);
            return failed;
        }
    }
}