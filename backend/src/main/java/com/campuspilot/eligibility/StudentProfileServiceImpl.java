package com.campuspilot.eligibility;

import com.campuspilot.student.StudentService;
import com.campuspilot.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;

/**
 * 学生画像服务实现
 * 身份来源：SecurityContext 当前登录用户(Agent无法伪造任意studentId)。
 * 说明：GPA/排名/经济困难为DEMO演示数据(按学号固定映射)，仅用于演示规则判断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentService studentService;

    @Override
    public StudentProfile buildCurrentStudentProfile() {
        StudentVO student = studentService.getCurrentStudent();
        if (student == null) {
            return null;
        }

        int grade = demoGrade(student);
        boolean hardship = "2021002".equals(student.getStudentId());

        return StudentProfile.builder()
            .userId(student.getUserId())
            .studentId(student.getStudentId())
            .name(student.getName())
            .grade(grade)
            .gpa(demoGpa(student.getStudentId()))
            .rank(demoRank(student.getStudentId()))
            .enrollmentStatus(student.getStatus())
            .hardship(hardship)
            .major(student.getMajor())
            .className(student.getClassName())
            .demo(true)
            .build();
    }

    /**
     * 在校年级(DEMO计算方式)：入学年至今的学年数，钳制在1-4。
     */
    private int demoGrade(StudentVO student) {
        int enrollmentYear = 2021;
        if (student.getEnrollmentDate() != null) {
            enrollmentYear = student.getEnrollmentDate().getYear();
        }
        int currentYear = Year.now().getValue();
        int grade = currentYear - enrollmentYear + 1;
        return Math.max(1, Math.min(4, grade));
    }

    private double demoGpa(String studentId) {
        // DEMO数据：仅两个演示学生
        if ("2021002".equals(studentId)) {
            return 3.2;
        }
        return 3.8;
    }

    private int demoRank(String studentId) {
        // DEMO数据：综合测评排名百分比，2021002 前40%，其余前10%
        if ("2021002".equals(studentId)) {
            return 40;
        }
        return 10;
    }
}