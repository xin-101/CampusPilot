package com.campuspilot.policy;

import com.campuspilot.vo.PolicyVO;

import java.util.List;

public interface PolicyService {
    
    List<PolicyVO> getAllPolicies();
    
    PolicyVO getPolicyById(Long id);
    
    List<PolicyVO> searchPolicies(String query, String category, int topK);
}