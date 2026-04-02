<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { meApi } from '../api/modules'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)

const menus = [
  { path: '/dashboard', title: '数据看板' },
  { path: '/positions', title: '持仓管理' },
  { path: '/watchlist', title: '自选基金' },
]

const activeMenu = computed(() => route.path)
const breadcrumbList = computed(() =>
  route.matched.filter((it) => typeof it.meta?.title === 'string' && it.path !== '/'),
)

const onCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

if (authStore.token && !authStore.user) {
  meApi()
    .then((resp) => authStore.setUser(resp.data.data))
    .catch(() => authStore.logout())
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar" :class="{ collapse: isCollapse }">
      <div class="sidebar-logo">
        <div class="logo-dot" />
        <span v-show="!isCollapse">Fund Console</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        router
        :collapse="isCollapse"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="isCollapse = !isCollapse">☰</el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbList" :key="item.path">
              {{ item.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="onCommand">
          <span class="header-user">{{ authStore.user?.nickname || authStore.user?.username || 'User' }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <div class="admin-tab">
        <el-tag type="primary" effect="light">{{ route.meta.title || '基金管理项目' }}</el-tag>
      </div>

      <main class="admin-content">
        <router-view />
      </main>
    </section>
  </div>
</template>
