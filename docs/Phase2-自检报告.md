# CampusPilot Phase 2 自检报告

## 一、自检概述

### 1.1 自检目的
对Phase 2后端基础架构与核心业务开发阶段进行真实运行验证，确认最小可运行系统（登录 → JWT → 学生信息 → Agent Chat → Tool Calling → 任务创建 → Agent Trace）全链路可用。

### 1.2 自检环境
| 项目 | 环境 |
|------|------|
| 操作系统 | Windows 11 |
| JDK | Oracle JDK 21.0.8 |
| Maven | 3.9.11 |
| MySQL | 8.0.42（Docker容器 mysql-8.0.42，端口3306） |
| Node.js | v22.19.0 |
| Backend | Spring Boot 3.2（dev profile，Mock Agent Provider）|
| 测试方式 | 真实HTTP调用（HTTP/1.1, JSON）|

## 二、验收链路验证结果（均在真实运行后端上执行）

验收链路：`登录 → JWT → /api/students/me → Agent Chat → MockAgentProvider → Tool Calling → 业务逻辑 → Task 创建 → Agent Trace → HTTP 返回`

### 2.1 基础健康检查

| # | 测试项 | 请求 | 实际结果 | 结论 |
|---|--------|------|----------|------|
| 1 | 健康检查 | `GET /api/health` | HTTP 200，`database:UP` | ✅ 通过 |
| 2 | 未认证访问 | `GET /api/tasks`（无Token） | HTTP 401，`"未登录或登录已过期"` | ✅ 通过 |
| 3 | 学生登录 | `POST /api/auth/login`（2021001/password） | HTTP 200，返回JWT | ✅ 通过 |
| 4 | 当前学生信息 | `GET /api/students/me`（带JWT） | HTTP 200，返回张三(2021001) | ✅ 通过 |

### 2.2 核心业务API

| # | 测试项 | 请求 | 实际结果 | 结论 |
|---|--------|------|----------|------|
| 5 | 任务列表 | `GET /api/tasks` | HTTP 200，返回当前用户任务 | ✅ 通过 |
| 6 | 任务详情 | `GET /api/tasks/1` | HTTP 200，国家奖学金申请 | ✅ 通过 |
| 7 | 政策列表 | `GET /api/policies` | HTTP 200，7条DEMO政策 | ✅ 通过 |
| 8 | 政策搜索 | `POST /api/policies/search`（query=奖学金） | HTTP 200，返回匹配政策 | ✅ 通过 |
| 9 | 数据归属校验 | `GET /api/tasks/3`（2021001访问2021002的任务） | 业务码404「任务不存在」 | ✅ 通过 |

> **说明**：#9 验证了 STUDENT 角色数据归属：任务3属于2021002，2021001访问时被拒绝（HTTP 200包装，业务码404）。该问题在自检中发现并修复。

### 2.3 Agent Chat 端到端（Mock Agent Provider）

#### 场景1：资格判断（COMPLEX路由）

请求：`POST /api/agent/chat` message=「帮我判断是否符合国家奖学金申请条件」

| 项 | 结果 |
|---|------|
| intent | `ELIGIBILITY_CHECK` |
| complexity | `COMPLEX`（路由至 RAG + TOOLS）|
| toolCalls | `get_student_info:SUCCESS`、`search_policy:SUCCESS` |
| sources（引用） | 1条（2025年国家奖学金管理办法）|
| executionSteps | 6步（意图识别→复杂度路由→知识库检索→2次工具调用→响应生成）|
| executionTrace | executionId/status=SUCCESS/duration≈1s 完整返回 |

#### 场景2：任务创建（ACTION路由）

请求：`POST /api/agent/chat` message=「帮我申请奖学金」

| 项 | 结果 |
|---|------|
| intent | `TASK_CREATE` |
| complexity | `ACTION`（路由至 WORKFLOW + TOOLS）|
| toolCalls | `create_todo:SUCCESS`，返回 taskId=4 |
| 数据库验证 | `GET /api/tasks` 该用户任务数 2→3，新增「国家奖学金申请」PENDING |

> **说明**：create_todo 真实写入 MySQL（Agent → Tool → Service → Mapper → DB），而非 mock 假数据。

### 2.4 开发中发现并修复的问题

| # | 问题 | 根因 | 修复方案 | 验证 |
|---|------|------|----------|------|
| 1 | 未认证返回403而非401 | SecurityConfig未配置AuthenticationEntryPoint | 增加401/403 Handler | ✅ 已验证 |
| 2 | `/api/students/me` 404 | 控制器映射为`/api/student`（单数）| 修正为`/api/students` | ✅ 已验证 |
| 3 | 工具调用`create_todo`失败 | 缺少MetaObjectHandler，created_at为null | 新增`MybatisPlusConfig`自动填充 | ✅ 已验证 |
| 4 | `get_student_info`失败 | 角色比较`"STUDENT".equals("ROLE_STUDENT")`不匹配 | 改为`role.contains("STUDENT")` | ✅ 已验证 |
| 5 | 「帮我判断是否符合国家奖学金」误判为POLICY_QUERY | 意图识别优先级错误 | 资格/判断类关键词优先匹配 | ✅ 已验证 |
| 6 | 学生可访问他人任务 | updateTaskStatus/getTaskById未做归属校验 | 增加STUDENT角色数据归属校验 | ✅ 已验证 |
| 7 | Demo密码为伪造BCrypt hash无法登录 | 旧hash无效 | 生成真实BCrypt hash（password）| ✅ 已验证 |
| 8 | dev库与schema库名不一致 | application-dev.yml指向campuspilot_dev | 统一为`campuspilot` | ✅ 已验证 |

### 2.5 编码与构建

| 项 | 结果 |
|---|------|
| 后端编译 | `mvn compile` 通过（EXIT 0）|
| 前端类型检查+构建 | `npm run build`（vue-tsc + vite）通过 |
| Git提交 | 完成并清理了误提交的`campuspilot/target/`构建产物 |

## 三、模块完成清单

| 模块 | 实现文件 | 状态 |
|------|----------|------|
| 公共层 | ApiResponse、BusinessException、GlobalExceptionHandler | ✅ |
| 安全 | JwtTokenProvider、JwtAuthenticationFilter、SecurityConfig（401/403）、SecurityUtils | ✅ |
| 认证 | AuthController/AuthService/AuthServiceImpl | ✅ |
| 学生 | StudentController/Service/ServiceImpl、StudentMapper | ✅ |
| 政策 | PolicyController/Service、4个Mapper、PolicyVO | ✅ |
| 任务 | TaskController/Service（归属校验）、TaskMapper | ✅ |
| Agent Provider | AgentProvider接口、MockAgentProvider、FastGPTProvider（预留）| ✅ |
| Agent 模型 | AgentRequest/Response、Citation/ExecutionStep/ToolCall/ExecutionTrace | ✅ |
| Agent 服务 | AgentService/AgentServiceImpl、AgentController | ✅ |
| Tool | AgentTool接口、ToolRegistry、get_student_info/search_policy/create_todo | ✅ |
| 数据库 | schema.sql（14表）、data.sql（DEMO数据）、MybatisPlusConfig | ✅ |
| 前端 | Login/Layout/Chat（Trace UI）/Tasks/Policies/Profile、Pinia、Axios | ✅ |

## 四、已实现与未实现对照

### 4.1 已实现（Phase 2 目标范围内）
- ✅ 登录认证 + JWT + 角色
- ✅ 学生/政策/任务基础CRUD
- ✅ Agent Chat 接口（Mock Provider）
- ✅ Agent Trace（执行轨迹：意图、路由、检索、工具、响应）
- ✅ Tool Calling 真实链路（含数据库写入）
- ✅ 知识库引用（sources），防止无依据回答
- ✅ 学生数据归属校验

### 4.2 待后续阶段实现（本阶段未实现，符合计划）
| 事项 | 规划阶段 |
|------|----------|
| SF-FastGPT 真实API对接（FastGPTProvider标注TODO）| Phase 3 |
| 真实 RAG 向量检索（当前为规则+关键词检索）| Phase 3 |
| 资格引擎规则库 | Phase 3 |
| 定时提醒/通知 | Phase 3 |
| 请销假/宿舍/证书等更多Workflow | Phase 4+ |

## 五、风险与后续计划

| 风险 | 等级 | 应对 |
|------|------|------|
| FastGPT API文档未获取 | 高 | MockAgentProvider保证本地闭环 |
| 本地RAG为关键词检索 | 中 | Phase 3实现向量化+资料导入 |

**下一步**：进入Phase 3（FastGPT真实对接 + RAG知识库 + 资格引擎），建议先获取SF-FastGPT部署环境与API文档。

---

*本报告基于2026-09-10在真实运行后端上执行的HTTP验证结果编写，非静态评估。*