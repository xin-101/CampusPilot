package com.campuspilot.agent.tool;

import com.campuspilot.eligibility.EligibilityCheckService;
import com.campuspilot.eligibility.EligibilityResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 资格判断工具
 * 规则：学生身份一律取自JWT SecurityContext，拒绝Agent传入任意studentId。
 */
@Component
@RequiredArgsConstructor
public class EligibilityCheckTool implements AgentTool {

    private final EligibilityCheckService eligibilityCheckService;

    @Override
    public String getName() {
        return "check_eligibility";
    }

    @Override
    public String getDescription() {
        return "基于资格规则引擎判断当前学生是否符合政策申请资格";
    }

    @Override
    public ToolResult execute(Map<String, Object> params, String userToken) {
        try {
            // 安全约束：忽略任何注入的studentId参数，仅使用当前登录学生
            if (params.containsKey("studentId")) {
                return ToolResult.error("不允许指定查询其他学生的资格(studentId参数已拒绝)，只能查询本人信息", "FORBIDDEN_STUDENT_ID");
            }

            String policyName = asString(params.get("policy"));
            String category = asString(params.get("category"));
            Long policyId = params.containsKey("policyId")
                ? Long.parseLong(params.get("policyId").toString()) : null;

            EligibilityResult result = eligibilityCheckService.check(policyName, category, policyId);
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.error("资格判断失败: " + e.getMessage(), "TOOL_CALL_FAILED");
        }
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }
}