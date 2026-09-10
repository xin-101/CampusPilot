package com.campuspilot.agent.provider;

import com.campuspilot.agent.model.*;
import com.campuspilot.agent.tool.AgentTool;
import com.campuspilot.agent.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class MockAgentProvider implements AgentProvider {
    
    private final ToolRegistry toolRegistry;
    
    @Override
    public AgentResponse chat(AgentRequest request) {
        log.info("Mock Agent收到消息: {}", request.getMessage());
        
        // 模拟延迟
        simulateDelay();
        
        // 1. 模拟意图识别
        String intent = mockIntentDetection(request.getMessage());
        log.info("Mock意图识别: {}", intent);
        
        // 2. 模拟复杂度判断
        String complexity = mockComplexityAnalysis(request.getMessage(), intent);
        log.info("Mock复杂度判断: {}", complexity);
        
        // 3. 模拟工具调用
        List<ToolCall> toolCalls = mockToolCalls(intent);
        log.info("Mock工具调用: {}", toolCalls.size());
        
        // 4. 生成模拟响应
        String response = mockResponse(request.getMessage(), intent, complexity, toolCalls);
        log.info("Mock响应: {}", response.substring(0, Math.min(100, response.length())) + "...");
        
        // 5. 生成模拟引用
        List<Citation> sources = mockCitations(intent);
        
        // 6. 生成执行步骤
        List<ExecutionStep> executionSteps = mockExecutionSteps(intent, toolCalls);
        
        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response(response)
            .intent(intent)
            .complexity(complexity)
            .sources(sources)
            .executionSteps(executionSteps)
            .toolCalls(toolCalls)
            .build();
    }
    
    private void simulateDelay() {
        try {
            Thread.sleep(1000 + (long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private String mockIntentDetection(String message) {
        if (message.contains("奖学金")) {
            return "POLICY_QUERY";
        } else if (message.contains("申请") || message.contains("帮我")) {
            return "TASK_CREATE";
        } else if (message.contains("成绩")) {
            return "SCORE_QUERY";
        } else if (message.contains("资格") || message.contains("条件")) {
            return "ELIGIBILITY_CHECK";
        } else if (message.contains("请假")) {
            return "POLICY_QUERY";
        } else if (message.contains("宿舍")) {
            return "POLICY_QUERY";
        }
        return "GENERAL_QUERY";
    }
    
    private String mockComplexityAnalysis(String message, String intent) {
        if (message.contains("帮我") || message.contains("申请") || message.contains("办理")) {
            return "ACTION";
        } else if (message.contains("判断") || message.contains("条件") || message.contains("资格")) {
            return "COMPLEX";
        }
        return "SIMPLE";
    }
    
    private List<ToolCall> mockToolCalls(String intent) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        switch (intent) {
            case "ELIGIBILITY_CHECK":
                toolCalls.add(ToolCall.builder()
                    .toolName("get_student_info")
                    .status("SUCCESS")
                    .duration(500)
                    .build());
                toolCalls.add(ToolCall.builder()
                    .toolName("get_student_scores")
                    .status("SUCCESS")
                    .duration(600)
                    .build());
                toolCalls.add(ToolCall.builder()
                    .toolName("search_policy")
                    .status("SUCCESS")
                    .duration(800)
                    .build());
                break;
                
            case "TASK_CREATE":
                toolCalls.add(ToolCall.builder()
                    .toolName("create_todo")
                    .status("SUCCESS")
                    .duration(400)
                    .build());
                break;
                
            case "SCORE_QUERY":
                toolCalls.add(ToolCall.builder()
                    .toolName("get_student_scores")
                    .status("SUCCESS")
                    .duration(500)
                    .build());
                break;
        }
        
        return toolCalls;
    }
    
    private String mockResponse(String message, String intent, String complexity, List<ToolCall> toolCalls) {
        StringBuilder response = new StringBuilder();
        
        switch (intent) {
            case "POLICY_QUERY":
                if (message.contains("奖学金")) {
                    response.append("根据《2025年国家奖学金管理办法》，国家奖学金申请条件如下：\n\n");
                    response.append("1. **年级要求**：大三或大四在校学生\n");
                    response.append("2. **成绩要求**：综合测评成绩排名在本专业前10%\n");
                    response.append("3. **绩点要求**：绩点不低于3.5\n");
                    response.append("4. **其他要求**：无违纪处分，品德优良\n\n");
                    response.append("**申请时间**：每年9月1日至9月30日\n\n");
                    response.append("**所需材料**：\n");
                    response.append("- 国家奖学金申请表\n");
                    response.append("- 成绩单（教务处盖章）\n");
                    response.append("- 综合测评证明（学院盖章）\n\n");
                    response.append("📎 *来源：教务处官网 2025年9月1日发布*");
                } else {
                    response.append("我来帮你查询相关政策信息。\n\n");
                    response.append("根据知识库查询，相关信息如下：...");
                }
                break;
                
            case "ELIGIBILITY_CHECK":
                response.append("我来帮你判断是否符合申请条件。\n\n");
                response.append("**资格判断结果**：\n\n");
                response.append("✅ **年级要求**：满足（大三学生）\n");
                response.append("✅ **绩点要求**：满足（绩点3.8 ≥ 3.5）\n");
                response.append("✅ **排名要求**：满足（排名前10%）\n");
                response.append("✅ **其他要求**：满足（无违纪处分）\n\n");
                response.append("**结论**：你符合国家奖学金的申请条件！\n\n");
                response.append("**建议**：\n");
                response.append("1. 尽快准备申请材料\n");
                response.append("2. 注意申请截止日期：9月30日\n");
                response.append("3. 可以点击下方按钮创建申请任务");
                break;
                
            case "TASK_CREATE":
                response.append("好的，我来帮你创建申请任务。\n\n");
                response.append("**任务创建成功** ✅\n\n");
                response.append("任务名称：国家奖学金申请\n");
                response.append("截止日期：2025年9月30日\n");
                response.append("优先级：高\n\n");
                response.append("**任务步骤**：\n");
                response.append("1. ✅ 查询奖学金政策\n");
                response.append("2. ⏳ 准备申请材料\n");
                response.append("3. ⏳ 提交申请\n\n");
                response.append("你可以在「我的任务」中查看任务详情。");
                break;
                
            case "SCORE_QUERY":
                response.append("我来查询你的成绩信息。\n\n");
                response.append("**成绩查询结果**：\n\n");
                response.append("| 课程 | 学分 | 成绩 |\n");
                response.append("|------|------|------|\n");
                response.append("| 数据结构 | 4 | 92 (A) |\n");
                response.append("| 操作系统 | 4 | 88 (B+) |\n");
                response.append("| 计算机网络 | 3 | 90 (A-) |\n\n");
                response.append("**总学分**：120\n");
                response.append("**绩点**：3.8\n");
                response.append("**排名**：15/150（前10%）");
                break;
                
            default:
                response.append("你好！我是CampusPilot校园事务智能助手。\n\n");
                response.append("我可以帮你：\n");
                response.append("- 📚 查询校园政策\n");
                response.append("- 🎓 判断申请资格\n");
                response.append("- 📝 办理校园事务\n");
                response.append("- ⏰ 设置提醒通知\n\n");
                response.append("请问有什么可以帮你的？");
        }
        
        return response.toString();
    }
    
    private List<Citation> mockCitations(String intent) {
        if ("POLICY_QUERY".equals(intent)) {
            return Arrays.asList(
                Citation.builder()
                    .policyId(1L)
                    .policyName("2025年国家奖学金管理办法")
                    .version("v1")
                    .source("教务处官网")
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .relevance(0.95)
                    .excerpt("国家奖学金申请条件：...")
                    .build()
            );
        }
        return Collections.emptyList();
    }
    
    private List<ExecutionStep> mockExecutionSteps(String intent, List<ToolCall> toolCalls) {
        List<ExecutionStep> steps = new ArrayList<>();
        
        // 意图识别
        steps.add(ExecutionStep.builder()
            .step(1)
            .type("INTENT")
            .name("意图识别")
            .status("SUCCESS")
            .duration(200)
            .build());
        
        // 知识库检索
        if ("POLICY_QUERY".equals(intent) || "ELIGIBILITY_CHECK".equals(intent)) {
            steps.add(ExecutionStep.builder()
                .step(2)
                .type("KNOWLEDGE")
                .name("知识库检索")
                .status("SUCCESS")
                .duration(500)
                .build());
        }
        
        // 工具调用
        int stepNum = steps.size() + 1;
        for (ToolCall toolCall : toolCalls) {
            steps.add(ExecutionStep.builder()
                .step(stepNum++)
                .type("TOOL")
                .name(toolCall.getToolName())
                .status(toolCall.getStatus())
                .duration(toolCall.getDuration())
                .build());
        }
        
        // 响应生成
        steps.add(ExecutionStep.builder()
            .step(stepNum)
            .type("RESPONSE")
            .name("响应生成")
            .status("SUCCESS")
            .duration(300)
            .build());
        
        return steps;
    }
    
    @Override
    public boolean healthCheck() {
        return true;
    }
    
    @Override
    public String getProviderName() {
        return "Mock";
    }
}