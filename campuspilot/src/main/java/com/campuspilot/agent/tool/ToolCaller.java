package com.campuspilot.agent.tool;

import com.campuspilot.agent.model.ToolCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具调用器：负责执行工具并记录 ToolCall(含耗时/状态/输入输出)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolCaller {

    private final ToolRegistry toolRegistry;

    /**
     * 执行工具(由当前JWT SecurityContext提供身份上下文)
     */
    public ToolCall execute(String toolName, Map<String, Object> params) {
        long t0 = System.currentTimeMillis();
        AgentTool tool = toolRegistry.getTool(toolName);
        ToolCall.ToolCallBuilder builder = ToolCall.builder()
            .toolName(toolName)
            .params(params);

        if (tool == null) {
            return builder
                .status("FAILED")
                .error("工具未注册: " + toolName)
                .duration(System.currentTimeMillis() - t0)
                .build();
        }

        try {
            ToolResult result = tool.execute(params, null);
            return builder
                .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                .result(result.getData())
                .error(result.isSuccess() ? null : result.getMessage())
                .duration(System.currentTimeMillis() - t0)
                .build();
        } catch (Exception e) {
            log.error("工具调用异常: {}", toolName, e);
            return builder
                .status("FAILED")
                .error(e.getMessage())
                .duration(System.currentTimeMillis() - t0)
                .build();
        }
    }
}