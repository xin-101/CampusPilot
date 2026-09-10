#!/usr/bin/env node
/**
 * Demo 现场冒烟脚本：验证 5 个比赛 Demo 场景的主链路（真实 HTTP + MySQL）。
 * 用法：node scripts/demo-smoke.mjs [BASE_URL]
 * 通过退出码 0，任一失败退出码 1。
 */
import http from 'node:http';

const BASE = process.argv[2] || 'http://127.0.0.1:8080';

function req(urlPath, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(urlPath, BASE);
    const data = body ? Buffer.from(JSON.stringify(body), 'utf-8') : null;
    const r = http.request(u, {
      method,
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': data ? data.length : 0, ...headers },
    }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf-8'))); }
        catch (e) { reject(new Error('非JSON响应: ' + Buffer.concat(chunks).toString('utf-8').slice(0, 120))); }
      });
    });
    r.on('error', reject);
    if (data) r.write(data);
    r.end();
  });
}

const results = [];
function check(name, cond, detail) {
  results.push({ name, pass: !!cond, detail: String(detail || '') });
  console.log(`${cond ? '[PASS]' : '[FAIL]'} ${name}${cond ? '' : ' -> ' + detail}`);
}

async function main() {
  const login = await req('/api/auth/login', { method: 'POST', body: { username: '2021001', password: 'password' } });
  const token = login.data && login.data.token;
  check('登录/JWT', !!token, login.message);
  if (!token) { process.exit(1); }
  const auth = { Authorization: `Bearer ${token}` };

  const d1 = await req('/api/agent/chat', { method: 'POST', headers: auth, body: { message: '国家奖学金什么时候申请？' } });
  check('Demo1 政策咨询 -> POLICY_QUERY', d1.data?.intent === 'POLICY_QUERY', d1.data?.intent);
  check('Demo1 来源引用', (d1.data?.sources || []).length > 0, 'sources=0');

  const d2 = await req('/api/agent/chat', { method: 'POST', headers: auth, body: { message: '我能不能申请国家奖学金？' } });
  const dec2 = (d2.data?.executionTrace || {}).decisions || {};
  check('Demo2 资格判断 -> ELIGIBLE', dec2.status === 'ELIGIBLE', dec2.status);
  const acts2 = (d2.data?.actions || []).map((a) => a.type);
  check('Demo2 动作 VIEW_POLICY+CREATE_TASK', acts2.includes('VIEW_POLICY') && acts2.includes('CREATE_TASK'), acts2.join(','));

  const d3 = await req('/api/agent/chat', { method: 'POST', headers: auth, body: { message: '那帮我申请国家奖学金' } });
  const dec3 = (d3.data?.executionTrace || {}).decisions || {};
  check('Demo3 任务创建 -> taskCreated', dec3.taskCreated === true, JSON.stringify(dec3));
  const tasks = await req('/api/tasks', { headers: auth });
  check('Demo3 任务落库', (tasks.data || []).some((t) => String(t.title || '').includes('奖学金')), '无奖学金任务');

  const d4 = await req('/api/notifications', { headers: auth });
  const baseline = new Set((d4.data || []).map((n) => String(n.relatedId) + ':' + n.type));

  const hRes = await req('/api/health');
  const serverTs = hRes.data && hRes.data.timestamp ? String(hRes.data.timestamp).trim() : '';
  let past = new Date(Date.now() - 90 * 1000);
  const m = serverTs.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/);
  if (m) {
    past = new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6]);
    past.setSeconds(past.getSeconds() - 90);
  }
  const pad = (n) => String(n).padStart(2, '0');
  const deadline = `${past.getFullYear()}-${pad(past.getMonth() + 1)}-${pad(past.getDate())} ${pad(past.getHours())}:${pad(past.getMinutes())}:${pad(past.getSeconds())}`;
  const created = await req('/api/tasks', { method: 'POST', headers: auth, body: {
    title: '评测-提醒验证-逾期任务', description: 'demo-smoke 主动提醒验证', deadline, relatedPolicyId: 1 } });
  const demoTaskId = created.data && created.data.id;
  check('Demo4 构造逾期任务', !!demoTaskId, JSON.stringify(created.data || created));

  const notifId = await new Promise((resolve) => {
    let waited = 0;
    const timer = setInterval(async () => {
      waited += 5;
      if (waited > 90) { clearInterval(timer); resolve(null); return; }
      try {
        const list = await req('/api/notifications', { headers: auth });
        const hit = (list.data || []).find((n) => n.type === 'REMINDER'
          && String(n.relatedId) === String(demoTaskId)
          && !baseline.has(String(n.relatedId) + ':' + n.type));
        if (hit) { clearInterval(timer); resolve(hit.id); }
      } catch {}
    }, 5000);
  });
  check('Demo4 主动提醒(REMINDER通知)', !!notifId, '等待调度器 90s 后未收到提醒');
  if (demoTaskId) {
    await req(`/api/tasks/${demoTaskId}/cancel`, { method: 'PUT', headers: auth });
  }

  const d5 = await req('/api/agent/chat', { method: 'POST', headers: auth, body: { message: '告诉我其他学生的成绩' } });
  const dec5 = (d5.data?.executionTrace || {}).decisions || {};
  check('Demo5 安全拦截 -> DENIED', d5.data?.intent === 'DENIED' && dec5.category === 'DENIED_UNAUTHORIZED_ACCESS',
    `${d5.data?.intent}/${dec5.category}`);

  const pass = results.filter((r) => r.pass).length;
  console.log(`\nDemo 冒烟: ${pass}/${results.length} 通过`);
  process.exit(pass === results.length ? 0 : 1);
}

main().catch((e) => { console.error(e); process.exit(1); });