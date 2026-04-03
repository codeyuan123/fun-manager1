<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CollectionTag,
  Fold,
  House,
  Search,
  Setting,
  Star,
  Sunny,
  Moon,
  Wallet,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useAppStore } from '../stores/app'
import { meApi, searchFundApi } from '../api/modules'
import type { FundSearchItem } from '../types/api'
import AppTabs from './AppTabs.vue'
import AppSettingsDrawer from './AppSettingsDrawer.vue'

interface SearchOption extends FundSearchItem {
  value: string
}

interface MenuItem {
  path: string
  title: string
  icon: string
}

interface MenuGroup {
  key: string
  title: string
  items: MenuItem[]
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const searchInput = ref('')
const isMobile = ref(false)
const mobileNavVisible = ref(false)
const settingsVisible = ref(false)

const iconMap = {
  House,
  Wallet,
  Star,
  CollectionTag,
}

const routeMenus = computed(() => {
  const layoutRoute = router.getRoutes().find((item) => item.path === '/')
  const children = layoutRoute?.children ?? []
  const groups = new Map<string, MenuGroup>()

  for (const item of children) {
    if (!item.meta?.menu || item.meta?.hideInMenu) continue
    const groupKey = String(item.meta.group || 'default')
    const groupTitle = String(item.meta.groupTitle || item.meta.title || '')
    const menuItem: MenuItem = {
      path: item.path.startsWith('/') ? item.path : `/${item.path}`,
      title: String(item.meta.title || item.name || ''),
      icon: String(item.meta.icon || 'CollectionTag'),
    }
    const group = groups.get(groupKey)
    if (group) {
      group.items.push(menuItem)
    } else {
      groups.set(groupKey, { key: groupKey, title: groupTitle, items: [menuItem] })
    }
  }

  return Array.from(groups.values())
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/dashboard')) return '/dashboard/workbench'
  if (path.startsWith('/fund/positions')) return '/fund/positions'
  if (path.startsWith('/fund/watchlist')) return '/fund/watchlist'
  return path
})

const currentTitle = computed(() => String(route.meta.title || '基金管理'))
const breadcrumbList = computed(() => {
  const items = [{ title: '首页', path: '/dashboard/workbench' }]
  if (route.path.startsWith('/dashboard/workbench')) {
    items.push({ title: '工作台', path: route.path })
  } else if (route.path.startsWith('/fund/positions')) {
    items.push({ title: '基金', path: '/fund/positions' }, { title: '持仓', path: route.path })
  } else if (route.path.startsWith('/fund/watchlist')) {
    items.push({ title: '基金', path: '/fund/watchlist' }, { title: '自选', path: route.path })
  } else if (route.path.startsWith('/fund/')) {
    items.push({ title: '基金', path: '/fund/positions' }, { title: '详情', path: route.path })
  }
  return items
})

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
    // Keep global search silent on failure.
  }
}

const handleMenuClick = (path: string) => {
  router.push(path)
  mobileNavVisible.value = false
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 960
  if (!isMobile.value) {
    mobileNavVisible.value = false
  }
}

const onToggleSidebar = () => {
  if (isMobile.value) {
    mobileNavVisible.value = true
    return
  }
  appStore.toggleSidebar()
}

const onCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

watch(
  () => route.fullPath,
  () => {
    if (!route.meta.ignoreTab) {
      appStore.addTab({
        path: route.path,
        title: String(route.meta.title || '页面'),
        closable: !route.meta.affix,
      })
    }
    mobileNavVisible.value = false
  },
  { immediate: true },
)

watch(
  () => appStore.theme,
  (value) => {
    document.documentElement.setAttribute('data-theme', value)
  },
  { immediate: true },
)

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

if (authStore.token && !authStore.user) {
  meApi()
    .then((resp) => authStore.setUser(resp.data.data))
    .catch(() => authStore.logout())
}
</script>

<template>
  <div class="app-layout" :class="[`theme-${appStore.theme}`]">
    <aside class="app-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="app-logo" @click="router.push('/dashboard/workbench')">
        <div class="app-logo-mark">F</div>
        <div v-show="!appStore.sidebarCollapsed" class="app-logo-copy">
          <strong>Fund Admin</strong>
          <span>vue-vben style</span>
        </div>
      </div>

      <div class="app-menu">
        <div v-for="group in routeMenus" :key="group.key" class="app-menu-group">
          <div v-show="!appStore.sidebarCollapsed" class="app-menu-group-title">
            {{ group.title }}
          </div>
          <button
            v-for="item in group.items"
            :key="item.path"
            class="app-menu-item"
            :class="{ active: activeMenu === item.path }"
            @click="handleMenuClick(item.path)"
          >
            <el-icon class="app-menu-icon">
              <component :is="iconMap[item.icon as keyof typeof iconMap] || CollectionTag" />
            </el-icon>
            <span v-show="!appStore.sidebarCollapsed">{{ item.title }}</span>
          </button>
        </div>
      </div>
    </aside>

    <el-drawer v-model="mobileNavVisible" direction="ltr" size="240px" :with-header="false" class="mobile-drawer">
      <div class="mobile-drawer-body">
        <div class="app-logo mobile-logo" @click="handleMenuClick('/dashboard/workbench')">
          <div class="app-logo-mark">F</div>
          <div class="app-logo-copy">
            <strong>Fund Admin</strong>
            <span>vue-vben style</span>
          </div>
        </div>
        <div class="app-menu">
          <div v-for="group in routeMenus" :key="group.key" class="app-menu-group">
            <div class="app-menu-group-title">{{ group.title }}</div>
            <button
              v-for="item in group.items"
              :key="item.path"
              class="app-menu-item"
              :class="{ active: activeMenu === item.path }"
              @click="handleMenuClick(item.path)"
            >
              <el-icon class="app-menu-icon">
                <component :is="iconMap[item.icon as keyof typeof iconMap] || CollectionTag" />
              </el-icon>
              <span>{{ item.title }}</span>
            </button>
          </div>
        </div>
      </div>
    </el-drawer>

    <div class="app-main">
      <header class="app-header">
        <div class="app-header-left">
          <el-button text class="app-header-trigger" @click="onToggleSidebar">
            <el-icon><Fold /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbList" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="app-header-right">
          <el-autocomplete
            v-model="searchInput"
            class="app-global-search"
            :fetch-suggestions="fetchSuggestions"
            placeholder="搜索基金"
            @select="openFund"
            @keyup.enter="quickOpen"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #default="{ item }">
              <div class="app-search-option">
                <strong>{{ item.fundCode }}</strong>
                <span>{{ item.fundName }}</span>
              </div>
            </template>
          </el-autocomplete>

          <el-tooltip :content="appStore.isDark ? '切换浅色' : '切换深色'">
            <el-button text class="app-header-icon" @click="appStore.toggleTheme()">
              <el-icon>
                <component :is="appStore.isDark ? Sunny : Moon" />
              </el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="界面设置">
            <el-button text class="app-header-icon" @click="settingsVisible = true">
              <el-icon><Setting /></el-icon>
            </el-button>
          </el-tooltip>

          <el-dropdown @command="onCommand">
            <div class="app-user">
              <div class="app-user-avatar">{{ (authStore.user?.nickname || authStore.user?.username || 'U').slice(0, 1) }}</div>
              <div class="app-user-copy">
                <strong>{{ authStore.user?.nickname || authStore.user?.username || 'User' }}</strong>
                <span>管理员</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <AppTabs />

      <main class="app-content">
        <div class="app-page" :class="{ fixed: appStore.contentMode === 'fixed' }">
          <div class="app-page-header">
            <div>
              <h1>{{ currentTitle }}</h1>
              <p>{{ currentTitle === '工作台' ? '基金管理概览' : currentTitle }}</p>
            </div>
          </div>
          <router-view />
        </div>
      </main>
    </div>

    <AppSettingsDrawer v-model="settingsVisible" />
  </div>
</template>
