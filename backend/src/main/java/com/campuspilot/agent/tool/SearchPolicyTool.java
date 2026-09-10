package com.campuspilot.agent.tool;

import com.campuspilot.policy.PolicyService;
import com.campuspilot.vo.PolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchPolicyTool implements AgentTool {
    
    private final PolicyService policyService;
    
    @Override
    public String getName() {
        return "search_policy";
    }
    
    @Override
    public String getDescription() {
        return "搜索政策知识库";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params, String userToken) {
        try {
            String query = (String) params.get("query");
            String category = (String) params.get("category");
            int topK = params.containsKey("topK") ? 
                Integer.parseInt(params.get("topK").toString()) : 5;
            
            List<PolicyVO> policies = policyService.searchPolicies(query, category, topK);
            
            return ToolResult.success(Map.of("policies", policies));
            
        } catch (Exception e) {
            return ToolResult.error("搜索政策失败: " + e.getMessage(), "TOOL_CALL_FAILED");
        }
    }
}