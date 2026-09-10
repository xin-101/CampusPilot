package com.campuspilot.agent.provider;

import com.campuspilot.agent.llm.*;
import com.campuspilot.agent.model.*;
import com.campuspilot.agent.security.AgentSecurityGuard;
import com.campuspilot.agent.security.SecurityAssessment;
import com.campuspilot.rag.RetrievalResult;
import com.campuspilot.workflow.WorkflowContext;
import com.campuspilot.workflow.WorkflowEngine;
import com.campuspilot.workflow.WorkflowResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 真实 LLM 驱动的 Agent Provider。
 * <p>
 * LLM 负责：意图识别、复杂度分析、自然语言生成。
 * 确定性代码负责：工作流路由、工具调用、规则引擎、权限、安全。
 * <p>
 * 架构：
 * User → SecurityGuard → LLM(intent) → WorkflowEngine(deterministic) → LLM(explain) → Response
 */
@Service
@Lazy
@RequiredArgsConstructor
@Slf4j
public class RealLLMAgentProvider implements AgentProvider {

    private static final List<String> WORKFLOW_INTENTS =
        List.of("POLICY_QUERY", "ELIGIBILITY_CHECK", "TASK_CREATE");

    private static final Pattern JSON_INTENT_PATTERN = Pattern.compile("\"intent\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_COMPLEXITY_PATTERN = Pattern.compile("\"complexity\"\\s*:\\s*\"([^\"]+)\"");

    private final LLMGateway llmGateway;
    private final LLMProperties llmProperties;
    private final WorkflowEngine workflowEngine;
    private final AgentSecurityGuard securityGuard;

    @Override
    public AgentResponse chat(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("RealLLM Agent收到消息: userId={}, length={}",
            request.getUserId(), request.getMessage() != null ? request.getMessage().length() : 0);

        // 0. 安全守卫（不依赖 LLM）
        SecurityAssessment assessment = securityGuard.inspect(request.getMessage());
        if (!assessment.isAllowed()) {
            log.info("RealLLM安全检查未通过: category={}", assessment.getCategory());
            return securityDeniedResponse(request, assessment, startTime);
        }

        // 1. LLM 意图识别
        long llmStart = System.currentTimeMillis();
        String intent;
        String complexity;
        try {
            String classificationPrompt = buildIntentClassificationPrompt(request.getMessage());
            String llmOutput = llmGateway.chat(getSystemPrompt(), classificationPrompt);
            intent = parseIntent(llmOutput);
            complexity = parseComplexity(llmOutput);
            long llmDuration = System.currentTimeMillis() - llmStart;
            log.info("LLM意图识别: intent={}, complexity={}, duration={}ms", intent, complexity, llmDuration);
        } catch (LLMGatewayException e) {
            log.error("LLM意图识别失败: {}", e.getMessage());
            return llmErrorResponse(request, startTime, "AI 服务暂时不可用，请稍后重试。");
        }

        // 2. 确定性工作流路由与执行（与 MockAgentProvider 相同逻辑）
        if (WORKFLOW_INTENTS.contains(intent)) {
            return executeWorkflowWithLLM(request, intent, complexity, startTime);
        }

        // 3. 回退路径（无工作流匹配）
        return buildFallbackWithLLM(request, intent, complexity, startTime);
    }

    /**
     * 执行工作流 + LLM 自然语言解释。
     */
    private AgentResponse executeWorkflowWithLLM(AgentRequest request, String intent,
                                                  String complexity, long startTime) {
        WorkflowContext context = WorkflowContext.builder()
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .username(request.getUsername())
            .role(request.getRole() != null ? request.getRole() : "STUDENT")
            .message(request.getMessage())
            .intent(intent)
            .complexity(complexity)
            .build();

        WorkflowResult result = workflowEngine.routeAndExecute(context);

        if (result == null) {
            return workflowNotFoundResponse(request, intent, startTime);
        }

        // LLM 生成自然语言解释
        String finalResponse;
        long llmExplainStart = System.currentTimeMillis();
        try {
            String explainPrompt = buildExplanationPrompt(request.getMessage(), intent, result);
            finalResponse = llmGateway.chat(getSystemPrompt(), explainPrompt);
            log.info("LLM解释生成: duration={}ms", System.currentTimeMillis() - llmExplainStart);
        } catch (LLMGatewayException e) {
            log.warn("LLM解释生成失败，使用工作流原始消息: {}", e.getMessage());
            finalResponse = result.getMessage();
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
            .response(finalResponse)
            .intent(intent)
            .complexity(complexity)
            .sources(result.getCitations())
            .executionSteps(steps)
            .toolCalls(result.getToolCalls())
            .executionTrace(executionTrace)
            .data(result.getData())
            .actions(buildActions(result))
            .build();
    }

    /**
     * 回退路径：LLM 生成通用回复。
     */
    private AgentResponse buildFallbackWithLLM(AgentRequest request, String intent,
                                                String complexity, long startTime) {
        long llmStart = System.currentTimeMillis();
        String fallbackResponse;
        try {
            String prompt = "用户问题：" + request.getMessage()
                + "\n\n请用友好的语气回答。如果涉及校园事务，请引导用户说明具体需求。"
                + "\n可用功能：查询政策、判断资格、办理事务、查看成绩、查看通知。";
            fallbackResponse = llmGateway.chat(getSystemPrompt(), prompt);
            log.info("LLM回退响应: duration={}ms", System.currentTimeMillis() - llmStart);
        } catch (LLMGatewayException e) {
            log.warn("LLM回退响应失败: {}", e.getMessage());
            fallbackResponse = "你好！我是CampusPilot校园事务智能助手。请问有什么可以帮你的？";
        }

        List<ExecutionStep> steps = buildFallbackSteps(intent, complexity);
        long endTime = System.currentTimeMillis();

        ExecutionTrace executionTrace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent(intent)
            .workflow(complexity)
            .workflowId("fallback")
            .workflowName("LLM回退路径")
            .steps(steps)
            .toolCalls(new ArrayList<>())
            .citations(new ArrayList<>())
            .decisions(Map.of("reason", "NO_WORKFLOW", "route", "llm_fallback"))
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status("SUCCESS")
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response(fallbackResponse)
            .intent(intent)
            .complexity(complexity)
            .sources(new ArrayList<>())
            .executionSteps(steps)
            .toolCalls(new ArrayList<>())
            .executionTrace(executionTrace)
            .build();
    }

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
                .type("VIEW_POLICY").label("查看政策")
                .params(policyId != null ? Map.of("policyId", policyId, "policyName", policyName) : Map.of())
                .build());
        } else if ("eligibility_check".equals(workflowId)) {
            actions.add(AgentAction.builder()
                .type("VIEW_POLICY").label("查看政策")
                .params(policyId != null ? Map.of("policyId", policyId, "policyName", policyName) : Map.of())
                .build());
            if (result.getEligibility() != null && "ELIGIBLE".equals(result.getEligibility().getStatus())) {
                actions.add(AgentAction.builder()
                    .type("CREATE_TASK").label("创建申请任务")
                    .params(Map.of("policyId", policyId, "policyName", policyName))
                    .build());
            }
        } else if ("task_creation".equals(workflowId)) {
            Map<String, Object> data = result.getData() != null ? result.getData() : Map.of();
            if (Boolean.TRUE.equals(data.get("taskCreated"))) {
                actions.add(AgentAction.builder()
                    .type("VIEW_TASKS").label("查看任务")
                    .params(data.get("taskId") != null ? Map.of("taskId", data.get("taskId")) : Map.of())
                    .build());
            }
        }
        return actions;
    }

    // ── Intent Classification ──────────────────────────────────────────

    private String getSystemPrompt() {
        return "你是 CampusPilot，高校校园学生事务智能体。\n\n"
            + "你的职责：\n"
            + "1. 理解学生问题\n"
            + "2. 查询可靠校园政策\n"
            + "3. 根据真实学生信息分析问题\n"
            + "4. 在需要时调用工具\n"
            + "5. 遵守业务规则和权限\n"
            + "6. 使用真实政策依据回答\n"
            + "7. 不编造政策\n"
            + "8. 不编造学生信息\n"
            + "9. 不编造任务\n\n"
            + "可执行的工作流：\n"
            + "- POLICY_QUERY：查询政策信息\n"
            + "- ELIGIBILITY_CHECK：判断申请资格\n"
            + "- TASK_CREATE：创建事务办理任务\n\n"
            + "规则：\n"
            + "- 如果用户在询问政策，返回 POLICY_QUERY\n"
            + "- 如果用户在询问自己是否符合条件（能不能/能否/可否/可以申请），返回 ELIGIBILITY_CHECK\n"
            + "- 如果用户要办理事务（帮我/申请/办理），返回 TASK_CREATE\n"
            + "- 政策类问题必须使用检索到的政策信息回答，不编造\n"
            + "- 资格判断必须调用规则引擎，不自行判断";
    }

    private String buildIntentClassificationPrompt(String message) {
        return "分析以下用户消息的意图和复杂度。\n\n"
            + "用户消息：" + message + "\n\n"
            + "请返回 JSON 格式：\n"
            + "{\"intent\": \"意图类型\", \"complexity\": \"复杂度级别\"}\n\n"
            + "意图类型必须是以下之一：\n"
            + "- POLICY_QUERY（政策咨询/查询）\n"
            + "- ELIGIBILITY_CHECK（资格判断/申请条件）\n"
            + "- TASK_CREATE（事务办理/申请）\n"
            + "- SCORE_QUERY（成绩查询）\n"
            + "- NOTIFICATION_QUERY（通知查询）\n"
            + "- TASK_QUERY（任务查询）\n"
            + "- GENERAL_QUERY（通用问答）\n\n"
            + "复杂度级别：\n"
            + "- SIMPLE（普通政策查询）\n"
            + "- COMPLEX（需要规则引擎判断）\n"
            + "- ACTION（需要创建事务）\n\n"
            + "只返回 JSON，不要其他内容。";
    }

    private String buildExplanationPrompt(String userMessage, String intent, WorkflowResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(userMessage).append("\n");
        sb.append("意图：").append(intent).append("\n");
        sb.append("工作流结果状态：").append(result.getStatus()).append("\n");

        if (result.getMessage() != null) {
            sb.append("工作流生成的回答：").append(result.getMessage()).append("\n");
        }
        if (result.getCitations() != null && !result.getCitations().isEmpty()) {
            sb.append("引用来源：");
            for (Citation c : result.getCitations()) {
                sb.append(c.getPolicyName()).append("（").append(c.getVersion()).append("）").append("; ");
            }
            sb.append("\n");
        }
        if (result.getEligibility() != null) {
            sb.append("资格判断结果：").append(result.getEligibility().getStatus()).append("\n");
        }

        sb.append("\n请基于以上信息，用自然语言为学生生成友好的回答。");
        sb.append("\n要求：\n");
        sb.append("- 使用 Markdown 格式\n");
        sb.append("- 引用政策名称和版本\n");
        sb.append("- 如果是资格判断，说明判断依据\n");
        sb.append("- 如果是事务办理，说明任务创建结果\n");
        sb.append("- 不要编造信息\n");

        return sb.toString();
    }

    // ── JSON Parsing ───────────────────────────────────────────────────

    private String parseIntent(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) return "GENERAL_QUERY";
        Matcher m = JSON_INTENT_PATTERN.matcher(llmOutput);
        if (m.find()) {
            String intent = m.group(1).trim().toUpperCase();
            // 白名单校验
            if (List.of("POLICY_QUERY", "ELIGIBILITY_CHECK", "TASK_CREATE",
                    "SCORE_QUERY", "NOTIFICATION_QUERY", "TASK_QUERY", "GENERAL_QUERY")
                .contains(intent)) {
                return intent;
            }
        }
        // JSON 解析失败，尝试关键词匹配作为 fallback
        return fallbackIntentDetection(llmOutput);
    }

    private String parseComplexity(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) return "SIMPLE";
        Matcher m = JSON_COMPLEXITY_PATTERN.matcher(llmOutput);
        if (m.find()) {
            String c = m.group(1).trim().toUpperCase();
            if (List.of("SIMPLE", "COMPLEX", "ACTION").contains(c)) {
                return c;
            }
        }
        return "SIMPLE";
    }

    private String fallbackIntentDetection(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("eligibility") || lower.contains("资格") || lower.contains("判断")) {
            return "ELIGIBILITY_CHECK";
        }
        if (lower.contains("task") || lower.contains("create") || lower.contains("办理")) {
            return "TASK_CREATE";
        }
        if (lower.contains("policy") || lower.contains("政策") || lower.contains("查询")) {
            return "POLICY_QUERY";
        }
        return "GENERAL_QUERY";
    }

    // ── Error / Denied / Fallback Responses ────────────────────────────

    private AgentResponse llmErrorResponse(AgentRequest request, long startTime, String message) {
        long endTime = System.currentTimeMillis();
        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response("⚠️ " + message)
            .intent("ERROR")
            .complexity("SIMPLE")
            .sources(new ArrayList<>())
            .executionSteps(new ArrayList<>())
            .toolCalls(new ArrayList<>())
            .executionTrace(ExecutionTrace.builder()
                .executionId(UUID.randomUUID().toString())
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .intent("ERROR")
                .workflow("none")
                .workflowId("llm_error")
                .workflowName("LLM服务异常")
                .steps(new ArrayList<>())
                .toolCalls(new ArrayList<>())
                .citations(new ArrayList<>())
                .decisions(Map.of("provider", "LLM", "error", message))
                .startTime(startTime)
                .endTime(endTime)
                .duration(endTime - startTime)
                .status("LLM_UNAVAILABLE")
                .error(message)
                .build())
            .build();
    }

    private AgentResponse securityDeniedResponse(AgentRequest request, SecurityAssessment assessment, long startTime) {
        long endTime = System.currentTimeMillis();
        ExecutionStep guardStep = ExecutionStep.builder()
            .step(1).type("SECURITY").name("安全守卫拦截").status("DENIED").duration(20)
            .output(Map.of("category", assessment.getCategory(),
                "matchedPattern", assessment.getMatchedPattern() == null ? "N/A" : assessment.getMatchedPattern()))
            .build();

        ExecutionTrace trace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId()).userId(request.getUserId())
            .intent("DENIED").workflow("none").workflowId("security_guard").workflowName("安全守卫")
            .steps(List.of(guardStep)).toolCalls(new ArrayList<>()).citations(new ArrayList<>())
            .decisions(Map.of("allowed", false, "category", assessment.getCategory()))
            .startTime(startTime).endTime(endTime).duration(endTime - startTime)
            .status("DENIED").error(assessment.getReason())
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId()).messageId(UUID.randomUUID().toString())
            .response("⚠️ **安全防护提示**\n\n" + assessment.getReason()
                + "\n\n该次请求已由安全守卫拦截，未执行任何工具与数据处理。")
            .intent("DENIED").complexity("SIMPLE").sources(new ArrayList<>())
            .executionSteps(List.of(guardStep)).toolCalls(new ArrayList<>()).executionTrace(trace)
            .build();
    }

    private AgentResponse workflowNotFoundResponse(AgentRequest request, String intent, long startTime) {
        long endTime = System.currentTimeMillis();
        ExecutionTrace trace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId()).userId(request.getUserId())
            .intent(intent).workflow("UNMATCHED").workflowName("未匹配工作流")
            .steps(new ArrayList<>()).toolCalls(new ArrayList<>()).citations(new ArrayList<>())
            .startTime(startTime).endTime(endTime).duration(endTime - startTime)
            .status("FAILED").error("intent no workflow")
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId()).messageId(UUID.randomUUID().toString())
            .response("该意图暂无匹配的工作流，请稍后重试。")
            .intent(intent).complexity("SIMPLE").sources(new ArrayList<>())
            .executionSteps(new ArrayList<>()).toolCalls(new ArrayList<>()).executionTrace(trace)
            .build();
    }

    private List<ExecutionStep> buildFallbackSteps(String intent, String complexity) {
        return List.of(
            ExecutionStep.builder().step(1).type("INTENT").name("LLM意图识别").status("SUCCESS").duration(200).build(),
            ExecutionStep.builder().step(2).type("ROUTER").name("复杂度路由").status("SUCCESS").duration(100)
                .output(Map.of("intent", intent, "complexity", complexity, "route", "llm_fallback")).build(),
            ExecutionStep.builder().step(3).type("RESPONSE").name("LLM响应生成").status("SUCCESS").duration(300).build()
        );
    }

    @Override
    public boolean healthCheck() {
        return llmProperties.isReallyConfigured();
    }

    @Override
    public String getProviderName() {
        return "RealLLM(" + llmProperties.getModel() + ")";
    }
}
