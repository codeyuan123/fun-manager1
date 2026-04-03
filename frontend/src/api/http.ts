import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../stores/auth'
import type { ApiResponse } from '../types/api'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

let handlingUnauthorized = false

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('fm_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || 'Request failed'))
    }
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event('fm-session-activity'))
    }
    return resp
  },
  async (error) => {
    if (error?.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      if (!handlingUnauthorized) {
        handlingUnauthorized = true
        ElMessage.error('登录已过期，请重新登录')
        if (router.currentRoute.value.path !== '/login') {
          await router.replace('/login')
        }
        window.setTimeout(() => {
          handlingUnauthorized = false
        }, 1500)
      }
    }
    return Promise.reject(error)
  },
)

export default http
