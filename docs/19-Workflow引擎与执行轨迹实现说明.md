# CampusPilot Workflow引擎与执行轨迹实现说明

## 一、概述

Phase 3 将"路由 + 步骤编排 + 工具调用 + 决策 + 引用 + 轨迹"落为代码化的 Workflow 引擎，
替代 Phase 2 的 Mock 响应。同一执行过程同时产出用户回答与结构化 `ExecutionTrace`，
供前端 Trace 面板与评测使用。

## 二、核心组件（campuspilot/src/main/java/com/campuspilot/workflow/）

| 类型 | 说明 |
|------|------|
| `AgentWorkflow` | 工作流接口：`id()` / `name()` / `description()` / `supportsIntent()` / `execute(ctx)` |
| `WorkflowRegistry` | 工作流注册表：按 intent 匹配路由 |
| `WorkflowEngine` | 路由与执行：`routeAndExecute(context)`，异常 → `WorkflowResult.failed` 并保留步骤 |
| `WorkflowContext` | 会话/用户/消息/意图/复杂度上下文 |
| `WorkflowResult` | 执行结果：status / message / citations / toolCalls / steps / decisions / retrieval / eligibility / data |
| `WorkflowSupport` | 分类推断等公共工具 |
| `PolicyConsultationWorkflow` | 政策咨询：ROUTER → RETRIEVAL(RAG) → DECISION(政策筛选) → RESPONSE |
| `EligibilityCheckWorkflow` | 资格判断：RETRIEVAL → get_student_info → check_eligibility → DECISION → RESPONSE |
| `TaskCreationWorkflow` | 事务办理：RETRIEVAL → check_eligibility(DECISION 拦截) → create_todo → NOTIFY → RESPONSE |

## 三、关键设计

1. **职责边界**：路由/检索/决策/工具由确定性代码承担；LLM 仅待接入 FastGPT 后做"生成式解释"。
2. **安全拦截前置**：MockAgentProvider 先跑 `AgentSecurityGuard`，命中即返回 DENIED，不执行任何工作流。
3. **资格拦截**：`TaskCreationWorkflow` 仅当 `eligibility.status == ELIGIBLE` 才调用 `create_todo`，
   否则 `decisions.taskCreated=false` 并说明原因（不创建任务）。
4. **结果传播**：`WorkflowResult.data`（如 taskCreated/taskId）经 `MockAgentProvider` 写入 `AgentResponse.data`，
   前端可结构化读取；Traces 中 `decisions.taskCreated` 与响应顶层 `data.taskCreated` 一致。
5. **数据一致性注意**：步骤输出等 Map 需容忍 null 值（`JsonMap`/显式判空），
   避免 `Map.of(...)` 遇 null 抛 NPE 导致整条工作流失败（该问题已在 3.9 修复）。

## 四、执行轨迹（ExecutionTrace）

Trace 携带可观测信息，供前端折叠面板展示与评测断言：

| 字段 | 说明 |
|------|------|
| executionId / sessionId / userId / intent | 会话标识 |
| workflowId / workflowName | 执行的工作流 |
| steps | 步骤列表：step / type / name / status / duration / input / output / error |
| toolCalls | 工具调用记录：toolName / status / duration / params / result / error |
| citations | RAG 引用来源 |
| decisions | 决策信息（路由/判断/任务创建等） |
| retrieval | 检索结果（provider / hits / note） |
| startTime / endTime / duration / status / error | 执行计时与状态 |

验证（2026-09-10 HTTP 回归）：
- 政策咨询 steps：`ROUTER | RETRIEVAL | DECISION | RESPONSE`
- 资格判断 steps：`RETRIEVAL | TOOL(get_student_info) | TOOL(check_eligibility) | DECISION | RESPONSE`
- 任务办理 steps：`RETRIEVAL | TOOL(check_eligibility) | DECISION | TOOL(create_todo) | NOTIFY | RESPONSE`

## 五、注册与路由

- 三个工作流在 `WorkflowRegistry` 启动时注册，`supportsIntent` 分别为
  `POLICY_QUERY` / `ELIGIBILITY_CHECK` / `TASK_CREATE`。
- `MockAgentProvider.mockIntentDetection` 按关键词优先级识别：
  资格/判断 > 办理/申请(任务) > 成绩 > 政策/宿舍/请假等 > 通用。
- 未命中意图 → `fallback` 路径（演示回退，含 `decisions.route=fallback`）。