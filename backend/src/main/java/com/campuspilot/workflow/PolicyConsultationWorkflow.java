package com.campuspilot.workflow;

import com.campuspilot.agent.model.ExecutionStep;
import com.campuspilot.rag.RAGService;
import com.campuspilot.rag.RetrievalQuery;
import com.campuspilot.rag.RetrievalResult;
import com.campuspilot.rag.RetrievedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 政策咨询工作流
 * 流程：意图识别 -> 知识库检索(有效期过滤) -> 政策来源引用 -> 答案生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyConsultationWorkflow implements AgentWorkflow {

    private final RAGService ragService;

    @Override
    public String id() {
        return "policy_consultation";
    }

    @Override
    public String name() {
        return "政策咨询工作流";
    }

    @Override
    public String description() {
        return "检索政策知识库并生成带来源引用与有效期说明的回答";
    }

    @Override
    public String supportsIntent() {
        return "POLICY_QUERY";
    }

    @Override
    public WorkflowResult execute(WorkflowContext context) {
        List<ExecutionStep> steps = new ArrayList<>();
        int seq = 1;

        // 1. 知识库检索(RAG，自动过滤过期/未生效政策)
        String category = WorkflowSupport.inferCategory(context.getMessage());
        long t0 = System.currentTimeMillis();
        RetrievalResult retrieval = ragService.retrieve(
            RetrievalQuery.builder()
                .query(context.getMessage())
                .intent(context.getIntent())
                .category(category)
                .userRole(context.getRole())
                .topK(5)
                .build()
        );
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("RETRIEVAL")
            .name("知识库检索(RAG)")
            .status(retrieval.isEmpty() ? "FAILED" : "SUCCESS")
            .duration(System.currentTimeMillis() - t0)
            .output(Map.of(
                "provider", retrieval.getProvider(),
                "hits", retrieval.getDocuments().size(),
                "note", retrieval.getNote()
            ))
            .build());

        // 2. 决策：推荐最相关政策
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("category", category != null ? category : "未指定");
        decision.put("provider", retrieval.getProvider());
        decision.put("totalEffectivePolicies", retrieval.getTotalCount());
        steps.add(ExecutionStep.builder()
            .step(seq++)
            .type("DECISION")
            .name("政策筛选")
            .status(retrieval.isEmpty() ? "FAILED" : "SUCCESS")
            .duration(10)
            .output(decision)
            .build());

        // 3. 答案生成
        String message;
        if (retrieval.isEmpty()) {
            message = "抱歉，我没有检索到相关有效政策。\n\n"
                + "可能原因：\n"
                + "- 政策名称不存在或分类不匹配\n"
                + "- 相关政策已过期或不在有效期\n\n"
                + "你可以换一种说法，例如「查询奖学金政策」「请假怎么办理」。";
        } else {
            List<RetrievedDocument> docs = retrieval.getDocuments();
            StringBuilder sb = new StringBuilder("根据知识库检索结果，为你找到")
                .append(docs.size()).append("条相关政策：\n\n");
            for (int i = 0; i < docs.size(); i++) {
                RetrievedDocument doc = docs.get(i);
                sb.append("📌 ").append(i + 1).append(". ")
                    .append(WorkflowSupport.renderPolicyDocument(doc));
                if (doc.getEffectiveDate() != null && doc.getExpiryDate() != null) {
                    sb.append("（生效期：").append(doc.getEffectiveDate()).append(" 至 ")
                        .append(doc.getExpiryDate()).append("）");
                }
                sb.append("\n\n");
            }
            if (docs.size() == 1) {
                sb.append("如需判断你是否符合申请条件，可以告诉我「帮我判断是否符合申请条件」，我通过规则引擎为你判断。");
            }
            message = sb.toString().trim();
        }
        steps.add(ExecutionStep.builder()
            .step(seq)
            .type("RESPONSE")
            .name("回答生成")
            .status("SUCCESS")
            .duration(50)
            .build());

        return WorkflowResult.builder()
            .status(retrieval.isEmpty() ? "SUCCESS_WITH_NO_HIT" : "SUCCESS")
            .workflowId(id())
            .workflowName(name())
            .message(message)
            .citations(ragService.toCitations(retrieval))
            .toolCalls(new ArrayList<>())
            .steps(steps)
            .decisions(decision)
            .retrieval(retrieval)
            .build();
    }
}