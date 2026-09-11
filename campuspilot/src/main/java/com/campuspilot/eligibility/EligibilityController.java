package com.campuspilot.eligibility;

import com.campuspilot.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资格判断接口(供测试/前端展示)
 */
@RestController
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
public class EligibilityController {

    private final EligibilityCheckService eligibilityCheckService;

    @PostMapping("/check")
    public ApiResponse<EligibilityResult> check(@RequestBody(required = false) Map<String, Object> request) {
        String policy = request != null ? asString(request.get("policy")) : null;
        String category = request != null ? asString(request.get("category")) : null;
        Long policyId = request != null && request.get("policyId") != null
            ? Long.parseLong(request.get("policyId").toString()) : null;

        if (category == null && policy == null && policyId == null) {
            return ApiResponse.error(400, "请指定政策分类或政策名称");
        }
        return ApiResponse.success(eligibilityCheckService.check(policy, category, policyId));
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }
}