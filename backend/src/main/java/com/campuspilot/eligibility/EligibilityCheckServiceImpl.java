package com.campuspilot.eligibility;

import com.campuspilot.policy.PolicyService;
import com.campuspilot.rag.RAGService;
import com.campuspilot.rag.RetrievalQuery;
import com.campuspilot.rag.RetrievalResult;
import com.campuspilot.rag.RetrievedDocument;
import com.campuspilot.vo.PolicyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资格判断服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EligibilityCheckServiceImpl implements EligibilityCheckService {

    private final StudentProfileService studentProfileService;
    private final EligibilityRuleEngine ruleEngine;
    private final RAGService ragService;
    private final PolicyService policyService;

    @Override
    public EligibilityResult check(String policyName, String category, Long policyId) {
        // 1. 政策来源定位(通过RAG检索，保证只使用有效期内政策)
        String resolvedCategory = category;
        List<String> sources = new ArrayList<>();

        if (policyId != null) {
            PolicyVO policy = policyService.getPolicyById(policyId);
            if (policy != null && "ACTIVE".equals(policy.getStatus())) {
                resolvedCategory = policy.getCategory();
                sources.add(policy.getTitle());
            }
        }

        if (sources.isEmpty() && (policyName != null || category != null)) {
            RetrievalResult result = ragService.retrieve(
                RetrievalQuery.builder()
                    .query(policyName != null && !policyName.isBlank() ? policyName : category)
                    .category(resolvedCategory)
                    .topK(3)
                    .build()
            );
            if (!result.isEmpty()) {
                List<RetrievedDocument> docs = result.getDocuments();
                RetrievedDocument best = docs.get(0);
                resolvedCategory = best.getCategory();
                sources.addAll(docs.stream()
                    .map(RetrievedDocument::getPolicyName)
                    .limit(2)
                    .collect(Collectors.toList()));
            }
        }

        // 2. 学生画像(来自SecurityContext，DEMO数据)
        StudentProfile student = studentProfileService.buildCurrentStudentProfile();
        if (student == null) {
            EligibilityResult empty = EligibilityResult.empty();
            empty.setExplanation("未找到当前登录学生的信息，无法判断资格。");
            return empty;
        }

        // 3. 规则引擎判断(确定性)
        EligibilityResult result = ruleEngine.evaluate(student, resolvedCategory);
        result.setPolicySources(sources);
        if (sources.isEmpty()) {
            result.setExplanation("未找到相关政策，请确认政策名称或分类是否正确。\n" + result.getExplanation());
        }
        log.info("EligibilityCheck: student={}, category={}, status={}",
            student.getStudentId(), resolvedCategory, result.getStatus());
        return result;
    }
}