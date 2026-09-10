package com.campuspilot.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资格判断结构化结果
 * status:
 * - ELIGIBLE: 全部规则通过
 * - NOT_ELIGIBLE: 存在必选规则不满足
 * - CONDITIONALLY_ELIGIBLE: 必选通过，但存在可选规则不满足
 * - INSUFFICIENT_DATA: 关键字段缺失，无法判断
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResult {

    /** 最终是否具备资格 */
    private boolean eligible;

    /** 判断状态 */
    private String status;

    /** 加权匹配得分(0-100) */
    private double score;

    /** 匹配成功的规则 */
    private List<RuleCheck> matchedRules;

    /** 匹配失败的规则 */
    private List<RuleCheck> failedRules;

    /** 缺失条件(学生字段缺失) */
    private List<String> missingConditions;

    /** 学生画像证据(DEMO) */
    private Map<String, Object> evidence;

    /** 政策来源说明 */
    private List<String> policySources;

    /** 人类可读解释 */
    private String explanation;

    /** 是否基于演示规则/演示数据 */
    private boolean demo;

    /** 判断时间 */
    private LocalDateTime checkedAt;

    public static EligibilityResult empty() {
        return EligibilityResult.builder()
            .eligible(false)
            .status("INSUFFICIENT_DATA")
            .matchedRules(new ArrayList<>())
            .failedRules(new ArrayList<>())
            .missingConditions(new ArrayList<>())
            .demo(true)
            .checkedAt(LocalDateTime.now())
            .build();
    }
}