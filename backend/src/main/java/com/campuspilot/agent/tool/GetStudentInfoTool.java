package com.campuspilot.agent.tool;

import com.campuspilot.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
    
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
            // 获取当前用户信息
            String username = jwtTokenProvider.getUsernameFromToken(userToken);
            String role = jwtTokenProvider.getRoleFromToken(userToken);
            
            // 如果是学生，只能查询自己的信息
            if ("STUDENT".equals(role)) {
                StudentVO student = studentService.getCurrentStudent();
                if (student == null) {
                    return ToolResult.error("未找到学生信息", "STUDENT_NOT_FOUND");
                }
                return ToolResult.success(student);
            }
            
            // 如果是管理员或辅导员，可以查询指定学生
            if (params.containsKey("studentId")) {
                Long studentId = Long.parseLong(params.get("studentId").toString());
                StudentVO student = studentService.getStudentByUserId(studentId);
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