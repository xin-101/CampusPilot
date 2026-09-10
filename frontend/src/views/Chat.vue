<template>
  <div class="chat-container">
    <div class="chat-main">
      <div class="chat-header">
        <h3>智能助手</h3>
        <el-tag type="info" size="small">DEMO MODE</el-tag>
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
            <div class="message-text" v-html="formatMessage(message.content)"></div>
            <div v-if="message.sources && message.sources.length > 0" class="message-sources">
              <el-divider content-position="left">引用来源</el-divider>
              <div v-for="source in message.sources" :key="source.policyId" class="source-item">
                <el-tag size="small" type="info">{{ source.version }}</el-tag>
                <span class="source-name">{{ source.policyName }}</span>
                <span class="source-relevance">相关度: {{ (source.relevance * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <details v-if="message.trace || (message.executionSteps && message.executionSteps.length > 0)" class="execution-trace">
              <summary class="execution-trace-summary">
                <span>执行轨迹</span>
                <span v-if="message.trace && message.trace.workflowName" class="trace-workflow">
                  {{ message.trace.workflowName }}
                </span>
                <span v-if="message.trace && message.trace.retrieval" class="trace-meta">
                  {{ message.trace.retrieval.provider }} · {{ message.trace.retrieval.documents?.length || 0 }} hits
                </span>
                <span v-else class="trace-meta">fallback</span>
              </summary>
              <div class="trace-body">
                <div v-if="message.trace && message.trace.decisions && Object.keys(message.trace.decisions).length" class="trace-decisions">
                  <div class="trace-section-title">决策信息</div>
                  <pre class="trace-json">{{ jsonPretty(message.trace.decisions) }}</pre>
                </div>
                <el-timeline>
                  <el-timeline-item
                    v-for="step in message.executionSteps"
                    :key="step.step"
                    :type="step.type === 'TOOL' || step.type === 'RETRIEVAL' ? 'primary' : step.status === 'FAILED' ? 'danger' : 'success'"
                    :hollow="step.type !== 'TOOL' && step.type !== 'RETRIEVAL'"
                  >
                    <div class="step-header">
                      <span class="step-type">{{ step.type }}</span>
                      <span class="step-name">{{ step.name }}</span>
                      <el-tag size="small" :type="step.status === 'SUCCESS' ? 'success' : step.status === 'FAILED' ? 'danger' : 'warning'">
                        {{ step.status }}
                      </el-tag>
                      <span class="step-duration">{{ step.duration }}ms</span>
                    </div>
                    <div v-if="step.error" class="step-error">{{ step.error }}</div>
                    <details v-if="step.input !== undefined || step.output !== undefined" class="step-details">
                      <summary>输入/输出</summary>
                      <div v-if="step.input !== undefined" class="step-io">
                        <span class="trace-section-title">输入</span>
                        <pre class="trace-json">{{ jsonPretty(step.input) }}</pre>
                      </div>
                      <div v-if="step.output !== undefined" class="step-io">
                        <span class="trace-section-title">输出</span>
                        <pre class="trace-json">{{ jsonPretty(step.output) }}</pre>
                      </div>
                    </details>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </details>
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
          </div>
        </div>
      </div>
      
      <div class="input-container">
        <el-input
          v-model="inputMessage"
          placeholder="输入你的问题..."
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { sendMessage as sendAgentMessage, type AgentMessage, type AgentResponse } from '../api/agent'
import { getTasks, type Task } from '../api/task'
import { ElMessage } from 'element-plus'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: any[]
  executionSteps?: any[]
  trace?: any
}

const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref()
const recentTasks = ref<Task[]>([])

const quickActions = [
  { icon: '🎓', text: '帮我判断是否符合国家奖学金申请条件' },
  { icon: '📝', text: '查询请假政策' },
  { icon: '📊', text: '查看我的成绩' },
  { icon: '🏠', text: '宿舍报修流程' }
]

onMounted(async () => {
  // 初始欢迎消息
  messages.value.push({
    id: 'welcome',
    role: 'assistant',
    content: '你好！我是CampusPilot校园事务智能助手。\n\n我可以帮你：\n- 📚 查询校园政策\n- 🎓 判断申请资格\n- 📝 办理校园事务\n- ⏰ 设置提醒通知\n\n请问有什么可以帮你的？'
  })
  
  // 加载最近任务
  try {
    const response = await getTasks()
    recentTasks.value = response.data.slice(0, 5)
  } catch (error) {
    console.error('加载任务失败:', error)
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
      executionSteps: agentResponse.executionSteps,
      trace: agentResponse.executionTrace
    }
    
    messages.value.push(assistantMessage)
  } catch (error: any) {
    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'))
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

function formatMessage(content: string): string {
  // 简单的Markdown格式化
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
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
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-header h3 {
  margin: 0;
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
  max-width: 70%;
  margin: 0 10px;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f4f4f5;
  line-height: 1.6;
}

.message.user .message-text {
  background: #409eff;
  color: white;
}

.message-sources {
  margin-top: 10px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 5px 0;
}

.source-name {
  flex: 1;
  font-size: 14px;
}

.source-relevance {
  color: #666;
  font-size: 12px;
}

.execution-trace {
  margin-top: 10px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
}

.execution-trace-summary {
  cursor: pointer;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
  list-style: none;
  user-select: none;
}

.execution-trace-summary::-webkit-details-marker,
.execution-trace-summary::marker {
  display: none;
  content: '';
}

.execution-trace-summary::before {
  content: '▶';
  font-size: 12px;
  color: #909399;
}

.execution-trace[open] .execution-trace-summary::before {
  content: '▼';
}

.trace-workflow {
  font-weight: normal;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 1px 8px;
  border-radius: 10px;
}

.trace-meta {
  font-weight: normal;
  font-size: 12px;
  color: #909399;
}

.trace-body {
  margin-top: 10px;
}

.trace-section-title {
  font-size: 12px;
  color: #909399;
  font-weight: bold;
  margin: 6px 0 4px;
}

.trace-decisions {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 10px;
}

.trace-json {
  margin: 0;
  font-size: 12px;
  color: #606266;
  background: #fff;
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
  gap: 10px;
}

.step-name {
  font-weight: bold;
}

.step-type {
  font-size: 11px;
  color: #409eff;
  background: #ecf5ff;
  padding: 0 6px;
  border-radius: 4px;
}

.step-duration {
  color: #666;
  font-size: 12px;
}

.step-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.step-details {
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  margin-top: 4px;
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
</style>