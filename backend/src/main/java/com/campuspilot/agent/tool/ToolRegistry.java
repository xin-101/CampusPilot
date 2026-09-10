package com.campuspilot.agent.tool;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ToolRegistry {
    
    private final Map<String, AgentTool> tools = new HashMap<>();
    
    @Autowired
    private List<AgentTool> toolList;
    
    @PostConstruct
    public void init() {
        for (AgentTool tool : toolList) {
            tools.put(tool.getName(), tool);
            log.info("Registered tool: {}", tool.getName());
        }
    }
    
    public AgentTool getTool(String name) {
        return tools.get(name);
    }
    
    public List<AgentTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
}