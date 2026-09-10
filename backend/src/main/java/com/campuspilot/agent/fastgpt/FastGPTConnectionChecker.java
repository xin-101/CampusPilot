package com.campuspilot.agent.fastgpt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FastGPT 连接检查器
 * 诚实上报连接状态，未配置时返回 NOT_CONFIGURED。
 * 不允许在缺少真实 SF-FastGPT 环境/API 文档时伪造"连接成功"。
 */
@Slf4j
@Component
public class FastGPTConnectionChecker {

    public enum Status {
        /** 未配置(无url/apiKey) */
        NOT_CONFIGURED,
        /** 已配置但未在真实环境验证（等待 FastGPT 环境） */
        CONFIGURED_WAITING_ENVIRONMENT,
        /** 已连接 */
        CONNECTED
    }

    private final FastGPTProperties properties;

    public FastGPTConnectionChecker(FastGPTProperties properties) {
        this.properties = properties;
    }

    /**
     * 检查状态。未配置 -> NOT_CONFIGURED；
     * 已配置 -> 返回 CONFIGURED_WAITING_ENVIRONMENT（暂不发起未经验证的HTTP调用）。
     */
    public Status check() {
        boolean hasUrl = properties.getUrl() != null && !properties.getUrl().isBlank();
        boolean hasKey = properties.getApiKey() != null && !properties.getApiKey().isBlank();
        if (!hasUrl || !hasKey) {
            return Status.NOT_CONFIGURED;
        }
        // TODO: 根据真实 SF-FastGPT API 文档实现连通性探测（如 /api/v1/... 健康接口），
        //       在真实环境联调时替换该分支并返回 CONNECTED。
        return Status.CONFIGURED_WAITING_ENVIRONMENT;
    }

    public String statusText() {
        Status s = check();
        switch (s) {
            case NOT_CONFIGURED:
                return "NOT_CONFIGURED";
            case CONFIGURED_WAITING_ENVIRONMENT:
                return "WAITING_FOR_FASTGPT_ENVIRONMENT";
            default:
                return "CONNECTED";
        }
    }
}