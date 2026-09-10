package com.campuspilot.agent.provider;

import com.campuspilot.agent.model.*;
import com.campuspilot.agent.tool.AgentTool;
import com.campuspilot.agent.tool.ToolRegistry;
import com.campuspilot.agent.tool.ToolResult;
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
        long startTime = System.currentTimeMillis();
        log.info("Mock Agent收到消息: {}", request.getMessage());
        
        // 模拟网络与LLM推理延迟
        simulateDelay();
        
        // 1. 意图识别
        String intent = mockIntentDetection(request.getMessage());
        log.info("Mock意图识别: {}", intent);
        
        // 2. 复杂度判断（路由决定）
        String complexity = mockComplexityAnalysis(request.getMessage(), intent);
        log.info("Mock复杂度判断: {}", complexity);
        
        // 3. 真实工具调用（Rule/业务逻辑在Service层）
        List<ToolCall> toolCalls = executeTools(intent, request);
        log.info("Mock工具调用: {} 个", toolCalls.size());
        
        // 4. 知识库引用
        List<Citation> sources = mockCitations(intent);
        
        // 5. 生成响应（基于工具真实返回结果）
        String response = buildResponse(request.getMessage(), intent, complexity, toolCalls);
        log.info("Mock响应: {}", response.substring(0, Math.min(120, response.length())) + "...");
        
        // 6. 执行轨迹（Agent Trace）
        List<ExecutionStep> executionSteps = buildExecutionSteps(intent, toolCalls);
        
        long endTime = System.currentTimeMillis();
        ExecutionTrace executionTrace = ExecutionTrace.builder()
            .executionId(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .intent(intent)
            .workflow(complexity)
            .steps(executionSteps)
            .startTime(startTime)
            .endTime(endTime)
            .duration(endTime - startTime)
            .status("SUCCESS")
            .build();
        
        return AgentResponse.builder()
            .sessionId(request.getSessionId())
            .messageId(UUID.randomUUID().toString())
            .response(response)
            .intent(intent)
            .complexity(complexity)
            .sources(sources)
            .executionSteps(executionSteps)
            .toolCalls(toolCalls)
            .executionTrace(executionTrace)
            .build();
    }
    
    private void simulateDelay() {
        try {
            Thread.sleep(800 + (long) (Math.random() * 800));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 意图识别（优先级：资格判断 > 任务创建 > 成绩 > 政策 > 通用）
     */
    private String mockIntentDetection(String message) {
        if (message.contains("判断") || message.contains("资格") || message.contains("是否符合")
                || (message.contains("条件") && (message.contains("符合") || message.contains("满足")))) {
            return "ELIGIBILITY_CHECK";
        } else if (message.contains("帮我") || message.contains("办理") || message.contains("申请")) {
            return "TASK_CREATE";
        } else if (message.contains("成绩") || message.contains("绩点")) {
            return "SCORE_QUERY";
        } else if (message.contains("请假") || message.contains("宿舍") || message.contains("奖")
                || message.contains("助") || message.contains("金") || message.contains("在读证明")
                || message.contains("政策") || message.contains("考试")) {
            return "POLICY_QUERY";
        }
        return "GENERAL_QUERY";
    }
    
    /**
     * 复杂度分析（决策优先：资格/判断类为COMPLEX，办理/申请类为ACTION，其余按长度）
     */
    private String mockComplexityAnalysis(String message, String intent) {
        if ("ELIGIBILITY_CHECK".equals(intent)
                || message.contains("判断") || message.contains("条件") || message.contains("资格")
                || message.contains("是否符合")) {
            return "COMPLEX";
        } else if ("TASK_CREATE".equals(intent)
                || message.contains("帮我") || message.contains("申请") || message.contains("办理")) {
            return "ACTION";
        }
        return "SIMPLE";
    }
    
    /**
     * 真实工具调用：Agent -> Tool -> Service -> Mapper -> DB
     */
    private List<ToolCall> executeTools(String intent, AgentRequest request) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        switch (intent) {
            case "ELIGIBILITY_CHECK": {
                // 工具1: 获取学生信息
                ToolCall studentCall = invokeTool("get_student_info", 
                    Map.of("studentId", request.getUserId() == null ? "0" : request.getUserId().toString()),
                    request);
                toolCalls.add(studentCall);
                
                // 工具2: 检索政策知识库
                ToolCall policyCall = invokeTool("search_policy",
                    Map.of("query", "奖学金", "category", "SCHOLARSHIP", "topK", "3"),
                    request);
                toolCalls.add(policyCall);
                break;
            }
            
            case "TASK_CREATE": {
                ToolCall todoCall = invokeTool("create_todo",
                    Map.of(
                        "title", "国家奖学金申请",
                        "description", "准备并提交国家奖学金申请材料",
                        "deadline", "2025-09-30 23:59:59",
                        "relatedPolicyId", "1"
                    ),
                    request);
                toolCalls.add(todoCall);
                break;
            }
            
            case "POLICY_QUERY": {
                ToolCall policyCall = invokeTool("search_policy",
                    Map.of("query", request.getMessage(), "topK", "5"),
                    request);
                toolCalls.add(policyCall);
                break;
            }
        }
        
        return toolCalls;
    }
    
    private ToolCall invokeTool(String toolName, Map<String, Object> params, AgentRequest request) {
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
    
    private String buildResponse(String message, String intent, String complexity, List<ToolCall> toolCalls) {
        switch (intent) {
            case "ELIGIBILITY_CHECK": {
                if (!toolCalls.isEmpty() && toolCalls.get(0).getStatus().equals("SUCCESS")) {
                    Object data = toolCalls.get(0).getResult();
                    if (data instanceof Map) {
                        Map<?, ?> student = (Map<?, ?>) data;
                        return "我来帮你判断是否符合国家奖学金申请条件。\n\n"
                            + "**资格判断结果**：\n\n"
                            + "✅ **年级要求**：满足（" + student.get("grade") + "级在读）\n"
                            + "✅ **学籍状态**：满足（" + student.get("status") + "）\n"
                            + "✅ **成绩要求**：请以教务处成绩单为准\n"
                            + "✅ **排名要求**：请以学院综合测评排名为准\n\n"
                            + "**结论**：你基本符合国家奖学金的申请条件，建议尽快准备申请材料。\n\n"
                            + "**建议**：\n"
                            + "1. 尽快准备成绩单（教务处盖章）与综合测评证明（学院盖章）\n"
                            + "2. 注意申请截止日期：9月30日\n"
                            + "3. 需要的话可以让我帮你创建申请任务";
                    }
                }
                return "我来帮你判断是否符合申请条件，但获取资料失败，请稍后重试。";
            }
            
            case "TASK_CREATE": {
                if (!toolCalls.isEmpty() && toolCalls.get(0).getStatus().equals("SUCCESS")) {
                    Object data = toolCalls.get(0).getResult();
                    String taskId = "";
                    if (data instanceof Map) {
                        Object id = ((Map<?, ?>) data).get("taskId");
                        taskId = id != null ? id.toString() : "";
                    }
                    return "好的，我来帮你创建「国家奖学金申请」任务。\n\n"
                        + "**任务创建成功** ✅" + (taskId.isEmpty() ? "" : "（任务ID: " + taskId + "）") + "\n\n"
                        + "**任务步骤**：\n"
                        + "1. ✅ 查询奖学金政策\n"
                        + "2. ⏳ 准备申请材料\n"
                        + "3. ⏳ 提交申请\n\n"
                        + "你可以在「我的任务」中查看任务详情，也可以在右侧确认创建结果。";
                }
                return "任务创建失败，请稍后重试。";
            }
            
            case "POLICY_QUERY": {
                if (message.contains("奖学金")) {
                    return "根据《2025年国家奖学金管理办法》，国家奖学金申请条件如下：\n\n"
                        + "1. **年级要求**：大三或大四在校学生\n"
                        + "2. **成绩要求**：综合测评成绩排名在本专业前10%\n"
                        + "3. **绩点要求**：绩点不低于3.5\n"
                        + "4. **其他要求**：无违纪处分，品德优良\n\n"
                        + "**申请时间**：每年9月1日至9月30日\n\n"
                        + "**所需材料**：\n"
                        + "- 国家奖学金申请表\n"
                        + "- 成绩单（教务处盖章）\n"
                        + "- 综合测评证明（学院盖章）\n\n"
                        + "📎 *来源：教务处官网 2025年9月1日发布*";
                } else if (message.contains("请假")) {
                    return "学生请假管理办法如下：\n\n"
                        + "1. 请假1天以内，由辅导员批准\n"
                        + "2. 请假1-3天，由学院副书记批准\n"
                        + "3. 请假3天以上，由学院院长批准\n\n"
                        + "**所需材料**：请假条 + 相关证明材料（病假需医院证明）\n\n"
                        + "📎 *来源：学生工作处*";
                } else if (message.contains("宿舍")) {
                    return "宿舍管理规定如下：\n\n"
                        + "1. 晚上11点门禁，超时需登记\n"
                        + "2. 禁止使用大功率电器\n"
                        + "3. 保持宿舍卫生整洁\n"
                        + "4. 禁止留宿外来人员\n\n"
                        + "📎 *来源：后勤管理处*";
                }
                return "我来帮你查询相关政策信息。根据知识库检索，请告诉我更具体的政策名称或关键词，例如：奖学金、请假、宿舍、考试、在读证明等。";
            }
            
            case "SCORE_QUERY":
                return "我来查询你的成绩信息。\n\n"
                    + "**成绩查询结果（DEMO数据）**：\n\n"
                    + "| 课程 | 学分 | 成绩 |\n"
                    + "|------|------|------|\n"
                    + "| 数据结构 | 4 | 92 (A) |\n"
                    + "| 操作系统 | 4 | 88 (B+) |\n"
                    + "| 计算机网络 | 3 | 90 (A-) |\n"
                    + "| 线性代数 | 4 | 85 (B) |\n\n"
                    + "**总学分**：120\n"
                    + "**绩点**：3.8\n"
                    + "**排名**：15/150（前10%）";
            
            default:
                return "你好！我是CampusPilot校园事务智能助手。\n\n"
                    + "我可以帮你：\n"
                    + "- 📚 查询校园政策（如：查询奖学金政策）\n"
                    + "- 🎓 判断申请资格（如：帮我判断是否符合国家奖学金申请条件）\n"
                    + "- 📝 办理校园事务（如：帮我申请奖学金）\n"
                    + "- 📊 查询成绩（如：查看我的成绩）\n\n"
                    + "请问有什么可以帮你的？";
        }
    }
    
    private List<Citation> mockCitations(String intent) {
        if ("POLICY_QUERY".equals(intent) || "ELIGIBILITY_CHECK".equals(intent)) {
            return Collections.singletonList(
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
    
    private List<ExecutionStep> buildExecutionSteps(String intent, List<ToolCall> toolCalls) {
        List<ExecutionStep> steps = new ArrayList<>();
        int stepNum = 1;
        
        steps.add(ExecutionStep.builder()
            .step(stepNum++)
            .type("INTENT")
            .name("意图识别")
            .status("SUCCESS")
            .duration(200)
            .build());
        
        steps.add(ExecutionStep.builder()
            .step(stepNum++)
            .type("ROUTER")
            .name("复杂度路由")
            .status("SUCCESS")
            .duration(100)
            .output(Map.of("route", getRouteForIntent(intent)))
            .build());
        
        if ("POLICY_QUERY".equals(intent) || "ELIGIBILITY_CHECK".equals(intent)) {
            steps.add(ExecutionStep.builder()
                .step(stepNum++)
                .type("KNOWLEDGE")
                .name("知识库检索")
                .status("SUCCESS")
                .duration(500)
                .build());
        }
        
        for (ToolCall toolCall : toolCalls) {
            steps.add(ExecutionStep.builder()
                .step(stepNum++)
                .type("TOOL")
                .name(toolCall.getToolName())
                .status(toolCall.getStatus())
                .duration(toolCall.getDuration())
                .input(toolCall.getParams())
                .output(toolCall.getResult())
                .error(toolCall.getError())
                .build());
        }
        
        steps.add(ExecutionStep.builder()
            .step(stepNum)
            .type("RESPONSE")
            .name("响应生成")
            .status("SUCCESS")
            .duration(300)
            .build());
        
        return steps;
    }
    
    private String getRouteForIntent(String intent) {
        switch (intent) {
            case "ELIGIBILITY_CHECK": return "RAG + TOOLS";
            case "TASK_CREATE": return "WORKFLOW + TOOLS";
            case "POLICY_QUERY": return "RAG";
            default: return "RAG";
        }
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