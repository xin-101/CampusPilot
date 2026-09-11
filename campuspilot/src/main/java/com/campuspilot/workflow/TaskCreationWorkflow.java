package com.campuspilot.workflow;

import com.campuspilot.agent.model.ExecutionStep;
import com.campuspilot.agent.model.ToolCall;
import com.campuspilot.agent.tool.ToolCaller;
import com.campuspilot.eligibility.EligibilityResult;
import com.campuspilot.notification.NotificationService;
import com.campuspilot.policy.PolicyService;
import com.campuspilot.rag.RAGService;
import com.campuspilot.rag.RetrievalQuery;
import com.campuspilot.rag.RetrievalResult;
import com.campuspilot.rag.RetrievedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 事务办理工作流(创建任务)
 * 安全约束：未通过资格判断(NOT_ELIGIBLE / INSUFFICIENT_DATA / CONDITIONALLY_ELIGIBLE)
 * 一律不创建任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCreationWorkflow implements AgentWorkflow {

    private static final String DEMO_DEADLINE = "2026-09-30 23:59:59";

    private final RAGService ragService;
    private final ToolCaller toolCaller;
    private final NotificationService notificationService;
    private final PolicyService policyService;

    @Override
    public String id() {
        return "task_creation";
    }

    @Override
    public String name() {
        return "事务办理工作流";
    }

    @Override
    public String description() {
        return "政策检索 -> 资格判断 -> (通过)创建任务 -> 通知";
    }

    @Override
    public String supportsIntent() {
        return "TASK_CREATE";
    }

    @Override
    public WorkflowResult execute(WorkflowContext context) {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ToolCall> toolCalls = new ArrayList<>();
        int seq = 1;

        String category = WorkflowSupport.inferCategory(context.getMessage());

        // 1. 政策检索(RAG)
        long t0 = System.currentTimeMillis();
        RetrievalResult retrieval = ragService.retrieve(
            RetrievalQuery.builder()
                .query(context.getMessage())
                .intent(context.getIntent())
                .category(category)
                .userRole(context.getRole())
                .topK(3)
                .build()
        );
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("RETRIEVAL")
            .name("政策检索(RAG)")
            .status(retrieval.isEmpty() ? "FAILED" : "SUCCESS")
            .duration(System.currentTimeMillis() - t0)
            .output(Map.of("provider", retrieval.getProvider(), "hits", retrieval.getDocuments().size()))
            .build());

        if (retrieval.isEmpty()) {
            String msg = "未找到可办理的相关有效政策，未创建任务。\n请换一种说法，例如「帮我申请奖学金」。";
            return WorkflowResult.builder()
                .status("SUCCESS")
                .workflowId(id())
                .workflowName(name())
                .message(msg)
                .citations(new ArrayList<>())
                .toolCalls(toolCalls)
                .steps(steps)
                .decisions(Map.of("taskCreated", false, "reason", "NO_POLICY"))
                .retrieval(retrieval)
                .build();
        }

        RetrievedDocument best = retrieval.getDocuments().get(0);

        // 2. 资格判断(规则引擎工具)
        Map<String, Object> checkParams = new LinkedHashMap<>();
        checkParams.put("policy", best.getPolicyName());
        checkParams.put("category", best.getCategory());
        checkParams.put("policyId", best.getPolicyId());
        ToolCall checkCall = toolCaller.execute("check_eligibility", checkParams);
        toolCalls.add(checkCall);
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("TOOL")
            .name("check_eligibility")
            .status(checkCall.getStatus())
            .duration(checkCall.getDuration())
            .input(checkCall.getParams())
            .output(checkCall.getResult())
            .error(checkCall.getError())
            .build());

        EligibilityResult eligibility = extractEligibility(checkCall);

        // 3. 决策：只有 ELIGIBLE 才允许创建任务
        boolean canCreate = eligibility != null && "ELIGIBLE".equals(eligibility.getStatus());
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("eligible", canCreate);
        decision.put("status", eligibility != null ? eligibility.getStatus() : "UNKNOWN");
        decision.put("taskCreated", false);
        decision.put("reason", canCreate ? null
            : (eligibility == null ? "CHECK_FAILED" : "NOT_ELIGIBLE_OR_INSUFFICIENT"));
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("DECISION")
            .name("资格拦截决策")
            .status(canCreate ? "SUCCESS" : "INFO")
            .duration(20)
            .output(decision)
            .build());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskCreated", false);

        String message;
        if (!canCreate) {
            message = "抱歉，我暂时不能为你创建任务。\n\n"
                + "**资格判断结果**：\n";
            if (eligibility != null) {
                message += eligibility.getExplanation();
            } else {
                message += "资格判断失败，请稍后重试。";
            }
            message += "\n\n为保障流程安全，未通过资格判断时系统不会创建任务（涉及「"
                + best.getPolicyName() + "」）。";
            decision.put("taskCreated", false);
        } else {
            // 4. 创建任务(工具调用 -> Service -> DB)
            // 知识库文档(id>=10000)不在 policies 表：tasks.policy_id 外键要求 DB 政策，
            // 通过标题回映射解析 DB 政策ID（未命中则该字段置空）
            Long dbPolicyId = best.getPolicyId();
            if (dbPolicyId != null && dbPolicyId >= 10000L) {
                Long resolved = policyService.resolveDbPolicyIdByName(best.getPolicyName(), best.getCategory());
                if (resolved != null) {
                    dbPolicyId = resolved;
                } else {
                    dbPolicyId = null;
                }
            }
            ToolCall todoCall = toolCaller.execute("create_todo", Map.of(
                "title", best.getPolicyName() + "申请",
                "description", "根据" + best.getPolicyName() + "办理申请任务",
                "deadline", DEMO_DEADLINE,
                "relatedPolicyId", dbPolicyId == null ? "" : dbPolicyId.toString()
            ));
            toolCalls.add(todoCall);
            steps.add(ExecutionStep.builder()
                .step(seq++)
                .type("TOOL")
                .name("create_todo")
                .status(todoCall.getStatus())
                .duration(todoCall.getDuration())
                .input(todoCall.getParams())
                .output(todoCall.getResult())
                .error(todoCall.getError())
                .build());

            boolean created = "SUCCESS".equals(todoCall.getStatus());
            data.put("taskCreated", created);
            Object taskId = created && todoCall.getResult() instanceof Map
                ? ((Map<?, ?>) todoCall.getResult()).get("taskId") : null;
            if (taskId != null) {
                data.put("taskId", taskId);
            }
            decision.put("taskCreated", created);

            if (created) {
                // 5. 任务创建成功 -> 发送通知(可追踪)
                Long taskNotificationId = null;
                if (context.getUserId() != null && taskId != null) {
                    taskNotificationId = notificationService.send(
                        context.getUserId(), null, "TASK",
                        "任务已创建",
                        "任务「" + best.getPolicyName() + "申请」已创建，请按时完成（截止 "
                            + DEMO_DEADLINE + "）。",
                        Long.parseLong(taskId.toString())
                    ).getId();
                    steps.add(ExecutionStep.builder()
                        .step(seq++)
                        .type("NOTIFY")
                        .name("发送任务通知")
                        .status("SUCCESS")
                        .duration(20)
                        .output(Map.of("notificationId", taskNotificationId, "type", "TASK"))
                        .build());
                }
                message = "好的，我来帮你办理「" + best.getPolicyName() + "」申请。\n\n"
                    + "✅ 资格判断通过，任务已创建"
                    + (taskId != null ? "（任务ID: " + taskId + "）" : "") + "\n\n"
                    + "**任务内容**：\n"
                    + "1. ✅ 查询相关政策\n"
                    + "2. ⏳ 准备申请材料\n"
                    + "3. ⏳ 在规定时间内提交申请（截止 " + DEMO_DEADLINE + "）\n\n"
                    + "你可以在「我的任务」中查看详情，系统也会在任务临近截止时发送提醒。";
            } else {
                message = "任务创建失败，请稍后重试。";
            }
        }

        steps.add(ExecutionStep.builder()
            .step(seq)
            .type("RESPONSE")
            .name("结果说明")
            .status("SUCCESS")
            .duration(50)
            .build());

        return WorkflowResult.builder()
            .status("SUCCESS")
            .workflowId(id())
            .workflowName(name())
            .message(message)
            .citations(ragService.toCitations(retrieval))
            .toolCalls(toolCalls)
            .steps(steps)
            .decisions(decision)
            .retrieval(retrieval)
            .eligibility(eligibility)
            .data(data)
            .build();
    }

    private EligibilityResult extractEligibility(ToolCall call) {
        if (!"SUCCESS".equals(call.getStatus()) || call.getResult() == null) {
            return null;
        }
        if (call.getResult() instanceof EligibilityResult) {
            return (EligibilityResult) call.getResult();
        }
        if (call.getResult() instanceof Map) {
            Object data = ((Map<?, ?>) call.getResult()).get("data");
            if (data instanceof EligibilityResult) {
                return (EligibilityResult) data;
            }
        }
        return null;
    }
}