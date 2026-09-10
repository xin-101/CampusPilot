# CampusPilot Tool API设计

## 一、Tool概述

### 1.1 设计目标
Tool是CampusPilot Agent与后端业务系统交互的桥梁。Tool需要：
- 提供统一的API接口
- 支持参数校验
- 支持权限控制
- 支持错误处理
- 支持日志记录

### 1.2 设计原则
1. **统一接口**：所有Tool使用统一的调用方式
2. **结构化返回**：返回结构化的JSON数据
3. **权限控制**：Tool内部进行权限校验
4. **幂等性**：相同参数调用结果一致
5. **可观测**：完整的调用日志

## 二、Tool定义

### 2.1 Tool元数据

```java
@Data
@Builder
public class ToolDefinition {
    private String name;              // 工具名称
    private String description;       // 工具描述
    private String httpMethod;        // HTTP方法
    private String url;               // API URL
    private List<Parameter> parameters;  // 参数列表
    private int timeout;              // 超时时间(ms)
    private int retryCount;           // 重试次数
    private List<String> requiredRoles;  // 所需角色
}

@Data
@Builder
public class Parameter {
    private String name;              // 参数名
    private String type;              // 参数类型
    private String description;       // 参数描述
    private boolean required;         // 是否必填
    private Object defaultValue;      // 默认值
    private List<String> options;     // 可选值
}
```

### 2.2 Tool注册

```java
@Service
public class ToolRegistry {
    
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册获取学生信息工具
        registerTool(ToolDefinition.builder()
            .name("get_student_info")
            .description("获取学生基本信息")
            .httpMethod("POST")
            .url("/api/agent/tools/student-info")
            .parameters(Arrays.asList(
                Parameter.builder()
                    .name("studentId")
                    .type("Long")
                    .description("学生ID")
                    .required(false)
                    .build()
            ))
            .timeout(5000)
            .retryCount(3)
            .requiredRoles(Arrays.asList("STUDENT", "COUNSELOR", "ADMIN"))
            .build());
        
        // 注册获取学生成绩工具
        registerTool(ToolDefinition.builder()
            .name("get_student_scores")
            .description("获取学生成绩信息")
            .httpMethod("POST")
            .url("/api/agent/tools/student-scores")
            .parameters(Arrays.asList(
                Parameter.builder()
                    .name("studentId")
                    .type("Long")
                    .description("学生ID")
                    .required(false)
                    .build(),
                Parameter.builder()
                    .name("semester")
                    .type("String")
                    .description("学期")
                    .required(false)
                    .options(Arrays.asList("2024-2025-1", "2024-2025-2"))
                    .build()
            ))
            .timeout(5000)
            .retryCount(3)
            .requiredRoles(Arrays.asList("STUDENT", "COUNSELOR", "ADMIN"))
            .build());
        
        // 注册搜索政策工具
        registerTool(ToolDefinition.builder()
            .name("search_policy")
            .description("搜索政策知识库")
            .httpMethod("POST")
            .url("/api/agent/tools/search-policy")
            .parameters(Arrays.asList(
                Parameter.builder()
                    .name("query")
                    .type("String")
                    .description("搜索查询")
                    .required(true)
                    .build(),
                Parameter.builder()
                    .name("category")
                    .type("String")
                    .description("政策分类")
                    .required(false)
                    .options(Arrays.asList("SCHOLARSHIP", "LEAVE", "EXAMINATION", "DORMITORY", "CERTIFICATE"))
                    .build(),
                Parameter.builder()
                    .name("topK")
                    .type("Integer")
                    .description("返回数量")
                    .required(false)
                    .defaultValue(5)
                    .build()
            ))
            .timeout(10000)
            .retryCount(2)
            .requiredRoles(Arrays.asList("STUDENT", "COUNSELOR", "ADMIN"))
            .build());
        
        // 注册检查资格工具
        registerTool(ToolDefinition.builder()
            .name("check_eligibility")
            .description("检查学生是否满足政策条件")
            .httpMethod("POST")
            .url("/api/agent/tools/check-eligibility")
            .parameters(Arrays.asList(
                Parameter.builder()
                    .name("policyId")
                    .type("Long")
                    .description("政策ID")
                    .required(true)
                    .build(),
                Parameter.builder()
                    .name("studentId")
                    .type("Long")
                    .description("学生ID")
                    .required(false)
                    .build()
            ))
            .timeout(10000)
            .retryCount(2)
            .requiredRoles(Arrays.asList("STUDENT", "COUNSELOR", "ADMIN"))
            .build());
        
        // 注册创建待办工具
        registerTool(ToolDefinition.builder()
            .name("create_todo")
            .description("创建待办事项")
            .httpMethod("POST")
            .url("/api/agent/tools/create-todo")
            .parameters(Arrays.asList(
                Parameter.builder()
                    .name("title")
                    .type("String")
                    .description("任务标题")
                    .required(true)
                    .build(),
                Parameter.builder()
                    .name("description")
                    .type("String")
                    .description("任务描述")
                    .required(false)
                    .build(),
                Parameter.builder()
                    .name("deadline")
                    .type("String")
                    .description("截止时间")
                    .required(false)
                    .build(),
                Parameter.builder()
                    .name("relatedPolicyId")
                    .type("Long")
                    .description("关联政策ID")
                    .required(false)
                    .build()
            ))
            .timeout(5000)
            .retryCount(3)
            .requiredRoles(Arrays.asList("STUDENT", "COUNSELOR", "ADMIN"))
            .build());
        
        // 注册发送通知工具
        registerTool(ToolDefinition.builder()
            .name("send_notification")
            .description("发送通知")
            .httpMethod("POST")
            .url("/api/agent/tools/send-notification")
            .parameters(Arrays.asList(
                Parameter.builder()
                    .name("studentId")
                    .type("Long")
                    .description("学生ID")
                    .required(true)
                    .build(),
                Parameter.builder()
                    .name("title")
                    .type("String")
                    .description("通知标题")
                    .required(true)
                    .build(),
                Parameter.builder()
                    .name("content")
                    .type("String")
                    .description("通知内容")
                    .required(true)
                    .build(),
                Parameter.builder()
                    .name("type")
                    .type("String")
                    .description("通知类型")
                    .required(false)
                    .options(Arrays.asList("INFO", "WARNING", "URGENT", "REMINDER"))
                    .defaultValue("INFO")
                    .build()
            ))
            .timeout(5000)
            .retryCount(3)
            .requiredRoles(Arrays.asList("STUDENT", "COUNSELOR", "ADMIN"))
            .build());
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

## 三、Tool调用

### 3.1 Tool调用器

```java
@Service
@Slf4j
public class ToolCaller {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private ToolExecutionLogMapper logMapper;
    
    public ToolResult call(String toolName, Map<String, Object> params, String userToken) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        // 1. 获取工具定义
        ToolDefinition tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return ToolResult.error("工具不存在: " + toolName, "TOOL_NOT_FOUND");
        }
        
        // 2. 参数校验
        ValidationResult validation = validateParams(params, tool);
        if (!validation.isValid()) {
            return ToolResult.error("参数校验失败: " + validation.getMessage(), "INVALID_PARAMS");
        }
        
        // 3. 权限检查
        if (!checkPermission(tool, userToken)) {
            return ToolResult.error("权限不足", "PERMISSION_DENIED");
        }
        
        // 4. 调用工具
        try {
            String url = buildUrl(tool, params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + userToken);
            headers.set("X-Request-Id", requestId);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<ToolResponse> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(tool.getHttpMethod()),
                request,
                ToolResponse.class
            );
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 5. 记录日志
            recordLog(toolName, params, response.getBody(), duration, requestId);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody().isSuccess()) {
                return ToolResult.success(response.getBody().getData());
            } else {
                return ToolResult.error(response.getBody().getMessage(), response.getBody().getCode());
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("工具调用失败: tool={}, requestId={}", toolName, requestId, e);
            
            // 记录错误日志
            recordLog(toolName, params, null, duration, requestId, e.getMessage());
            
            return ToolResult.error("工具调用失败: " + e.getMessage(), "TOOL_CALL_FAILED");
        }
    }
    
    private ValidationResult validateParams(Map<String, Object> params, ToolDefinition tool) {
        for (Parameter param : tool.getParameters()) {
            if (param.isRequired() && !params.containsKey(param.getName())) {
                return ValidationResult.invalid("缺少必填参数: " + param.getName());
            }
            
            if (params.containsKey(param.getName())) {
                Object value = params.get(param.getName());
                if (!validateParamType(value, param.getType())) {
                    return ValidationResult.invalid("参数类型错误: " + param.getName());
                }
            }
        }
        
        return ValidationResult.valid();
    }
    
    private boolean validateParamType(Object value, String expectedType) {
        switch (expectedType) {
            case "String":
                return value instanceof String;
            case "Long":
                return value instanceof Number;
            case "Integer":
                return value instanceof Number;
            case "Boolean":
                return value instanceof Boolean;
            default:
                return true;
        }
    }
    
    private boolean checkPermission(ToolDefinition tool, String userToken) {
        // 解析Token获取用户角色
        String userRole = parseUserRole(userToken);
        
        // 检查是否有权限
        return tool.getRequiredRoles().contains(userRole);
    }
    
    private String parseUserRole(String userToken) {
        // 解析JWT Token获取用户角色
        try {
            // 实际实现应该解析JWT
            return "STUDENT";
        } catch (Exception e) {
            return null;
        }
    }
    
    private String buildUrl(ToolDefinition tool, Map<String, Object> params) {
        // 构建URL
        return "http://localhost:8080" + tool.getUrl();
    }
    
    private void recordLog(String toolName, Map<String, Object> params, ToolResponse response, long duration, String requestId) {
        recordLog(toolName, params, response, duration, requestId, null);
    }
    
    private void recordLog(String toolName, Map<String, Object> params, ToolResponse response, long duration, String requestId, String error) {
        ToolExecutionLog log = ToolExecutionLog.builder()
            .requestId(requestId)
            .toolName(toolName)
            .toolInput(serializeParams(params))
            .toolOutput(response != null ? serializeResponse(response) : null)
            .duration(duration)
            .status(error == null ? "SUCCESS" : "FAILED")
            .error(error)
            .createdAt(new Date())
            .build();
        
        logMapper.insert(log);
    }
    
    private String serializeParams(Map<String, Object> params) {
        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    private String serializeResponse(ToolResponse response) {
        try {
            return new ObjectMapper().writeValueAsString(response);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

### 3.2 Tool结果

```java
@Data
@Builder
public class ToolResult {
    private boolean success;
    private Object data;
    private String message;
    private String code;
    private String requestId;
    
    public static ToolResult success(Object data) {
        return ToolResult.builder()
            .success(true)
            .data(data)
            .message("success")
            .build();
    }
    
    public static ToolResult error(String message, String code) {
        return ToolResult.builder()
            .success(false)
            .message(message)
            .code(code)
            .build();
    }
}
```

## 四、Tool API实现

### 4.1 获取学生信息

```java
@RestController
@RequestMapping("/api/agent/tools")
@Slf4j
public class AgentToolController {
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private StudentScoreService studentScoreService;
    
    @Autowired
    private PolicyService policyService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/student-info")
    public ToolResponse getStudentInfo(@RequestBody Map<String, Object> params, 
                                       @RequestHeader("Authorization") String token) {
        try {
            Long studentId = params.containsKey("studentId") ? 
                Long.parseLong(params.get("studentId").toString()) : null;
            
            StudentInfo student = studentService.getStudentInfo(studentId, token);
            
            return ToolResponse.success(student);
        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            return ToolResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/student-scores")
    public ToolResponse getStudentScores(@RequestBody Map<String, Object> params,
                                         @RequestHeader("Authorization") String token) {
        try {
            Long studentId = params.containsKey("studentId") ? 
                Long.parseLong(params.get("studentId").toString()) : null;
            String semester = (String) params.get("semester");
            
            StudentScores scores = studentScoreService.getStudentScores(studentId, semester, token);
            
            return ToolResponse.success(scores);
        } catch (Exception e) {
            log.error("获取学生成绩失败", e);
            return ToolResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/search-policy")
    public ToolResponse searchPolicy(@RequestBody Map<String, Object> params,
                                     @RequestHeader("Authorization") String token) {
        try {
            String query = (String) params.get("query");
            String category = (String) params.get("category");
            int topK = params.containsKey("topK") ? 
                Integer.parseInt(params.get("topK").toString()) : 5;
            
            List<PolicySearchResult> results = policyService.searchPolicy(query, category, topK);
            
            return ToolResponse.success(Map.of("policies", results));
        } catch (Exception e) {
            log.error("搜索政策失败", e);
            return ToolResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/check-eligibility")
    public ToolResponse checkEligibility(@RequestBody Map<String, Object> params,
                                         @RequestHeader("Authorization") String token) {
        try {
            Long policyId = Long.parseLong(params.get("policyId").toString());
            Long studentId = params.containsKey("studentId") ? 
                Long.parseLong(params.get("studentId").toString()) : null;
            
            EligibilityResult result = policyService.checkEligibility(policyId, studentId, token);
            
            return ToolResponse.success(result);
        } catch (Exception e) {
            log.error("检查资格失败", e);
            return ToolResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/create-todo")
    public ToolResponse createTodo(@RequestBody Map<String, Object> params,
                                   @RequestHeader("Authorization") String token) {
        try {
            String title = (String) params.get("title");
            String description = (String) params.get("description");
            String deadline = (String) params.get("deadline");
            Long relatedPolicyId = params.containsKey("relatedPolicyId") ? 
                Long.parseLong(params.get("relatedPolicyId").toString()) : null;
            
            Task task = taskService.createTask(title, description, deadline, relatedPolicyId, token);
            
            return ToolResponse.success(Map.of(
                "taskId", task.getId(),
                "message", "待办创建成功"
            ));
        } catch (Exception e) {
            log.error("创建待办失败", e);
            return ToolResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/send-notification")
    public ToolResponse sendNotification(@RequestBody Map<String, Object> params,
                                         @RequestHeader("Authorization") String token) {
        try {
            Long studentId = Long.parseLong(params.get("studentId").toString());
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            String type = params.containsKey("type") ? (String) params.get("type") : "INFO";
            
            Notification notification = notificationService.send(studentId, title, content, type);
            
            return ToolResponse.success(Map.of(
                "notificationId", notification.getId(),
                "message", "通知发送成功"
            ));
        } catch (Exception e) {
            log.error("发送通知失败", e);
            return ToolResponse.error(e.getMessage());
        }
    }
}
```

### 4.2 Tool响应

```java
@Data
@Builder
public class ToolResponse {
    private boolean success;
    private Object data;
    private String message;
    private String code;
    
    public static ToolResponse success(Object data) {
        return ToolResponse.builder()
            .success(true)
            .data(data)
            .message("success")
            .build();
    }
    
    public static ToolResponse error(String message) {
        return ToolResponse.builder()
            .success(false)
            .message(message)
            .build();
    }
}
```

## 五、Tool权限控制

### 5.1 权限模型

```java
@Service
public class ToolPermissionService {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserService userService;
    
    public boolean checkPermission(String toolName, String userToken) {
        // 1. 解析Token
        String userId = tokenProvider.getUserIdFromToken(userToken);
        if (userId == null) {
            return false;
        }
        
        // 2. 获取用户角色
        User user = userService.getUserById(Long.parseLong(userId));
        if (user == null) {
            return false;
        }
        
        // 3. 获取工具定义
        ToolDefinition tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return false;
        }
        
        // 4. 检查角色权限
        return tool.getRequiredRoles().contains(user.getRole());
    }
    
    public boolean checkDataPermission(String toolName, Long targetUserId, String userToken) {
        // 1. 解析Token
        String userId = tokenProvider.getUserIdFromToken(userToken);
        if (userId == null) {
            return false;
        }
        
        // 2. 获取当前用户
        User currentUser = userService.getUserById(Long.parseLong(userId));
        if (currentUser == null) {
            return false;
        }
        
        // 3. 数据权限检查
        switch (currentUser.getRole()) {
            case "STUDENT":
                // 学生只能访问自己的数据
                return currentUser.getId().equals(targetUserId);
                
            case "COUNSELOR":
                // 辅导员可以访问授权学生数据
                return counselorService.isAuthorized(currentUser.getId(), targetUserId);
                
            case "ADMIN":
                // 管理员可以访问所有数据
                return true;
                
            default:
                return false;
        }
    }
}
```

### 5.2 权限注解

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String tool();
    PermissionLevel level() default PermissionLevel.ROLE;
}

public enum PermissionLevel {
    ROLE,      // 角色权限
    DATA       // 数据权限
}

@Aspect
@Component
public class PermissionAspect {
    
    @Autowired
    private ToolPermissionService permissionService;
    
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        // 获取用户Token
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String token = attributes.getRequest().getHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            throw new UnauthorizedException("未授权");
        }
        
        String userToken = token.substring(7);
        
        // 检查权限
        if (!permissionService.checkPermission(requiresPermission.tool(), userToken)) {
            throw new ForbiddenException("权限不足");
        }
        
        return joinPoint.proceed();
    }
}
```

## 六、Tool日志记录

### 6.1 日志模型

```java
@Data
@Builder
public class ToolExecutionLog {
    private Long id;
    private String requestId;
    private String toolName;
    private String toolInput;
    private String toolOutput;
    private Long duration;
    private String status;
    private String error;
    private Date createdAt;
}
```

### 6.2 日志记录

```java
@Service
public class ToolExecutionLogger {
    
    @Autowired
    private ToolExecutionLogMapper logMapper;
    
    public void log(String toolName, Map<String, Object> input, Object output, 
                    long duration, String status, String error, String requestId) {
        ToolExecutionLog log = ToolExecutionLog.builder()
            .requestId(requestId)
            .toolName(toolName)
            .toolInput(serializeInput(input))
            .toolOutput(serializeOutput(output))
            .duration(duration)
            .status(status)
            .error(error)
            .createdAt(new Date())
            .build();
        
        logMapper.insert(log);
    }
    
    private String serializeInput(Map<String, Object> input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(input);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    private String serializeOutput(Object output) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(output);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

### 6.3 日志查询

```java
@Service
public class ToolLogQueryService {
    
    @Autowired
    private ToolExecutionLogMapper logMapper;
    
    public List<ToolExecutionLog> queryLogs(String toolName, String status, 
                                            Date startDate, Date endDate,
                                            int page, int pageSize) {
        LambdaQueryWrapper<ToolExecutionLog> wrapper = new LambdaQueryWrapper<>();
        
        if (toolName != null) {
            wrapper.eq(ToolExecutionLog::getToolName, toolName);
        }
        
        if (status != null) {
            wrapper.eq(ToolExecutionLog::getStatus, status);
        }
        
        if (startDate != null) {
            wrapper.ge(ToolExecutionLog::getCreatedAt, startDate);
        }
        
        if (endDate != null) {
            wrapper.le(ToolExecutionLog::getCreatedAt, endDate);
        }
        
        wrapper.orderByDesc(ToolExecutionLog::getCreatedAt);
        
        return logMapper.selectPage(new Page<>(page, pageSize), wrapper).getRecords();
    }
    
    public ToolLogStatistics getStatistics(Date startDate, Date endDate) {
        // 统计工具调用情况
        List<ToolExecutionLog> logs = queryLogs(null, null, startDate, endDate, 1, Integer.MAX_VALUE);
        
        Map<String, Long> toolCounts = logs.stream()
            .collect(Collectors.groupingBy(ToolExecutionLog::getToolName, Collectors.counting()));
        
        Map<String, Long> statusCounts = logs.stream()
            .collect(Collectors.groupingBy(ToolExecutionLog::getStatus, Collectors.counting()));
        
        double averageDuration = logs.stream()
            .mapToLong(ToolExecutionLog::getDuration)
            .average()
            .orElse(0.0);
        
        return ToolLogStatistics.builder()
            .totalCalls(logs.size())
            .toolCounts(toolCounts)
            .statusCounts(statusCounts)
            .averageDuration(averageDuration)
            .build();
    }
}
```

## 七、Tool错误处理

### 7.1 错误码定义

```java
public enum ToolErrorCode {
    // 参数错误
    INVALID_PARAMS("INVALID_PARAMS", "参数错误"),
    MISSING_REQUIRED_PARAM("MISSING_REQUIRED_PARAM", "缺少必填参数"),
    INVALID_PARAM_TYPE("INVALID_PARAM_TYPE", "参数类型错误"),
    
    // 权限错误
    PERMISSION_DENIED("PERMISSION_DENIED", "权限不足"),
    UNAUTHORIZED("UNAUTHORIZED", "未授权"),
    DATA_ACCESS_DENIED("DATA_ACCESS_DENIED", "数据访问权限不足"),
    
    // 工具错误
    TOOL_NOT_FOUND("TOOL_NOT_FOUND", "工具不存在"),
    TOOL_CALL_FAILED("TOOL_CALL_FAILED", "工具调用失败"),
    TOOL_TIMEOUT("TOOL_TIMEOUT", "工具调用超时"),
    
    // 业务错误
    STUDENT_NOT_FOUND("STUDENT_NOT_FOUND", "学生不存在"),
    POLICY_NOT_FOUND("POLICY_NOT_FOUND", "政策不存在"),
    TASK_NOT_FOUND("TASK_NOT_FOUND", "任务不存在"),
    
    // 系统错误
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "服务不可用");
    
    private final String code;
    private final String message;
    
    ToolErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
```

### 7.2 错误处理

```java
@ControllerAdvice
@Slf4j
public class ToolExceptionHandler {
    
    @ExceptionHandler(ToolException.class)
    public ResponseEntity<ToolResponse> handleToolException(ToolException e) {
        log.error("工具调用异常: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ToolResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ToolResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.error("未授权访问: {}", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ToolResponse.error("未授权"));
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ToolResponse> handleForbiddenException(ForbiddenException e) {
        log.error("权限不足: {}", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ToolResponse.error("权限不足"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ToolResponse> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ToolResponse.error("系统内部错误"));
    }
}
```

## 八、Tool测试

### 8.1 单元测试

```java
@SpringBootTest
@ActiveProfiles("test")
class ToolCallerTest {
    
    @Autowired
    private ToolCaller toolCaller;
    
    @Test
    void testGetStudentInfo() {
        Map<String, Object> params = Map.of("studentId", 1);
        String token = "test-token";
        
        ToolResult result = toolCaller.call("get_student_info", params, token);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
    
    @Test
    void testGetStudentInfoWithoutPermission() {
        Map<String, Object> params = Map.of("studentId", 2);
        String token = "student-token";  // 学生Token
        
        ToolResult result = toolCaller.call("get_student_info", params, token);
        
        // 学生只能查询自己的信息
        assertFalse(result.isSuccess());
        assertEquals("PERMISSION_DENIED", result.getCode());
    }
    
    @Test
    void testSearchPolicy() {
        Map<String, Object> params = Map.of("query", "奖学金");
        String token = "test-token";
        
        ToolResult result = toolCaller.call("search_policy", params, token);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
```

### 8.2 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentToolControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testGetStudentInfo() throws Exception {
        Map<String, Object> params = Map.of("studentId", 1);
        
        mockMvc.perform(post("/api/agent/tools/student-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params))
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists());
    }
    
    @Test
    void testSearchPolicy() throws Exception {
        Map<String, Object> params = Map.of("query", "奖学金");
        
        mockMvc.perform(post("/api/agent/tools/search-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params))
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.policies").exists());
    }
}
```

---

*本文档为CampusPilot Tool API的设计、实现、测试提供完整的指导。*