<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { meApi, searchFundApi } from '../api/modules'
import type { FundSearchItem } from '../types/api'
import { fundTypeLabel } from '../utils/format'

interface SearchOption extends FundSearchItem {
  value: string
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const searchInput = ref('')
const isCollapse = ref(false)

const menus = [
  { path: '/dashboard', title: '资产看板', caption: '实时收益与仓位脉搏' },
  { path: '/positions', title: '持仓台账', caption: '交易流水与仓位明细' },
  { path: '/watchlist', title: '自选基金', caption: '搜索、观察与跟踪池' },
]

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/dashboard')) return '/dashboard'
  if (path.startsWith('/positions')) return '/positions'
  if (path.startsWith('/watchlist')) return '/watchlist'
  return ''
})

const currentTitle = computed(() => String(route.meta.title || '基金终端'))
const userLabel = computed(() => authStore.user?.nickname || authStore.user?.username || '操作员')

const fetchSuggestions = async (query: string, callback: (items: SearchOption[]) => void) => {
  const keyword = query.trim()
  if (!keyword) {
    callback([])
    return
  }
  try {
    const response = await searchFundApi(keyword)
    callback(
      response.data.data.slice(0, 8).map((item) => ({
        ...item,
        value: `${item.fundCode} ${item.fundName}`,
      })),
    )
  } catch {
    callback([])
  }
}

const openFund = (item: SearchOption | { fundCode: string }) => {
  if (!item.fundCode) return
  searchInput.value = ''
  router.push(`/fund/${item.fundCode}`)
}

const quickOpen = async () => {
  const keyword = searchInput.value.trim()
  if (!keyword) return
  const guess = keyword.split(/\s+/)[0]
  if (/^\d{6}$/.test(guess)) {
    router.push(`/fund/${guess}`)
    searchInput.value = ''
    return
  }
  try {
    const response = await searchFundApi(keyword)
    const first = response.data.data[0]
    if (first) {
      openFund(first)
    }
  } catch {
    // Ignore silent quick search failures in the shell search box.
  }
}

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
  <div class="terminal-shell">
    <aside class="terminal-sidebar" :class="{ collapse: isCollapse }">
      <div class="terminal-brand">
        <div class="brand-mark">
          <span />
          <span />
        </div>
        <div v-show="!isCollapse">
          <strong>基金终端</strong>
          <small>东方财富实时数据</small>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="terminal-menu"
        router
        :collapse="isCollapse"
        background-color="transparent"
        text-color="var(--fm-text-muted)"
        active-text-color="var(--fm-text-main)"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <div class="menu-copy">
            <strong>{{ item.title }}</strong>
            <span v-show="!isCollapse">{{ item.caption }}</span>
          </div>
        </el-menu-item>
      </el-menu>

      <div class="terminal-sidebar-foot" v-show="!isCollapse">
        <span>时区</span>
        <strong>Asia/Shanghai</strong>
      </div>
    </aside>

    <section class="terminal-main">
      <header class="terminal-header">
        <div class="terminal-header-left">
          <el-button text class="collapse-btn" @click="isCollapse = !isCollapse">
            {{ isCollapse ? '展开导航' : '收起导航' }}
          </el-button>
          <div class="terminal-header-copy">
            <span class="eyebrow">实时工作台</span>
            <h1>{{ currentTitle }}</h1>
          </div>
        </div>

        <div class="terminal-header-right">
          <el-autocomplete
            v-model="searchInput"
            class="terminal-search"
            :fetch-suggestions="fetchSuggestions"
            placeholder="输入基金代码或名称快速跳转"
            @select="openFund"
            @keyup.enter="quickOpen"
          >
            <template #default="{ item }">
              <div class="trade-suggestion">
                <strong>{{ item.fundCode }}</strong>
                <span>{{ item.fundName }}</span>
                <em>{{ fundTypeLabel(item.fundType) }}</em>
              </div>
            </template>
          </el-autocomplete>

          <el-dropdown @command="onCommand">
            <div class="operator-chip">
              <span class="operator-label">当前用户</span>
              <strong>{{ userLabel }}</strong>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="terminal-content">
        <router-view />
      </main>
    </section>
  </div>
</template>
