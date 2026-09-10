package com.campuspilot.policy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Policy;
import com.campuspilot.entity.PolicyCondition;
import com.campuspilot.entity.PolicyVersion;
import com.campuspilot.mapper.PolicyConditionMapper;
import com.campuspilot.mapper.PolicyMapper;
import com.campuspilot.mapper.PolicyVersionMapper;
import com.campuspilot.vo.PolicyConditionVO;
import com.campuspilot.vo.PolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {
    
    private final PolicyMapper policyMapper;
    private final PolicyVersionMapper versionMapper;
    private final PolicyConditionMapper conditionMapper;
    private final PolicyVersionService policyVersionService;
    
    @Override
    public List<PolicyVO> getAllPolicies() {
        List<Policy> policies = policyMapper.selectList(
            new LambdaQueryWrapper<Policy>()
                .eq(Policy::getStatus, "ACTIVE")
                .eq(Policy::getIsDeleted, 0)
                .orderByDesc(Policy::getCreatedAt)
        );
        
        return policies.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    @Override
    public PolicyVO getPolicyById(Long id) {
        Policy policy = policyMapper.selectById(id);
        if (policy == null || policy.getIsDeleted() == 1) {
            return null;
        }
        return convertToVO(policy);
    }
    
    @Override
    public List<PolicyVO> searchPolicies(String query, String category, int topK) {
        LambdaQueryWrapper<Policy> wrapper = new LambdaQueryWrapper<Policy>()
            .eq(Policy::getStatus, "ACTIVE")
            .eq(Policy::getIsDeleted, 0);
        
        if (query != null && !query.isEmpty()) {
            wrapper.and(w -> w
                .like(Policy::getTitle, query)
                .or()
                .like(Policy::getDescription, query)
            );
        }
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Policy::getCategory, category);
        }
        
        wrapper.orderByDesc(Policy::getCreatedAt);
        wrapper.last("LIMIT " + topK);
        
        List<Policy> policies = policyMapper.selectList(wrapper);
        
        return policies.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    private PolicyVO convertToVO(Policy policy) {
        // 获取当前有效版本（版本管理统一走 PolicyVersionService）
        PolicyVersion currentVersion = policyVersionService.getCurrentVersion(policy.getId());
        
        List<PolicyConditionVO> conditions = new ArrayList<>();
        if (currentVersion != null) {
            conditions = getConditions(currentVersion.getId());
        }
        
        return PolicyVO.builder()
            .id(policy.getId())
            .title(policy.getTitle())
            .category(policy.getCategory())
            .department(policy.getDepartment())
            .description(policy.getDescription())
            .status(policy.getStatus())
            .createdAt(policy.getCreatedAt())
            .updatedAt(policy.getUpdatedAt())
            .currentVersion(currentVersion != null ? currentVersion.getVersion() : null)
            .currentContent(currentVersion != null ? currentVersion.getContent() : null)
            .effectiveDate(currentVersion != null ? currentVersion.getEffectiveDate() : null)
            .expiryDate(currentVersion != null ? currentVersion.getExpiryDate() : null)
            .source(currentVersion != null ? currentVersion.getSource() : null)
            .conditions(conditions)
            .build();
    }
    
    private List<PolicyConditionVO> getConditions(Long policyVersionId) {
        List<PolicyCondition> conditions = conditionMapper.selectList(
            new LambdaQueryWrapper<PolicyCondition>()
                .eq(PolicyCondition::getPolicyVersionId, policyVersionId)
                .eq(PolicyCondition::getIsDeleted, 0)
        );
        
        return conditions.stream()
            .map(condition -> PolicyConditionVO.builder()
                .id(condition.getId())
                .conditionName(condition.getConditionName())
                .conditionType(condition.getConditionType())
                .conditionValue(condition.getConditionValue())
                .operator(condition.getOperator())
                .description(condition.getDescription())
                .build())
            .collect(Collectors.toList());
    }
}