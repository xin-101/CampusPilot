# Demo 1：政策咨询

## 场景
用户想知道「国家奖学金」的申请时间与条件，Agent 走政策咨询工作流。

## 触发
```
POST /api/agent/chat
Authorization: Bearer <JWT(2021001)>
{"message": "国家奖学金什么时候申请？"}
```

## 期望执行轨迹（真实断言）
```
ROUTER:工作流路由 | RETRIEVAL:知识库检索(RAG) | DECISION:政策筛选 | RESPONSE:回答生成
```
- trace.workflowId = policy_consultation
- trace.citations 非空（命中国家奖学金政策版本）
- 响应含政策有效期（2026-01-01 ~ 2026-12-31）

## 前端展示要求
- 回答文本
- 来源引用卡片：政策名称 / 版本 / 状态=有效 / 来源 /（来源链接：暂无，不伪造 URL）
- 下一步行动卡片：`[查看政策]`（actions 驱动，非文本猜测）

## 验证依据
- `test_phase3.mjs` 用例 2（政策查询）✅ 2026-09-10
- 政策库 7 条 DEMO 政策（policies 表），国家奖学金 = id 1

## 展示要点
强调：RAG 引用政策来源 → Agent 回答带依据、带有效期 →「从问得到」