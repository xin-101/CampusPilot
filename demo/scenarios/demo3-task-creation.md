# Demo 3：办理申请

## 场景
用户在资格判断通过后说「那帮我申请国家奖学金」，Agent 事务办理工作流
执行：资格拦截 → create_todo → TASK 通知，真实写入 MySQL。

## 触发
```
POST /api/agent/chat
Authorization: Bearer <JWT(2021001)>
{"message": "那帮我申请国家奖学金"}
```

## 期望执行轨迹（真实断言）
```
RETRIEVAL | TOOL(check_eligibility) | DECISION | TOOL(create_todo) | NOTIFY | RESPONSE
```
- trace.workflowId = task_creation
- decisions.eligible = true、decisions.taskCreated = true
- data.taskCreated = true、data.taskId = <新任务ID>
- 响应含截止时间 `2026-09-30 23:59:59`
- `GET /api/tasks` 该用户任务数 +1（真实落库）
- 通知列表新增 TASK 类型（related_id = 任务ID）

## 前端展示要求
- 成功卡片：✅ 资格判断通过，任务已创建（任务ID: N）
- 任务：国家奖学金申请 / 状态：PENDING / 截止时间：2026-09-30 23:59:59
- 下一步：1. 准备材料 → 2. 提交申请 → 3. 等待审核
- 下一步行动卡片：`[查看任务]`

## 安全约束（展示亮点）
- 未通过资格判断（NOT_ELIGIBLE / INSUFFICIENT_DATA / CONDITIONALLY_ELIGIBLE）
  时 decision.taskCreated=false，不创建任务、不发送通知。

## 验证依据（2026-09-10 实测）
| 项 | 值 |
|----|-----|
| 历史循环回归 taskId | 5、6（两次回归各 +1） |
| `test_phase3.mjs` 用例 4 | decisions.taskCreated=true ✅ data.taskCreated=true ✅ |

## 展示要点
强调：**从问得到 → 办得到**，工具调用真实落库，通知真实入库。