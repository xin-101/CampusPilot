#!/usr/bin/env node
/**
 * CampusPilot Phase 4 综合评测 Runner (M1-M6)
 *
 * M1 RAG 检索命中率    -> /api/rag/retrieve 断言目标政策在结果中
 * M2 意图识别准确率    -> /api/agent/chat  断言 intent / workflowId
 * M3 回答质量(关键词)  -> /api/agent/chat  断言回复包含要点
 * M4 安全拦截          -> /api/agent/chat  断言 DENIED + decisions.category
 * M5 任务执行完整度    -> 资格判断动作 + 任务落库(DB) + 角色边界
 * M6 响应性能          -> 全量对话 p95 延迟 <= 预算
 *
 * 用法：node evaluation/phase4/run-phase4-eval.mjs [BASE_URL]
 * 结果：evaluation/results/phase4-evaluation-result.json + phase4-evaluation-report.md
 */
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.argv[2] || 'http://127.0.0.1:8080';
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..', '..');
const RESULTS_DIR = path.join(REPO_ROOT, 'evaluation', 'results');
const CASES_FILE = path.join(__dirname, 'cases.json');

function httpRequest(base, urlPath, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(urlPath, base);
    const data = body ? Buffer.from(JSON.stringify(body), 'utf-8') : null;
    const started = Date.now();
    const req = http.request(u, {
      method,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': data ? data.length : 0,
        ...headers,
      },
    }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf-8');
        const ms = Date.now() - started;
        try {
          resolve({ json: JSON.parse(text), ms });
        } catch {
          reject(new Error(`非JSON响应(${res.statusCode}): ${text.slice(0, 120)}`));
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

const logins = {};
async function tokenFor(username) {
  if (!logins[username]) {
    const { json } = await httpRequest(BASE, '/api/auth/login', {
      method: 'POST',
      body: { username, password: 'password' },
    });
    if (!json.success) throw new Error(`登录失败 ${username}: ${(json.message || '')}`);
    logins[username] = json.data.token;
  }
  return logins[username];
}

const latencySamples = [];
function trackLatency(ms) {
  latencySamples.push(ms);
}

function pct(sorted, p) {
  if (!sorted.length) return 0;
  const idx = Math.min(sorted.length - 1, Math.ceil(sorted.length * p / 100) - 1);
  return sorted[idx];
}

const summary = {};

function record(group, name, checks) {
  const pass = Object.values(checks).every((v) => String(v).startsWith('PASS'));
  const g = summary[group] || (summary[group] = { passed: 0, total: 0, cases: [] });
  g.total++;
  if (pass) g.passed++;
  g.cases.push({ name, group, pass, checks });
}

async function runIntent(c) {
  const token = await tokenFor('2021001');
  const { json, ms } = await httpRequest(BASE, '/api/agent/chat', {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: { message: c.query },
  });
  trackLatency(ms);
  if (!json.success) return [{ api: `接口失败 ${json.message || ''}` }];
  const d = json.data || {};
  const checks = {};
  checks.intent = d.intent === c.expectIntent ? 'PASS' : `FAIL 实际 ${d.intent}`;
  if (c.expectWorkflow) {
    const wf = (d.executionTrace || {}).workflowId;
    checks.workflowId = wf === c.expectWorkflow ? 'PASS' : `FAIL 实际 ${wf}`;
  }
  return checks;
}

async function runRetrieval(c) {
  const token = await tokenFor('2021001');
  const { json } = await httpRequest(BASE, '/api/rag/retrieve', {
    method: 'POST', headers: { Authorization: `Bearer ${token}` },
    body: { query: c.query, topK: 5, category: c.category || null },
  });
  if (!json.success || !json.data) return [{ api: 'FAIL RAG接口未成功' }];
  const ids = (json.data.documents || []).map((d) => Number(d.policyId));
  const checks = {};
  const missing = c.expectPolicyIdsInclude.filter((id) => !ids.includes(id));
  checks.hit = missing.length === 0
    ? `PASS ${ids.length} docs`
    : `FAIL 缺 ${missing.join(',')} [${ids.join(',')}]`;
  return checks;
}

async function runAnswer(c) {
  const token = await tokenFor('2021001');
  const { json, ms } = await httpRequest(BASE, '/api/agent/chat', {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: { message: c.query },
  });
  trackLatency(ms);
  if (!json.success) return [{ api: 'FAIL 接口失败' }];
  const text = String((json.data && json.data.response) || '');
  const checks = {};
  for (const kw of c.expectContains) {
    checks[`包含「${kw}」`] = text.includes(kw) ? 'PASS' : 'FAIL 未命中';
  }
  if (!(json.data && json.data.sources && json.data.sources.length)) {
    checks['来源引用'] = 'FAIL 无来源';
  }
  return checks;
}

async function runSecurity(c) {
  const token = await tokenFor('2021001');
  const { json, ms } = await httpRequest(BASE, '/api/agent/chat', {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: { message: c.query },
  });
  trackLatency(ms);
  if (!json.success) return [{ api: 'FAIL 接口失败' }];
  const d = json.data || {};
  const dec = (d.executionTrace || {}).decisions || {};
  const checks = {};
  checks.intent = d.intent === 'DENIED' ? 'PASS' : `FAIL 实际 ${d.intent}`;
  checks.category = dec.category === c.expectCategory ? 'PASS' : `FAIL 实际 ${dec.category}`;
  return checks;
}

async function runTask(c) {
  const token = await tokenFor(c.username);
  const { json: tasksBefore } = await httpRequest(BASE, '/api/tasks', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const beforeCount = (tasksBefore.data || []).length;
  const { json, ms } = await httpRequest(BASE, '/api/agent/chat', {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: { message: c.query },
  });
  trackLatency(ms);
  if (!json.success) return [{ api: 'FAIL 接口失败' }];
  const d = json.data || {};
  const dec = (d.executionTrace || {}).decisions || {};
  const actions = (d.actions || []).map((a) => a.type);
  const { json: tasksAfter } = await httpRequest(BASE, '/api/tasks', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const afterList = tasksAfter.data || [];
  const checks = {};

  if (c.expectEligible !== undefined) {
    checks.eligible = dec.eligible === c.expectEligible
      ? 'PASS' : `FAIL 实际 ${dec.eligible}`;
  }
  if (c.expectStatus) {
    checks.status = dec.status === c.expectStatus ? 'PASS' : `FAIL 实际 ${dec.status}`;
  }
  if (c.expectActionsInclude) {
    for (const a of c.expectActionsInclude) {
      checks[`动作 ${a}`] = actions.includes(a) ? 'PASS' : `FAIL 缺 ${a}`;
    }
  }
  if (c.expectTaskCreated !== undefined) {
    const ok = dec.taskCreated === c.expectTaskCreated;
    checks.taskCreated = ok ? 'PASS' : `FAIL 实际 ${dec.taskCreated}`;
    if (c.expectTaskCreated) {
      const newest = afterList[0];
      const delta = afterList.length - beforeCount;
      checks['任务落库+1'] = delta >= 1 ? `PASS (${delta})` : 'FAIL 无新增';
      if (newest) {
        checks.title = String(newest.title || '').includes(c.expectTaskTitleContains || '')
          ? 'PASS' : `FAIL ${newest.title}`;
        checks.policyId = c.expectTaskPolicyIdIn.includes(Number(newest.policyId))
          ? 'PASS' : `FAIL ${newest.policyId}`;
      }
    } else {
      checks['未落库'] = afterList.length === beforeCount ? 'PASS' : 'FAIL 竟然新增了任务';
    }
  }
  if (c.expectNoLeak) {
    const text = JSON.stringify({ response: d.response || '', decisions: dec });
    for (const kw of c.expectNoLeak) {
      checks[`不泄露「${kw}」`] = text.includes(kw) ? 'FAIL 泄露' : 'PASS';
    }
  }
  return checks;
}

async function main() {
  const cases = JSON.parse(fs.readFileSync(CASES_FILE, 'utf-8'));
  const budget = cases.meta.budget.latencyBudgetMs;

  for (const c of cases.intent) record('M2 意图识别', c.name, await runIntent(c));
  for (const c of cases.retrieval) record('M1 RAG命中率', c.name, await runRetrieval(c));
  for (const c of cases.answer) record('M3 回答质量', c.name, await runAnswer(c));
  for (const c of cases.security) record('M4 安全拦截', c.name, await runSecurity(c));
  for (const c of cases.task) record('M5 任务执行', c.name, await runTask(c));
  for (const c of cases.roleAccess) record('M5 角色边界', c.name, await runTask(c));

  const sorted = [...latencySamples].sort((a, b) => a - b);
  const p50 = pct(sorted, 50);
  const p95 = pct(sorted, 95);
  const max = sorted[sorted.length - 1] || 0;
  const m6Pass = p95 <= budget;
  summary['M6 性能p95'] = {
    passed: m6Pass ? 1 : 0, total: 1,
    cases: [{ name: `p95=${p95}ms (budget<=${budget}ms)`, pass: m6Pass, checks: { p95, p50, max, samples: latencySamples.length } }],
  };

  const all = Object.values(summary).flatMap((g) => g.cases);
  const passAll = all.filter((x) => x.pass).length;
  const totalAll = all.length;
  const overallPass = passAll === totalAll;

  const timestamp = new Date().toISOString().replace(/:/g, '-').slice(0, 19);
  const result = {
    meta: { name: cases.meta.name, date: cases.meta.date, baseUrl: BASE, timestamp },
    summary: Object.entries(summary).map(([g, s]) => ({
      group: g, passed: s.passed, total: s.total, rate: Math.round(s.passed * 1000 / s.total) / 10,
    })),
    m6Latency: { p50, p95, max, samples: latencySamples.length, budgetMs: budget },
    cases: all.map((x) => ({ name: x.name, group: x.group, pass: x.pass, checks: x.checks })),
    overall: { passed: passAll, total: totalAll, pass: overallPass },
    ragNote: '见 evaluation/rag/results/rag-evaluation-report.md 专题报告',
  };
  fs.mkdirSync(RESULTS_DIR, { recursive: true });
  fs.writeFileSync(path.join(RESULTS_DIR, 'phase4-evaluation-result.json'), JSON.stringify(result, null, 2));
  fs.writeFileSync(path.join(RESULTS_DIR, 'phase4-evaluation-report.md'), buildReport(result, cases.meta));
  console.log('=== Phase 4 评测 (M1-M6) ===');
  console.log(result.summary.map((g) => `  ${g.group}: ${g.passed}/${g.total} (${g.rate}%)`).join('\n'));
  console.log(`  M6 性能: p50=${p50}ms p95=${p95}ms max=${max}ms budget<=${budget}ms`);
  console.log(`  ALL: ${passAll}/${totalAll} ${overallPass ? 'PASSED' : 'FAILED'}`);
  console.log(`报告: ${path.join(RESULTS_DIR, 'phase4-evaluation-report.md')}`);
  process.exit(overallPass ? 0 : 1);
}

function buildReport(result, meta) {
  const lines = [];
  lines.push(`# CampusPilot Phase 4 综合评测报告（${meta.date}）`);
  lines.push('');
  lines.push('> 所有结果均来自真实 HTTP 调用（MySQL + RAG 向量检索 + 规则引擎），未伪造任何指标。');
  lines.push('');
  lines.push('## 总览');
  lines.push('');
  lines.push('| 指标组 | 通过 | 总数 | 通过率 |');
  lines.push('|--------|------|------|--------|');
  for (const g of result.summary) {
    lines.push(`| ${g.group} | ${g.passed} | ${g.total} | ${g.rate}% |`);
  }
  lines.push(`| **ALL** | **${result.overall.passed}** | **${result.overall.total}** | ${(result.overall.passed * 100 / result.overall.total).toFixed(1)}% |`);
  lines.push('');
  lines.push(`## M6 响应性能（p95 ≦ ${result.m6Latency.budgetMs}ms）`);
  lines.push('');
  lines.push(`| 指标 | 值 |`);
  lines.push(`|------|-----|`);
  lines.push(`| p50 | ${result.m6Latency.p50} ms |`);
  lines.push(`| p95 | ${result.m6Latency.p95} ms |`);
  lines.push(`| max | ${result.m6Latency.max} ms |`);
  lines.push(`| 采样数 | ${result.m6Latency.samples} |`);
  lines.push('');
  lines.push('## 明细');
  lines.push('');
  lines.push('| 用例 | 结果 | 检查项 |');
  lines.push('|------|------|--------|');
  for (const c of result.cases) {
    const detail = Object.entries(c.checks || {}).map(([k, v]) => `${k}:${v}`).join('; ');
    lines.push(`| ${c.name} <br><sub>${c.group}</sub> | ${c.pass ? '✅' : '❌'} | ${detail} |`);
  }
  lines.push('');
  lines.push('## 说明');
  lines.push('');
  lines.push('- LCS 说明：相位校验独立运行，RAG 专题见 evaluation/rag。');
  lines.push('- 免责声明：Demo 数据(is_demo=1)仅用于系统演示，资格/政策结论不代表任何真实校规。');
  return lines.join('\n');
}

main().catch((e) => { console.error(e); process.exit(1); });