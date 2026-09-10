package com.campuspilot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyConditionVO {
    
    private Long id;
    private String conditionName;
    private String conditionType;
    private String conditionValue;
    private String operator;
    private String description;
}