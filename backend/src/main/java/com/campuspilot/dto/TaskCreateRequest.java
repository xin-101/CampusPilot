package com.campuspilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    private String description;
    
    private String deadline;
    
    private Long relatedPolicyId;
}