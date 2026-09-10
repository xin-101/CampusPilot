import api from './auth'

export interface AgentMessage {
  message: string
  sessionId?: string
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
}

export function sendMessage(message: AgentMessage) {
  return api.post('/agent/chat', message)
}