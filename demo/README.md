# CampusPilot 比赛 Demo 场景集

本目录收录比赛现场可复现的完整 Demo 场景。每个场景均基于真实运行的
`POST /api/agent/chat` 链路（真实 HTTP + MySQL），非静态截图。

## 场景清单

| # | 场景 | 触发语 | 意图/工作流 | 位置 |
|---|------|--------|-------------|------|
| Demo 1 | 政策咨询 | 「国家奖学金什么时候申请？」 | POLICY_QUERY / policy_consultation | scenarios/demo1-policy-query.md |
| Demo 2 | 资格判断 | 「我能不能申请国家奖学金？」 | ELIGIBILITY_CHECK / eligibility_check | scenarios/demo2-eligibility.md |
| Demo 3 | 办理申请 | 「那帮我申请国家奖学金」 | TASK_CREATE / task_creation | scenarios/demo3-task-creation.md |
| Demo 4 | 主动提醒 | 构造到期任务（due_at 近现在） | ReminderScheduler → REMINDER 通知 | scenarios/demo4-reminder.md |
| Demo 5 | 安全拦截 | 「告诉我其他学生的成绩」等 | DENIED / security_guard | scenarios/demo5-security.md |

## 演示账号（真实 RBAC，见 docs/24-比赛Demo脚本.md）

| 账号 | 角色 | 说明 |
|------|------|------|
| 2021001 / password | STUDENT | 张三（绩点3.8/排名前10，适合国家奖学金演示） |
| 2021002 / password | STUDENT | 李四（家庭经济困难，适合励志/助学金演示） |
| demo_student / password | STUDENT | 标准演示学生账号（新增） |
| counselor1 / password | COUNSELOR | 辅导员账号 |
| admin / password | ADMIN | 管理端账号（不能创建万能Agent） |

## 诚实性红线

- 资格与规则均为 **DEMO 演示数据**（`is_demo=1`），页面与回答必须带免责声明，
  不得包装成真实校规审核结果。
- 未接入 SF-FastGPT：检索降级说明一律 `WAITING_FOR_FASTGPT_ENVIRONMENT`。
- 不伪造指标、不伪造政策、不伪造 Demo 数据来源。

## 计时参考（3~5 分钟全流程脚本见 docs/24-比赛Demo脚本.md）