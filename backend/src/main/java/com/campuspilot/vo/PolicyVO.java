package com.campuspilot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyVO {
    
    private Long id;
    private String title;
    private String category;
    private String department;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 当前有效版本信息
    private String currentVersion;
    private String currentContent;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String source;
    private List<PolicyConditionVO> conditions;
}