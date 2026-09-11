package com.campuspilot.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName("students")
public class Student {
    
    @TableId(type = IdType.AUTO)
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
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Long createdBy;
    
    private Long updatedBy;
    
    @TableLogic
    private Integer isDeleted;
}