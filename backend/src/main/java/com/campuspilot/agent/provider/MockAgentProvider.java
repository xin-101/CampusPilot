package com.campuspilot.agent.provider;

import com.campuspilot.agent.model.*;
import com.campuspilot.agent.security.AgentSecurityGuard;
import com.campuspilot.agent.security.SecurityAssessment;
import com.campuspilot.rag.RetrievalResult;
import com.campuspilot.workflow.WorkflowContext;
import com.campuspilot.workflow.WorkflowEngine;
import com.campuspilot.workflow.WorkflowResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class MockAgentProvider implements AgentProvider {

    private static final List<String> WORKFLOW_INTENTS =
        List.of("POLICY_QUERY", "ELIGIBILITY_CHECK", "TASK_CREATE");

    private final WorkflowEngine workflowEngine;
    private final AgentSecurityGuard securityGuard;

    @Override
    public AgentResponse chat(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Mock Agent收到消息: {}, length={}", request.getMessage(), request.getMessage() != null ? request.getMessage().length() : 0);

        // 0. 安全守卫：输入校验与恶意注入拦截
        SecurityAssessment assessment = securityGuard.inspect(request.getMessage());
        if (!assessment.isAllowed()) {
            log.info("Mock Agent安全检查未通过: category={}, pattern={}",
                assessment.getCategory(), assessment.getMatchedPattern());
            return securityDeniedResponse(request, assessment, startTime);
        }

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
            .data(result.getData())
            .actions(buildActions(result))
            .build();
    }

    /**
     * 根据工作流结果确定性生成"下一步行动"，前端据此渲染按钮。
     */
    private List<AgentAction> buildActions(WorkflowResult result) {
        List<AgentAction> actions = new ArrayList<>();
        String workflowId = result.getWorkflowId();
        RetrievalResult retrieval = result.getRetrieval();

        Long policyId = null;
        String policyName = null;
        if (retrieval != null && !retrieval.isEmpty() && retrieval.getDocuments().get(0) != null) {
            policyId = retrieval.getDocuments().get(0).getPolicyId();
            policyName = retrieval.getDocuments().get(0).getPolicyName();
        }

        if ("policy_consultation".equals(workflowId)) {
            actions.add(AgentAction.builder()
                .type("VIEW_POLICY")
                .label("查看政策")
                .params(policyId != null ? Map.of("policyId", policyId, "policyName", policyName) : Map.of())
                .build());
        } else if ("eligibility_check".equals(workflowId)) {
            actions.add(AgentAction.builder()
                .type("VIEW_POLICY")
                .label("查看政策")
                .params(policyId != null ? Map.of("policyId", policyId, "policyName", policyName) : Map.of())
                .build());
            if (result.getEligibility() != null
                && "ELIGIBLE".equals(result.getEligibility().getStatus())) {
                actions.add(AgentAction.builder()
                    .type("CREATE_TASK")
                    .label("创建申请任务")
                    .params(Map.of("policyId", policyId, "policyName", policyName))
                    .build());
            }
        } else if ("task_creation".equals(workflowId)) {
            Map<String, Object> data = result.getData() != null ? result.getData() : Map.of();
            if (Boolean.TRUE.equals(data.get("taskCreated"))) {
                Object taskId = data.get("taskId");
                actions.add(AgentAction.builder()
                    .type("VIEW_TASKS")
                    .label("查看任务")
                    .params(taskId != null ? Map.of("taskId", taskId) : Map.of())
                    .build());
            } else {
                actions.add(AgentAction.builder()
                    .type("VIEW_POLICY")
                    .label("查看政策")
                    .params(policyId != null ? Map.of("policyId", policyId, "policyName", policyName) : Map.of())
                    .build());
            }
        }
        return actions;
    }

    private void simulateDelay() {
        try {
            Thread.sleep(500 + (long) (Math.random() * 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 安全拦截响应：不执行任何工具/工作流，向用户说明被拦截原因(可追踪)。
     */
    private AgentResponse securityDeniedResponse(AgentRequest request, SecurityAssessment assessment, long startTime) {
        long endTime = System.currentTimeMillis();
        ExecutionStep guardStep = ExecutionStep.builder()
            .step(1)
            .type("SECURITY")
            .name("安全守卫拦截")
            .status("DENIED")
            .duration(20)
            .output(java.util.Map.of(
                "category", assessment.getCategory(),
                "matchedPattern", assessment.getMatchedPattern() == null ? "N/A" : assessment.getMatchedPattern()
            ))
            .build();

        List<ExecutionStep> steps = List.of(guardStep);
        ExecutionTrace trace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent("DENIED")
            .workflow("none")
            .workflowId("security_guard")
            .workflowName("安全守卫")
            .steps(steps)
            .toolCalls(new ArrayList<>())
            .citations(new ArrayList<>())
            .decisions(java.util.Map.of("allowed", false, "category", assessment.getCategory()))
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status("DENIED")
            .error(assessment.getReason())
            .build();

        String response = "⚠️ **安全防护提示**\n\n" + assessment.getReason()
            + "\n\n该次请求已由安全守卫拦截，未执行任何工具与数据处理。"
            + "如需帮助，可以问我：「查询奖学金政策」「帮我判断是否符合国家奖学金条件」「帮我申请奖学金」。";

        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response(response)
            .intent("DENIED")
            .complexity("SIMPLE")
            .sources(new ArrayList<>())
            .executionSteps(steps)
            .toolCalls(new ArrayList<>())
            .executionTrace(trace)
            .build();
    }

    /**
     * 意图识别（优先级：资格判断 > 任务创建 > 成绩 > 通知 > 任务查询 > 政策 > 通用）
     * 资格类：显式判断/资格行为 + "能不能/能否/可否/可以申请/能申请"
     * 排除纯政策询问："申请条件"/"满足什么条件" 等只问规则不问自身资格
     */
    private String mockIntentDetection(String message) {
        boolean explicitEligibilityAction =
                message.contains("判断") || message.contains("资格") || message.contains("是否符合")
                || message.contains("能不能") || message.contains("能否") || message.contains("可否")
                || message.contains("可以申请") || message.contains("能申请");
        boolean asksOwnCondition =
                message.contains("符合条件") || message.contains("满足条件") || message.contains("达到条件")
                || message.contains("达标条件");
        if (explicitEligibilityAction || asksOwnCondition) {
            return "ELIGIBILITY_CHECK";
        }
        boolean infoLike = message.contains("什么时候") || message.contains("如何")
                || message.contains("怎么") || message.contains("怎样") || message.contains("多少")
                || message.contains("多久") || message.contains("什么") || message.contains("条件")
                || message.contains("标准") || message.contains("流程") || message.contains("材料")
                || message.contains("时间") || message.contains("在哪") || message.contains("哪里")
                || message.contains("哪些") || message.contains("怎么办") || message.contains("有哪些");
        boolean explicitAction = message.contains("帮我") || message.contains("请帮我")
                || message.contains("替我") || message.contains("帮忙") || message.contains("给我办")
                || message.contains("预约") || message.contains("报名") || message.contains("提交")
                || message.contains("请办理") || message.contains("帮我办");
        if (explicitAction || ((message.contains("申请") || message.contains("办理")) && !infoLike)) {
            return "TASK_CREATE";
        } else if (message.contains("成绩") || message.contains("绩点") || message.contains("gpa")) {
            return "SCORE_QUERY";
        } else if (message.contains("通知") || message.contains("消息") || message.contains("提醒")
                || message.contains("未读") || message.contains("待办通知")) {
            return "NOTIFICATION_QUERY";
        } else if (message.contains("我的任务") || message.contains("待办") || message.contains("任务列表")
                || message.contains("任务状态") || message.contains("办理进度")) {
            return "TASK_QUERY";
        } else if (message.contains("请假") || message.contains("宿舍") || message.contains("寝室")
                || message.contains("住宿") || message.contains("奖") || message.contains("助")
                || message.contains("金") || message.contains("补助") || message.contains("资助")
                || message.contains("贷款") || message.contains("在读证明") || message.contains("学生证")
                || message.contains("综合测评") || message.contains("测评") || message.contains("四六级")
                || message.contains("离校") || message.contains("勤工") || message.contains("学分")
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
            case "TASK_QUERY":
                return "📋 **任务查询（DEMO数据）**\n\n"
                    + "你可以通过「任务管理」页面查看所有待办任务。\n\n"
                    + "或直接输入以下操作：\n"
                    + "- 「帮我申请奖学金」— 创建新的申请任务\n"
                    + "- 「查看我的任务」— 跳转任务列表\n\n"
                    + "如需帮助，请告诉我你想办理的具体事务。";
            case "NOTIFICATION_QUERY":
                return "🔔 **通知查询**\n\n"
                    + "你可以通过顶部导航栏的通知图标查看未读通知。\n\n"
                    + "我会在以下场景主动通知你：\n"
                    + "- ✅ 新任务创建\n"
                    + "- ⏰ 任务截止提醒\n"
                    + "- 📢 系统公告\n\n"
                    + "通知会自动推送，无需手动刷新。";
            default:
                return "你好！我是CampusPilot校园事务智能助手。\n\n"
                    + "我可以帮你：\n"
                    + "- 📚 查询校园政策（如：查询奖学金政策）\n"
                    + "- 🎓 判断申请资格（如：帮我判断是否符合国家奖学金申请条件）\n"
                    + "- 📝 办理校园事务（如：帮我申请奖学金）\n"
                    + "- 📊 查询成绩（如：查看我的成绩）\n"
                    + "- 🔔 查看通知（如：查看我的待办通知）\n"
                    + "- 📋 管理任务（如：查看我的待办任务）\n\n"
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