package com.campuspilot.agent.tool;

import java.util.Map;

public interface AgentTool {
    
    String getName();
    
    String getDescription();
    
    ToolResult execute(Map<String, Object> params, String userToken);
}