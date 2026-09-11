package com.campuspilot.policy;

import com.campuspilot.entity.PolicyVersion;

import java.time.LocalDate;
import java.util.List;

/**
 * 政策版本管理服务
 * 负责：当前版本查询、版本历史、生效期判断。
 */
public interface PolicyVersionService {

    /**
     * 获取政策当前有效版本(状态ACTIVE且生效期内，版本号最大)
     */
    PolicyVersion getCurrentVersion(Long policyId);

    /**
     * 获取政策全部版本历史
     */
    List<PolicyVersion> getVersionHistory(Long policyId);

    /**
     * 判断某版本在指定日期是否生效(状态ACTIVE且 effectiveDate<=date<=expiryDate或expiryDate为空)
     */
    boolean isEffective(PolicyVersion version, LocalDate date);

    /**
     * 判断某政策在指定日期是否有生效版本(存在至少一个有效版本且政策本身ACTIVE)
     */
    boolean isPolicyEffective(Long policyId, LocalDate date);

    /**
     * 查询指定日期生效的所有政策版本(排除过期/失效，返回最新版本)
     */
    List<PolicyVersion> listEffectiveVersions(LocalDate date);
}