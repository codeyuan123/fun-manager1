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
    <div class="login-grid" />
    <div class="login-panel">
      <section class="login-copy">
        <span class="eyebrow">基金终端</span>
        <h1>接入真实基金行情、净值走势和持仓结构的专业基金工作台。</h1>
        <p>
          登录后即可查看真实估值、净值曲线、季度持仓、基金经理与资产配置，并完成自选和交易记录管理。
        </p>
        <div class="login-stats">
          <div>
            <strong>实时</strong>
            <span>估值与净值同步</span>
          </div>
          <div>
            <strong>深度</strong>
            <span>经理、配置、持仓一体化</span>
          </div>
          <div>
            <strong>稳定</strong>
            <span>桌面与移动端均可用</span>
          </div>
        </div>
      </section>

      <section class="login-form-shell">
        <div class="login-form-head">
          <span class="panel-kicker">登录入口</span>
          <h2>进入基金终端</h2>
          <p>使用默认账号进入系统。</p>
        </div>

        <el-form @submit.prevent class="login-form">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="admin123" />
          </el-form-item>
        </el-form>

        <el-button type="primary" class="login-button" :loading="loading" @click="submit">进入系统</el-button>
        <div class="login-footnote">默认账号：<strong>admin / admin123</strong></div>
      </section>
    </div>
  </div>
</template>
