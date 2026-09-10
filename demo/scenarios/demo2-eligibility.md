# Demo 2：资格判断

## 场景
用户问「我能不能申请国家奖学金」，Agent 走资格判断工作流：
政策检索 → 学生信息 → 学生成绩画像 → 规则引擎确定性判断。

## 触发
```
POST /api/agent/chat
Authorization: Bearer <JWT(2021001) 张三>
{"message": "我能不能申请国家奖学金？"}
```

## 期望执行轨迹（真实断言）
```
RETRIEVAL | TOOL(get_student_info) | TOOL(check_eligibility) | DECISION | RESPONSE
```
- trace.workflowId = eligibility_check
- decisions.status = ELIGIBLE（张三：绩点3.8 / 排名前10 / 大四）
- decisions.judgmentSource = ELIGIBILITY_RULE_ENGINE
- toolCalls 含 get_student_info、check_eligibility（均为 SUCCESS）

## 前端展示要求
- 判断结果块：`符合`（演示规则）
- 符合条件明细：年级 ✓ / 绩点 ≥3.5 ✓ / 综合测评排名前10 ✓
- 政策依据：国家奖学金管理办法（来源引用卡片）
- 数据更新时间 / 免责声明（DEMO 数据）
- 下一步行动卡片：`[创建申请任务]` `[查看政策]`

## 规则依据（eligibility_rules，source_policy_id=1）
| 规则 | 字段 | 期望 | 张三实际 |
|------|------|------|----------|
| DEMO_RULE_SCH_GRADE | GRADE IN 3,4 | 3,4 | 4 ✓ |
| DEMO_RULE_SCH_GPA | GPA GTE 3.5 | 3.5 | 3.8 ✓ |
| DEMO_RULE_SCH_RANK | RANK LTE 10 | 10 | 10 ✓ |

> 关键：按 `source_policy_id` 取规则，励志 hardship 规则不会误入国家奖学金判断（Phase 3.9 已修复）。

## 验证依据
- `test_phase3.mjs` 用例 3（资格判断）✅ decisions.status=ELIGIBLE

## 展示要点
强调：**LLM 只做解释、规则引擎做确定性决策**（防幻觉），
且学生数据来自 JWT 身份，不可越权查他人。