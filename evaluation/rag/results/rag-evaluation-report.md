# RAG 评测报告（Phase 4）

- 运行时间：2026-09-10T13:16:45.375Z
- 检索提供方：LOCAL_VECTOR_TFIDF
- 结果：**PASSED**

## 汇总

| 分组 | 总数 | 通过 | 失败 | 通过率 |
|------|------|------|------|--------|
| hit-rate | 6 | 6 | 0 | 100% |
| topk | 2 | 2 | 0 | 100% |
| coverage | 2 | 2 | 0 | 100% |
| expired-filter | 2 | 2 | 0 | 100% |
| wrong-policy-exclusion | 2 | 2 | 0 | 100% |
| no-result | 2 | 2 | 0 | 100% |
| ALL | 16 | 16 | 0 | 100% |

## 明细

### RAG-001 ✅ PASS — hit-rate

- 查询：国家奖学金奖励标准是多少钱
- `docsNotEmpty`：PASS
- `top1Contains`：PASS
- `hitExpectedIds`：PASS

### RAG-002 ✅ PASS — hit-rate

- 查询：国家助学金可以每月发多少
- `docsNotEmpty`：PASS
- `top1Contains`：PASS
- `hitExpectedIds`：PASS

### RAG-003 ✅ PASS — hit-rate

- 查询：学生寝室晚上几点关门
- `docsNotEmpty`：PASS
- `top1Contains`：PASS
- `hitExpectedIds`：PASS

### RAG-004 ✅ PASS — hit-rate

- 查询：请假超过三天谁来审批
- `docsNotEmpty`：PASS
- `top1Contains`：PASS
- `hitExpectedIds`：PASS

### RAG-005 ✅ PASS — hit-rate

- 查询：在读证明在哪里办理
- `docsNotEmpty`：PASS
- `top1Contains`：PASS
- `hitExpectedIds`：PASS

### RAG-006 ✅ PASS — hit-rate

- 查询：课程考试作弊怎么处理
- `docsNotEmpty`：PASS
- `top1Contains`：PASS
- `hitExpectedIds`：PASS

### RAG-007 ✅ PASS — topk

- 查询：奖学金
- `docsNotEmpty`：PASS
- `topK<=1`：PASS
- `minResults`：PASS
- `top1Contains`：PASS

### RAG-008 ✅ PASS — topk

- 查询：助学金
- `docsNotEmpty`：PASS
- `topK<=3`：PASS
- `minResults`：PASS
- `maxResults`：PASS

### RAG-009 ✅ PASS — coverage

- 查询：国家奖学金申请条件和奖励标准
- `docsNotEmpty`：PASS
- `sourcesNotEmpty`：PASS
- `hitExpectedIds`：PASS

### RAG-010 ✅ PASS — coverage

- 查询：学生综合素质测评哪个维度占比最高
- `docsNotEmpty`：PASS
- `sourcesNotEmpty`：PASS
- `hitCategory`：PASS

### RAG-011 ✅ PASS — expired-filter

- 查询：国家奖学金旧版管理办法前15%
- `docsNotEmpty`：PASS
- `hitExpectedIds`：PASS
- `noExpiredDoc`：PASS

### RAG-012 ✅ PASS — expired-filter

- 查询：国家奖学金怎么办
- `docsNotEmpty`：PASS
- `noExpiredId`：PASS

### RAG-013 ✅ PASS — wrong-policy-exclusion

- 查询：助学金申请流程
- `docsNotEmpty`：PASS
- `excludeCategory`：PASS

### RAG-014 ✅ PASS — wrong-policy-exclusion

- 查询：宿舍大功率电器
- `docsNotEmpty`：PASS
- `excludeCategory`：PASS

### RAG-015 ✅ PASS — no-result

- 查询：量子力学课程注册费是多少
- `graceful`：PASS

### RAG-016 ✅ PASS — no-result

- 查询：龙舟社团报名时间
- `graceful`：PASS
