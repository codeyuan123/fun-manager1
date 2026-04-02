import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '../types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('fm_token') || '')
  const user = ref<UserInfo | null>(null)

  const setToken = (nextToken: string) => {
    token.value = nextToken
    localStorage.setItem('fm_token', nextToken)
  }

  const setUser = (nextUser: UserInfo | null) => {
    user.value = nextUser
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('fm_token')
  }

  return {
    token,
    user,
    setToken,
    setUser,
    logout,
  }
})
