package com.campuspilot.agent.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent消息安全检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAssessment {

    /** 是否放行 */
    private boolean allowed;

    /** 拒绝原因(中文,可向用户说明的安全边界) */
    private String reason;

    /** 拒绝类别 */
    private String category;

    /** 匹配到的违规模式片段 */
    private String matchedPattern;

    /** 建议的意图(可选) */
    private String intent;

    public static SecurityAssessment allow() {
        return SecurityAssessment.builder().allowed(true).category("ALLOWED").build();
    }

    public static SecurityAssessment deny(String category, String matchedPattern, String reason) {
        return SecurityAssessment.builder()
            .allowed(false)
            .category(category)
            .matchedPattern(matchedPattern)
            .reason(reason)
            .build();
    }
}