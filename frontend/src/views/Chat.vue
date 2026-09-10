<template>
  <div class="chat-container">
    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-title">
          <h3>CampusPilot</h3>
          <span class="chat-subtitle">高校校园学生事务智能体</span>
        </div>
        <div class="chat-header-tags">
          <el-tag type="warning" size="small" effect="plain">DEMO DATA</el-tag>
          <el-tag type="info" size="small">RAG · RuleEngine · Workflow · Trace</el-tag>
        </div>
      </div>

      <div class="messages-container" ref="messagesContainer">
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message', message.role]"
        >
          <div class="message-avatar">
            <el-icon v-if="message.role === 'user'" :size="20"><User /></el-icon>
            <el-icon v-else :size="20"><Monitor /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-meta" v-if="message.role === 'assistant' && message.intent">
              <el-tag size="small" type="primary" effect="plain">{{ intentLabel(message.intent) }}</el-tag>
              <el-tag v-if="message.complexity" size="small" type="info" effect="plain">{{ message.complexity }}</el-tag>
            </div>

            <div class="message-text" v-html="formatMessage(message.content)"></div>

            <!-- 来源引用卡片 -->
            <el-card
              v-if="message.sources && message.sources.length > 0"
              class="sources-card"
              shadow="never"
            >
              <template #header>
                <div class="sources-card-header">
                  <span>回答依据</span>
                  <span class="sources-count">{{ message.sources.length }} 个来源</span>
                </div>
              </template>
              <div v-for="source in message.sources" :key="source.policyId" class="source-item">
                <div class="source-line">
                  <el-icon color="#409eff"><Document /></el-icon>
                  <span class="source-name">{{ source.policyName }}</span>
                  <el-tag size="small" type="info">版本 {{ source.version || 'v1' }}</el-tag>
                  <el-tag size="small" :type="isExpired(source) ? 'danger' : 'success'">
                    {{ isExpired(source) ? '已过期' : '有效' }}
                  </el-tag>
                </div>
                <div class="source-sub">
                  <span>来源：{{ source.source || '学生资助相关政策' }}</span>
                  <span v-if="source.effectiveDate && source.expiryDate">
                    有效期：{{ source.effectiveDate }} ~ {{ source.expiryDate }}
                  </span>
                  <span v-if="source.relevance">相关度：{{ (source.relevance * 100).toFixed(0) }}%</span>
                  <span v-else>来源链接：暂无（不伪造 URL）</span>
                </div>
              </div>
            </el-card>

            <!-- 下一步行动 -->
            <div v-if="message.actions && message.actions.length > 0" class="actions-card">
              <div class="actions-title">建议下一步</div>
              <div class="actions-row">
                <el-button
                  v-for="action in message.actions"
                  :key="action.type"
                  type="primary"
                  size="small"
                  round
                  :plain="action.type !== 'CREATE_TASK'"
                  @click="handleAction(action)"
                >
                  <el-icon v-if="action.type === 'CREATE_TASK'" style="margin-right:4px"><Plus /></el-icon>
                  {{ action.label }}
                </el-button>
              </div>
            </div>

            <!-- 执行轨迹 -->
            <el-collapse v-if="message.trace" class="trace-collapse">
              <el-collapse-item name="trace">
                <template #title>
                  <div class="trace-summary">
                    <el-icon class="trace-icon"><Odometer /></el-icon>
                    <span class="trace-title">Agent 执行过程</span>
                    <el-tag v-if="message.trace.workflowName" size="small" type="primary">
                      {{ message.trace.workflowName }}
                    </el-tag>
                    <el-tag v-else size="small" type="danger">{{ message.trace.workflowId || message.trace.intent }}</el-tag>
                    <el-tag size="small" :type="traceStatusType(message.trace.status)">{{ message.trace.status }}</el-tag>
                    <span class="trace-meta">
                      {{ message.trace.retrieval?.provider || '—' }} ·
                      {{ message.trace.retrieval?.documents?.length || 0 }} hits ·
                      {{ message.trace.duration || 0 }}ms
                    </span>
                  </div>
                </template>

                <!-- 步骤总览 -->
                <template v-if="tracePhases(message.executionSteps || []).length">
                  <el-steps :active="stepIndex(message.executionSteps || [])" align-center class="trace-steps">
                    <el-step
                      v-for="phase in tracePhases(message.executionSteps || [])"
                      :key="phase"
                      :title="phase"
                      :status="phaseStatus(phase, message.executionSteps || [])"
                    />
                  </el-steps>
                </template>

                <!-- 决策信息 -->
                <el-card v-if="message.trace.decisions && Object.keys(message.trace.decisions).length" shadow="never" class="decision-card">
                  <div class="trace-section-title">决策信息</div>
                  <div class="decision-grid">
                    <div v-for="(v, k) in message.trace.decisions" :key="k" class="decision-cell">
                      <span class="decision-key">{{ k }}</span>
                      <el-tag
                        v-if="typeof v === 'boolean'"
                        size="small"
                        :type="v ? 'success' : 'danger'"
                      >{{ v ? 'true' : 'false' }}</el-tag>
                      <span v-else class="decision-value">{{ v }}</span>
                    </div>
                  </div>
                  <el-button size="small" text type="primary" @click="traceDecisionsJson[message.id] = !traceDecisionsJson[message.id]">
                    {{ traceDecisionsJson[message.id] ? '收起 JSON' : '查看 JSON' }}
                  </el-button>
                  <pre v-if="traceDecisionsJson[message.id]" class="trace-json">{{ jsonPretty(message.trace.decisions) }}</pre>
                </el-card>

                <div class="trace-section-title">执行步骤</div>
                <el-timeline class="trace-timeline">
                  <el-timeline-item
                    v-for="step in message.executionSteps || []"
                    :key="step.step"
                    :type="stepTimelineType(step)"
                    :hollow="['ROUTER','INTENT','RESPONSE','DECISION'].includes(step.type)"
                  >
                    <div class="step-header">
                      <el-tag size="small" :type="stepTagType(step)" effect="plain">{{ step.type }}</el-tag>
                      <span class="step-name">{{ step.name }}</span>
                      <el-tag size="small" :type="stepStatusType(step.status)">{{ step.status }}</el-tag>
                      <span class="step-duration">{{ step.duration }}ms</span>
                    </div>
                    <div v-if="step.error" class="step-error">{{ step.error }}</div>
                    <el-collapse v-if="step.input !== undefined || step.output !== undefined" class="step-details-collapse">
                      <el-collapse-item name="io">
                        <template #title><span class="step-details-title">输入 / 输出</span></template>
                        <div v-if="step.input !== undefined" class="step-io">
                          <div class="trace-section-title">输入</div>
                          <pre class="trace-json">{{ jsonPretty(step.input) }}</pre>
                        </div>
                        <div v-if="step.output !== undefined" class="step-io">
                          <div class="trace-section-title">输出</div>
                          <pre class="trace-json">{{ jsonPretty(step.output) }}</pre>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </el-timeline-item>
                </el-timeline>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="message-avatar">
            <el-icon :size="20"><Monitor /></el-icon>
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <div class="thinking-text">正在分析你的问题…（意图识别 → 政策检索 → 工具调用 → 规则判断）</div>
          </div>
        </div>
      </div>

      <div class="input-container">
        <el-input
          v-model="inputMessage"
          placeholder="输入你的问题，例如：我能不能申请国家奖学金？"
          @keyup.enter="sendMessage"
          :disabled="loading"
          size="large"
        >
          <template #append>
            <el-button
              type="primary"
              @click="sendMessage"
              :loading="loading"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div class="chat-sidebar">
      <div class="sidebar-section">
        <h4>快捷操作</h4>
        <el-button
          v-for="action in quickActions"
          :key="action.text"
          @click="quickAction(action.text)"
          class="quick-action-btn"
        >
          {{ action.icon }} {{ action.text }}
        </el-button>
      </div>

      <div class="sidebar-section">
        <h4>最近任务</h4>
        <div v-if="recentTasks.length === 0" class="empty-tasks">
          暂无任务
        </div>
        <div v-else class="task-list">
          <div v-for="task in recentTasks" :key="task.id" class="task-item">
            <el-tag :type="task.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
              {{ task.status === 'COMPLETED' ? '已完成' : '进行中' }}
            </el-tag>
            <span class="task-title">{{ task.title }}</span>
          </div>
        </div>
        <el-button v-if="recentTasks.length" size="small" text type="primary" @click="router.push('/tasks')">
          查看全部任务 →
        </el-button>
      </div>

      <div class="sidebar-section sidebar-note">
        <h4>演示说明</h4>
        <p class="note-text">资格判断与政策数据均为 DEMO 演示数据（规则引擎确定性计算），不代表真实校规审核结果。FastGPT 接入状态：WAITING_FOR_FASTGPT_ENVIRONMENT。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { sendMessage as sendAgentMessage, type AgentMessage, type AgentResponse, type AgentAction } from '../api/agent'
import { getTasks, type Task } from '../api/task'
import { ElMessage } from 'element-plus'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: any[]
  executionSteps?: any[]
  trace?: any
  actions?: AgentAction[]
  intent?: string
  complexity?: string
}

const router = useRouter()
const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref()
const recentTasks = ref<Task[]>([])
const traceDecisionsJson = reactive<Record<string, boolean>>({})

const quickActions = [
  { icon: '🎓', text: '我能不能申请国家奖学金？' },
  { icon: '📝', text: '那帮我申请国家奖学金' },
  { icon: '📚', text: '国家奖学金什么时候申请？' },
  { icon: '📊', text: '查看我的成绩' }
]

onMounted(async () => {
  messages.value.push({
    id: 'welcome',
    role: 'assistant',
    content: '你好！我是 CampusPilot 校园事务智能助手。\n\n' +
      '我可以帮你完成从「问得到」到「办得到」的全流程：\n' +
      '- 📚 查询校园政策（带来源引用与有效期）\n' +
      '- 🎓 判断申请资格（规则引擎确定性计算）\n' +
      '- 📝 办理申请任务（真实写入数据库并通知）\n' +
      '- 🔒 守护你的数据安全（越权/注入拦截）\n\n' +
      '试试对我说：「我能不能申请国家奖学金？」'
  })

  try {
    const response = await getTasks()
    recentTasks.value = response.data.slice(0, 5)
  } catch (error) {
    console.error('加载任务失败:', error)
    ElMessage.info('最近任务加载失败，稍后可刷新查看')
  }
})

async function sendMessage() {
  if (!inputMessage.value.trim() || loading.value) return

  const userMessage: Message = {
    id: Date.now().toString(),
    role: 'user',
    content: inputMessage.value
  }

  messages.value.push(userMessage)
  inputMessage.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const request: AgentMessage = {
      message: userMessage.content
    }

    const response = await sendAgentMessage(request) as any
    const agentResponse: AgentResponse = response.data

    const assistantMessage: Message = {
      id: agentResponse.messageId,
      role: 'assistant',
      content: agentResponse.response,
      sources: agentResponse.sources,
      executionSteps: agentResponse.executionSteps || agentResponse.executionTrace?.steps || [],
      trace: agentResponse.executionTrace,
      actions: agentResponse.actions,
      intent: agentResponse.intent,
      complexity: agentResponse.complexity
    }

    messages.value.push(assistantMessage)
  } catch (error: any) {
    ElMessage.error('发送消息失败，请稍后重试。')
    console.error('Agent请求异常:', error)
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function quickAction(text: string) {
  inputMessage.value = text
  sendMessage()
}

function handleAction(action: AgentAction) {
  if (action.type === 'CREATE_TASK') {
    const policyName = action.params?.policyName
    inputMessage.value = policyName ? `帮我申请${policyName}` : '帮我申请奖学金'
    sendMessage()
  } else if (action.type === 'VIEW_POLICY') {
    const policyId = action.params?.policyId
    router.push({ path: '/policies', query: policyId ? { policyId: String(policyId) } : {} })
  } else if (action.type === 'VIEW_TASKS') {
    router.push('/tasks')
  }
}

function formatMessage(content: string): string {
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/^(\d+)\. /gm, '<span class="list-num">$1.</span> ')
}

function isExpired(source: any): boolean {
  return !!(source.expiryDate && new Date(source.expiryDate) < new Date())
}

function intentLabel(intent: string): string {
  const map: Record<string, string> = {
    POLICY_QUERY: '政策咨询',
    ELIGIBILITY_CHECK: '资格判断',
    TASK_CREATE: '事务办理',
    SCORE_QUERY: '成绩查询',
    GENERAL_QUERY: '通用咨询',
    DENIED: '已拦截'
  }
  return map[intent] || intent
}

function traceStatusType(status: string): any {
  return status === 'SUCCESS' ? 'success' : status === 'DENIED' || status === 'FAILED' ? 'danger' : 'warning'
}

function stepIndex(steps: any[]): number {
  return steps ? steps.length : 0
}

function tracePhases(steps: any[]): string[] {
  if (!steps || !steps.length) return []
  const phases: string[] = []
  const typeToPhase: Record<string, string> = {
    ROUTER: '意图路由',
    INTENT: '意图识别',
    RETRIEVAL: '知识检索',
    TOOL: '工具调用',
    DECISION: '规则判断',
    NOTIFY: '发送通知',
    RESPONSE: '生成结果'
  }
  for (const s of steps) {
    const p = typeToPhase[s.type] || s.type
    if (!phases.includes(p)) phases.push(p)
  }
  return phases.slice(0, 8)
}

function phaseStatus(phase: string, steps: any[]): any {
  const typeToPhase: Record<string, string> = {
    ROUTER: '意图路由',
    INTENT: '意图识别',
    RETRIEVAL: '知识检索',
    TOOL: '工具调用',
    DECISION: '规则判断',
    NOTIFY: '发送通知',
    RESPONSE: '生成结果'
  }
  const failed = steps.some(s => s.status === 'FAILED')
  const stepForPhase = steps.find(s => (typeToPhase[s.type] || s.type) === phase)
  if (failed) return 'error'
  if (stepForPhase && stepForPhase.status !== 'SUCCESS' && stepForPhase.type !== 'TOOL') return 'process'
  if (stepForPhase) return 'success'
  return phase === steps[steps.length - 1] ? 'process' : 'wait'
}

function stepTimelineType(step: any): any {
  if (step.status === 'FAILED' || step.status === 'DENIED') return 'danger'
  if (step.type === 'TOOL' || step.type === 'RETRIEVAL' || step.type === 'NOTIFY') return 'primary'
  return 'success'
}

function stepTagType(step: any): any {
  if (step.type === 'TOOL') return 'primary'
  if (step.type === 'RETRIEVAL') return 'primary'
  if (step.type === 'DECISION') return 'success'
  if (step.type === 'NOTIFY') return 'warning'
  return 'info'
}

function stepStatusType(status: string): any {
  return status === 'SUCCESS' ? 'success' : status === 'FAILED' || status === 'DENIED' ? 'danger' : 'warning'
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function jsonPretty(value: any): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch (e) {
    return String(value)
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f7fa;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  margin: 10px;
  overflow: hidden;
}

.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.chat-title h3 {
  margin: 0;
}

.chat-subtitle {
  color: #909399;
  font-size: 12px;
}

.chat-header-tags {
  display: flex;
  gap: 8px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: #67c23a;
}

.message-content {
  max-width: 72%;
  margin: 0 10px;
}

.message.user .message-content {
  text-align: right;
}

.message-meta {
  margin-bottom: 6px;
  display: flex;
  gap: 6px;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f4f4f5;
  line-height: 1.6;
  word-break: break-word;
}

.message.user .message-text {
  background: #409eff;
  color: white;
  text-align: left;
}

.sources-card {
  margin-top: 10px;
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
}

.sources-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #303133;
}

.sources-count {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.source-item {
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
}

.source-item:last-child {
  border-bottom: none;
}

.source-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.source-name {
  font-weight: bold;
  color: #303133;
}

.source-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.actions-card {
  margin-top: 10px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  padding: 10px 12px;
}

.actions-title {
  font-size: 12px;
  color: #409eff;
  font-weight: bold;
  margin-bottom: 8px;
}

.actions-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.trace-collapse {
  margin-top: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.trace-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.trace-icon {
  color: #409eff;
}

.trace-title {
  font-weight: bold;
  color: #303133;
}

.trace-meta {
  font-size: 12px;
  color: #909399;
}

.trace-steps {
  margin: 12px 0;
}

.trace-section-title {
  font-size: 12px;
  color: #909399;
  font-weight: bold;
  margin: 10px 0 6px;
}

.decision-card {
  margin-bottom: 8px;
  background: #fff;
}

.decision-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
  margin-bottom: 4px;
}

.decision-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8f9fa;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 13px;
}

.decision-key {
  color: #909399;
  font-size: 12px;
}

.decision-value {
  color: #303133;
}

.trace-json {
  margin: 0;
  font-size: 12px;
  color: #606266;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 6px 10px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.step-name {
  font-weight: bold;
}

.step-duration {
  color: #909399;
  font-size: 12px;
}

.step-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.step-details-collapse {
  margin-top: 6px;
}

.step-details-title {
  font-size: 12px;
  color: #606266;
}

.step-io {
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f4f4f5;
  border-radius: 12px;
  width: fit-content;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.thinking-text {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.input-container {
  padding: 15px 20px;
  border-top: 1px solid #eee;
}

.chat-sidebar {
  width: 300px;
  background: white;
  border-radius: 8px;
  margin: 10px 10px 10px 0;
  padding: 20px;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 30px;
}

.sidebar-section h4 {
  margin: 0 0 15px;
  color: #333;
}

.quick-action-btn {
  width: 100%;
  margin-bottom: 10px;
  text-align: left;
  justify-content: flex-start;
}

.empty-tasks {
  color: #999;
  text-align: center;
  padding: 20px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-note .note-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>