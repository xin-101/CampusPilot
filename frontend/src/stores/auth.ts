import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<any>(null)

  const isAuthenticated = computed(() => !!token.value)
  const username = computed(() => user.value?.username || '')
  const role = computed(() => user.value?.role || '')

  async function login(username: string, password: string) {
    const response = await loginApi(username, password)
    token.value = response.data.token
    localStorage.setItem('token', response.data.token)
    await fetchUser()
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      const response = await getCurrentUser()
      user.value = response.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      logout()
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    user,
    isAuthenticated,
    username,
    role,
    login,
    fetchUser,
    logout
  }
})