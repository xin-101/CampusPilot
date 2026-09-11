package com.campuspilot.student;

import com.campuspilot.vo.StudentVO;

public interface StudentService {
    
    StudentVO getCurrentStudent();
    
    StudentVO getStudentByUserId(Long userId);
    
    StudentVO getStudentByStudentId(String studentId);
}