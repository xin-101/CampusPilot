# CampusPilot FastGPT集成设计

## 一、FastGPT概述

### 1.1 FastGPT简介
SF-FastGPT是一个开源的AI应用开发平台，支持知识库、工作流、工具调用等功能。CampusPilot将使用FastGPT作为核心Agent平台，实现LLM推理、RAG检索、Workflow执行和Tool Calling。

### 1.2 集成目标
- 实现CampusPilot与FastGPT的无缝集成
- 支持知识库检索和RAG问答
- 支持工作流编排和执行
- 支持工具调用和API对接
- 提供Mock机制支持开发测试

### 1.3 集成原则
1. **接口抽象**：定义统一的AgentProvider接口
2. **可插拔**：支持FastGPT和Mock两种实现
3. **容错性**：FastGPT不可用时优雅降级
4. **可观测性**：完整的调用日志和追踪

## 二、集成架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                    CampusPilot Backend                          │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐│
│  │                    AgentService                           ││
│  │  - 消息处理                                                ││
│  │  - 会话管理                                                ││
│  │  - 执行追踪                                                ││
│  └────────────────────────────────────────────────────────────┘│
│                              │                                  │
│                              ▼                                  │
│  ┌────────────────────────────────────────────────────────────┐│
│  │                    AgentProvider接口                       ││
│  │  - chat(message, sessionId)                               ││
│  │  - getExecutionTrace(executionId)                         ││
│  │  - healthCheck()                                          ││
│  └────────────────────────────────────────────────────────────┘│
│                              │                                  │
│            ┌─────────────────┼─────────────────┐              │
│            │                 │                 │              │
│            ▼                 ▼                 ▼              │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐│
│  │ FastGPTProvider │ │ MockAgentProvider│ │ 其他Provider    ││
│  │  - 真实API调用   │ │  - Mock实现      │ │  - 扩展预留      ││
│  └─────────────────┘ └─────────────────┘ └─────────────────┘│
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────────────────┐│
│  │                    FastGPT Client                         ││
│  │  - HTTP客户端                                              ││
│  │  - 请求封装                                                ││
│  │  - 响应解析                                                ││
│  │  - 错误处理                                                ││
│  └────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SF-FastGPT Platform                          │
│                                                                 │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐        │
│  │ Knowledge Base │ │   Workflow    │ │ Tool Calling  │        │
│  │   (知识库)     │ │   (工作流)    │ │  (工具调用)   │        │
│  └───────────────┘ └───────────────┘ └───────────────┘        │
│                                                                 │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐        │
│  │   LLM Engine  │ │ RAG Retrieval │ │ Agent Control │        │
│  │   (大模型)    │ │  (检索增强)   │ │ (Agent控制)   │        │
│  └───────────────┘ └───────────────┘ └───────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
用户消息 → CampusPilot Backend → AgentProvider → FastGPT → 返回结果
```

## 三、AgentProvider接口设计

### 3.1 接口定义

```java
public interface AgentProvider {
    
    /**
     * 发送消息到Agent
     * @param message 用户消息
     * @param sessionId 会话ID
     * @return Agent响应
     */
    AgentResponse chat(String message, String sessionId);
    
    /**
     * 获取执行追踪
     * @param executionId 执行ID
     * @return 执行追踪
     */
    ExecutionTrace getExecutionTrace(String executionId);
    
    /**
     * 健康检查
     * @return 是否健康
     */
    boolean healthCheck();
    
    /**
     * 获取Provider名称
     * @return Provider名称
     */
    String getProviderName();
}
```

### 3.2 响应数据结构

```java
@Data
@Builder
public class AgentResponse {
    private String sessionId;
    private String messageId;
    private String response;
    private String intent;
    private Complexity complexity;
    private List<Citation> sources;
    private List<ExecutionStep> executionSteps;
    private List<ToolCall> toolCalls;
    private List<SuggestedAction> suggestedActions;
    private ExecutionTrace executionTrace;
}

@Data
@Builder
public class ExecutionStep {
    private String type;
    private String name;
    private String status;
    private long duration;
    private Object input;
    private Object output;
}

@Data
@Builder
public class ToolCall {
    private String toolName;
    private Map<String, Object> params;
    private Object result;
    private long duration;
    private String status;
}

@Data
@Builder
public class Citation {
    private Long policyId;
    private String policyName;
    private String version;
    private String source;
    private LocalDate effectiveDate;
    private double relevance;
    private String excerpt;
}
```

## 四、FastGPT Provider实现

### 4.1 FastGPT Client

```java
@Component
@Slf4j
public class FastGPTClient {
    
    @Value("${agent.fastgpt.url}")
    private String baseUrl;
    
    @Value("${agent.fastgpt.apiKey}")
    private String apiKey;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 调用FastGPT Chat API
     */
    public FastGPTResponse chat(FastGPTRequest request) {
        String url = baseUrl + "/api/chat";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<FastGPTRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<FastGPTResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                FastGPTResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("FastGPT Chat API调用失败: {}", response.getStatusCode());
                throw new RuntimeException("FastGPT API调用失败");
            }
        } catch (Exception e) {
            log.error("FastGPT Chat API调用异常", e);
            throw new RuntimeException("FastGPT API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 调用FastGPT知识库搜索API
     */
    public List<KnowledgeSearchResult> searchKnowledge(String knowledgeId, String query, int topK) {
        String url = baseUrl + "/api/knowledge/search";
        
        Map<String, Object> request = Map.of(
            "knowledgeId", knowledgeId,
            "query", query,
            "topK", topK
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseKnowledgeResults(response.getBody());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("FastGPT知识库搜索API调用异常", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 调用FastGPT工作流API
     */
    public FastGPTWorkflowResponse runWorkflow(String workflowId, Map<String, Object> inputs) {
        String url = baseUrl + "/api/workflow/run";
        
        Map<String, Object> request = Map.of(
            "workflowId", workflowId,
            "inputs", inputs,
            "responseMode", "blocking"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<FastGPTWorkflowResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                FastGPTWorkflowResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            throw new RuntimeException("FastGPT工作流API调用失败");
        } catch (Exception e) {
            log.error("FastGPT工作流API调用异常", e);
            throw new RuntimeException("FastGPT工作流API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 调用FastGPT工具API
     */
    public FastGPTToolResponse runTool(String toolId, Map<String, Object> params) {
        String url = baseUrl + "/api/tool/run";
        
        Map<String, Object> request = Map.of(
            "toolId", toolId,
            "params", params
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<FastGPTToolResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                FastGPTToolResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            throw new RuntimeException("FastGPT工具API调用失败");
        } catch (Exception e) {
            log.error("FastGPT工具API调用异常", e);
            throw new RuntimeException("FastGPT工具API调用异常: " + e.getMessage());
        }
    }
}
```

### 4.2 FastGPT Request/Response

```java
@Data
@Builder
public class FastGPTRequest {
    private String chatId;
    private String stream;
    private List<FastGPTMessage> messages;
    private Map<String, Object> variables;
}

@Data
@Builder
public class FastGPTMessage {
    private String role;
    private String content;
}

@Data
public class FastGPTResponse {
    private String chatId;
    private String messageId;
    private String content;
    private List<FastGPTSource> sources;
    private Map<String, Object> metadata;
}

@Data
public class FastGPTSource {
    private String id;
    private String name;
    private String content;
    private double score;
    private Map<String, Object> metadata;
}

@Data
public class FastGPTWorkflowResponse {
    private String workflowRunId;
    private Map<String, Object> data;
    private String status;
}

@Data
public class FastGPTToolResponse {
    private boolean success;
    private Object data;
    private String message;
}
```

### 4.3 FastGPT Provider实现

```java
@Service
@Profile("prod")
@Slf4j
public class FastGPTProvider implements AgentProvider {
    
    @Autowired
    private FastGPTClient fastGPTClient;
    
    @Autowired
    private IntentDetector intentDetector;
    
    @Autowired
    private ComplexityRouter complexityRouter;
    
    @Autowired
    private ExecutionTracer tracer;
    
    @Override
    public AgentResponse chat(String message, String sessionId) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 意图识别
            Intent intent = intentDetector.detect(message);
            
            // 2. 复杂度判断
            Complexity complexity = complexityRouter.analyzeComplexity(message, intent);
            
            // 3. 构建FastGPT请求
            FastGPTRequest request = buildRequest(message, sessionId, intent, complexity);
            
            // 4. 调用FastGPT
            FastGPTResponse fastGPTResponse = fastGPTClient.chat(request);
            
            // 5. 构建响应
            AgentResponse response = AgentResponse.builder()
                .sessionId(sessionId)
                .messageId(UUID.randomUUID().toString())
                .response(fastGPTResponse.getContent())
                .intent(intent.name())
                .complexity(complexity)
                .sources(convertSources(fastGPTResponse.getSources()))
                .executionSteps(collectExecutionSteps(executionId))
                .build();
            
            // 6. 记录执行日志
            ExecutionTrace trace = ExecutionTrace.builder()
                .executionId(executionId)
                .sessionId(sessionId)
                .userId(getCurrentUserId())
                .intent(intent.name())
                .startTime(startTime)
                .endTime(System.currentTimeMillis())
                .duration(System.currentTimeMillis() - startTime)
                .status("SUCCESS")
                .build();
            tracer.record(trace);
            
            return response;
            
        } catch (Exception e) {
            log.error("FastGPT Agent调用失败", e);
            
            // 记录失败日志
            ExecutionTrace trace = ExecutionTrace.builder()
                .executionId(executionId)
                .sessionId(sessionId)
                .userId(getCurrentUserId())
                .startTime(startTime)
                .endTime(System.currentTimeMillis())
                .duration(System.currentTimeMillis() - startTime)
                .status("FAILED")
                .error(e.getMessage())
                .build();
            tracer.record(trace);
            
            throw new RuntimeException("Agent调用失败: " + e.getMessage());
        }
    }
    
    private FastGPTRequest buildRequest(String message, String sessionId, Intent intent, Complexity complexity) {
        List<FastGPTMessage> messages = new ArrayList<>();
        
        // 系统提示
        messages.add(FastGPTMessage.builder()
            .role("system")
            .content(buildSystemPrompt())
            .build());
        
        // 用户消息
        messages.add(FastGPTMessage.builder()
            .role("user")
            .content(message)
            .build());
        
        return FastGPTRequest.builder()
            .chatId(sessionId)
            .stream("false")
            .messages(messages)
            .variables(Map.of(
                "intent", intent.name(),
                "complexity", complexity.name()
            ))
            .build();
    }
    
    private String buildSystemPrompt() {
        return """
            你是CampusPilot校园事务智能助手。
            
            你的职责：
            1. 回答学生关于校园政策和事务的问题
            2. 帮助学生判断是否满足申请条件
            3. 帮助学生办理校园事务
            4. 提醒学生重要的截止日期
            
            重要规则：
            - 只基于知识库内容回答，不编造政策
            - 明确标注信息来源
            - 无法确认时明确告知用户
            - 保护用户隐私，不泄露敏感信息
            
            请用友好、专业的语言回答学生问题。
            """;
    }
    
    @Override
    public ExecutionTrace getExecutionTrace(String executionId) {
        // 从数据库获取执行追踪
        return tracer.getTrace(executionId);
    }
    
    @Override
    public boolean healthCheck() {
        try {
            // 调用FastGPT健康检查接口
            String url = fastGPTClient.getBaseUrl() + "/api/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("FastGPT健康检查失败", e);
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return "FastGPT";
    }
}
```

## 五、Mock Provider实现

### 5.1 Mock Provider

```java
@Service
@Profile("dev")
@Slf4j
public class MockAgentProvider implements AgentProvider {
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private ExecutionTracer tracer;
    
    @Override
    public AgentResponse chat(String message, String sessionId) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        log.info("Mock Agent收到消息: {}", message);
        
        // 模拟延迟
        simulateDelay();
        
        // 1. 模拟意图识别
        Intent intent = mockIntentDetection(message);
        log.info("Mock意图识别: {}", intent);
        
        // 2. 模拟复杂度判断
        Complexity complexity = mockComplexityAnalysis(message, intent);
        log.info("Mock复杂度判断: {}", complexity);
        
        // 3. 模拟工具调用
        List<ToolCall> toolCalls = mockToolCalls(intent);
        log.info("Mock工具调用: {}", toolCalls.size());
        
        // 4. 生成模拟响应
        String response = mockResponse(message, intent, complexity, toolCalls);
        log.info("Mock响应: {}", response.substring(0, Math.min(100, response.length())) + "...");
        
        // 5. 生成模拟引用
        List<Citation> sources = mockCitations(intent);
        
        // 6. 生成执行步骤
        List<ExecutionStep> executionSteps = mockExecutionSteps(intent, toolCalls);
        
        // 7. 记录执行日志
        ExecutionTrace trace = ExecutionTrace.builder()
            .executionId(executionId)
            .sessionId(sessionId)
            .userId(getCurrentUserId())
            .intent(intent.name())
            .startTime(startTime)
            .endTime(System.currentTimeMillis())
            .duration(System.currentTimeMillis() - startTime)
            .status("SUCCESS")
            .build();
        tracer.record(trace);
        
        return AgentResponse.builder()
            .sessionId(sessionId)
            .messageId(UUID.randomUUID().toString())
            .response(response)
            .intent(intent.name())
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
    
    private Intent mockIntentDetection(String message) {
        if (message.contains("奖学金")) {
            return Intent.POLICY_QUERY;
        } else if (message.contains("申请") || message.contains("帮我")) {
            return Intent.TASK_CREATE;
        } else if (message.contains("成绩")) {
            return Intent.SCORE_QUERY;
        } else if (message.contains("资格") || message.contains("条件")) {
            return Intent.ELIGIBILITY_CHECK;
        } else if (message.contains("请假")) {
            return Intent.POLICY_QUERY;
        } else if (message.contains("宿舍")) {
            return Intent.POLICY_QUERY;
        }
        return Intent.GENERAL_QUERY;
    }
    
    private Complexity mockComplexityAnalysis(String message, Intent intent) {
        if (message.contains("帮我") || message.contains("申请") || message.contains("办理")) {
            return Complexity.ACTION;
        } else if (message.contains("判断") || message.contains("条件") || message.contains("资格")) {
            return Complexity.COMPLEX;
        }
        return Complexity.SIMPLE;
    }
    
    private List<ToolCall> mockToolCalls(Intent intent) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        switch (intent) {
            case ELIGIBILITY_CHECK:
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
                
            case TASK_CREATE:
                toolCalls.add(ToolCall.builder()
                    .toolName("create_todo")
                    .status("SUCCESS")
                    .duration(400)
                    .build());
                break;
                
            case SCORE_QUERY:
                toolCalls.add(ToolCall.builder()
                    .toolName("get_student_scores")
                    .status("SUCCESS")
                    .duration(500)
                    .build());
                break;
        }
        
        return toolCalls;
    }
    
    private String mockResponse(String message, Intent intent, Complexity complexity, List<ToolCall> toolCalls) {
        StringBuilder response = new StringBuilder();
        
        switch (intent) {
            case POLICY_QUERY:
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
                
            case ELIGIBILITY_CHECK:
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
                
            case TASK_CREATE:
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
                
            case SCORE_QUERY:
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
    
    private List<Citation> mockCitations(Intent intent) {
        if (intent == Intent.POLICY_QUERY) {
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
    
    private List<ExecutionStep> mockExecutionSteps(Intent intent, List<ToolCall> toolCalls) {
        List<ExecutionStep> steps = new ArrayList<>();
        
        // 意图识别
        steps.add(ExecutionStep.builder()
            .type("intent")
            .name("意图识别")
            .status("SUCCESS")
            .duration(200)
            .build());
        
        // 知识库检索
        if (intent == Intent.POLICY_QUERY || intent == Intent.ELIGIBILITY_CHECK) {
            steps.add(ExecutionStep.builder()
                .type("knowledge")
                .name("知识库检索")
                .status("SUCCESS")
                .duration(500)
                .build());
        }
        
        // 工具调用
        for (ToolCall toolCall : toolCalls) {
            steps.add(ExecutionStep.builder()
                .type("tool")
                .name(toolCall.getToolName())
                .status(toolCall.getStatus())
                .duration(toolCall.getDuration())
                .build());
        }
        
        // 响应生成
        steps.add(ExecutionStep.builder()
            .type("response")
            .name("响应生成")
            .status("SUCCESS")
            .duration(300)
            .build());
        
        return steps;
    }
    
    @Override
    public ExecutionTrace getExecutionTrace(String executionId) {
        return tracer.getTrace(executionId);
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
```

## 六、FastGPT知识库设计

### 6.1 知识库结构

```
CampusPilot Knowledge Base
├── 奖学金政策
│   ├── 国家奖学金管理办法
│   ├── 国家励志奖学金管理办法
│   └── 国家助学金管理办法
├── 请假政策
│   ├── 学生请假管理办法
│   ├── 病假办理流程
│   └── 事假办理流程
├── 考试政策
│   ├── 考试管理办法
│   ├── 补考管理办法
│   └── 重修管理办法
├── 宿舍政策
│   ├── 宿舍管理规定
│   ├── 宿舍报修流程
│   └── 晚归管理规定
└── 证明开具
    ├── 在读证明办理流程
    ├── 成绩证明办理流程
    └── 学籍证明办理流程
```

### 6.2 文档切分策略

```java
@Service
public class DocumentSplitter {
    
    public List<DocumentChunk> split(String document, String policyId) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // 1. 按段落切分
        String[] paragraphs = document.split("\n\n");
        
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            if (!paragraph.isEmpty()) {
                chunks.add(DocumentChunk.builder()
                    .id(policyId + "_chunk_" + i)
                    .content(paragraph)
                    .policyId(policyId)
                    .chunkIndex(i)
                    .metadata(extractMetadata(paragraph))
                    .build());
            }
        }
        
        // 2. 如果段落太长，进一步切分
        chunks = chunks.stream()
            .flatMap(chunk -> {
                if (chunk.getContent().length() > 1000) {
                    return splitLongChunk(chunk).stream();
                }
                return Stream.of(chunk);
            })
            .collect(Collectors.toList());
        
        return chunks;
    }
    
    private Map<String, Object> extractMetadata(String paragraph) {
        Map<String, Object> metadata = new HashMap<>();
        
        // 提取标题
        if (paragraph.startsWith("#")) {
            metadata.put("isTitle", true);
            metadata.put("level", paragraph.indexOf(" "));
        }
        
        // 提取关键词
        List<String> keywords = extractKeywords(paragraph);
        metadata.put("keywords", keywords);
        
        return metadata;
    }
    
    private List<DocumentChunk> splitLongChunk(DocumentChunk chunk) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = chunk.getContent();
        
        // 按句子切分
        String[] sentences = content.split("(?<=[。！？])");
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > 500) {
                if (currentChunk.length() > 0) {
                    chunks.add(DocumentChunk.builder()
                        .id(chunk.getId() + "_" + chunkIndex)
                        .content(currentChunk.toString())
                        .policyId(chunk.getPolicyId())
                        .chunkIndex(chunkIndex)
                        .metadata(chunk.getMetadata())
                        .build());
                    chunkIndex++;
                }
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence);
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(DocumentChunk.builder()
                .id(chunk.getId() + "_" + chunkIndex)
                .content(currentChunk.toString())
                .policyId(chunk.getPolicyId())
                .chunkIndex(chunkIndex)
                .metadata(chunk.getMetadata())
                .build());
        }
        
        return chunks;
    }
}
```

### 6.3 Metadata设计

```java
@Data
@Builder
public class PolicyMetadata {
    private String policyId;
    private String title;
    private String category;
    private String department;
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private List<String> keywords;
    private List<String> audience;
}
```

### 6.4 政策版本管理

```java
@Service
public class PolicyVersionManager {
    
    @Autowired
    private PolicyMapper policyMapper;
    
    @Autowired
    private PolicyVersionMapper versionMapper;
    
    /**
     * 获取当前有效版本
     */
    public PolicyVersion getActiveVersion(Long policyId) {
        return versionMapper.selectOne(
            new LambdaQueryWrapper<PolicyVersion>()
                .eq(PolicyVersion::getPolicyId, policyId)
                .eq(PolicyVersion::getStatus, "ACTIVE")
                .le(PolicyVersion::getEffectiveDate, LocalDate.now())
                .ge(PolicyVersion::getExpiryDate, LocalDate.now())
                .orderByDesc(PolicyVersion::getVersion)
                .last("LIMIT 1")
        );
    }
    
    /**
     * 获取所有版本
     */
    public List<PolicyVersion> getAllVersions(Long policyId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<PolicyVersion>()
                .eq(PolicyVersion::getPolicyId, policyId)
                .orderByDesc(PolicyVersion::getVersion)
        );
    }
    
    /**
     * 检查政策是否有效
     */
    public boolean isActive(Long policyId) {
        PolicyVersion version = getActiveVersion(policyId);
        return version != null;
    }
}
```

## 七、FastGPT Workflow设计

### 7.1 工作流定义

#### Workflow A：政策咨询工作流

```json
{
  "id": "policy_consultation",
  "name": "政策咨询工作流",
  "description": "处理政策咨询类问题",
  "steps": [
    {
      "order": 1,
      "type": "LLM",
      "name": "Query Rewrite",
      "prompt": "请将以下用户查询改写为更适合检索的形式：{{query}}"
    },
    {
      "order": 2,
      "type": "KNOWLEDGE",
      "name": "Knowledge Search",
      "knowledgeId": "{{knowledgeId}}",
      "query": "{{rewrittenQuery}}",
      "topK": 5
    },
    {
      "order": 3,
      "type": "LLM",
      "name": "Answer Generation",
      "prompt": "根据以下知识库内容回答用户问题：\n\n知识库内容：{{knowledgeResults}}\n\n用户问题：{{query}}\n\n请生成回答，并标注来源。"
    }
  ]
}
```

#### Workflow B：资格判断工作流

```json
{
  "id": "eligibility_check",
  "name": "资格判断工作流",
  "description": "判断学生是否满足政策条件",
  "steps": [
    {
      "order": 1,
      "type": "TOOL",
      "name": "Get Student Info",
      "toolId": "get_student_info",
      "params": {
        "studentId": "{{studentId}}"
      }
    },
    {
      "order": 2,
      "type": "TOOL",
      "name": "Get Student Scores",
      "toolId": "get_student_scores",
      "params": {
        "studentId": "{{studentId}}"
      }
    },
    {
      "order": 3,
      "type": "KNOWLEDGE",
      "name": "Search Policy",
      "knowledgeId": "{{knowledgeId}}",
      "query": "{{policyName}}",
      "topK": 3
    },
    {
      "order": 4,
      "type": "RULE",
      "name": "Condition Match",
      "rules": "{{policyConditions}}",
      "studentInfo": "{{studentInfo}}",
      "studentScores": "{{studentScores}}"
    },
    {
      "order": 5,
      "type": "LLM",
      "name": "Result Explanation",
      "prompt": "根据以下信息生成资格判断结果：\n\n政策条件：{{policyConditions}}\n学生信息：{{studentInfo}}\n学生成绩：{{studentScores}}\n匹配结果：{{matchResult}}\n\n请生成通俗易懂的解释。"
    }
  ]
}
```

#### Workflow C：事务办理工作流

```json
{
  "id": "task_creation",
  "name": "事务办理工作流",
  "description": "创建事务办理任务",
  "steps": [
    {
      "order": 1,
      "type": "LLM",
      "name": "Intent Analysis",
      "prompt": "分析用户请求，确定需要办理的事务类型：{{query}}"
    },
    {
      "order": 2,
      "type": "KNOWLEDGE",
      "name": "Search Policy",
      "knowledgeId": "{{knowledgeId}}",
      "query": "{{taskType}}办理流程",
      "topK": 3
    },
    {
      "order": 3,
      "type": "LLM",
      "name": "Plan Generation",
      "prompt": "根据以下信息生成办理计划：\n\n事务类型：{{taskType}}\n政策信息：{{policyInfo}}\n\n请生成：\n1. 任务标题\n2. 任务描述\n3. 所需步骤\n4. 所需材料\n5. 截止时间"
    },
    {
      "order": 4,
      "type": "TOOL",
      "name": "Create Todo",
      "toolId": "create_todo",
      "params": "{{taskPlan}}"
    },
    {
      "order": 5,
      "type": "LLM",
      "name": "Response Generation",
      "prompt": "根据以下信息生成回复：\n\n任务创建结果：{{taskResult}}\n\n请生成友好的回复，告知用户任务已创建。"
    }
  ]
}
```

## 八、FastGPT配置管理

### 8.1 配置文件

```yaml
# application.yml
agent:
  provider: ${AGENT_PROVIDER:mock}
  fastgpt:
    url: ${FASTGPT_URL:http://localhost:3000}
    apiKey: ${FASTGPT_API_KEY:}
    knowledgeId: ${FASTGPT_KNOWLEDGE_ID:}
    workflowId:
      policyConsultation: ${FASTGPT_WORKFLOW_POLICY:}
      eligibilityCheck: ${FASTGPT_WORKFLOW_ELIGIBILITY:}
      taskCreation: ${FASTGPT_WORKFLOW_TASK:}
    timeout: 30000
    retryCount: 3
```

### 8.2 环境变量

```bash
# .env.example
# Agent配置
AGENT_PROVIDER=mock  # mock 或 fastgpt

# FastGPT配置
FASTGPT_URL=http://localhost:3000
FASTGPT_API_KEY=your-api-key
FASTGPT_KNOWLEDGE_ID=your-knowledge-id

# FastGPT工作流配置
FASTGPT_WORKFLOW_POLICY=your-policy-workflow-id
FASTGPT_WORKFLOW_ELIGIBILITY=your-eligibility-workflow-id
FASTGPT_WORKFLOW_TASK=your-task-workflow-id
```

### 8.3 配置类

```java
@Configuration
@ConfigurationProperties(prefix = "agent.fastgpt")
@Data
public class FastGPTConfig {
    
    private String url;
    private String apiKey;
    private String knowledgeId;
    private WorkflowIds workflowId = new WorkflowIds();
    private int timeout = 30000;
    private int retryCount = 3;
    
    @Data
    public static class WorkflowIds {
        private String policyConsultation;
        private String eligibilityCheck;
        private String taskCreation;
    }
}
```

## 九、FastGPT集成测试

### 9.1 测试策略

```java
@SpringBootTest
@ActiveProfiles("test")
class FastGPTIntegrationTest {
    
    @Autowired
    private AgentProvider agentProvider;
    
    @Test
    void testPolicyConsultation() {
        // 测试政策咨询
        AgentResponse response = agentProvider.chat("国家奖学金什么时候申请？", "test-session");
        
        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().contains("国家奖学金"));
    }
    
    @Test
    void testEligibilityCheck() {
        // 测试资格判断
        AgentResponse response = agentProvider.chat("我能申请国家奖学金吗？", "test-session");
        
        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().contains("资格"));
    }
    
    @Test
    void testTaskCreation() {
        // 测试任务创建
        AgentResponse response = agentProvider.chat("帮我申请国家奖学金", "test-session");
        
        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().contains("任务"));
    }
}
```

### 9.2 Mock测试

```java
@SpringBootTest
@ActiveProfiles("dev")
class MockAgentProviderTest {
    
    @Autowired
    private AgentProvider agentProvider;
    
    @Test
    void testMockProvider() {
        // 验证Mock Provider正常工作
        assertTrue(agentProvider instanceof MockAgentProvider);
        
        AgentResponse response = agentProvider.chat("测试消息", "test-session");
        assertNotNull(response);
        assertEquals("Mock", agentProvider.getProviderName());
    }
    
    @Test
    void testMockHealthCheck() {
        assertTrue(agentProvider.healthCheck());
    }
}
```

## 十、FastGPT部署指南

### 10.1 开发环境

```bash
# 使用Mock模式
export AGENT_PROVIDER=mock

# 启动后端
mvn spring-boot:run
```

### 10.2 生产环境

```bash
# 配置FastGPT
export AGENT_PROVIDER=fastgpt
export FASTGPT_URL=http://your-fastgpt-url
export FASTGPT_API_KEY=your-api-key
export FASTGPT_KNOWLEDGE_ID=your-knowledge-id

# 启动后端
mvn spring-boot:run
```

### 10.3 Docker部署

```yaml
# docker-compose.yml
services:
  backend:
    build: ./campuspilot
    environment:
      - AGENT_PROVIDER=fastgpt
      - FASTGPT_URL=http://fastgpt:3000
      - FASTGPT_API_KEY=${FASTGPT_API_KEY}
      - FASTGPT_KNOWLEDGE_ID=${FASTGPT_KNOWLEDGE_ID}
    depends_on:
      - fastgpt
  
  fastgpt:
    image: fastgpt/fastgpt:latest
    ports:
      - "3000:3000"
    environment:
      - OPENAI_BASE_URL=${OPENAI_BASE_URL}
      - OPENAI_KEY=${OPENAI_KEY}
```

---

*本文档为CampusPilot与SF-FastGPT的集成提供完整的设计和实施指导。*