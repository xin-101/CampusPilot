import api from './auth'

export interface Task {
  id: number
  title: string
  description: string
  status: string
  priority: string
  deadline: string
  createdAt: string
  updatedAt: string
}

export function getTasks() {
  return api.get('/tasks')
}

export function getTaskById(id: number) {
  return api.get(`/tasks/${id}`)
}

export function createTask(task: { title: string; description?: string; deadline?: string }) {
  return api.post('/tasks', task)
}

export function completeTask(id: number) {
  return api.put(`/tasks/${id}/complete`)
}

export function cancelTask(id: number) {
  return api.put(`/tasks/${id}/cancel`)
}