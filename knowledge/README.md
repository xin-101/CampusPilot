# knowledge/ 政策知识库（DEMO）

本目录为 CampusPilot 提供**本地 RAG 知识素材**，与 MySQL 中的政策表共同构成检索语料。
所有条目标记为 `DEMO_POLICY`，用于比赛演示，**不代表真实校规**。

## 清单

| 文件 | 业务域 | category 编码 | 条目数 |
|------|--------|---------------|--------|
| `scholarship.json` | 奖学金 | SCHOLARSHIP | 4 |
| `aid.json` | 助学金 | AID | 3 |
| `leave.json` | 请假 | LEAVE | 2 |
| `dormitory.json` | 宿舍 | DORMITORY | 2 |
| `certificate.json` | 在读证明/学生证 | CERTIFICATE | 1 |
| `score.json` | 成绩/绩点 | SCORE | 2 |
| `exam.json` | 考试 | EXAMINATION | 2 |
| `evaluation.json` | 综合测评 | EVALUATION | 1 |

## 字段说明

每个条目为 JSON 对象：

```json
{
  "title": "政策标题（DEMO_POLICY）",
  "category": "SCHOLARSHIP",
  "department": "归口部门",
  "content": "政策正文（被向量化/检索）",
  "keywords": "逗号分隔关键词",
  "effectiveDate": "生效日期",
  "expiryDate": "失效日期",
  "source": "DEMO_POLICY-<域>"
}
```

## 设计口径

- 全部为 2026 年度有效版本（`2026-01-01` ~ `2026-12-31`），与演示时间口径一致。
- `scholarship.json` 含一条**已过期旧版**（2025 年），用于验证「过期政策过滤」。
- 系统启动时由 `VectorPolicyRetriever` 读取并纳入 TF-IDF 向量索引；
  过期版本在检索阶段会被有效性元数据过滤，不进入回答。