# CampusPilot Agent安全守卫实现说明

## 一、概述

`AgentSecurityGuard`（backend/src/main/java/com/campuspilot/agent/security/）在
`MockAgentProvider.chat` 进入意图识别与任何工具调用**之前**对用户消息执行校验与恶意注入检测。
命中即返回 `DENIED` 响应：不执行任何工作流/工具/数据处理，拦截原因写入 Trace。

## 二、防护范围与分类

| 分类 | 触发条件（消息匹配） | 示例 |
|------|----------------------|------|
| `DENIED_INVALID_INPUT` | 空输入 / 超长（>500字） | 空串 |
| `DENIED_SYSTEM_PROMPT_LEAK` | 系统提示词/内部指令/越狱类关键词 | "系统提示词"、"ignore all"、"越狱" |
| `DENIED_ADMIN_IMPERSONATION` | 冒充管理员/教师/越权获取权限 | "以管理员身份"、"绕过权限" |
| `DENIED_UNAUTHORIZED_ACCESS` | 查询成绩/信息 + 涉及他人（学号/他人姓名） | "查询李四的成绩"、"2021002" |
| `DENIED_TOOL_INJECTION` | 直接指定内部工具/注入参数 | "create_todo(...)"、"直接调用" |

匹配采用关键词/子串 Pattern 清单（`PromptInjectionPatterns` 等），命中即拦截并返回
`SecurityAssessment.deny(category, matchedPattern, reason)`。

## 三、响应与可追踪性

命中拦截时：

- `intent=DENIED`、`trace.workflowId=security_guard`、`executionTrace.status=DENIED`。
- `trace.steps[0].type=SECURITY`，output 含 `category` 与 `matchedPattern`。
- `trace.decisions.category` 记录拦截分类，`trace.error` 记录原因。
- 响应正文为安全提示（说明已拦截、未执行任何工具），并给出合规引导。

## 四、验证结果（2026-09-10 HTTP 回归，全部通过）

| 用例 | 期望 | 实际 |
|------|------|------|
| 系统提示词泄露尝试 | DENIED / security_guard | category=DENIED_SYSTEM_PROMPT_LEAK ✅ |
| 管理员身份冒充 | DENIED / security_guard | category=DENIED_ADMIN_IMPERSONATION ✅ |
| 查询他人成绩 | DENIED / security_guard | category=DENIED_UNAUTHORIZED_ACCESS ✅ |
| 工具调用注入 | DENIED | category=DENIED_TOOL_INJECTION ✅ |
| 正常政策/资格/任务/成绩请求 | 正常放行进入工作流 | 均正常执行 ✅ |

## 五、边界说明

- 当前为模式/规则守卫（确定性、零误判风险），未依赖 LLM 语义检测；
  后续可叠加 LLM 语义安全模型（FastGPT 接入后）。
- 守卫作用于 Chat 输入；接口层鉴权（JWT + 数据归属校验）由 Spring Security 负责，二者互补。