package com.campuspilot.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * RAG检索查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalQuery {

    private String query;

    /** 可选意图(ELIGIBILITY_CHECK/POLICY_QUERY/TASK_CREATE等) */
    private String intent;

    /** 可选政策分类(SCHOLARSHIP/AID/LEAVE/EXAMINATION/DORMITORY/CERTIFICATE) */
    private String category;

    /** 检索时有效日期(默认当前日期，用于过滤过期政策) */
    private LocalDate effectiveDate;

    /** 当前用户角色(用于权限门控) */
    private String userRole;

    /** 返回条数 */
    private int topK;

    public int getTopK() {
        return topK > 0 ? topK : 5;
    }

    public LocalDate resolveEffectiveDate() {
        return effectiveDate != null ? effectiveDate : LocalDate.now();
    }
}