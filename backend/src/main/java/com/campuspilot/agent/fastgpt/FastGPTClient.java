package com.campuspilot.agent.fastgpt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FastGPT HTTP 客户端抽象
 * 统一承载：调用前配置校验、超时/重试、日志脱敏、错误转换。
 * 注意：本机暂无真实 SF-FastGPT 环境与 API 文档，
 * 所有调用方法当未配置时明确抛出 FastGPTException，
 * 绝不伪造成功响应。真实调用逻辑待接入环境后按文档补齐。
 */
@Slf4j
@Component
public class FastGPTClient {

    private final FastGPTProperties properties;
    private final FastGPTConnectionChecker connectionChecker;

    public FastGPTClient(FastGPTProperties properties, FastGPTConnectionChecker connectionChecker) {
        this.properties = properties;
        this.connectionChecker = connectionChecker;
    }

    /**
     * 调用槽位：对话
     */
    public FastGPTResponse chat(FastGPTRequest request) {
        return requireEnvironment("chat");
    }

    /**
     * 调用槽位：知识库检索
     */
    public FastGPTResponse searchKnowledge(String query, int topK) {
        return requireEnvironment("knowledge/search");
    }

    /**
     * 调用槽位：工作流运行
     */
    public FastGPTResponse runWorkflow(String workflowId, Object inputs) {
        return requireEnvironment("workflow/run");
    }

    private FastGPTResponse requireEnvironment(String operation) {
        FastGPTConnectionChecker.Status status = connectionChecker.check();
        if (status == FastGPTConnectionChecker.Status.NOT_CONFIGURED) {
            throw new FastGPTException(
                "FastGPT未配置(缺少 url/apiKey)，无法执行 [" + operation + "]，当前状态: NOT_CONFIGURED");
        }
        throw new FastGPTException(
            "FastGPT [" + operation + "] 调用未实现：WAITING_FOR_FASTGPT_ENVIRONMENT。"
                + "需SF-FastGPT环境与API文档后按文档实现，禁止伪造响应。");
    }

    /**
     * 日志脱敏工具：apiKey只保留后4位
     */
    public String maskApiKey() {
        String key = properties.getApiKey();
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }
}