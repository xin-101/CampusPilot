# CampusPilot Workflow设计

## 一、Workflow概述

### 1.1 设计目标
Workflow是CampusPilot Agent的核心执行引擎，负责编排和执行复杂的业务流程。Workflow需要：
- 支持多步骤流程编排
- 支持条件分支
- 支持工具调用
- 支持错误处理
- 支持执行追踪

### 1.2 设计原则
1. **声明式定义**：使用JSON/YAML定义工作流
2. **可组合**：支持工作流嵌套和组合
3. **可追踪**：完整的执行日志和追踪
4. **可恢复**：支持失败重试和恢复
5. **可观测**：实时监控执行状态

## 二、Workflow分类

### 2.1 Workflow A：政策咨询工作流

#### 流程设计
```
用户问题
  ↓
意图识别
  ↓
Query Rewrite
  ↓
知识库检索
  ↓
政策验证
  ↓
答案生成
  ↓
来源引用
```

#### 实现代码

```java
@Service
public class PolicyConsultationWorkflow implements Workflow {
    
    @Autowired
    private IntentDetector intentDetector;
    
    @Autowired
    private QueryRewriter queryRewriter;
    
    @Autowired
    private VectorRetriever vectorRetriever;
    
    @Autowired
    private Reranker reranker;
    
    @Autowired
    private CitationGenerator citationGenerator;
    
    @Autowired
    private AnswerGenerator answerGenerator;
    
    @Override
    public WorkflowResult execute(WorkflowContext context) {
        WorkflowResult result = new WorkflowResult();
        
        try {
            // 1. 意图识别
            Intent intent = intentDetector.detect(context.getMessage());
            result.addStep("intent_detection", "SUCCESS", intent);
            
            // 2. Query Rewrite
            String rewrittenQuery = queryRewriter.rewrite(context.getMessage());
            result.addStep("query_rewrite", "SUCCESS", rewrittenQuery);
            
            // 3. 知识库检索
            Map<String, Object> filters = metadataFilter.filter(rewrittenQuery, context);
            List<SearchResult> searchResults = vectorRetriever.retrieve(rewrittenQuery, filters, 10);
            result.addStep("knowledge_retrieval", "SUCCESS", searchResults.size() + " results");
            
            // 4. Rerank
            List<SearchResult> rerankedResults = reranker.rerank(rewrittenQuery, searchResults);
            result.addStep("rerank", "SUCCESS", rerankedResults.size() + " results");
            
            // 5. 政策验证
            List<SearchResult> validatedResults = validatePolicies(rerankedResults);
            result.addStep("policy_validation", "SUCCESS", validatedResults.size() + " valid");
            
            // 6. 生成引用
            List<Citation> citations = citationGenerator.generate(rewrittenQuery, validatedResults);
            result.addStep("citation_generation", "SUCCESS", citations.size() + " citations");
            
            // 7. 生成答案
            String answer = answerGenerator.generate(context.getMessage(), validatedResults, citations);
            result.addStep("answer_generation", "SUCCESS", answer.length() + " chars");
            
            // 设置最终结果
            result.setStatus("SUCCESS");
            result.setAnswer(answer);
            result.setCitations(citations);
            
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    private List<SearchResult> validatePolicies(List<SearchResult> results) {
        return results.stream()
            .filter(result -> {
                // 验证政策是否有效
                PolicyVersion version = policyVersionManager.getActiveVersion(Long.parseLong(result.getPolicyId()));
                return version != null;
            })
            .collect(Collectors.toList());
    }
}
```

### 2.2 Workflow B：资格判断工作流

#### 流程设计
```
用户申请
  ↓
政策查询
  ↓
学生信息获取
  ↓
成绩信息获取
  ↓
条件匹配
  ↓
资格判断
  ↓
结果生成
```

#### 实现代码

```java
@Service
public class EligibilityCheckWorkflow implements Workflow {
    
    @Autowired
    private PolicySearchService policySearchService;
    
    @Autowired
    private StudentInfoService studentInfoService;
    
    @Autowired
    private StudentScoreService studentScoreService;
    
    @Autowired
    private ConditionMatcher conditionMatcher;
    
    @Autowired
    private ResultExplainer resultExplainer;
    
    @Override
    public WorkflowResult execute(WorkflowContext context) {
        WorkflowResult result = new WorkflowResult();
        
        try {
            // 1. 获取政策信息
            Long policyId = context.getPolicyId();
            PolicyVersion policy = policyVersionManager.getActiveVersion(policyId);
            result.addStep("policy_retrieval", "SUCCESS", policy.getTitle());
            
            // 2. 获取学生信息
            Long studentId = context.getStudentId();
            StudentInfo student = studentInfoService.getStudentInfo(studentId);
            result.addStep("student_info", "SUCCESS", student.getName());
            
            // 3. 获取成绩信息
            StudentScores scores = studentScoreService.getStudentScores(studentId);
            result.addStep("student_scores", "SUCCESS", "GPA: " + scores.getGpa());
            
            // 4. 提取条件
            List<Condition> conditions = conditionExtractor.extract(policy.getContent());
            result.addStep("condition_extraction", "SUCCESS", conditions.size() + " conditions");
            
            // 5. 条件匹配
            MatchResult matchResult = conditionMatcher.match(conditions, student, scores);
            result.addStep("condition_matching", "SUCCESS", "Eligible: " + matchResult.isEligible());
            
            // 6. 生成解释
            String explanation = resultExplainer.explain(matchResult, policy.getTitle());
            result.addStep("result_explanation", "SUCCESS", explanation.length() + " chars");
            
            // 设置最终结果
            result.setStatus("SUCCESS");
            result.setEligible(matchResult.isEligible());
            result.setConditions(matchResult.getConditions());
            result.setExplanation(explanation);
            
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setError(e.getMessage());
        }
        
        return result;
    }
}
```

### 2.3 Workflow C：事务办理工作流

#### 流程设计
```
办理请求
  ↓
意图识别
  ↓
政策查询
  ↓
资格判断
  ↓
材料检查
  ↓
办理计划生成
  ↓
待办创建
  ↓
通知发送
```

#### 实现代码

```java
@Service
public class TaskCreationWorkflow implements Workflow {
    
    @Autowired
    private IntentDetector intentDetector;
    
    @Autowired
    private PolicySearchService policySearchService;
    
    @Autowired
    private EligibilityCheckService eligibilityCheckService;
    
    @Autowired
    private MaterialChecker materialChecker;
    
    @Autowired
    private PlanGenerator planGenerator;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public WorkflowResult execute(WorkflowContext context) {
        WorkflowResult result = new WorkflowResult();
        
        try {
            // 1. 意图识别
            Intent intent = intentDetector.detect(context.getMessage());
            result.addStep("intent_detection", "SUCCESS", intent);
            
            // 2. 政策查询
            List<PolicyVersion> policies = policySearchService.search(context.getMessage());
            result.addStep("policy_search", "SUCCESS", policies.size() + " policies");
            
            // 3. 资格判断
            Long studentId = context.getStudentId();
            for (PolicyVersion policy : policies) {
                EligibilityResult eligibility = eligibilityCheckService.check(policy.getId(), studentId);
                if (eligibility.isEligible()) {
                    result.addStep("eligibility_check", "SUCCESS", "Eligible for " + policy.getTitle());
                    
                    // 4. 材料检查
                    List<Material> materials = materialChecker.check(policy.getId(), studentId);
                    result.addStep("material_check", "SUCCESS", materials.size() + " materials");
                    
                    // 5. 生成办理计划
                    TaskPlan plan = planGenerator.generate(policy, studentId, materials);
                    result.addStep("plan_generation", "SUCCESS", plan.getSteps().size() + " steps");
                    
                    // 6. 创建待办
                    Task task = taskService.createTask(plan);
                    result.addStep("task_creation", "SUCCESS", "Task ID: " + task.getId());
                    
                    // 7. 发送通知
                    notificationService.send(TaskNotification.builder()
                        .userId(context.getUserId())
                        .title("任务创建成功")
                        .content("你已成功创建任务：" + task.getTitle())
                        .type("INFO")
                        .relatedTaskId(task.getId())
                        .build());
                    result.addStep("notification", "SUCCESS", "Notification sent");
                    
                    // 设置最终结果
                    result.setStatus("SUCCESS");
                    result.setTask(task);
                    result.setPlan(plan);
                    
                    break;
                }
            }
            
            // 如果没有找到可申请的政策
            if (result.getStatus() == null) {
                result.setStatus("FAILED");
                result.setError("未找到符合申请条件的政策");
            }
            
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setError(e.getMessage());
        }
        
        return result;
    }
}
```

### 2.4 Workflow D：主动提醒工作流

#### 流程设计
```
定时任务/系统事件
  ↓
任务查询
  ↓
截止日期检测
  ↓
学生状态检查
  ↓
提醒生成
  ↓
通知发送
```

#### 实现代码

```java
@Service
public class ProactiveReminderWorkflow implements Workflow {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private StudentInfoService studentInfoService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Scheduled(cron = "0 0 9 * * ?")  // 每天早上9点执行
    public void executeScheduled() {
        execute(new WorkflowContext());
    }
    
    @Override
    public WorkflowResult execute(WorkflowContext context) {
        WorkflowResult result = new WorkflowResult();
        
        try {
            // 1. 查询即将到期的任务
            List<Task> urgentTasks = taskService.findTasksDueSoon(3);  // 3天内到期
            result.addStep("task_query", "SUCCESS", urgentTasks.size() + " tasks");
            
            for (Task task : urgentTasks) {
                // 2. 检查截止日期
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline().toLocalDate());
                result.addStep("deadline_check", "SUCCESS", task.getTitle() + ": " + daysLeft + " days left");
                
                // 3. 检查学生状态
                StudentInfo student = studentInfoService.getStudentInfoByUserId(task.getUserId());
                result.addStep("student_check", "SUCCESS", student.getName());
                
                // 4. 生成提醒
                String reminderContent = generateReminderContent(task, daysLeft);
                result.addStep("reminder_generation", "SUCCESS", reminderContent.length() + " chars");
                
                // 5. 发送通知
                notificationService.send(Notification.builder()
                    .userId(task.getUserId())
                    .title("任务截止提醒")
                    .content(reminderContent)
                    .type("REMINDER")
                    .relatedTaskId(task.getId())
                    .build());
                result.addStep("notification", "SUCCESS", "Notification sent to " + student.getName());
            }
            
            result.setStatus("SUCCESS");
            result.setProcessedCount(urgentTasks.size());
            
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    private String generateReminderContent(Task task, long daysLeft) {
        if (daysLeft <= 1) {
            return String.format(
                "⚠️ 你的任务「%s」将在明天截止，请尽快完成！",
                task.getTitle()
            );
        } else if (daysLeft <= 3) {
            return String.format(
                "⏰ 你的任务「%s」将在%d天后截止，请尽快安排时间完成。",
                task.getTitle(),
                daysLeft
            );
        } else {
            return String.format(
                "📅 你的任务「%s」将在%d天后截止，还有充足时间准备。",
                task.getTitle(),
                daysLeft
            );
        }
    }
}
```

## 三、Workflow引擎设计

### 3.1 Workflow定义

```java
@Data
@Builder
public class WorkflowDefinition {
    private String id;
    private String name;
    private String description;
    private List<WorkflowStep> steps;
    private String triggerIntent;
    private Map<String, Object> variables;
}

@Data
@Builder
public class WorkflowStep {
    private int order;
    private String name;
    private String type;  // TOOL, LLM, RULE, CONDITION, WORKFLOW
    private String toolName;
    private String prompt;
    private Map<String, Object> params;
    private List<WorkflowCondition> conditions;
    private String nextStepOnSuccess;
    private String nextStepOnFailure;
}

@Data
@Builder
public class WorkflowCondition {
    private String field;
    private String operator;
    private Object value;
    private String nextStep;
}
```

### 3.2 Workflow执行器

```java
@Service
public class WorkflowEngine {
    
    @Autowired
    private ToolCaller toolCaller;
    
    @Autowired
    private RuleEngine ruleEngine;
    
    @Autowired
    private LLMService llmService;
    
    @Autowired
    private WorkflowRegistry workflowRegistry;
    
    public WorkflowResult execute(String workflowId, WorkflowContext context) {
        WorkflowDefinition workflow = workflowRegistry.getWorkflow(workflowId);
        if (workflow == null) {
            throw new RuntimeException("Workflow not found: " + workflowId);
        }
        
        return executeWorkflow(workflow, context);
    }
    
    private WorkflowResult executeWorkflow(WorkflowDefinition workflow, WorkflowContext context) {
        WorkflowResult result = new WorkflowResult();
        Map<String, Object> stepResults = new HashMap<>();
        
        for (WorkflowStep step : workflow.getSteps()) {
            try {
                Object stepResult = executeStep(step, context, stepResults);
                stepResults.put(step.getName(), stepResult);
                
                result.addStep(step.getName(), "SUCCESS", stepResult);
                
                // 检查是否需要跳转
                if (step.getConditions() != null) {
                    String nextStep = evaluateConditions(step.getConditions(), stepResult);
                    if (nextStep != null) {
                        // 跳转到指定步骤
                        continue;
                    }
                }
                
            } catch (Exception e) {
                result.addStep(step.getName(), "FAILED", e.getMessage());
                
                if (step.getNextStepOnFailure() != null) {
                    // 跳转到失败处理步骤
                    continue;
                }
                
                result.setStatus("FAILED");
                result.setError("Workflow执行失败: " + step.getName());
                return result;
            }
        }
        
        result.setStatus("SUCCESS");
        result.setResult(stepResults);
        return result;
    }
    
    private Object executeStep(WorkflowStep step, WorkflowContext context, Map<String, Object> stepResults) {
        switch (step.getType()) {
            case "TOOL":
                Map<String, Object> params = resolveParams(step.getParams(), context, stepResults);
                ToolResult toolResult = toolCaller.call(step.getToolName(), params, context.getUserToken());
                if (toolResult.isSuccess()) {
                    return toolResult.getData();
                } else {
                    throw new RuntimeException("工具调用失败: " + toolResult.getMessage());
                }
                
            case "RULE":
                return ruleEngine.evaluate(context, stepResults);
                
            case "LLM":
                String prompt = resolvePrompt(step.getPrompt(), context, stepResults);
                return llmService.generate(prompt);
                
            case "CONDITION":
                return evaluateConditions(step.getConditions(), stepResults);
                
            default:
                throw new RuntimeException("未知的步骤类型: " + step.getType());
        }
    }
    
    private Map<String, Object> resolveParams(Map<String, Object> params, WorkflowContext context, Map<String, Object> stepResults) {
        Map<String, Object> resolved = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof String && ((String) value).startsWith("${")) {
                // 解析变量
                String variableName = ((String) value).substring(2, ((String) value).length() - 1);
                if (context.getVariables().containsKey(variableName)) {
                    value = context.getVariables().get(variableName);
                } else if (stepResults.containsKey(variableName)) {
                    value = stepResults.get(variableName);
                }
            }
            
            resolved.put(entry.getKey(), value);
        }
        
        return resolved;
    }
}
```

### 3.3 Workflow注册

```java
@Service
public class WorkflowRegistry {
    
    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册政策咨询工作流
        registerWorkflow(WorkflowDefinition.builder()
            .id("policy_consultation")
            .name("政策咨询工作流")
            .description("处理政策咨询类问题")
            .triggerIntent("POLICY_QUERY")
            .steps(Arrays.asList(
                WorkflowStep.builder()
                    .order(1)
                    .name("query_rewrite")
                    .type("LLM")
                    .prompt("请将以下用户查询改写为更适合检索的形式：{{query}}")
                    .build(),
                WorkflowStep.builder()
                    .order(2)
                    .name("knowledge_retrieval")
                    .type("TOOL")
                    .toolName("search_policy")
                    .params(Map.of("query", "${rewrittenQuery}"))
                    .build(),
                WorkflowStep.builder()
                    .order(3)
                    .name("answer_generation")
                    .type("LLM")
                    .prompt("根据以下知识库内容回答用户问题：{{knowledgeResults}}")
                    .build()
            ))
            .build());
        
        // 注册资格判断工作流
        registerWorkflow(WorkflowDefinition.builder()
            .id("eligibility_check")
            .name("资格判断工作流")
            .description("判断学生是否满足政策条件")
            .triggerIntent("ELIGIBILITY_CHECK")
            .steps(Arrays.asList(
                WorkflowStep.builder()
                    .order(1)
                    .name("get_student_info")
                    .type("TOOL")
                    .toolName("get_student_info")
                    .params(Map.of("studentId", "${studentId}"))
                    .build(),
                WorkflowStep.builder()
                    .order(2)
                    .name("get_student_scores")
                    .type("TOOL")
                    .toolName("get_student_scores")
                    .params(Map.of("studentId", "${studentId}"))
                    .build(),
                WorkflowStep.builder()
                    .order(3)
                    .name("search_policy")
                    .type("TOOL")
                    .toolName("search_policy")
                    .params(Map.of("query", "${policyName}"))
                    .build(),
                WorkflowStep.builder()
                    .order(4)
                    .name("condition_match")
                    .type("RULE")
                    .build(),
                WorkflowStep.builder()
                    .order(5)
                    .name("result_explanation")
                    .type("LLM")
                    .prompt("根据以下信息生成资格判断结果：{{matchConditionResult}}")
                    .build()
            ))
            .build());
        
        // 注册其他工作流...
    }
    
    public void registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getId(), workflow);
    }
    
    public WorkflowDefinition getWorkflow(String id) {
        return workflows.get(id);
    }
    
    public WorkflowDefinition getWorkflowByIntent(Intent intent) {
        return workflows.values().stream()
            .filter(w -> w.getTriggerIntent().equals(intent.name()))
            .findFirst()
            .orElse(null);
    }
}
```

## 四、Workflow上下文

### 4.1 上下文定义

```java
@Data
@Builder
public class WorkflowContext {
    private String sessionId;
    private Long userId;
    private Long studentId;
    private String message;
    private Intent intent;
    private Long policyId;
    private String policyName;
    private Map<String, Object> variables;
    private String userToken;
    
    public WorkflowContext() {
        this.variables = new HashMap<>();
    }
    
    public void setVariable(String key, Object value) {
        this.variables.put(key, value);
    }
    
    public Object getVariable(String key) {
        return this.variables.get(key);
    }
}
```

### 4.2 上下文传播

```java
@Service
public class ContextManager {
    
    private final Map<String, WorkflowContext> contexts = new ConcurrentHashMap<>();
    
    public WorkflowContext getOrCreateContext(String sessionId) {
        return contexts.computeIfAbsent(sessionId, k -> WorkflowContext.builder()
            .sessionId(k)
            .variables(new HashMap<>())
            .build());
    }
    
    public void updateContext(String sessionId, Map<String, Object> updates) {
        WorkflowContext context = contexts.get(sessionId);
        if (context != null) {
            context.getVariables().putAll(updates);
        }
    }
    
    public void removeContext(String sessionId) {
        contexts.remove(sessionId);
    }
}
```

## 五、Workflow错误处理

### 5.1 错误处理策略

```java
@Service
public class WorkflowErrorHandler {
    
    public WorkflowResult handleError(WorkflowStep step, Exception error, WorkflowContext context) {
        // 1. 记录错误日志
        logError(step, error, context);
        
        // 2. 根据错误类型处理
        if (error instanceof ToolCallException) {
            return handleToolCallError(step, (ToolCallException) error, context);
        } else if (error instanceof LLMException) {
            return handleLLMError(step, (LLMException) error, context);
        } else {
            return handleGenericError(step, error, context);
        }
    }
    
    private WorkflowResult handleToolCallError(WorkflowStep step, ToolCallException error, WorkflowContext context) {
        // 工具调用失败
        WorkflowResult result = new WorkflowResult();
        result.setStatus("FAILED");
        result.setError("工具调用失败: " + step.getToolName());
        
        // 尝试重试
        if (step.getRetryCount() < step.getMaxRetryCount()) {
            step.setRetryCount(step.getRetryCount() + 1);
            return retryStep(step, context);
        }
        
        return result;
    }
    
    private WorkflowResult handleLLMError(WorkflowStep step, LLMException error, WorkflowContext context) {
        // LLM调用失败
        WorkflowResult result = new WorkflowResult();
        result.setStatus("FAILED");
        result.setError("AI服务暂时不可用，请稍后再试");
        
        return result;
    }
    
    private WorkflowResult handleGenericError(WorkflowStep step, Exception error, WorkflowContext context) {
        // 通用错误处理
        WorkflowResult result = new WorkflowResult();
        result.setStatus("FAILED");
        result.setError("系统错误: " + error.getMessage());
        
        return result;
    }
}
```

### 5.2 重试机制

```java
@Service
public class RetryHandler {
    
    public WorkflowResult retryStep(WorkflowStep step, WorkflowContext context) {
        int maxRetries = 3;
        int retryDelay = 1000;  // 1秒
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(retryDelay * (i + 1));  // 递增延迟
                return executeStep(step, context);
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
            }
        }
        
        throw new RuntimeException("重试次数超限");
    }
}
```

## 六、Workflow监控

### 6.1 监控指标

```java
@Data
@Builder
public class WorkflowMetrics {
    private String workflowId;
    private int totalExecutions;
    private int successfulExecutions;
    private int failedExecutions;
    private double successRate;
    private long averageExecutionTime;
    private long minExecutionTime;
    private long maxExecutionTime;
}
```

### 6.2 监控实现

```java
@Service
public class WorkflowMonitor {
    
    private final Map<String, WorkflowMetrics> metrics = new ConcurrentHashMap<>();
    
    public void recordExecution(String workflowId, boolean success, long duration) {
        WorkflowMetrics metric = metrics.computeIfAbsent(workflowId, k -> 
            WorkflowMetrics.builder()
                .workflowId(k)
                .totalExecutions(0)
                .successfulExecutions(0)
                .failedExecutions(0)
                .build()
        );
        
        metric.setTotalExecutions(metric.getTotalExecutions() + 1);
        
        if (success) {
            metric.setSuccessfulExecutions(metric.getSuccessfulExecutions() + 1);
        } else {
            metric.setFailedExecutions(metric.getFailedExecutions() + 1);
        }
        
        metric.setSuccessRate(
            (double) metric.getSuccessfulExecutions() / metric.getTotalExecutions()
        );
        
        // 更新执行时间
        if (metric.getMinExecutionTime() == 0 || duration < metric.getMinExecutionTime()) {
            metric.setMinExecutionTime(duration);
        }
        if (duration > metric.getMaxExecutionTime()) {
            metric.setMaxExecutionTime(duration);
        }
        
        // 计算平均执行时间
        long totalTime = metric.getAverageExecutionTime() * (metric.getTotalExecutions() - 1) + duration;
        metric.setAverageExecutionTime(totalTime / metric.getTotalExecutions());
    }
    
    public WorkflowMetrics getMetrics(String workflowId) {
        return metrics.get(workflowId);
    }
    
    public List<WorkflowMetrics> getAllMetrics() {
        return new ArrayList<>(metrics.values());
    }
}
```

---

*本文档为CampusPilot Workflow的设计、实现、监控提供完整的指导。*