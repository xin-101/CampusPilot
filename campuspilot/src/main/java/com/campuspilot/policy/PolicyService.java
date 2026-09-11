package com.campuspilot.policy;

import com.campuspilot.vo.PolicyVO;

import java.util.List;

public interface PolicyService {
    
    List<PolicyVO> getAllPolicies();
    
    PolicyVO getPolicyById(Long id);
    
    List<PolicyVO> searchPolicies(String query, String category, int topK);

    /**
     * 将知识库文档(DEMO_POLICY, id>=10000)标题回映射到 DB 政策：
     * 同分类内按最长公共连续子串匹配 ACTIVE 政策，长度>=6 才算命中。
     * @param policyName 知识库文档标题（如 国家奖学金管理办法（DEMO_POLICY））
     * @param category 分类（SCHOLARSHIP 等）
     * @return DB 政策ID；未命中返回 null
     */
    Long resolveDbPolicyIdByName(String policyName, String category);
}