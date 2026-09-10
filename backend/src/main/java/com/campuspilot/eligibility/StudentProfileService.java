package com.campuspilot.eligibility;

/**
 * 学生画像服务：基于当前登录学生(来自JWT SecurityContext)构建规则引擎所需的画像。
 * 不允许从Agent参数接收任意studentId构造画像。
 */
public interface StudentProfileService {

    /**
     * 构建当前登录学生的画像(DEMO数据，isDemo=true)
     */
    StudentProfile buildCurrentStudentProfile();
}