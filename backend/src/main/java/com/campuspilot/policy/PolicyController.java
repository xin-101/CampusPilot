package com.campuspilot.policy;

import com.campuspilot.common.result.ApiResponse;
import com.campuspilot.vo.PolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {
    
    private final PolicyService policyService;
    
    @GetMapping
    public ApiResponse<List<PolicyVO>> getAllPolicies() {
        List<PolicyVO> policies = policyService.getAllPolicies();
        return ApiResponse.success(policies);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<PolicyVO> getPolicyById(@PathVariable Long id) {
        PolicyVO policy = policyService.getPolicyById(id);
        if (policy == null) {
            return ApiResponse.error(404, "政策不存在");
        }
        return ApiResponse.success(policy);
    }
    
    @PostMapping("/search")
    public ApiResponse<List<PolicyVO>> searchPolicies(@RequestBody PolicySearchRequest request) {
        List<PolicyVO> policies = policyService.searchPolicies(
            request.getQuery(), 
            request.getCategory(), 
            request.getTopK() > 0 ? request.getTopK() : 5
        );
        return ApiResponse.success(policies);
    }
}