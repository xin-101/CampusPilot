package com.campuspilot.agent.tool;

import com.campuspilot.task.TaskService;
import com.campuspilot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateTodoTool implements AgentTool {
    
    private final TaskService taskService;
    
    @Override
    public String getName() {
        return "create_todo";
    }
    
    @Override
    public String getDescription() {
        return "创建待办事项";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params, String userToken) {
        try {
            String title = (String) params.get("title");
            String description = (String) params.get("description");
            String deadline = (String) params.get("deadline");
            Long relatedPolicyId = params.containsKey("relatedPolicyId") ? 
                Long.parseLong(params.get("relatedPolicyId").toString()) : null;
            
            TaskVO task = taskService.createTask(title, description, deadline, relatedPolicyId);
            
            return ToolResult.success(Map.of(
                "taskId", task.getId(),
                "message", "待办创建成功"
            ));
            
        } catch (Exception e) {
            return ToolResult.error("创建待办失败: " + e.getMessage(), "TOOL_CALL_FAILED");
        }
    }
}