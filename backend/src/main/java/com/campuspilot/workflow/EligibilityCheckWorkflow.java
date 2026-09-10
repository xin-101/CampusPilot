package com.campuspilot.workflow;

import com.campuspilot.agent.model.ExecutionStep;
import com.campuspilot.agent.model.ToolCall;
import com.campuspilot.agent.tool.ToolCaller;
import com.campuspilot.eligibility.EligibilityResult;
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
 * 资格判断工作流
 * 流程：政策检索 -> 学生信息(工具) -> 成绩画像(规则引擎字段) -> 资格判断 -> 解释
 * 最终判断由规则引擎确定性完成，职责写入执行轨迹。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EligibilityCheckWorkflow implements AgentWorkflow {

    private final RAGService ragService;
    private final ToolCaller toolCaller;

    @Override
    public String id() {
        return "eligibility_check";
    }

    @Override
    public String name() {
        return "资格判断工作流";
    }

    @Override
    public String description() {
        return "检索相关政策 -> 获取学生画像 -> 规则引擎确定性判断资格";
    }

    @Override
    public String supportsIntent() {
        return "ELIGIBILITY_CHECK";
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

        RetrievedDocument best = retrieval.isEmpty() ? null : retrieval.getDocuments().get(0);
        if (best != null) {
            category = best.getCategory();
        }

        // 2. 获取学生信息(工具调用，身份来自JWT SecurityContext)
        ToolCall studentCall = toolCaller.execute("get_student_info", Map.of());
        toolCalls.add(studentCall);
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("TOOL")
            .name("get_student_info")
            .status(studentCall.getStatus())
            .duration(studentCall.getDuration())
            .input(studentCall.getParams())
            .output(studentCall.getResult())
            .error(studentCall.getError())
            .build());

        // 3. 资格判断(规则引擎工具调用)
        Map<String, Object> eligibilityParams = new LinkedHashMap<>();
        if (best != null) {
            eligibilityParams.put("policy", best.getPolicyName());
            eligibilityParams.put("category", best.getCategory());
            eligibilityParams.put("policyId", best.getPolicyId());
        } else if (category != null) {
            eligibilityParams.put("category", category);
        }
        ToolCall checkCall = toolCaller.execute("check_eligibility", eligibilityParams);
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

        // 4. 决策：记录资格结论(职责由规则引擎承担)
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("status", eligibility != null ? eligibility.getStatus() : "UNKNOWN");
        decision.put("eligible", eligibility != null ? eligibility.isEligible() : false);
        decision.put("category", category);
        decision.put("policy", best != null ? best.getPolicyName() : "未识别");
        decision.put("judgmentSource", "ELIGIBILITY_RULE_ENGINE");
        decision.put("demoData", true);
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("DECISION")
            .name("资格判断决策")
            .status(eligibility != null && "ELIGIBLE".equals(eligibility.getStatus()) ? "SUCCESS" : "INFO")
            .duration(20)
            .output(decision)
            .build());

        // 5. 答案生成
        String message = buildAnswer(best, eligibility);
        steps.add(ExecutionStep.builder()
            .step(seq)
            .type("RESPONSE")
            .name("结果解释")
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

    private String buildAnswer(RetrievedDocument best, EligibilityResult eligibility) {
        StringBuilder sb = new StringBuilder();
        if (eligibleStatus(eligibility)) {
            sb.append("我来帮你判断").append(best != null ? "「" + best.getPolicyName() + "」" : "").append("的申请资格。\n\n");
        }
        if (eligibility == null) {
            sb.append("资格判断暂时无法完成，请稍后重试。");
            return sb.toString();
        }

        sb.append("**资格判断结果（基于规则引擎，数据为DEMO演示）**：\n\n");
        sb.append(eligibility.getExplanation());

        if (!eligibility.getPolicySources().isEmpty()) {
            sb.append("\n**依据政策**：");
            sb.append(String.join("、", eligibility.getPolicySources()));
            sb.append("\n");
        }
        if (best != null && best.getEffectiveDate() != null && best.getExpiryDate() != null) {
            sb.append("（当前有效版本生效期：").append(best.getEffectiveDate())
              .append(" 至 ").append(best.getExpiryDate()).append("）\n");
        }
        sb.append("\n⚠️ 以上为DEMO演示数据判断，仅供系统演示，不代表真实校规审核结果。");
        if ("ELIGIBLE".equals(eligibility.getStatus())) {
            sb.append("\n\n我可以帮你创建对应的申请任务，对我说「帮我申请」即可。");
        }
        return sb.toString();
    }

    private boolean eligibleStatus(EligibilityResult r) {
        return r != null && r.getStatus() != null && !r.getStatus().startsWith("ELIGIBLE")
            && !"NOT_ELIGIBLE".equals(r.getStatus());
    }
}