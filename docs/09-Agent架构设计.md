# CampusPilot Agent架构设计

## 一、Agent架构概述

### 1.1 架构目标
CampusPilot Agent架构旨在实现一个真正具备理解、检索、判断、规划、工具调用、执行、任务跟踪和主动提醒能力的智能体系统。架构需要支持：
- 复杂度自适应：根据任务复杂度自动选择处理策略
- 多工具协作：支持多个工具的协同调用
- 工作流编排：支持复杂业务流程的自动执行
- 安全可控：权限控制、防幻觉、防注入

### 1.2 架构原则
1. **Agent与业务解耦**：Agent层通过API调用业务层，不直接访问数据库
2. **LLM与规则引擎分离**：LLM负责理解和解释，规则引擎负责确定性判断
3. **可插拔Provider**：支持FastGPT和Mock两种Provider
4. **可观测性**：完整的执行日志和追踪
5. **容错性**：优雅降级和错误处理

## 二、Agent分层架构

### 2.1 分层设计

```
┌─────────────────────────────────────────────────────────────────┐
│                    对话层 (Conversation Layer)                   │
│  用户消息处理 │ 会话管理 │ 多轮上下文 │ 历史消息                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    意图层 (Intent Layer)                        │
│  意图识别 │ 复杂度判断 │ 任务路由 │ Slot Filling                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    知识层 (Knowledge Layer)                     │
│  RAG检索 │ 政策查询 │ 版本管理 │ 来源引用 │ 置信度评估                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    推理层 (Reasoning Layer)                     │
│  资格判断 │ 条件匹配 │ 任务规划 │ 决策生成 │ 风险评估                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    执行层 (Action Layer)                        │
│  工具调用 │ API执行 │ 任务创建 │ 通知发送 │ 状态更新                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    跟踪层 (Tracking Layer)                      │
│  进度跟踪 │ 状态监控 │ 日志记录 │ 指标统计 │ 主动提醒                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 各层职责

#### 对话层 (Conversation Layer)
**职责**：
- 接收用户消息
- 管理会话状态
- 维护多轮上下文
- 历史消息存储

**输入**：
- 用户消息
- 会话ID
- 用户身份

**输出**：
- 结构化消息对象
- 会话上下文

#### 意图层 (Intent Layer)
**职责**：
- 识别用户意图
- 判断任务复杂度
- 路由到合适的处理流程
- 提取关键参数

**输入**：
- 用户消息
- 会话上下文

**输出**：
- 意图分类
- 复杂度等级
- 提取的参数

#### 知识层 (Knowledge Layer)
**职责**：
- 检索相关知识
- 查询政策信息
- 管理政策版本
- 生成引用来源
- 评估检索置信度

**输入**：
- 查询意图
- 提取的参数

**输出**：
- 相关知识片段
- 政策信息
- 置信度分数
- 引用来源

#### 推理层 (Reasoning Layer)
**职责**：
- 判断申请资格
- 匹配政策条件
- 规划任务流程
- 生成决策建议
- 评估风险

**输入**：
- 政策信息
- 学生信息
- 学生成绩

**输出**：
- 资格判断结果
- 条件匹配情况
- 任务执行计划
- 风险评估

#### 执行层 (Action Layer)
**职责**：
- 调用后端工具
- 执行API操作
- 创建任务
- 发送通知
- 更新状态

**输入**：
- 执行计划
- 参数列表

**输出**：
- 执行结果
- 工具调用日志

#### 跟踪层 (Tracking Layer)
**职责**：
- 跟踪执行进度
- 监控状态变化
- 记录执行日志
- 统计性能指标
- 触发主动提醒

**输入**：
- 执行日志
- 状态变化

**输出**：
- 进度报告
- 监控指标
- 提醒通知

## 三、复杂度路由设计

### 3.1 复杂度分类

#### Simple (简单)
**特征**：
- 只涉及信息查询
- 不需要工具调用
- 单轮对话即可完成
- 例如：政策查询、流程咨询

**处理策略**：
```
RAG问答
  ↓
直接返回答案
```

#### Complex (复杂)
**特征**：
- 需要多个工具协作
- 需要获取学生信息
- 需要判断条件
- 例如：资格判断、条件查询

**处理策略**：
```
RAG + Tools
  ↓
调用工具获取信息
  ↓
生成综合回答
```

#### Action (任务)
**特征**：
- 需要创建任务
- 需要跟踪进度
- 需要主动提醒
- 例如：事务办理、申请提交

**处理策略**：
```
Workflow
  ↓
多步骤执行
  ↓
创建任务
  ↓
发送通知
```

### 3.2 路由算法

```python
def route_user_input(user_message, session_context):
    # 1. 意图识别
    intent = detect_intent(user_message)
    
    # 2. 复杂度判断
    complexity = analyze_complexity(user_message, intent, session_context)
    
    # 3. 路由决策
    if complexity == "SIMPLE":
        return SimpleRouter()
    elif complexity == "COMPLEX":
        return ComplexRouter()
    elif complexity == "ACTION":
        return ActionRouter()
    else:
        return DefaultRouter()

def analyze_complexity(message, intent, context):
    # 规则1：包含动作词（申请、办理、提交）→ ACTION
    if contains_action_words(message):
        return "ACTION"
    
    # 规则2：需要多个信息源 → COMPLEX
    if requires_multiple_sources(intent):
        return "COMPLEX"
    
    # 规则3：简单查询 → SIMPLE
    if is_simple_query(intent):
        return "SIMPLE"
    
    # 规则4：默认为COMPLEX
    return "COMPLEX"
```

### 3.3 路由实现

```java
@Service
public class ComplexityRouter {
    
    @Autowired
    private IntentDetector intentDetector;
    
    @Autowired
    private LLMService llmService;
    
    public RouteResult route(String message, SessionContext context) {
        // 1. 意图识别
        Intent intent = intentDetector.detect(message);
        
        // 2. 复杂度判断
        Complexity complexity = analyzeComplexity(message, intent, context);
        
        // 3. 返回路由结果
        return RouteResult.builder()
            .intent(intent)
            .complexity(complexity)
            .router(getRouter(complexity))
            .build();
    }
    
    private Complexity analyzeComplexity(String message, Intent intent, SessionContext context) {
        // 基于规则的复杂度判断
        if (containsActionWords(message)) {
            return Complexity.ACTION;
        }
        
        if (requiresMultipleTools(intent)) {
            return Complexity.COMPLEX;
        }
        
        return Complexity.SIMPLE;
    }
    
    private boolean containsActionWords(String message) {
        String[] actionWords = {"申请", "办理", "提交", "创建", "帮我"};
        for (String word : actionWords) {
            if (message.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
```

## 四、意图识别设计

### 4.1 意图分类

#### 政策咨询类
- **POLICY_QUERY**：政策查询
- **POLICY_COMPARE**：政策对比
- **POLICY_EXPLAIN**：政策解释

#### 资格判断类
- **ELIGIBILITY_CHECK**：资格检查
- **CONDITION_MATCH**：条件匹配
- **DOCUMENT_CHECK**：材料检查

#### 事务办理类
- **TASK_CREATE**：创建任务
- **TASK_STATUS**：任务状态查询
- **TASK_UPDATE**：任务更新

#### 信息查询类
- **STUDENT_INFO**：学生信息查询
- **SCORE_QUERY**：成绩查询
- **STATUS_QUERY**：状态查询

#### 主动服务类
- **REMINDER**：提醒
- **DEADLINE_CHECK**：截止日期检查
- **RISK_ALERT**：风险预警

### 4.2 意图识别实现

```java
@Service
public class IntentDetector {
    
    @Autowired
    private LLMService llmService;
    
    @Autowired
    private RuleEngine ruleEngine;
    
    public Intent detect(String message) {
        // 1. 规则匹配
        Intent ruleBasedIntent = ruleEngine.matchIntent(message);
        if (ruleBasedIntent != null) {
            return ruleBasedIntent;
        }
        
        // 2. LLM识别
        Intent llmIntent = llmService.detectIntent(message);
        
        // 3. 置信度评估
        if (llmIntent.getConfidence() > 0.8) {
            return llmIntent;
        }
        
        // 4. 默认意图
        return Intent.GENERAL_QUERY;
    }
}

// 意图定义
public enum Intent {
    // 政策咨询
    POLICY_QUERY,
    POLICY_COMPARE,
    POLICY_EXPLAIN,
    
    // 资格判断
    ELIGIBILITY_CHECK,
    CONDITION_MATCH,
    DOCUMENT_CHECK,
    
    // 事务办理
    TASK_CREATE,
    TASK_STATUS,
    TASK_UPDATE,
    
    // 信息查询
    STUDENT_INFO,
    SCORE_QUERY,
    STATUS_QUERY,
    
    // 主动服务
    REMINDER,
    DEADLINE_CHECK,
    RISK_ALERT,
    
    // 默认
    GENERAL_QUERY
}
```

### 4.3 Slot Filling

```java
@Data
public class SlotFillingResult {
    private Intent intent;
    private Map<String, Object> slots;
    private List<String> missingSlots;
    private boolean complete;
}

public SlotFillingResult fillSlots(String message, Intent intent) {
    Map<String, Object> slots = new HashMap<>();
    
    // 根据意图提取槽位
    switch (intent) {
        case ELIGIBILITY_CHECK:
            // 提取政策名称
            slots.put("policyName", extractPolicyName(message));
            // 提取学生信息（可选）
            slots.put("studentId", extractStudentId(message));
            break;
            
        case TASK_CREATE:
            // 提取任务类型
            slots.put("taskType", extractTaskType(message));
            // 提取截止时间
            slots.put("deadline", extractDeadline(message));
            break;
    }
    
    // 检查完整性
    List<String> missingSlots = findMissingSlots(slots, intent);
    
    return SlotFillingResult.builder()
        .intent(intent)
        .slots(slots)
        .missingSlots(missingSlots)
        .complete(missingSlots.isEmpty())
        .build();
}
```

## 五、RAG检索设计

### 5.1 RAG流程

```
用户查询
  ↓
Query Rewrite (查询改写)
  ↓
Intent Detection (意图检测)
  ↓
Metadata Filter (元数据过滤)
  ↓
Vector Retrieval (向量检索)
  ↓
Rerank (重排序)
  ↓
Relevant Chunks (相关片段)
  ↓
LLM (大语言模型)
  ↓
Answer (回答)
  ↓
Citation (引用)
```

### 5.2 Query Rewrite

```java
@Service
public class QueryRewriter {
    
    @Autowired
    private LLMService llmService;
    
    public String rewrite(String originalQuery) {
        // 1. 同义词扩展
        String expandedQuery = expandSynonyms(originalQuery);
        
        // 2. 查询改写
        String rewrittenQuery = llmService.rewriteQuery(originalQuery);
        
        // 3. 查询合并
        return mergeQueries(originalQuery, expandedQuery, rewrittenQuery);
    }
    
    private String expandSynonyms(String query) {
        // 同义词映射
        Map<String, List<String>> synonyms = Map.of(
            "奖学金", Arrays.asList("助学金", "奖励", "资助"),
            "申请", Arrays.asList("办理", "提交", "申领"),
            "条件", Arrays.asList("要求", "资格", "标准")
        );
        
        // 扩展查询
        StringBuilder expanded = new StringBuilder(query);
        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String synonym : entry.getValue()) {
                    expanded.append(" ").append(synonym);
                }
            }
        }
        
        return expanded.toString();
    }
}
```

### 5.3 Metadata Filter

```java
@Service
public class MetadataFilter {
    
    public MetadataFilterResult filter(String query, Map<String, Object> context) {
        MetadataFilterResult result = new MetadataFilterResult();
        
        // 1. 政策分类过滤
        if (context.containsKey("category")) {
            result.addFilter("category", context.get("category"));
        }
        
        // 2. 政策状态过滤（只查询有效政策）
        result.addFilter("status", "ACTIVE");
        
        // 3. 时间过滤（只查询当前有效版本）
        result.addFilter("effectiveDate", "<=", LocalDate.now());
        result.addFilter("expiryDate", ">=", LocalDate.now());
        
        // 4. 年级过滤（如果适用）
        if (context.containsKey("grade")) {
            result.addFilter("audience", "CONTAINS", context.get("grade"));
        }
        
        return result;
    }
}
```

### 5.4 Vector Retrieval

```java
@Service
public class VectorRetriever {
    
    @Autowired
    private FastGPTClient fastGPTClient;
    
    public List<RetrievedChunk> retrieve(String query, MetadataFilterResult filters, int topK) {
        // 1. 构建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .filters(filters.toMap())
            .topK(topK)
            .build();
        
        // 2. 调用FastGPT知识库API
        List<SearchResult> results = fastGPTClient.searchKnowledge(request);
        
        // 3. 转换为检索片段
        return results.stream()
            .map(this::convertToChunk)
            .collect(Collectors.toList());
    }
}
```

### 5.5 Rerank

```java
@Service
public class Reranker {
    
    @Autowired
    private LLMService llmService;
    
    public List<RerankedChunk> rerank(String query, List<RetrievedChunk> chunks) {
        // 1. 计算相关性分数
        List<ScoredChunk> scoredChunks = chunks.stream()
            .map(chunk -> {
                double score = calculateRelevance(query, chunk);
                return new ScoredChunk(chunk, score);
            })
            .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
            .collect(Collectors.toList());
        
        // 2. 去重
        List<RerankedChunk> deduplicated = deduplicate(scoredChunks);
        
        // 3. 返回Top-K
        return deduplicated.stream()
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private double calculateRelevance(String query, RetrievedChunk chunk) {
        // 语义相似度 + 关键词匹配 + 政策时效性
        double semanticScore = llmService.calculateSimilarity(query, chunk.getContent());
        double keywordScore = calculateKeywordMatch(query, chunk.getContent());
        double timelinessScore = calculateTimeliness(chunk.getEffectiveDate());
        
        return semanticScore * 0.6 + keywordScore * 0.3 + timelinessScore * 0.1;
    }
}
```

### 5.6 Citation Generation

```java
@Service
public class CitationGenerator {
    
    public List<Citation> generate(String query, List<RerankedChunk> chunks) {
        return chunks.stream()
            .map(chunk -> Citation.builder()
                .policyId(chunk.getPolicyId())
                .policyName(chunk.getPolicyName())
                .version(chunk.getVersion())
                .source(chunk.getSource())
                .effectiveDate(chunk.getEffectiveDate())
                .relevance(chunk.getScore())
                .excerpt(chunk.getContent().substring(0, Math.min(200, chunk.getContent().length())))
                .build())
            .collect(Collectors.toList());
    }
}
```

## 六、资格判断引擎

### 6.1 判断流程

```
LLM提取条件
  ↓
规则引擎处理
  ↓
确定性判断
  ↓
LLM解释结果
```

**重要原则**：LLM负责理解和解释，规则引擎负责最终判断。

### 6.2 条件提取

```java
@Service
public class ConditionExtractor {
    
    @Autowired
    private LLMService llmService;
    
    public List<Condition> extract(String policyContent) {
        // 使用LLM提取政策条件
        String prompt = buildExtractionPrompt(policyContent);
        String response = llmService.generate(prompt);
        
        // 解析LLM输出
        return parseConditions(response);
    }
    
    private String buildExtractionPrompt(String policyContent) {
        return String.format("""
            请从以下政策内容中提取申请条件：
            
            政策内容：
            %s
            
            请以JSON格式输出条件列表，包含以下字段：
            - conditionName: 条件名称
            - conditionType: 条件类型（GRADE/SCORE/CREDIT/STATUS）
            - conditionValue: 条件值
            - operator: 运算符（EQ/GT/LT/GTE/LTE/IN/BETWEEN）
            
            只输出JSON，不要其他内容。
            """, policyContent);
    }
}
```

### 6.3 条件匹配

```java
@Service
public class ConditionMatcher {
    
    public MatchResult match(List<Condition> conditions, StudentInfo student, StudentScores scores) {
        List<ConditionResult> results = new ArrayList<>();
        
        for (Condition condition : conditions) {
            ConditionResult result = matchCondition(condition, student, scores);
            results.add(result);
        }
        
        // 计算总体结果
        boolean allMet = results.stream().allMatch(ConditionResult::isMet);
        double score = calculateScore(results);
        
        return MatchResult.builder()
            .eligible(allMet)
            .score(score)
            .conditions(results)
            .build();
    }
    
    private ConditionResult matchCondition(Condition condition, StudentInfo student, StudentScores scores) {
        Object actualValue = getActualValue(condition, student, scores);
        boolean met = evaluateCondition(condition, actualValue);
        
        return ConditionResult.builder()
            .conditionName(condition.getConditionName())
            .required(condition.getConditionValue())
            .actual(actualValue.toString())
            .met(met)
            .build();
    }
    
    private Object getActualValue(Condition condition, StudentInfo student, StudentScores scores) {
        switch (condition.getConditionType()) {
            case GRADE:
                return student.getGrade();
            case SCORE:
                return scores.getGpa();
            case CREDIT:
                return scores.getTotalCredits();
            case STATUS:
                return student.getStatus();
            default:
                return null;
        }
    }
    
    private boolean evaluateCondition(Condition condition, Object actualValue) {
        switch (condition.getOperator()) {
            case EQ:
                return actualValue.toString().equals(condition.getConditionValue());
            case GT:
                return Double.parseDouble(actualValue.toString()) > Double.parseDouble(condition.getConditionValue());
            case LT:
                return Double.parseDouble(actualValue.toString()) < Double.parseDouble(condition.getConditionValue());
            case GTE:
                return Double.parseDouble(actualValue.toString()) >= Double.parseDouble(condition.getConditionValue());
            case LTE:
                return Double.parseDouble(actualValue.toString()) <= Double.parseDouble(condition.getConditionValue());
            case IN:
                return Arrays.asList(condition.getConditionValue().split(",")).contains(actualValue.toString());
            case BETWEEN:
                String[] range = condition.getConditionValue().split("-");
                double value = Double.parseDouble(actualValue.toString());
                return value >= Double.parseDouble(range[0]) && value <= Double.parseDouble(range[1]);
            default:
                return false;
        }
    }
}
```

### 6.4 结果解释

```java
@Service
public class ResultExplainer {
    
    @Autowired
    private LLMService llmService;
    
    public String explain(MatchResult result, String policyName) {
        String prompt = buildExplanationPrompt(result, policyName);
        return llmService.generate(prompt);
    }
    
    private String buildExplanationPrompt(MatchResult result, String policyName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下资格判断结果，生成通俗易懂的解释：\n\n");
        prompt.append("政策名称：").append(policyName).append("\n");
        prompt.append("是否符合资格：").append(result.isEligible() ? "是" : "否").append("\n");
        prompt.append("综合评分：").append(result.getScore()).append("\n\n");
        prompt.append("条件详情：\n");
        
        for (ConditionResult condition : result.getConditions()) {
            prompt.append("- ").append(condition.getConditionName())
                  .append("：要求").append(condition.getRequired())
                  .append("，实际").append(condition.getActual())
                  .append(condition.isMet() ? " ✓" : " ✗")
                  .append("\n");
        }
        
        prompt.append("\n请生成解释，包括：\n");
        prompt.append("1. 总体评价\n");
        prompt.append("2. 满足的条件\n");
        prompt.append("3. 不满足的条件\n");
        prompt.append("4. 改进建议\n");
        
        return prompt.toString();
    }
}
```

## 七、工具调用设计

### 7.1 工具定义

```java
@Data
@Builder
public class ToolDefinition {
    private String name;
    private String description;
    private String httpMethod;
    private String url;
    private List<Parameter> parameters;
    private int timeout;
    private int retryCount;
}

@Data
@Builder
public class Parameter {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private Object defaultValue;
}
```

### 7.2 工具注册

```java
@Service
public class ToolRegistry {
    
    private final Map<String, ToolDefinition> tools = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册获取学生信息工具
        registerTool(ToolDefinition.builder()
            .name("get_student_info")
            .description("获取学生基本信息")
            .httpMethod("POST")
            .url("/api/agent/tools/student-info")
            .parameters(Arrays.asList(
                Parameter.builder().name("studentId").type("Long").description("学生ID").required(false).build()
            ))
            .timeout(5000)
            .retryCount(3)
            .build());
        
        // 注册获取学生成绩工具
        registerTool(ToolDefinition.builder()
            .name("get_student_scores")
            .description("获取学生成绩信息")
            .httpMethod("POST")
            .url("/api/agent/tools/student-scores")
            .parameters(Arrays.asList(
                Parameter.builder().name("studentId").type("Long").description("学生ID").required(false).build(),
                Parameter.builder().name("semester").type("String").description("学期").required(false).build()
            ))
            .timeout(5000)
            .retryCount(3)
            .build());
        
        // 注册其他工具...
    }
    
    public void registerTool(ToolDefinition tool) {
        tools.put(tool.getName(), tool);
    }
    
    public ToolDefinition getTool(String name) {
        return tools.get(name);
    }
    
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(tools.values());
    }
}
```

### 7.3 工具调用

```java
@Service
public class ToolCaller {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    public ToolResult call(String toolName, Map<String, Object> params, String userToken) {
        // 1. 获取工具定义
        ToolDefinition tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return ToolResult.error("工具不存在: " + toolName);
        }
        
        // 2. 参数校验
        ValidationResult validation = validateParams(params, tool);
        if (!validation.isValid()) {
            return ToolResult.error("参数校验失败: " + validation.getMessage());
        }
        
        // 3. 权限检查
        if (!checkPermission(toolName, userToken)) {
            return ToolResult.error("权限不足");
        }
        
        // 4. 调用工具
        try {
            String url = buildUrl(tool, params);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + userToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<ToolResponse> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(tool.getHttpMethod()),
                request,
                ToolResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody().isSuccess()) {
                return ToolResult.success(response.getBody().getData());
            } else {
                return ToolResult.error(response.getBody().getMessage());
            }
        } catch (Exception e) {
            return ToolResult.error("工具调用失败: " + e.getMessage());
        }
    }
    
    private boolean checkPermission(String toolName, String userToken) {
        // 根据工具名称和用户Token检查权限
        // 这里应该调用后端权限检查接口
        return true;
    }
}
```

### 7.4 工具选择

```java
@Service
public class ToolSelector {
    
    @Autowired
    private LLMService llmService;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    public List<ToolCall> selectTools(Intent intent, Map<String, Object> slots) {
        // 1. 基于规则的工具选择
        List<ToolCall> ruleBasedTools = selectByRules(intent, slots);
        
        // 2. LLM辅助选择
        List<ToolCall> llmTools = selectByLLM(intent, slots);
        
        // 3. 合并结果
        return mergeToolCalls(ruleBasedTools, llmTools);
    }
    
    private List<ToolCall> selectByRules(Intent intent, Map<String, Object> slots) {
        List<ToolCall> tools = new ArrayList<>();
        
        switch (intent) {
            case ELIGIBILITY_CHECK:
                // 需要获取学生信息
                tools.add(ToolCall.builder()
                    .toolName("get_student_info")
                    .params(Map.of("studentId", slots.getOrDefault("studentId", "current")))
                    .build());
                
                // 需要获取学生成绩
                tools.add(ToolCall.builder()
                    .toolName("get_student_scores")
                    .params(Map.of("studentId", slots.getOrDefault("studentId", "current")))
                    .build());
                
                // 需要搜索政策
                tools.add(ToolCall.builder()
                    .toolName("search_policy")
                    .params(Map.of("query", slots.get("policyName")))
                    .build());
                break;
                
            case TASK_CREATE:
                // 需要创建任务
                tools.add(ToolCall.builder()
                    .toolName("create_todo")
                    .params(slots)
                    .build());
                break;
        }
        
        return tools;
    }
}
```

## 八、工作流设计

### 8.1 工作流定义

```java
@Data
@Builder
public class WorkflowDefinition {
    private String id;
    private String name;
    private String description;
    private List<WorkflowStep> steps;
    private String triggerIntent;
}

@Data
@Builder
public class WorkflowStep {
    private int order;
    private String name;
    private String type;  // TOOL, LLM, RULE, CONDITION
    private String toolName;
    private String prompt;
    private Map<String, Object> params;
    private List<WorkflowCondition> conditions;
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

### 8.2 工作流注册

```java
@Service
public class WorkflowRegistry {
    
    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册资格判断工作流
        registerWorkflow(WorkflowDefinition.builder()
            .id("eligibility_check")
            .name("资格判断工作流")
            .description("判断学生是否满足政策条件")
            .triggerIntent("ELIGIBILITY_CHECK")
            .steps(Arrays.asList(
                WorkflowStep.builder()
                    .order(1)
                    .name("获取学生信息")
                    .type("TOOL")
                    .toolName("get_student_info")
                    .build(),
                WorkflowStep.builder()
                    .order(2)
                    .name("获取学生成绩")
                    .type("TOOL")
                    .toolName("get_student_scores")
                    .build(),
                WorkflowStep.builder()
                    .order(3)
                    .name("搜索政策")
                    .type("TOOL")
                    .toolName("search_policy")
                    .build(),
                WorkflowStep.builder()
                    .order(4)
                    .name("条件匹配")
                    .type("RULE")
                    .build(),
                WorkflowStep.builder()
                    .order(5)
                    .name("生成结果")
                    .type("LLM")
                    .prompt("根据以下信息生成资格判断结果：...")
                    .build()
            ))
            .build());
        
        // 注册其他工作流...
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

### 8.3 工作流执行

```java
@Service
public class WorkflowExecutor {
    
    @Autowired
    private ToolCaller toolCaller;
    
    @Autowired
    private RuleEngine ruleEngine;
    
    @Autowired
    private LLMService llmService;
    
    public WorkflowResult execute(WorkflowDefinition workflow, Map<String, Object> context, String userToken) {
        WorkflowResult result = new WorkflowResult();
        Map<String, Object> stepResults = new HashMap<>();
        
        for (WorkflowStep step : workflow.getSteps()) {
            try {
                Object stepResult = executeStep(step, context, stepResults, userToken);
                stepResults.put(step.getName(), stepResult);
                
                result.addStep(StepResult.builder()
                    .stepName(step.getName())
                    .status("SUCCESS")
                    .result(stepResult)
                    .build());
            } catch (Exception e) {
                result.addStep(StepResult.builder()
                    .stepName(step.getName())
                    .status("FAILED")
                    .error(e.getMessage())
                    .build());
                
                result.setStatus("FAILED");
                result.setError("工作流执行失败: " + step.getName());
                return result;
            }
        }
        
        result.setStatus("SUCCESS");
        result.setResult(stepResults);
        return result;
    }
    
    private Object executeStep(WorkflowStep step, Map<String, Object> context, Map<String, Object> stepResults, String userToken) {
        switch (step.getType()) {
            case "TOOL":
                Map<String, Object> params = resolveParams(step.getParams(), context, stepResults);
                ToolResult toolResult = toolCaller.call(step.getToolName(), params, userToken);
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
                // 条件判断逻辑
                return evaluateConditions(step.getConditions(), context, stepResults);
                
            default:
                throw new RuntimeException("未知的步骤类型: " + step.getType());
        }
    }
}
```

## 九、防幻觉设计

### 9.1 幻觉检测

```java
@Service
public class HallucinationDetector {
    
    @Autowired
    private KnowledgeBase knowledgeBase;
    
    public HallucinationResult detect(String response, List<Citation> citations) {
        // 1. 检查引用有效性
        boolean hasValidCitations = validateCitations(citations);
        
        // 2. 检查事实一致性
        boolean isFactuallyConsistent = checkFactualConsistency(response, citations);
        
        // 3. 检查编造内容
        boolean hasFabrication = detectFabrication(response);
        
        // 4. 计算置信度
        double confidence = calculateConfidence(hasValidCitations, isFactuallyConsistent, hasFabrication);
        
        return HallucinationResult.builder()
            .hasHallucination(!hasValidCitations || !isFactuallyConsistent || hasFabrication)
            .confidence(confidence)
            .hasValidCitations(hasValidCitations)
            .isFactuallyConsistent(isFactuallyConsistent)
            .hasFabrication(hasFabrication)
            .build();
    }
    
    private boolean validateCitations(List<Citation> citations) {
        if (citations == null || citations.isEmpty()) {
            return false;
        }
        
        for (Citation citation : citations) {
            // 检查政策是否存在
            Policy policy = knowledgeBase.getPolicy(citation.getPolicyId());
            if (policy == null) {
                return false;
            }
            
            // 检查政策版本是否有效
            PolicyVersion version = knowledgeBase.getPolicyVersion(citation.getPolicyId(), citation.getVersion());
            if (version == null || !version.getStatus().equals("ACTIVE")) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean checkFactualConsistency(String response, List<Citation> citations) {
        // 使用LLM检查事实一致性
        String prompt = String.format("""
            请检查以下回答是否与引用的政策内容一致：
            
            回答：%s
            
            引用内容：
            %s
            
            请回答"是"或"否"。
            """, response, formatCitations(citations));
        
        String result = llmService.generate(prompt);
        return result.trim().equals("是");
    }
}
```

### 9.2 防幻觉策略

```java
@Service
public class AntiHallucinationStrategy {
    
    @Autowired
    private HallucinationDetector detector;
    
    public String process(String response, List<Citation> citations) {
        // 1. 检测幻觉
        HallucinationResult result = detector.detect(response, citations);
        
        // 2. 根据检测结果处理
        if (result.isHasHallucination()) {
            if (result.getConfidence() < 0.3) {
                // 高度疑似幻觉，拒绝回答
                return "知识库暂未找到有效政策依据，请联系相关部门确认。";
            } else if (result.getConfidence() < 0.6) {
                // 中度疑似幻觉，添加免责声明
                return response + "\n\n⚠️ 以上信息仅供参考，具体请以学校官方发布为准。";
            } else {
                // 低度疑似幻觉，正常返回但标记
                return response;
            }
        }
        
        return response;
    }
}
```

## 十、主动服务设计

### 10.1 截止日期监控

```java
@Service
public class DeadlineMonitor {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Scheduled(cron = "0 0 9 * * ?")  // 每天早上9点执行
    public void checkDeadlines() {
        // 1. 查询即将到期的任务
        List<Task> urgentTasks = taskService.findTasksDueSoon(3);  // 3天内到期
        
        // 2. 生成提醒
        for (Task task : urgentTasks) {
            generateReminder(task);
        }
    }
    
    private void generateReminder(Task task) {
        // 计算剩余天数
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline().toLocalDate());
        
        // 生成提醒内容
        String content = String.format(
            "你的任务「%s」将在%d天后截止，请尽快完成。",
            task.getTitle(),
            daysLeft
        );
        
        // 发送通知
        notificationService.send(Notification.builder()
            .userId(task.getUserId())
            .title("任务截止提醒")
            .content(content)
            .type("REMINDER")
            .relatedTaskId(task.getId())
            .build());
    }
}
```

### 10.2 状态跟踪

```java
@Service
public class StatusTracker {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private NotificationService notificationService;
    
    @EventListener
    public void onTaskStatusChange(TaskStatusChangeEvent event) {
        Task task = event.getTask();
        String oldStatus = event.getOldStatus();
        String newStatus = event.getNewStatus();
        
        // 生成状态变更通知
        String content = String.format(
            "你的任务「%s」状态已更新：%s → %s",
            task.getTitle(),
            getStatusName(oldStatus),
            getStatusName(newStatus)
        );
        
        notificationService.send(Notification.builder()
            .userId(task.getUserId())
            .title("任务状态更新")
            .content(content)
            .type("INFO")
            .relatedTaskId(task.getId())
            .build());
    }
}
```

## 十一、Agent执行追踪

### 11.1 追踪数据结构

```java
@Data
@Builder
public class ExecutionTrace {
    private String executionId;
    private String sessionId;
    private Long userId;
    private String messageId;
    private String intent;
    private String workflow;
    private List<TraceStep> steps;
    private long startTime;
    private long endTime;
    private long duration;
    private String status;
    private String error;
}

@Data
@Builder
public class TraceStep {
    private String type;  // intent, knowledge, tool, decision, response
    private String name;
    private String status;
    private long startTime;
    private long endTime;
    private long duration;
    private Object input;
    private Object output;
    private String error;
}
```

### 11.2 追踪记录

```java
@Service
public class ExecutionTracer {
    
    @Autowired
    private AgentExecutionLogMapper logMapper;
    
    public void record(ExecutionTrace trace) {
        // 1. 保存执行日志
        AgentExecutionLog log = AgentExecutionLog.builder()
            .executionId(trace.getExecutionId())
            .sessionId(trace.getSessionId())
            .userId(trace.getUserId())
            .messageId(trace.getMessageId())
            .intent(trace.getIntent())
            .workflow(trace.getWorkflow())
            .tools(extractToolNames(trace.getSteps()))
            .startTime(new Date(trace.getStartTime()))
            .endTime(new Date(trace.getEndTime()))
            .duration(trace.getDuration())
            .status(trace.getStatus())
            .error(trace.getError())
            .build();
        
        logMapper.insert(log);
        
        // 2. 保存步骤日志
        for (TraceStep step : trace.getSteps()) {
            ToolExecutionLog toolLog = ToolExecutionLog.builder()
                .executionLogId(log.getId())
                .toolName(step.getName())
                .toolInput(serializeInput(step.getInput()))
                .toolOutput(serializeOutput(step.getOutput()))
                .duration(step.getDuration())
                .status(step.getStatus())
                .error(step.getError())
                .build();
            
            toolLogMapper.insert(toolLog);
        }
    }
    
    private List<String> extractToolNames(List<TraceStep> steps) {
        return steps.stream()
            .filter(step -> step.getType().equals("tool"))
            .map(TraceStep::getName)
            .collect(Collectors.toList());
    }
}
```

## 十二、Mock机制

### 12.1 Mock Provider

```java
@Service
@Profile("dev")
public class MockAgentProvider implements AgentProvider {
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Override
    public AgentResponse chat(String message, String sessionId) {
        // 模拟延迟
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 模拟意图识别
        Intent intent = mockIntentDetection(message);
        
        // 模拟复杂度判断
        Complexity complexity = mockComplexityAnalysis(message, intent);
        
        // 模拟工具调用
        List<ToolCall> toolCalls = mockToolSelection(intent);
        
        // 模拟响应生成
        String response = mockResponseGeneration(message, intent, complexity, toolCalls);
        
        // 生成执行追踪
        ExecutionTrace trace = mockExecutionTrace(message, intent, complexity, toolCalls);
        
        return AgentResponse.builder()
            .response(response)
            .intent(intent.name())
            .complexity(complexity.name())
            .toolCalls(toolCalls)
            .executionTrace(trace)
            .build();
    }
    
    private Intent mockIntentDetection(String message) {
        if (message.contains("奖学金")) {
            return Intent.POLICY_QUERY;
        } else if (message.contains("申请") || message.contains("办理")) {
            return Intent.TASK_CREATE;
        } else if (message.contains("成绩")) {
            return Intent.SCORE_QUERY;
        }
        return Intent.GENERAL_QUERY;
    }
    
    private Complexity mockComplexityAnalysis(String message, Intent intent) {
        if (message.contains("帮我") || message.contains("申请")) {
            return Complexity.ACTION;
        } else if (message.contains("判断") || message.contains("条件")) {
            return Complexity.COMPLEX;
        }
        return Complexity.SIMPLE;
    }
}
```

---

*本Agent架构设计文档为CampusPilot Agent系统的开发、测试、优化提供完整的设计指导。*