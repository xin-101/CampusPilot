package com.campuspilot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentVO {
    
    private Long id;
    private Long userId;
    private String studentId;
    private String name;
    private String grade;
    private String major;
    private String className;
    private LocalDate enrollmentDate;
    private String status;
    private String phone;
    private String email;
    private String avatar;
    private LocalDateTime createdAt;
}