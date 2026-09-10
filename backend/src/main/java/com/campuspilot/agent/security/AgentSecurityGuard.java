package com.campuspilot.agent.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent安全守卫：对用户消息进行输入校验与恶意注入检测。
 * 防护范围：
 * 1. 系统提示词/内部指令泄露
 * 2. 冒用管理员/教职工身份
 * 3. 越权查询他人信息(学生只能查自己)
 * 4. 工具调用/参数注入
 * 5. 无效输入
 */
@Slf4j
@Component
public class AgentSecurityGuard {

    private static final List<String> PROMPT_LEAK_PATTERNS = List.of(
        "系统提示词", "system prompt", "systemprompt", "你的提示词", "系统设定", "角色设定",
        "内部指令", "初始设定", "隐藏指令", "底层指令", "指令集", "prompt injection",
        "ignore previous", "ignore all", "忽略之前", "忽略以上", "忽略所有", "忽略历史",
        "不再执行之前的指令", "不要按之前的指令", "developer mode", "开发模式", "dev mode",
        "unlock", "越狱", "隐藏提示"
    );

    private static final List<String> ADMIN_IMPERSONATION_PATTERNS = List.of(
        "冒充管理员", "以管理员身份", "把我设为管理员", "给我管理员", "管理员权限",
        "假扮成管理员", "用管理员的身份", "赋予我管理员", "越权", "绕过权限", "绕过认证",
        "冒充老师", "冒充辅导员", "我是管理员"
    );

    private static final List<String> OTHER_STUDENT_PATTERNS = List.of(
        "其他学生", "其他同学", "别人的", "李四", "2021002", "查询张三"
    );

    private static final List<String> TOOL_INJECTION_PATTERNS = List.of(
        "create_todo(", "check_eligibility(", "get_student_info(", "search_policy(",
        "tool_calls", "工具调用参数", "注入参数", "构造参数", "直接调用"
    );

    /**
     * 检查用户消息
     */
    public SecurityAssessment inspect(String message) {
        if (message == null || message.isBlank()) {
            return SecurityAssessment.deny("DENIED_INVALID_INPUT", "<blank>", "消息为空，请重新输入。");
        }
        if (message.length() > 500) {
            return SecurityAssessment.deny("DENIED_INVALID_INPUT", "length=" + message.length(),
                "消息过长（最多500字），请精简后重试。");
        }

        String m = message.toLowerCase();

        for (String p : PROMPT_LEAK_PATTERNS) {
            if (message.contains(p) || m.contains(p)) {
                log.warn("SecurityGuard: prompt leak attempt detected: {}", p);
                return SecurityAssessment.deny("DENIED_SYSTEM_PROMPT_LEAK", p,
                    "我不会泄露系统提示词或内部指令。你可以询问校园政策、申请资格等内容。");
            }
        }

        for (String p : ADMIN_IMPERSONATION_PATTERNS) {
            if (message.contains(p)) {
                log.warn("SecurityGuard: admin impersonation attempt detected: {}", p);
                return SecurityAssessment.deny("DENIED_ADMIN_IMPERSONATION", p,
                    "你当前的账号权限为普通学生，无法执行管理员/教职工操作。此行为已被安全防护拦截。");
            }
        }

        // 成绩/信息类查询 + 他人学号/姓名 -> 越权访问(最多2次子串检查)
        if (message.contains("成绩") || message.contains("绩点") || message.contains("信息")) {
            for (String p : OTHER_STUDENT_PATTERNS) {
                if (message.contains(p)) {
                    log.warn("SecurityGuard: other-student identity query detected: {}", p);
                    return SecurityAssessment.deny("DENIED_UNAUTHORIZED_ACCESS", p,
                        "出于隐私与数据安全考虑，你只能查询本人的成绩与个人信息。");
                }
            }
        }

        for (String p : TOOL_INJECTION_PATTERNS) {
            if (m.contains(p)) {
                log.warn("SecurityGuard: tool invocation injection detected: {}", p);
                return SecurityAssessment.deny("DENIED_TOOL_INJECTION", p,
                    "不支持直接指定内部工具调用参数。你也可以说「帮我判断是否符合奖学金条件」「帮我创建申请任务」。");
            }
        }

        return SecurityAssessment.allow();
    }
}