package com.campuspilot.agent.provider;

import com.campuspilot.agent.model.*;
import com.campuspilot.rag.RetrievalResult;
import com.campuspilot.workflow.WorkflowContext;
import com.campuspilot.workflow.WorkflowEngine;
import com.campuspilot.workflow.WorkflowResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 开发环境Agent提供方
 * 意图路由：
 * - POLICY_QUERY / ELIGIBILITY_CHECK / TASK_CREATE -> WorkflowEngine(确定性工作流)
 * - SCORE_QUERY / GENERAL_QUERY -> 演示回退
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class MockAgentProvider implements AgentProvider {

    private static final List<String> WORKFLOW_INTENTS =
        List.of("POLICY_QUERY", "ELIGIBILITY_CHECK", "TASK_CREATE");

    private final WorkflowEngine workflowEngine;

    @Override
    public AgentResponse chat(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Mock Agent收到消息: {}", request.getMessage());

        // 模拟网络与LLM推理延迟
        simulateDelay();

        // 1. 意图识别
        String intent = mockIntentDetection(request.getMessage());
        log.info("Mock意图识别: {}", intent);

        // 2. 复杂度判断（路由决定，保留用于兼容）
        String complexity = mockComplexityAnalysis(request.getMessage(), intent);

        // 3. 工作流路由与执行
        if (WORKFLOW_INTENTS.contains(intent)) {
            AgentResponse response = executeWorkflow(request, intent, startTime);
            log.info("Mock工作流响应: workflow={}", response.getIntent());
            return response;
        }

        // 4. 回退路径(SCORE_QUERY / GENERAL_QUERY / 工作流未命中)
        List<ExecutionStep> steps = buildFallbackSteps(intent, complexity);
        String response = buildFallbackResponse(request.getMessage(), intent);

        long endTime = System.currentTimeMillis();
        ExecutionTrace executionTrace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent(intent)
            .workflow(complexity)
            .workflowId("fallback")
            .workflowName("演示回退路径")
            .steps(steps)
            .toolCalls(new ArrayList<>())
            .citations(new ArrayList<>())
            .decisions(Map.of("reason", "NO_WORKFLOW", "route", "fallback"))
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status("SUCCESS")
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response(response)
            .intent(intent)
            .complexity(complexity)
            .sources(new ArrayList<>())
            .executionSteps(steps)
            .toolCalls(new ArrayList<>())
            .executionTrace(executionTrace)
            .build();
    }

    private AgentResponse executeWorkflow(AgentRequest request, String intent, long startTime) {
        WorkflowContext context = WorkflowContext.builder()
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .username(request.getUsername())
            .role(request.getRole() != null ? request.getRole() : "STUDENT")
            .message(request.getMessage())
            .intent(intent)
            .complexity(mockComplexityAnalysis(request.getMessage(), intent))
            .build();

        WorkflowResult result = workflowEngine.routeAndExecute(context);

        if (result == null) {
            long now = System.currentTimeMillis();
            ExecutionTrace trace = ExecutionTrace.builder()
                .executionId(UUID.randomUUID().toString())
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .intent(intent)
                .workflow("UNMATCHED")
                .workflowName("未匹配工作流")
                .steps(new ArrayList<>())
                .toolCalls(new ArrayList<>())
                .citations(new ArrayList<>())
                .startTime(startTime)
                .endTime(now)
                .duration(now - startTime)
                .status("FAILED")
                .error("intent no workflow")
                .build();
            return AgentResponse.builder()
                .sessionId(request.getSessionId())
                .messageId(UUID.randomUUID().toString())
                .response("该意图暂无匹配的工作流，请稍后重试。")
                .intent(intent)
                .complexity(context.getComplexity())
                .sources(new ArrayList<>())
                .executionSteps(new ArrayList<>())
                .toolCalls(new ArrayList<>())
                .executionTrace(trace)
                .build();
        }

        long endTime = System.currentTimeMillis();
        List<ExecutionStep> steps = result.getSteps() != null ? result.getSteps() : new ArrayList<>();
        Map<String, Object> decisions = result.getDecisions() != null ? result.getDecisions() : Map.of();
        RetrievalResult retrieval = result.getRetrieval();

        ExecutionTrace executionTrace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent(intent)
            .workflow(result.getWorkflowId())
            .workflowId(result.getWorkflowId())
            .workflowName(result.getWorkflowName())
            .steps(steps)
            .toolCalls(result.getToolCalls())
            .citations(result.getCitations())
            .decisions(decisions)
            .retrieval(retrieval)
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status(result.getStatus())
            .error(result.getError())
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response(result.getMessage())
            .intent(intent)
            .complexity(context.getComplexity())
            .sources(result.getCitations())
            .executionSteps(steps)
            .toolCalls(result.getToolCalls())
            .executionTrace(executionTrace)
            .build();
    }

    private void simulateDelay() {
        try {
            Thread.sleep(500 + (long) (Math.random() * 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 意图识别（优先级：资格判断 > 任务创建 > 成绩 > 政策 > 通用）
     */
    private String mockIntentDetection(String message) {
        if (message.contains("判断") || message.contains("资格") || message.contains("是否符合")
                || (message.contains("条件") && (message.contains("符合") || message.contains("满足")))) {
            return "ELIGIBILITY_CHECK";
        } else if (message.contains("帮我") || message.contains("办理") || message.contains("申请")) {
            return "TASK_CREATE";
        } else if (message.contains("成绩") || message.contains("绩点")) {
            return "SCORE_QUERY";
        } else if (message.contains("请假") || message.contains("宿舍") || message.contains("奖")
                || message.contains("助") || message.contains("金") || message.contains("在读证明")
                || message.contains("政策") || message.contains("考试")) {
            return "POLICY_QUERY";
        }
        return "GENERAL_QUERY";
    }

    /**
     * 复杂度分析（决策优先：资格/判断类为COMPLEX，办理/申请类为ACTION，其余按长度）
     */
    private String mockComplexityAnalysis(String message, String intent) {
        if ("ELIGIBILITY_CHECK".equals(intent)
                || message.contains("判断") || message.contains("条件") || message.contains("资格")
                || message.contains("是否符合")) {
            return "COMPLEX";
        } else if ("TASK_CREATE".equals(intent)
                || message.contains("帮我") || message.contains("申请") || message.contains("办理")) {
            return "ACTION";
        }
        return "SIMPLE";
    }

    private String buildFallbackResponse(String message, String intent) {
        switch (intent) {
            case "SCORE_QUERY":
                return "我来查询你的成绩信息。\n\n"
                    + "**成绩查询结果（DEMO数据）**：\n\n"
                    + "| 课程 | 学分 | 成绩 |\n"
                    + "|------|------|------|\n"
                    + "| 数据结构 | 4 | 92 (A) |\n"
                    + "| 操作系统 | 4 | 88 (B+) |\n"
                    + "| 计算机网络 | 3 | 90 (A-) |\n"
                    + "| 线性代数 | 4 | 85 (B) |\n\n"
                    + "**总学分**：120\n"
                    + "**绩点**：3.8\n"
                    + "**排名**：15/150（前10%）";
            default:
                return "你好！我是CampusPilot校园事务智能助手。\n\n"
                    + "我可以帮你：\n"
                    + "- 📚 查询校园政策（如：查询奖学金政策）\n"
                    + "- 🎓 判断申请资格（如：帮我判断是否符合国家奖学金申请条件）\n"
                    + "- 📝 办理校园事务（如：帮我申请奖学金）\n"
                    + "- 📊 查询成绩（如：查看我的成绩）\n\n"
                    + "请问有什么可以帮你的？";
        }
    }

    private List<ExecutionStep> buildFallbackSteps(String intent, String complexity) {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(ExecutionStep.builder()
            .step(1)
            .type("INTENT")
            .name("意图识别")
            .status("SUCCESS")
            .duration(200)
            .build());
        steps.add(ExecutionStep.builder()
            .step(2)
            .type("ROUTER")
            .name("复杂度路由")
            .status("SUCCESS")
            .duration(100)
            .output(Map.of("intent", intent, "complexity", complexity, "route", "fallback"))
            .build());
        steps.add(ExecutionStep.builder()
            .step(3)
            .type("RESPONSE")
            .name("演示响应")
            .status("SUCCESS")
            .duration(300)
            .build());
        return steps;
    }

    @Override
    public boolean healthCheck() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "Mock";
    }
}