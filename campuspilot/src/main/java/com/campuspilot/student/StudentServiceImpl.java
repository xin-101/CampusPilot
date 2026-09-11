package com.campuspilot.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Student;
import com.campuspilot.entity.User;
import com.campuspilot.mapper.StudentMapper;
import com.campuspilot.mapper.UserMapper;
import com.campuspilot.security.SecurityUtils;
import com.campuspilot.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    
    @Override
    public StudentVO getCurrentStudent() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return null;
        }
        
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)
        );
        
        if (user == null) {
            return null;
        }
        
        return getStudentByUserId(user.getId());
    }
    
    @Override
    public StudentVO getStudentByUserId(Long userId) {
        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId)
                .eq(Student::getIsDeleted, 0)
        );
        
        if (student == null) {
            return null;
        }
        
        return convertToVO(student);
    }
    
    @Override
    public StudentVO getStudentByStudentId(String studentId) {
        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentId, studentId)
                .eq(Student::getIsDeleted, 0)
        );
        
        if (student == null) {
            return null;
        }
        
        return convertToVO(student);
    }
    
    private StudentVO convertToVO(Student student) {
        return StudentVO.builder()
            .id(student.getId())
            .userId(student.getUserId())
            .studentId(student.getStudentId())
            .name(student.getName())
            .grade(student.getGrade())
            .major(student.getMajor())
            .className(student.getClassName())
            .enrollmentDate(student.getEnrollmentDate())
            .status(student.getStatus())
            .phone(student.getPhone())
            .email(student.getEmail())
            .avatar(student.getAvatar())
            .createdAt(student.getCreatedAt())
            .build();
    }
}