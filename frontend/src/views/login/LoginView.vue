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
    <div class="login-card">
      <div class="login-title">基金管理平台</div>
      <div class="login-subtitle">实时收益推测 / 自选 / 数据看板</div>
      <el-form @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="loading" class="login-button" @click="submit">
        登录
      </el-button>
      <p class="hint">默认账号：admin / admin123</p>
    </div>
  </div>
</template>
