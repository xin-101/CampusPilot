package com.campuspilot.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {
    
    private Long policyId;
    private String policyName;
    private String version;
    private String source;
    private LocalDate effectiveDate;
    private double relevance;
    private String excerpt;
}