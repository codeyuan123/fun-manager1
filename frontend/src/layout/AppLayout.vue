<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const onCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="app-shell">
    <aside class="left-nav">
      <div class="brand">
        <div class="brand-kicker">FUND OPS</div>
        <h1>基金管理台</h1>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="nav-menu"
        router
        :ellipsis="false"
      >
        <el-menu-item index="/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/positions">持仓管理</el-menu-item>
        <el-menu-item index="/watchlist">自选基金</el-menu-item>
      </el-menu>
    </aside>
    <section class="main-pane">
      <header class="top-bar">
        <div class="top-title">{{ route.meta.title || '基金管理项目' }}</div>
        <el-dropdown @command="onCommand">
          <span class="user-text">
            {{ authStore.user?.nickname || authStore.user?.username || 'User' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="content-pane">
        <router-view />
      </main>
    </section>
  </div>
</template>
