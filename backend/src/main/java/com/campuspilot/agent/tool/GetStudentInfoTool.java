package com.campuspilot.agent.tool;

import com.campuspilot.security.SecurityUtils;
import com.campuspilot.student.StudentService;
import com.campuspilot.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetStudentInfoTool implements AgentTool {
    
    private final StudentService studentService;
    
    @Override
    public String getName() {
        return "get_student_info";
    }
    
    @Override
    public String getDescription() {
        return "获取学生基本信息";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params, String userToken) {
        try {
            // 当前登录用户角色（来自JWT SecurityContext，Agent无法伪造）
            String role = SecurityUtils.getCurrentRole();
            
            // 学生只能查询自己的信息（SecurityContext保证，不接受Agent指定的任意ID）
            if (role != null && role.contains("STUDENT")) {
                StudentVO student = studentService.getCurrentStudent();
                if (student == null) {
                    return ToolResult.error("未找到学生信息", "STUDENT_NOT_FOUND");
                }
                return ToolResult.success(student);
            }
            
            // 辅导员/管理员可以查询指定学生
            if (params.containsKey("studentId")) {
                Long userId = Long.parseLong(params.get("studentId").toString());
                StudentVO student = studentService.getStudentByUserId(userId);
                if (student == null) {
                    return ToolResult.error("未找到学生信息", "STUDENT_NOT_FOUND");
                }
                return ToolResult.success(student);
            }
            
            // 默认查询当前学生
            StudentVO student = studentService.getCurrentStudent();
            if (student == null) {
                return ToolResult.error("未找到学生信息", "STUDENT_NOT_FOUND");
            }
            return ToolResult.success(student);
            
        } catch (Exception e) {
            return ToolResult.error("获取学生信息失败: " + e.getMessage(), "TOOL_CALL_FAILED");
        }
    }
}