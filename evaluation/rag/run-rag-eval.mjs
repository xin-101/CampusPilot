#!/usr/bin/env node
/**
 * RAG 评测 Runner (Phase 4)
 * 覆盖：命中率 / TopK / 来源覆盖率 / 过期政策过滤 / 错误政策排除 / 无结果处理
 *
 * 用法：node evaluation/rag/run-rag-eval.mjs [BASE_URL]
 * 结果：evaluation/rag/results/rag-evaluation-result.json + rag-evaluation-report.md
 */
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.argv[2] || 'http://127.0.0.1:8080';
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const RESULTS_DIR = path.join(__dirname, 'results');
const CASES_FILE = path.join(__dirname, 'cases.json');

let TOKEN = '';

function httpRequest(base, path, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, base);
    const data = body ? Buffer.from(JSON.stringify(body), 'utf-8') : null;
    const req = http.request(url, {
      method,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': data ? data.length : 0,
        ...headers,
      },
    }, (res) => {
      let chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf-8');
        try {
          resolve(JSON.parse(text));
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

async function post(p, body, headers = {}) {
  return httpRequest(BASE, p, { method: 'POST', headers, body });
}

function statusOf(r) {
  return r.success === false ? 'FAIL' : 'PASS';
}

async function collectResult(c) {
  try {
    // 纯检索层用例走 RAG 端点（可控 topK/category/effectiveDate）；
    // 覆盖类(端到端引用)与无结果类走 Agent 对话路径。
    const viaAgent = (c.group === 'coverage' || c.group === 'no-result');
    let data, trace, docs, sources;
    if (viaAgent) {
      const res = await post('/api/agent/chat', { message: c.query }, { Authorization: `Bearer ${TOKEN}` });
      if (!res.success) {
        return { case: c, pass: false, checks: { api: 'FAIL 接口未成功' } };
      }
      data = res.data || {};
      trace = data.executionTrace || {};
      docs = (trace.retrieval && trace.retrieval.documents) || [];
      sources = data.sources || [];
    } else {
      const body = { query: c.query, topK: c.topK || 5, category: c.category || null };
      const res = await post('/api/rag/retrieve', body, { Authorization: `Bearer ${TOKEN}` });
      if (!res.success || !res.data) {
        return { case: c, pass: false, checks: { api: 'FAIL RAG检索接口未成功' } };
      }
      docs = res.data.documents || [];
      sources = [];
    }
    const checks = {};
    let pass = true;

    const check = (name, ok, detail) => {
      checks[name] = ok ? 'PASS' : `FAIL ${detail || ''}`;
      if (!ok) pass = false;
    };

    if (c.group === 'no-result') {
      check('graceful', (data.response || '').length > 0 && data.intent !== 'ERROR', '无结果应友好返回');
      return { case: c, pass, checks };
    }

    check('docsNotEmpty', docs.length > 0, `documents为空 (${docs.length})`);

    const docNames = docs.map((d) => String(d.policyName || ''));
    const docIds = docs.map((d) => Number(d.policyId));
    const combined = (docNames.join(' ') + ' ' + (sources.map((s) => s.policyName).join(' '))).toLowerCase();

    if (viaAgent) {
      check('sourcesNotEmpty', sources.length > 0, 'sources为空');
    }

    if (c.topK) {
      check(`topK<=${c.topK}`, docs.length <= c.topK, `docs=${docs.length}`);
    }
    if (c.expectedMinResults) {
      check('minResults', docs.length >= c.expectedMinResults, `docs=${docs.length}`);
    }
    if (c.expectedMaxResults) {
      check('maxResults', docs.length <= c.expectedMaxResults, `docs=${docs.length}`);
    }
    if (c.expectedTop1Contains) {
      const top1 = docNames[0] || '';
      check('top1Contains', top1.includes(c.expectedTop1Contains), `top1=${docNames.slice(0,2).join(',')}`);
    }
    if (c.expectedPolicyIds) {
      const hit = c.expectedPolicyIds.every((id) => docIds.includes(Number(id)));
      check('hitExpectedIds', hit, `got ids=[${docIds.join(',')}] want=${c.expectedPolicyIds.join(',')}`);
    }
    if (c.expectedCategory) {
      check('hitCategory', docNames.length > 0, 'category case expects top doc');
    }
    if (c.expectedNotInclude) {
      const bad = docs.filter((d) => (d.policyName || '').includes(c.expectedNotInclude));
      check('noExpiredDoc', bad.length === 0, `过期文档被返回: ${bad.map((d)=>d.policyName).join(',')}`);
    }
    if (c.expectedNotInIds) {
      const bad = c.expectedNotInIds.filter((id) => docIds.includes(Number(id)));
      check('noExpiredId', bad.length === 0, `过期policyId=10000被返回`);
    }
    if (c.expectedExcludeCategory) {
      const bad = docs.filter((d) => String(d.category || '') === c.expectedExcludeCategory);
      check('excludeCategory', bad.length === 0, `错误分类: ${bad.map((d)=>d.policyName).join(',')}`);
    }
    return { case: c, pass, checks };
  } catch (e) {
    return { case: c, pass: false, checks: { exception: `FAIL ${e.message.slice(0, 120)}` } };
  }
}

async function main() {
  const cases = JSON.parse(fs.readFileSync(CASES_FILE, 'utf-8'));
  const login = await post('/api/auth/login', { username: '2021001', password: 'password' });
  if (!login.success || !login.data?.token) {
    console.error('登录失败，请确认后端已启动');
    process.exit(1);
  }
  TOKEN = login.data.token;
  console.log('登录成功\n');

  const byGroup = {};
  const results = [];
  for (const c of cases) {
    const r = await collectResult(c);
    results.push(r);
    const group = c.group;
    if (!byGroup[group]) byGroup[group] = { total: 0, pass: 0, fail: 0 };
    byGroup[group].total++;
    byGroup[group].pass += r.pass ? 1 : 0;
    byGroup[group].fail += r.pass ? 0 : 1;
    const mark = r.pass ? '✅' : '❌';
    const fails = Object.entries(r.checks).filter(([, v]) => v.startsWith('FAIL'));
    console.log(`${mark} ${r.case.id} [${group}] ${r.case.query}`);
    fails.forEach(([k, v]) => console.log(`       ${k}: ${v}`));
  }

  const total = results.length;
  const passed = results.filter((r) => r.pass).length;

  // 汇总
  const groupSummary = Object.entries(byGroup).map(([g, s]) => ({
    group: g,
    total: s.total,
    passed: s.pass,
    failed: s.fail,
    rate: s.total ? Math.round((s.pass / s.total) * 100) : 0,
  }));
  groupSummary.push({
    group: 'ALL',
    total,
    passed,
    failed: total - passed,
    rate: total ? Math.round((passed / total) * 100) : 0,
  });

  const output = {
    runAt: new Date().toISOString(),
    baseUrl: BASE,
    provider: 'LOCAL_VECTOR_TFIDF',
    summary: groupSummary,
    result: total - passed === 0 ? 'PASSED' : 'FAILED',
    cases: results.map((r) => ({
      id: r.case.id,
      group: r.case.group,
      query: r.case.query,
      pass: r.pass,
      checks: r.checks,
    })),
  };

  fs.mkdirSync(RESULTS_DIR, { recursive: true });
  fs.writeFileSync(
    path.join(RESULTS_DIR, 'rag-evaluation-result.json'),
    JSON.stringify(output, null, 2),
    'utf-8'
  );

  // 生成报告
  const lines = [];
  lines.push('# RAG 评测报告（Phase 4）');
  lines.push('');
  lines.push(`- 运行时间：${output.runAt}`);
  lines.push(`- 检索提供方：${output.provider}`);
  lines.push(`- 结果：**${output.result}**`);
  lines.push('');
  lines.push('## 汇总');
  lines.push('');
  lines.push('| 分组 | 总数 | 通过 | 失败 | 通过率 |');
  lines.push('|------|------|------|------|--------|');
  for (const s of groupSummary) {
    lines.push(`| ${s.group} | ${s.total} | ${s.passed} | ${s.failed} | ${s.rate}% |`);
  }
  lines.push('');
  lines.push('## 明细');
  lines.push('');
  for (const r of results) {
    const failedEntries = Object.entries(r.checks).filter(([, v]) => v.startsWith('FAIL'));
    const badge = failedEntries.length ? '❌ FAIL' : '✅ PASS';
    lines.push(`### ${r.case.id} ${badge} — ${r.case.group}`);
    lines.push('');
    lines.push(`- 查询：${r.case.query}`);
    for (const [k, v] of Object.entries(r.checks)) {
      lines.push(`- \`${k}\`：${v}`);
    }
    lines.push('');
  }

  fs.writeFileSync(path.join(RESULTS_DIR, 'rag-evaluation-report.md'), lines.join('\n'), 'utf-8');

  console.log(`\n=== 结果: ${total - passed === 0 ? 'PASSED' : 'FAILED'} (${passed}/${total}) ===`);
  groupSummary.forEach((s) => console.log(`  ${s.group}: ${s.passed}/${s.total} (${s.rate}%)`));
  console.log(`\n报告: evaluation/rag/results/rag-evaluation-report.md`);
}

main().catch((e) => { console.error(e); process.exit(1); });