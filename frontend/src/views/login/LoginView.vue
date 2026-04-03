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
    await router.replace('/dashboard/workbench')
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
      <section class="login-copy">
        <span class="eyebrow">Vue Vben Admin</span>
        <h1>基金管理后台</h1>
        <p>统一查看持仓、自选、基金详情和收益趋势。</p>
      </section>

      <section class="login-form-shell">
        <div class="login-form-head">
          <span class="panel-kicker">Login</span>
          <h2>账号登录</h2>
          <p>使用默认账号即可进入。</p>
        </div>

        <el-form @submit.prevent class="login-form">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="admin123" />
          </el-form-item>
        </el-form>

        <el-button type="primary" class="login-button" :loading="loading" @click="submit">进入系统</el-button>
        <div class="login-footnote">默认账号：admin / admin123</div>
      </section>
    </div>
  </div>
</template>
