import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '../types/api'

const SESSION_TIMEOUT_MS = 30 * 60 * 1000
const SESSION_WARNING_MS = 5 * 60 * 1000
const ACTIVITY_THROTTLE_MS = 10 * 1000

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('fm_token') || '')
  const user = ref<UserInfo | null>(null)
  const lastActivityAt = ref<number>(Number(localStorage.getItem('fm_last_activity_at') || 0))
  const sessionWarningVisible = ref(false)
  const sessionRemainingSeconds = ref(0)

  let timer: number | null = null

  const setToken = (nextToken: string) => {
    token.value = nextToken
    localStorage.setItem('fm_token', nextToken)
    markActivity(true)
  }

  const setUser = (nextUser: UserInfo | null) => {
    user.value = nextUser
  }

  const clearSessionMeta = () => {
    lastActivityAt.value = 0
    sessionWarningVisible.value = false
    sessionRemainingSeconds.value = 0
    localStorage.removeItem('fm_last_activity_at')
  }

  const stopSessionMonitor = () => {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  const logout = () => {
    stopSessionMonitor()
    token.value = ''
    user.value = null
    clearSessionMeta()
    localStorage.removeItem('fm_token')
  }

  const markActivity = (force = false) => {
    if (!token.value) return
    const now = Date.now()
    if (!force && lastActivityAt.value && now - lastActivityAt.value < ACTIVITY_THROTTLE_MS) {
      return
    }
    lastActivityAt.value = now
    localStorage.setItem('fm_last_activity_at', String(now))
    sessionWarningVisible.value = false
  }

  const ensureSession = () => {
    if (!token.value) return false
    const stored = Number(localStorage.getItem('fm_last_activity_at') || 0)
    if (!stored) {
      markActivity(true)
      return true
    }
    if (Date.now() - stored >= SESSION_TIMEOUT_MS) {
      logout()
      return false
    }
    lastActivityAt.value = stored
    return true
  }

  const startSessionMonitor = (onTimeout: () => void) => {
    stopSessionMonitor()
    if (!ensureSession()) return

    const tick = () => {
      if (!token.value) return
      const stored = Number(localStorage.getItem('fm_last_activity_at') || lastActivityAt.value || 0)
      if (!stored) {
        markActivity(true)
        return
      }
      lastActivityAt.value = stored
      const remainingMs = SESSION_TIMEOUT_MS - (Date.now() - stored)
      if (remainingMs <= 0) {
        logout()
        onTimeout()
        return
      }
      sessionRemainingSeconds.value = Math.ceil(remainingMs / 1000)
      sessionWarningVisible.value = remainingMs <= SESSION_WARNING_MS
    }

    tick()
    timer = window.setInterval(tick, 1000)
  }

  const continueSession = () => {
    markActivity(true)
  }

  return {
    token,
    user,
    lastActivityAt,
    sessionWarningVisible,
    sessionRemainingSeconds,
    setToken,
    setUser,
    logout,
    markActivity,
    ensureSession,
    startSessionMonitor,
    stopSessionMonitor,
    continueSession,
  }
})
