package com.campuspilot.rag;

import com.campuspilot.agent.model.Citation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 服务默认实现(本地)
 * 默认使用 KeywordPolicyRetriever；可通过 agent.rag.provider=fastgpt 切换。
 */
@Slf4j
@Service
public class LocalRAGService implements RAGService {

    private final KeywordPolicyRetriever keywordRetriever;
    private final FastGptPolicyRetriever fastGptRetriever;

    @Value("${agent.rag.provider:local}")
    private String provider;

    public LocalRAGService(KeywordPolicyRetriever keywordRetriever, FastGptPolicyRetriever fastGptRetriever) {
        this.keywordRetriever = keywordRetriever;
        this.fastGptRetriever = fastGptRetriever;
    }

    @Override
    public RetrievalResult retrieve(RetrievalQuery query) {
        if (query == null) {
            query = RetrievalQuery.builder().query("").topK(5).build();
        }
        return resolveRetriever().retrieve(query);
    }

    @Override
    public List<Citation> toCitations(RetrievalResult result) {
        if (result == null || result.getDocuments() == null) {
            return new ArrayList<>();
        }
        return result.getDocuments().stream()
            .map(doc -> Citation.builder()
                .policyId(doc.getPolicyId())
                .policyName(doc.getPolicyName())
                .version(doc.getVersion())
                .source(doc.getSource())
                .effectiveDate(doc.getEffectiveDate())
                .relevance(Math.max(0, Math.min(1, doc.getScore())))
                .excerpt(doc.getExcerpt())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public String getProviderName() {
        return resolveRetriever().providerName();
    }

    @Override
    public String getStatus() {
        if ("fastgpt".equalsIgnoreCase(provider)) {
            return "WAITING_FOR_FASTGPT_ENVIRONMENT";
        }
        return "LOCAL_KEYWORD";
    }

    private PolicyRetriever resolveRetriever() {
        if ("fastgpt".equalsIgnoreCase(provider)) {
            return fastGptRetriever;
        }
        return keywordRetriever;
    }
}