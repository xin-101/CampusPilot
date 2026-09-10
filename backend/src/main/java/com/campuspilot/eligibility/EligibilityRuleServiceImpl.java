package com.campuspilot.eligibility;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityRuleServiceImpl implements EligibilityRuleService {

    private final EligibilityRuleMapper eligibilityRuleMapper;

    @Override
    public List<EligibilityRule> getEnabledRulesByCategory(String category) {
        if (category == null || category.isBlank()) {
            return getAllEnabledRules();
        }
        return eligibilityRuleMapper.selectList(
            new LambdaQueryWrapper<EligibilityRule>()
                .eq(EligibilityRule::getCategory, category)
                .eq(EligibilityRule::getEnabled, 1)
                .eq(EligibilityRule::getIsDeleted, 0)
                .orderByAsc(EligibilityRule::getWeight)
        );
    }

    @Override
    public List<EligibilityRule> getEnabledRulesByPolicy(Long policyId) {
        if (policyId == null) {
            return getAllEnabledRules();
        }
        return eligibilityRuleMapper.selectList(
            new LambdaQueryWrapper<EligibilityRule>()
                .eq(EligibilityRule::getSourcePolicyId, policyId)
                .eq(EligibilityRule::getEnabled, 1)
                .eq(EligibilityRule::getIsDeleted, 0)
                .orderByAsc(EligibilityRule::getWeight)
        );
    }

    @Override
    public List<EligibilityRule> getAllEnabledRules() {
        return eligibilityRuleMapper.selectList(
            new LambdaQueryWrapper<EligibilityRule>()
                .eq(EligibilityRule::getEnabled, 1)
                .eq(EligibilityRule::getIsDeleted, 0)
                .orderByAsc(EligibilityRule::getCategory)
                .orderByAsc(EligibilityRule::getWeight)
        );
    }
}