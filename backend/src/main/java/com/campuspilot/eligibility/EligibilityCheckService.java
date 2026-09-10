package com.campuspilot.eligibility;

/**
 * 资格判断服务：政策检索 -> 学生画像 -> 规则引擎 -> 解释
 */
public interface EligibilityCheckService {

    /**
     * 判断当前登录学生是否符合某政策/分类的申请资格
     * @param policyName 政策名称(可选，用于检索定位政策来源)
     * @param category 政策分类(必填: SCHOLARSHIP/AID/...)
     * @param policyId 政策ID(可选，优先)
     */
    EligibilityResult check(String policyName, String category, Long policyId);
}