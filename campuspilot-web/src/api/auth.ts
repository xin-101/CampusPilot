import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      ElMessage.warning('会话已过期，请重新登录')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export function login(username: string, password: string) {
  return api.post('/auth/login', { username, password })
}

export function getCurrentUser() {
  return api.get('/students/me')
}

export default api