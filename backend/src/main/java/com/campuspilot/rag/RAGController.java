package com.campuspilot.rag;

import com.campuspilot.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 检索评测接口
 * 供评测脚本与前端调试直接调用底层检索层（绕过意图路由），
 * 便于验证中间结果：文本向量化 → 相似度 → 过滤 → topK → 重排序。
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;

    @PostMapping("/retrieve")
    public ApiResponse<Map<String, Object>> retrieve(@RequestBody RetrieveRequest request) {
        RetrievalQuery query = RetrievalQuery.builder()
            .query(request.getQuery())
            .category(request.getCategory())
            .intent(request.getIntent())
            .effectiveDate(request.getEffectiveDate())
            .userRole(request.getUserRole() == null ? "STUDENT" : request.getUserRole())
            .topK(request.getTopK() > 0 ? request.getTopK() : 5)
            .build();

        RetrievalResult result = ragService.retrieve(query);

        Map<String, Object> body = new HashMap<>();
        body.put("provider", result.getProvider());
        body.put("totalCount", result.getTotalCount());
        body.put("topK", query.getTopK());
        body.put("note", result.getNote());
        body.put("documents", result.getDocuments().stream()
            .map(doc -> {
                Map<String, Object> m = new HashMap<>();
                m.put("policyId", doc.getPolicyId());
                m.put("policyName", doc.getPolicyName());
                m.put("category", doc.getCategory());
                m.put("version", doc.getVersion());
                m.put("score", doc.getScore());
                m.put("effectiveDate", doc.getEffectiveDate() != null ? doc.getEffectiveDate().toString() : null);
                m.put("expiryDate", doc.getExpiryDate() != null ? doc.getExpiryDate().toString() : null);
                m.put("source", doc.getSource());
                m.put("excerpt", doc.getExcerpt());
                return m;
            })
            .collect(Collectors.toList()));

        return ApiResponse.success(body);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, String>> status() {
        Map<String, String> body = new HashMap<>();
        body.put("provider", ragService.getProviderName());
        body.put("status", ragService.getStatus());
        return ApiResponse.success(body);
    }

    public static class RetrieveRequest {
        private String query;
        private String category;
        private String intent;
        private LocalDate effectiveDate;
        private String userRole;
        private int topK;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
    }
}