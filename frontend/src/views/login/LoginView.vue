<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi, meApi } from '../../api/modules'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const submit = async () => {
  loading.value = true
  try {
    const loginResp = await loginApi({ ...form })
    authStore.setToken(loginResp.data.data.token)
    const meResp = await meApi()
    authStore.setUser(meResp.data.data)
    ElMessage.success('登录成功')
    await router.replace('/dashboard')
  } catch (error: any) {
    ElMessage.error(error?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <section class="login-hero">
        <div class="login-kicker">FUND MANAGER SYSTEM</div>
        <h1 class="login-headline">基金管理后台</h1>
        <p class="login-desc">
          统一管理基金持仓、自选与收益看板，支持实时估值推测、交易流水与收益趋势分析。
        </p>
      </section>

      <section class="login-form-wrap">
        <h2 class="login-title">账号登录</h2>
        <p class="login-sub">使用系统账号进入管理台</p>
        <el-form @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="loading" class="login-button" @click="submit">
          登录系统
        </el-button>
        <div class="login-hint">默认账号：admin / admin123</div>
      </section>
    </div>
  </div>
</template>
