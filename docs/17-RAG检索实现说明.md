# CampusPilot RAG检索实现说明

## 一、概述

Phase 3 将知识库检索抽象为 `RAGService`，面向 Agent/Workflow 提供统一的政策知识获取入口，
屏蔽"本地关键词检索 / FastGPT 知识库检索"的差异。当前本地检索已真实可用；
FastGPT 因 SF-FastGPT 环境未就绪，以 `WAITING_FOR_FASTGPT_ENVIRONMENT` 状态诚实降级，不做任何伪造。

## 二、架构

```
Agent/Workflow
      │
      ▼
RAGService (LocalRAGService)
      │
      ├── PolicyRetriever (PolicyRetriever抽象)
      │      ├── KeywordPolicyRetriever  (本地关键词检索，默认)
      │      └── FastGptPolicyRetriever   (FastGPT知识库检索，待接入)
      │
      ▼
policy_versions 表（有效期内政策版本 + keywords 本体关键词）
```

### 核心接口

| 类型 | 说明 |
|------|------|
| `RAGService` | 统一检索入口：`retrieve(query)`、`toCitations(result)`、`getProviderName()`、`getStatus()` |
| `PolicyRetriever` | 检索器抽象：`retrieve(query)`、`providerName()` |
| `RetrievalQuery` | 检索参数：query / intent / category / userRole / policyId / topK 等 |
| `RetrievalResult` | 检索结果：provider、documents、totalCount、note（警告/降级原因） |
| `RetrievedDocument` | 命中文档：policyId、标题、分类、正文、有效期、keywords、score、metadata |

## 三、本地关键词检索（KeywordPolicyRetriever）

- 查询入口对 `policy_versions` 组装 `LambdaQueryWrapper`：
  1. `status = ACTIVE`（只允许有效期内政策版本）
  2. 分类过滤（可选，`isPolicyCategoryWithSub` 前缀匹配，如 `SCHOLARSHIP`）
  3. 关键词过滤（可选，`keywords LIKE %kw%`）
- 命中文档仅包含 `effective_date <= now <= expiry_date` 的版本（`isPolicyEffective` 判断）。
- topK 内对结果做相关性打分（关键词命中数/位置），按分数降序返回。
- 状态 = `LOCAL_KEYWORD`。

## 四、FastGPT 适配器（FastGptPolicyRetriever）

- 依赖 `FastGPTConnectionChecker.check()` 判定环境状态：
  - `NOT_CONFIGURED`：FastGPT url/apiKey 为空 → 不检索，返回空结果 + 明确 note。
  - `CONFIGURED_WAITING_ENVIRONMENT`：已配置但无真实联调环境 → 不执行检索，note 说明"不伪造结果"。
  - 其他：TODO（待 SF-FastGPT API 文档）实现真实知识库检索。
- 状态 = `FASTGPT`，`FastGPTProvider` 的 healthCheck 返回 `properties.isConfigured()`。

> **原则（不伪造）**：未接入真实环境前，任何路径都不返回制造出来的"检索结果"，
> 而是返回空文档集 + `WAITING_FOR_FASTGPT_ENVIRONMENT` 说明，写入 `RetrievalResult.note` 并可被 Trace 展示。

## 五、检索质量与引用

- `RAGService.toCitations(result)` 将命中文档转换为 `Citation`（标题/来源/片段/有效期），
  Agent 响应与前端 Trace 可展示引用，防止无依据回答。
- Workflow 决策仅使用 top1 文档的 policyId/category，保证"以有效期内政策版本为准"。

## 六、已知限制与后续

| 限制 | 说明 | 后续 |
|------|------|------|
| 标量关键词检索 | 无向量/语义检索与 rerank | SF-FastGPT 知识库接入后升级 |
| 无资料导入工具 | keywords 为手工标注 | Phase 4 增加管理端导入 |

---

*本文档与 docs/11-知识库设计.md、docs/10-FastGPT集成设计.md 配套。*