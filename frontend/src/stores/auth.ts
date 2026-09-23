import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { getToken, setToken } from '@/api/client'
import type { User } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<User | null>(null)
  const ready = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

  function applySession(result: { token: string; user: User }) {
    token.value = result.token
    user.value = result.user
    setToken(result.token)
  }

  async function login(username: string, password: string) {
    const result = await authApi.login(username, password)
    applySession(result)
  }

  async function register(username: string, password: string) {
    const result = await authApi.register(username, password)
    applySession(result)
  }

  /** Restore session from stored JWT via GET /api/auth/me on app refresh. */
  async function restore() {
    if (!token.value) {
      ready.value = true
      return
    }
    try {
      user.value = await authApi.me()
    } catch {
      token.value = null
      user.value = null
      setToken(null)
    } finally {
      ready.value = true
    }
  }

  function logout() {
    token.value = null
    user.value = null
    setToken(null)
    ready.value = true
  }

  return {
    token,
    user,
    ready,
    isAuthenticated,
    login,
    register,
    restore,
    logout,
  }
})
