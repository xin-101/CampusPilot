package com.campuspilot.agent.provider;

import com.campuspilot.agent.fastgpt.FastGPTClient;
import com.campuspilot.agent.fastgpt.FastGPTException;
import com.campuspilot.agent.fastgpt.FastGPTProperties;
import com.campuspilot.agent.model.*;
import com.campuspilot.agent.security.AgentSecurityGuard;
import com.campuspilot.agent.security.SecurityAssessment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 生产环境Agent提供方 (SF-FastGPT)
 * 当前阶段：环境未配置或API未实现时，返回 WAITING_FOR_FASTGPT_ENVIRONMENT，
 * 绝不伪造成功响应。
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class FastGPTProvider implements AgentProvider {

    private final FastGPTProperties properties;
    private final FastGPTClient client;
    private final AgentSecurityGuard securityGuard;

    @Override
    public AgentResponse chat(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("FastGPT Agent收到消息: {}", request.getMessage());

        // 安全守卫
        SecurityAssessment assessment = securityGuard.inspect(request.getMessage());
        if (!assessment.isAllowed()) {
            return securityDeniedResponse(request, assessment, startTime);
        }

        // 环境检查：未配置直接返回 WAITING_FOR_FASTGPT_ENVIRONMENT
        if (!properties.isConfigured()) {
            log.info("FastGPT未配置(url为空)，返回 WAITING_FOR_FASTGPT_ENVIRONMENT");
            return waitingForEnvironmentResponse(request, startTime,
                "FastGPT服务尚未配置（缺少agent.fastgpt.url）。当前返回等待环境提示，绝不伪造响应。");
        }

        // 尝试调用 client（会抛 FastGPTException）
        try {
            // TODO: 根据实际 SF-FastGPT API 文档实现真实调用
            // 1. client.chat(request) 或 client.runWorkflow(...)
            // 2. 解析 FastGPTResponse → AgentResponse
            // 3. 工具调用：FastGPTFunctionCall → 本系统 ToolCaller
            client.chat(new com.campuspilot.agent.fastgpt.FastGPTRequest());
            throw new UnsupportedOperationException(
                "FastGPT真实调用尚未实现，请提供API文档后补齐。当前环境: " + properties.getUrl());
        } catch (FastGPTException e) {
            return waitingForEnvironmentResponse(request, startTime, e.getMessage());
        } catch (Exception e) {
            log.error("FastGPT调用异常", e);
            return errorResponse(request, startTime, "FastGPT调用失败: " + e.getMessage());
        }
    }

    private AgentResponse waitingForEnvironmentResponse(AgentRequest request, long startTime, String detail) {
        long endTime = System.currentTimeMillis();
        ExecutionStep step = ExecutionStep.builder()
            .step(1)
            .type("ENVIRONMENT")
            .name("环境状态检查")
            .status("WAITING")
            .duration(endTime - startTime)
            .output(Map.of("url", properties.getUrl() == null ? "" : properties.getUrl(),
                "provider", properties.isConfigured() ? "configured" : "NOT_CONFIGURED",
                "apiImplemented", false))
            .build();

        ExecutionTrace trace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent("WAITING_FOR_FASTGPT_ENVIRONMENT")
            .workflow("none")
            .workflowId("fastgpt_environment")
            .workflowName("FastGPT环境等待")
            .steps(List.of(step))
            .toolCalls(new ArrayList<>())
            .citations(new ArrayList<>())
            .decisions(Map.of("provider", "FastGPT", "implemented", false))
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status("WAITING_FOR_FASTGPT_ENVIRONMENT")
            .error(detail)
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response("⚠️ **等待FastGPT环境接入**\n\n"
                + "当前环境暂无SF-FastGPT服务（或API尚未对接）。为保证系统诚信，本智能体不会伪造响应。\n\n"
                + "**当前状态**：\n"
                + "- 环境配置：" + (properties.isConfigured() ? "已配置（url: " + properties.getUrl() + "）" : "未配置") + "\n"
                + "- API实现：待接入后补齐\n"
                + "- RAG检索：使用本地本地政策库（KeywordPolicyRetriever），可正常使用\n"
                + "- 工具调用：学生信息/政策检索/资格判断/任务创建均可正常使用\n\n"
                + "你可以继续提问，系统将以本地检索与规则引擎模式提供服务。\n\n"
                + "_技术说明：_" + detail)
            .intent("WAITING_FOR_FASTGPT_ENVIRONMENT")
            .complexity("SIMPLE")
            .sources(new ArrayList<>())
            .executionSteps(List.of(step))
            .toolCalls(new ArrayList<>())
            .executionTrace(trace)
            .build();
    }

    private AgentResponse securityDeniedResponse(AgentRequest request, SecurityAssessment assessment, long startTime) {
        long endTime = System.currentTimeMillis();
        ExecutionStep guardStep = ExecutionStep.builder()
            .step(1)
            .type("SECURITY")
            .name("安全守卫拦截")
            .status("DENIED")
            .duration(endTime - startTime)
            .output(Map.of("category", assessment.getCategory()))
            .build();

        ExecutionTrace trace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent("DENIED")
            .workflow("none")
            .workflowId("security_guard")
            .workflowName("安全守卫")
            .steps(List.of(guardStep))
            .toolCalls(new ArrayList<>())
            .citations(new ArrayList<>())
            .decisions(Map.of("allowed", false, "category", assessment.getCategory()))
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status("DENIED")
            .error(assessment.getReason())
            .build();

        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response("⚠️ **安全防护提示**\n\n" + assessment.getReason()
                + "\n\n该次请求已由安全守卫拦截，未执行任何工具与数据处理。")
            .intent("DENIED")
            .complexity("SIMPLE")
            .sources(new ArrayList<>())
            .executionSteps(List.of(guardStep))
            .toolCalls(new ArrayList<>())
            .executionTrace(trace)
            .build();
    }

    private AgentResponse errorResponse(AgentRequest request, long startTime, String message) {
        long endTime = System.currentTimeMillis();
        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response("系统异常：" + message)
            .intent("ERROR")
            .complexity("SIMPLE")
            .sources(new ArrayList<>())
            .executionSteps(new ArrayList<>())
            .toolCalls(new ArrayList<>())
            .executionTrace(ExecutionTrace.builder()
                .executionId(UUID.randomUUID().toString())
                .startTime(startTime)
                .endTime(endTime)
                .duration(endTime - startTime)
                .status("ERROR")
                .error(message)
                .build())
            .build();
    }

    @Override
    public boolean healthCheck() {
        return properties.isConfigured();
    }

    @Override
    public String getProviderName() {
        return "FastGPT";
    }
}