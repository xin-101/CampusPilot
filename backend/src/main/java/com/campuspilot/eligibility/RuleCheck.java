package com.campuspilot.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条规则评估结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleCheck {

    private String ruleId;

    private String name;

    /** 学生字段 */
    private String field;

    /** 运算符 */
    private String operator;

    /** 期望值 */
    private String expectedValue;

    /** 实际值 */
    private String actualValue;

    /** 是否通过 */
    private boolean passed;

    /** 是否必选 */
    private boolean required;

    /** 权重 */
    private int weight;

    /** 备注(如"字段缺失") */
    private String message;
}