# FastGPT 联调清单（SF-FastGPT 接入手册）

> 目标：将真实 SF-FastGPT 服务接入本系统，替换本地 `MockAgentProvider`，
> 同时**保持安全守卫、资格规则引擎、RAG 检索、任务落库等国产化能力不变**。

## 1. 配置项

`backend/src/main/resources/application-prod.yml`（或环境变量）：

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `agent.provider` | `AGENT_PROVIDER=fastgpt` | 使用 FastGPT 提供方（dev 默认 `mock`） |
| `agent.fastgpt.url` | `FASTGPT_BASE_URL` | FastGPT 服务地址，如 `https://fastgpt.example.com` |
| `agent.fastgpt.api-key` | `FASTGPT_API_KEY` | API Key（**禁止**打印/入库） |
| `agent.fastgpt.knowledge-id` | `FASTGPT_KNOWLEDGE_ID` | 政策知识库 ID（本地知识库 `knowledge/*.json` 的上传目标） |
| `agent.fastgpt.workflow-id.policy-consultation` | — | 政策咨询工作流 ID |
| `agent.fastgpt.workflow-id.eligibility-check` | — | 资格判断工作流 ID |
| `agent.fastgpt.workflow-id.task-creation` | — | 任务办理工作流 ID |
| `agent.fastgpt.timeout` / `retry-count` | — | 超时(默认30s) / 重试(默认3) |

配置类：`com.campuspilot.agent.fastgpt.FastGPTProperties`（`@ConfigurationProperties("agent.fastgpt")`）。

## 2. 客户端对接

- 入口类：`com.campuspilot.agent.fastgpt.FastGPTClient`
- 已预留三个槽位：`chat(...)`、`searchKnowledge(query, topK)`、`runWorkflow(...)`
- 当前所有方法在**未配置时明确抛出 `FastGPTException`**，绝不伪造成功响应；
  接入时按 SF-FastGPT 实际 API 文档实现真实调用与响应解析。

建议的 HTTP 契约（与 SF-FastGPT v4 API 对齐）：
```
POST {url}/v1/chat/completions
Authorization: Bearer {api-key}
{
  "model": "sf-fastgpt",
  "messages": [{ "role": "user", "content": "<用户消息>" }],
  "tools": [ <本系统 ToolSchema> ]
}
```
响应中本地需解析：`choices[0].message.content`、`tool_calls`，并做 JSON 校验与脱敏校验。

## 3. 与本系统的连接点

| 连接点 | 说明 |
|--------|------|
| `AgentSecurityGuard.inspect()` | FastGPT 入口前**先拦截**越权/注入/冒充/隐私类请求（与 mock 一致） |
| `AgentProvider.chat(AgentRequest)` | 统一返回 `AgentResponse`（含 `executionTrace` / `citations`） |
| 资格规则引擎 | 资格判断**仍由本地规则引擎完成**（`eligibility_rules`），不以 LLM 结果为准，避免幻觉 |
| RAG 检索 | `knowledge/*.json` 可上传到 FastGPT 知识库；本地 `VectorPolicyRetriever` 同时可用作兜底 |
| 工具调用 | `ToolCaller` 执行 `get_student_info / search_policies / check_eligibility / create_todo / send_notification` |
| `healthCheck()` / 降级 | 未配置或调用失败 → 返回 `WAITING_FOR_FASTGPT_ENVIRONMENT`（诚实降级，不伪造） |

## 4. 联调步骤

1. 准备 SF-FastGPT 环境，将 `knowledge/*.json` 8 分类政策上传为知识库，取 `knowledge-id`。
2. 配置 `agent.fastgpt.*`（或环境变量），确认 `FastGPTConnectionChecker` 探测成功。
3. 用 `profiles=prod` 启动后端，发起：
   - 「国家奖学金什么时候申请？」→ 应命中 policy_consultation 工作流并返回带引用回答；
   - 「我能不能申请国家奖学金？」→ 资格结论应来自规则引擎（数据源为规则而非大模型）；
   - 「那帮我申请国家奖学金」→ 任务真实落库（`/api/tasks`）。
4. 用 `evaluation/rag/run-rag-eval.mjs`（RAG 命中率）与 `evaluation/phase4/run-phase4-eval.mjs`（M1–M6）
   验证接入后指标满足：RAG 命中率 ≥ 90%、意图识别、回答质量、安全拦截 100%、p95 任务延迟 ≤ 1500ms。
5. 回归：`scripts/run-evaluation.ps1` 全绿后才能进入评审。

## 5. 红线（接入后不得破坏）

- 不打印 JWT / API Key / 密码 / Authorization。
- 不伪造 FastGPT 检索结果与评测指标——接入前一律 `WAITING_FOR_FASTGPT_ENVIRONMENT`。
- 资格判断必须以本地规则引擎为准并保留免责声明（`is_demo=1`）。
- 工具执行（落库/通知）仍需 SQL 参数化与所有权校验（`fetchTaskWithOwnershipCheck`）。

## 6. 完成状态

| 项 | 状态 |
|----|------|
| 配置与连接探测 | ✅ 已实现（`FastGPTConnectionChecker`） |
| 三个调用槽位与错误转换 | ✅ 已实现（未配置即抛 FastGPTException） |
| 安全守卫前置 | ✅ 已实现（mock/fastgpt 共用） |
| 真实 HTTP 调用实现 | ⏳ 待提供 SF-FastGPT API 文档后补齐 |
| 知识库上传（本地→FastGPT） | ⏳ 待联调环境 |
| 接入回归（RAG/Phase4/Phase3） | ⏳ 待联调环境 |