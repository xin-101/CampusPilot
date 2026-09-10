package com.campuspilot.eligibility;

import java.util.List;

/**
 * 资格规则加载服务
 */
public interface EligibilityRuleService {

    /**
     * 查询某政策分类下启用的规则
     */
    List<EligibilityRule> getEnabledRulesByCategory(String category);

    /**
     * 查询某政策关联的启用规则(source_policy_id 精确匹配)
     */
    List<EligibilityRule> getEnabledRulesByPolicy(Long policyId);

    /**
     * 查询所有启用规则
     */
    List<EligibilityRule> getAllEnabledRules();
}