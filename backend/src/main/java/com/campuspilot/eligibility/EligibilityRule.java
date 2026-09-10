package com.campuspilot.eligibility;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资格规则(演示规则)
 * ruleId 前缀 DEMO_RULE 表示演示规则，仅用于演示，非真实校规。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("eligibility_rules")
public class EligibilityRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务规则编码 */
    private String ruleId;

    /** 规则名称 */
    private String name;

    /** 政策分类: SCHOLARSHIP/AID/LEAVE/EXAMINATION/DORMITORY/CERTIFICATE */
    private String category;

    /** 学生字段: GRADE(年级)/GPA(绩点)/RANK(排名百分比)/STATUS(学籍)/HARDSHIP(经济困难) */
    private String fieldName;

    /** 运算符: EQ/GT/GTE/LT/LTE/IN/CONTAINS */
    private String operator;

    /** 期望值 */
    private String expectedValue;

    /** 权重 */
    private Integer weight;

    /** 是否必选(必选条件不满足则视为不符) */
    private Integer required;

    /** 规则说明 */
    private String description;

    /** 是否启用 */
    private Integer enabled;

    /** 规则版本 */
    private String version;

    /** 来源政策ID */
    private Long sourcePolicyId;

    /** 是否演示规则 */
    private Integer isDemo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}