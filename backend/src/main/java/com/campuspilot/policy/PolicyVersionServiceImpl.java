package com.campuspilot.policy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Policy;
import com.campuspilot.entity.PolicyVersion;
import com.campuspilot.mapper.PolicyMapper;
import com.campuspilot.mapper.PolicyVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyVersionServiceImpl implements PolicyVersionService {

    private final PolicyMapper policyMapper;
    private final PolicyVersionMapper versionMapper;

    @Override
    public PolicyVersion getCurrentVersion(Long policyId) {
        return versionMapper.selectOne(
            new LambdaQueryWrapper<PolicyVersion>()
                .eq(PolicyVersion::getPolicyId, policyId)
                .eq(PolicyVersion::getStatus, "ACTIVE")
                .le(PolicyVersion::getEffectiveDate, LocalDate.now())
                .and(w -> w.isNull(PolicyVersion::getExpiryDate)
                    .or()
                    .ge(PolicyVersion::getExpiryDate, LocalDate.now()))
                .orderByDesc(PolicyVersion::getVersion)
                .last("LIMIT 1")
        );
    }

    @Override
    public List<PolicyVersion> getVersionHistory(Long policyId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<PolicyVersion>()
                .eq(PolicyVersion::getPolicyId, policyId)
                .orderByDesc(PolicyVersion::getVersion)
        );
    }

    @Override
    public boolean isEffective(PolicyVersion version, LocalDate date) {
        if (version == null) {
            return false;
        }
        LocalDate ref = date != null ? date : LocalDate.now();
        boolean statusOk = "ACTIVE".equals(version.getStatus());
        boolean effectiveOk = version.getEffectiveDate() != null && !version.getEffectiveDate().isAfter(ref);
        boolean notExpired = version.getExpiryDate() == null || !version.getExpiryDate().isBefore(ref);
        return statusOk && effectiveOk && notExpired;
    }

    @Override
    public boolean isPolicyEffective(Long policyId, LocalDate date) {
        Policy policy = policyMapper.selectById(policyId);
        if (policy == null || policy.getIsDeleted() == 1 || !"ACTIVE".equals(policy.getStatus())) {
            return false;
        }
        PolicyVersion current = getCurrentVersion(policyId);
        return isEffective(current, date);
    }

    @Override
    public List<PolicyVersion> listEffectiveVersions(LocalDate date) {
        List<Policy> activePolicies = policyMapper.selectList(
            new LambdaQueryWrapper<Policy>()
                .eq(Policy::getStatus, "ACTIVE")
                .eq(Policy::getIsDeleted, 0)
        );
        if (activePolicies.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> policyIds = activePolicies.stream().map(Policy::getId).collect(Collectors.toList());
        List<PolicyVersion> versions = versionMapper.selectList(
            new LambdaQueryWrapper<PolicyVersion>()
                .in(PolicyVersion::getPolicyId, policyIds)
                .eq(PolicyVersion::getStatus, "ACTIVE")
                .le(PolicyVersion::getEffectiveDate, date != null ? date : LocalDate.now())
                .and(w -> w.isNull(PolicyVersion::getExpiryDate)
                    .or()
                    .ge(PolicyVersion::getExpiryDate, date != null ? date : LocalDate.now()))
        );
        return versions;
    }
}