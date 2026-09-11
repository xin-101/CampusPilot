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
@TableName("policy_versions")
public class PolicyVersion {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long policyId;
    
    private String version;
    
    private String content;
    
    private String keywords;
    
    private LocalDate effectiveDate;
    
    private LocalDate expiryDate;
    
    private String source;
    
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Long createdBy;
    
    private Long updatedBy;
    
    @TableLogic
    private Integer isDeleted;
}