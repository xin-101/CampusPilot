package com.campuspilot.agent.fastgpt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SF-FastGPT 连接配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.fastgpt")
public class FastGPTProperties {

    /** FastGPT 服务地址 */
    private String url = "";

    /** API Key */
    private String apiKey = "";

    /** 知识库ID */
    private String knowledgeId = "";

    /** 工作流ID映射 */
    private WorkflowIds workflowId = new WorkflowIds();

    /** HTTP超时(ms) */
    private long timeout = 30000;

    /** 重试次数 */
    private int retryCount = 3;

    @Data
    public static class WorkflowIds {
        private String policyConsultation = "";
        private String eligibilityCheck = "";
        private String taskCreation = "";
    }

    public boolean isConfigured() {
        return url != null && !url.isBlank();
    }
}