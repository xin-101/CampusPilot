# CampusPilot Phase 3 自检报告

## 一、自检概述

### 1.1 自检目的
对 Phase 3（RAG 工程化 + 资格规则引擎 + Workflow 引擎 + 通知/提醒 + 执行轨迹 + 安全守卫 + FastGPT 适配）
进行真实运行验证，确认端到端可运行且诚实降级（不伪造 FastGPT 检索结果）。
全部验证基于真实启动的 Spring Boot 3.2 后端与 Docker MySQL 实例上执行的 HTTP 调用，非静态评估。

### 1.2 自检环境
| 项目 | 环境 |
|------|------|
| 操作系统 | Windows 11 |
| JDK | Oracle JDK 21 |
| Maven | 3.9.11 |
| MySQL | 8.0.42（Docker `mysql-8.0.42`，库 `campuspilot`，`database/init.sql` 全量数据） |
| Node.js | v22 |
| Backend | Spring Boot 3.2（dev profile） |
| 测试方式 | 真实 HTTP（12 组回归用例全部通过），脚本 `test_phase3.mjs` |

## 二、验收链路验证结果

验收链路：`登录(JWT) → /api/agent/chat → 安全守卫 → 意图识别 → WorkflowEngine → RAG/工具体系 → 业务落库 → ExecutionTrace → HTTP 返回 → 前端 Trace 面板`

### 2.1 核心用例结果（2026-09-10 自动化回归 12/12 通过）

| # | 用例 | 实际结果 | 结论 |
|---|------|----------|------|
| 1 | 登录获取 JWT | 返回 token | ✅ |
| 2 | 政策查询（policy_consultation） | steps=`ROUTER\|RETRIEVAL\|DECISION\|RESPONSE`，citations 非空，含有效期 | ✅ |
| 3 | 资格判断（eligibility_check） | `decisions.status=ELIGIBLE`（张三 国家奖学金），judgmentSource=ELIGIBILITY_RULE_ENGINE，toolCalls=get_student_info+check_eligibility | ✅ |
| 4 | 任务办理（task_creation） | `decision.eligible=true、taskCreated=true`，trace.taskId=5，含 NOTIFY 步骤，响应含 2026-09-30 截止 | ✅ |
| 5 | 通知接口 | 列表含 TASK 类型，unread-count 正常 | ✅ |
| 6 | 成绩查询（fallback 回退） | intent=SCORE_QUERY，workflow=fallback，含 DEMO 数据 | ✅ |
| 7 | 安全：提示词泄露 | intent=DENIED，workflowId=security_guard，steps[0].type=SECURITY | ✅ |
| 8 | 安全：管理员冒充 | decisions.category=DENIED_ADMIN_IMPERSONATION | ✅ |
| 9 | 安全：查询他人信息 | decisions.category=DENIED_UNAUTHORIZED_ACCESS | ✅ |
| 10 | 安全：工具注入 | decisions.category=DENIED_TOOL_INJECTION | ✅ |
| 11 | Phase2 回归：任务列表 | 返回当前用户任务（创建后 4 条） | ✅ |
| 12 | Phase2 回归：政策列表 | 返回 7 条 DEMO 政策 | ✅ |

### 2.2 关键链路真实性说明
- 任务创建：`ToolCaller → create_todo → TaskService/Mapper → MySQL`，任务 ID、通知记录真实入库。
- 资格判断：规则引擎从 `eligibility_rules` 真实加载真实数据计算，非 mock 假结果。
- RAG 降级：FastGPT 未配置/未就绪时返回空结果与 `WAITING_FOR_FASTGPT_ENVIRONMENT` 说明，不做伪造。

## 三、开发中发现并修复的问题

| # | 问题 | 根因 | 修复 | 验证 |
|---|------|------|------|------|
| 1 | 政策咨询/任务办理工作流抛"执行失败" | `Map.of` 含 null(note/provider) 抛 NPE | 步骤输出判空 | ✅ 回归通过 |
| 2 | 张三判国家奖学金非 ELIGIBLE | 励志 hardship 规则 category 亦为 SCHOLARSHIP，误参与判断 | 按 `source_policy_id` 取规则（未命中按分类兜底） | ✅ 回归通过 |
| 3 | 任务创建结果前端拿不到 taskId | `AgentResponse` 无 data 字段，`WorkflowResult.data` 未传播 | 增加 data 并传播 | ✅ 回归通过 |
| 4 | DB 数据被清空 | 引入 schema 时 source 执行了 DROP TABLE | 改用完整 `database/init.sql`（schema+data）重灌 | ✅ users/通知恢复 |
| 5 | notifications 建表冲突 | schema.sql 中旧版重复 create（task_id 列） | 移除旧表定义，data.sql 对齐新列 | ✅ |

## 四、模块完成清单

| 模块 | 实现文件 | 状态 |
|------|----------|------|
| RAG 抽象 | rag/RAGService、LocalRAGService、PolicyRetriever、KeywordPolicyRetriever、FastGptPolicyRetriever、RetrievalQuery/Result、RetrievedDocument | ✅ 本地检索可用；FastGPT=WAITING |
| 资格引擎 | eligibility/EligibilityRuleEngine、RuleService(Impl)、CheckService(Impl)、StudentProfile、EligibilityResult、EligibilityController | ✅ |
| Workflow 引擎 | workflow/Engine、Registry、Context、Result、Support、Tools×3 | ✅ |
| 工具 | agent/tool/ToolCaller、get_student_info、check_eligibility、create_todo | ✅ |
| 通知/提醒 | notification/Entity Mapper、Service(Impl)、VO、Controller、ReminderScheduler | ✅ |
| 执行轨迹 | agent/model/ExecutionTrace（workflowId/steps/toolCalls/citations/decisions/retrieval） | ✅ |
| 安全守卫 | agent/security/AgentSecurityGuard、SecurityAssessment | ✅ |
| FastGPT 适配 | agent/fastgpt/Properties、Client、ConnectionChecker、FastGPTProvider（诚实降级） | ✅（待环境） |
| 前端 | Chat.vue Trace 折叠面板、api/agent.ts（executionTrace） | ✅ |
| 评测数据集 | evaluation/README.md、evaluation/cases.json（32 条 E001–E032） | ✅ |

## 五、已实现 vs 待后续

### 5.1 已实现
- RAG 本地关键词检索真实可用并接入三个工作流，引用/有效期进入 Trace。
- 确定性资格规则引擎 + 政策级规则归属（source_policy_id）。
- 三工作流 + 意图路由 + 安全拦截前置 + 任务创建资格拦截。
- 任务创建 → TASK 通知；定时提醒（每分钟扫描、幂等 reminder_sent）。
- 前端 Trace 可视化（可折叠、结构化展示）。

### 5.2 待后续
| 事项 | 说明 |
|------|------|
| SF-FastGPT 真实 API 对接 | 需 SF-FastGPT 部署环境与 API 文档；当前 WAITING_FOR_FASTGPT_ENVIRONMENT |
| RAG 语义检索/rerank | 本地仅关键词；依赖 FastGPT 知识库 |
| 资格规则管理端 | 规则当前为 DB 数据（is_demo） |
| 评测 M1/M2/M6 分数回填 | 依赖 FastGPT 生成答案后补跑 NOT_RUN 用例 |

## 六、风险与后续计划

| 风险 | 等级 | 应对 |
|------|------|------|
| FastGPT 环境未就绪 | 高 | MockAgentProvider + 本地 RAG 保证闭环；适配器诚实降级不伪造 |
| 关键词检索相关性上限 | 中 | FastGPT 接入后升级向量检索 |
| 评测依赖真实 LLM 生成 | 中 | 确定性用例已 PASSED_3_9；生成类用例待环境补齐 |

**下一步**：Phase 4 起，优先申请 SF-FastGPT 环境补齐真实知识库检索与生成式答案，并回填评测分数。

---

*本报告基于 2026-09-10 真实运行后端上的 HTTP 验证结果编写，非静态评估。*