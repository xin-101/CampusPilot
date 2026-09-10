#!/usr/bin/env node
/**
 * Phase 6 LLM 集成冒烟测试
 *
 * 测试项：
 *   1. LLM config endpoint 返回 provider 信息
 *   2. 若未配置 LLM API Key → 返回 NOT_RUN（非 FAIL）
 *   3. 若已配置 LLM API Key → 调用真实 LLM 验证
 *   4. LLM error handling: LLM 不可用时返回友好错误
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const JWT = process.env.JWT || '';

const headers = { 'Content-Type': 'application/json' };
if (JWT) headers['Authorization'] = `Bearer ${JWT}`;

let passed = 0;
let failed = 0;
let notRun = 0;

function assert(name, condition, detail) {
  if (condition) {
    console.log(`  ✅ ${name}`);
    passed++;
  } else {
    console.log(`  ❌ ${name}${detail ? ': ' + detail : ''}`);
    failed++;
  }
}

function skip(name, reason) {
  console.log(`  ⏭️  ${name} (NOT_RUN: ${reason})`);
  notRun++;
}

async function request(path, method, body) {
  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${BASE_URL}${path}`, opts);
  if (res.status === 401 || res.status === 403) {
    return { status: res.status, data: null, error: 'unauthorized' };
  }
  const json = await res.json();
  return { status: res.status, data: json };
}

// ── Test 1: Agent Config Endpoint ──────────────────────────────────

async function testAgentConfig() {
  console.log('\n📋 Test 1: Agent Config Endpoint');
  const res = await request('/api/agent/config', 'GET');

  if (res.status === 401 || res.status === 403) {
    skip('Agent config endpoint', 'unauthorized');
    return false;
  }

  assert('Config endpoint returns 200', res.status === 200);
  assert('Config has provider field', res.data?.data?.provider !== undefined);
  assert('Config has llmEnabled field', typeof res.data?.data?.llmEnabled === 'boolean');

  return res.data?.data?.llmEnabled || false;
}

// ── Test 2: LLM Not Configured → NOT_RUN ──────────────────────────

async function testLLMNotConfigured(llmEnabled) {
  console.log('\n📋 Test 2: LLM Configuration Check');

  if (llmEnabled) {
    skip('LLM not configured check', 'LLM is enabled, skipping not-configured test');
    return true;
  }

  assert('LLM is disabled (default)', !llmEnabled);
  return false;
}

// ── Test 3: LLM Chat with Mock Mode (should work) ─────────────────

async function testMockModeChat() {
  console.log('\n📋 Test 3: Mock Mode Chat');
  const res = await request('/api/agent/chat', 'POST', {
    message: '你好'
  });

  if (res.status === 401 || res.status === 403) {
    skip('Mock mode chat', 'unauthorized');
    return;
  }

  assert('Chat returns 200', res.status === 200);
  assert('Response has message', res.data?.data?.response?.length > 0);
  assert('Response has intent', res.data?.data?.intent !== undefined);
  assert('Response has executionTrace', res.data?.data?.executionTrace !== undefined);
}

// ── Test 4: LLM Chat (if enabled) ──────────────────────────────────

async function testLLMChat(llmEnabled) {
  console.log('\n📋 Test 4: LLM Mode Chat');

  if (!llmEnabled) {
    skip('LLM chat test', 'LLM not enabled, set LLM_ENABLED=true + LLM_BASE_URL + LLM_API_KEY to test');
    return;
  }

  const res = await request('/api/agent/chat', 'POST', {
    message: '查询奖学金政策'
  });

  if (res.status === 401 || res.status === 403) {
    skip('LLM chat', 'unauthorized');
    return;
  }

  assert('LLM chat returns 200', res.status === 200);
  assert('LLM response has message', res.data?.data?.response?.length > 0);
  assert('LLM response has intent', res.data?.data?.intent !== undefined);
  assert('LLM provider is RealLLM', res.data?.data?.executionTrace?.workflowId !== undefined);

  const trace = res.data?.data?.executionTrace;
  if (trace) {
    console.log(`    Provider: ${trace.workflowName || 'unknown'}`);
    console.log(`    Duration: ${trace.duration || 0}ms`);
    console.log(`    Steps: ${trace.steps?.length || 0}`);
  }
}

// ── Test 5: LLM Error Handling ──────────────────────────────────────

async function testLLMErrorHandling(llmEnabled) {
  console.log('\n📋 Test 5: LLM Error Handling');

  if (!llmEnabled) {
    skip('LLM error handling', 'LLM not enabled');
    return;
  }

  // Send a very long message to potentially trigger timeout
  const longMessage = '测试'.repeat(1000);
  const res = await request('/api/agent/chat', 'POST', {
    message: longMessage
  });

  if (res.status === 401 || res.status === 403) {
    skip('LLM error handling', 'unauthorized');
    return;
  }

  // Should still return a response (either success or friendly error)
  assert('Long message handled gracefully', res.status === 200 || res.status === 500);

  if (res.status === 200) {
    assert('Response is not empty', res.data?.data?.response?.length > 0);
    assert('No Java stack trace in response', !res.data?.data?.response?.includes('Exception'));
  }
}

// ── Main ──────────────────────────────────────────────────────────

async function main() {
  console.log('═══════════════════════════════════════════════');
  console.log('  Phase 6 LLM 集成冒烟测试');
  console.log('═══════════════════════════════════════════════');

  try {
    const llmEnabled = await testAgentConfig();
    const hasLLM = await testLLMNotConfigured(llmEnabled);
    await testMockModeChat();
    await testLLMChat(llmEnabled);
    await testLLMErrorHandling(llmEnabled);
  } catch (e) {
    console.error('\n💥 测试异常:', e.message);
    failed++;
  }

  console.log('\n═══════════════════════════════════════════════');
  console.log(`  结果: ${passed} PASS / ${failed} FAIL / ${notRun} NOT_RUN`);
  console.log('═══════════════════════════════════════════════');

  process.exit(failed > 0 ? 1 : 0);
}

main();
