package com.campuspilot.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生结构化画像(用于资格规则引擎)
 * 说明：GPA/排名/经济困难等字段为DEMO演示数据，isDemo=true 表示仅用于演示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {

    private Long userId;

    private String studentId;

    private String name;

    /** 在校年级(1=大一 ... 4=大四) */
    private Integer grade;

    /** 绩点(0-4) DEMO */
    private Double gpa;

    /** 综合测评排名百分比(1-100) DEMO */
    private Integer rank;

    /** 学籍状态: ENROLLED/GRADUATED/... */
    private String enrollmentStatus;

    /** 是否家庭经济困难 DEMO */
    private Boolean hardship;

    private String major;

    private String className;

    /** 该画像是否为演示数据 */
    private boolean demo;
}