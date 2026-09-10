package com.campuspilot.student;

import com.campuspilot.common.result.ApiResponse;
import com.campuspilot.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {
    
    private final StudentService studentService;
    
    @GetMapping("/me")
    public ApiResponse<StudentVO> getCurrentStudent() {
        StudentVO student = studentService.getCurrentStudent();
        if (student == null) {
            return ApiResponse.error(404, "未找到当前学生信息");
        }
        return ApiResponse.success(student);
    }
    
    @GetMapping("/profile")
    public ApiResponse<StudentVO> getStudentProfile() {
        StudentVO student = studentService.getCurrentStudent();
        if (student == null) {
            return ApiResponse.error(404, "未找到学生信息");
        }
        return ApiResponse.success(student);
    }
}