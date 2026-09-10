import api from './auth'

export interface AgentMessage {
  message: string
  sessionId?: string
}

export interface AgentAction {
  type: 'CREATE_TASK' | 'VIEW_POLICY' | 'VIEW_TASKS' | string
  label: string
  params?: any
}

export interface AgentResponse {
  sessionId: string
  messageId: string
  response: string
  intent: string
  complexity: string
  sources: any[]
  executionSteps: any[]
  toolCalls: any[]
  executionTrace?: any
  data?: any
  actions?: AgentAction[]
}

export function sendMessage(message: AgentMessage) {
  return api.post('/agent/chat', message)
}