package com.campuspilot.rag;

import com.campuspilot.agent.fastgpt.FastGPTClient;
import com.campuspilot.agent.fastgpt.FastGPTConnectionChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * FastGPT 知识库检索器（待接入）
 * 当前状态：WAITING_FOR_FASTGPT_ENVIRONMENT。
 * 未配置环境时返回空结果并明确标注，不做任何伪造检索。
 * Todo: 接入环境后，通过 {@link FastGPTClient#searchKnowledge} 调用 FastGPT 知识库并按文档解析。
 */
@Slf4j
@Component
public class FastGptPolicyRetriever implements PolicyRetriever {

    private final FastGPTConnectionChecker connectionChecker;
    private final FastGPTClient fastGPTClient;

    public FastGptPolicyRetriever(FastGPTConnectionChecker connectionChecker, FastGPTClient fastGPTClient) {
        this.connectionChecker = connectionChecker;
        this.fastGPTClient = fastGPTClient;
    }

    @Override
    public RetrievalResult retrieve(RetrievalQuery query) {
        FastGPTConnectionChecker.Status status = connectionChecker.check();
        switch (status) {
            case NOT_CONFIGURED:
                return RetrievalResult.builder()
                    .provider(providerName())
                    .query(query)
                    .documents(new ArrayList<>())
                    .totalCount(0)
                    .note("WAITING_FOR_FASTGPT_ENVIRONMENT: FastGPT未配置(url/apiKey为空)，未执行知识库检索")
                    .build();
            case CONFIGURED_WAITING_ENVIRONMENT:
                return RetrievalResult.builder()
                    .provider(providerName())
                    .query(query)
                    .documents(new ArrayList<>())
                    .totalCount(0)
                    .note("WAITING_FOR_FASTGPT_ENVIRONMENT: FastGPT已配置但尚无真实联调环境，检索未执行(不伪造结果)")
                    .build();
            default:
                // TODO: 接入真实 FastGPT 知识库检索，按 SF-FastGPT API 文档解析结果
                return RetrievalResult.builder()
                    .provider(providerName())
                    .query(query)
                    .documents(new ArrayList<>())
                    .totalCount(0)
                    .note("WAITING_FOR_FASTGPT_ENVIRONMENT: 知识库检索实现待按FastGPT API文档完成")
                    .build();
        }
    }

    @Override
    public String providerName() {
        return "FASTGPT";
    }
}