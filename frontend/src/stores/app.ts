import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

type ThemeMode = 'light' | 'dark'
type ContentMode = 'fluid' | 'fixed'

export interface AppTabItem {
  path: string
  title: string
  closable: boolean
}

const readStorage = <T>(key: string, fallback: T): T => {
  const raw = localStorage.getItem(key)
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

export const useAppStore = defineStore('app', () => {
  const theme = ref<ThemeMode>(readStorage<ThemeMode>('fm_theme', 'light'))
  const sidebarCollapsed = ref<boolean>(readStorage<boolean>('fm_sidebar_collapsed', false))
  const tabbarEnabled = ref<boolean>(readStorage<boolean>('fm_tabbar_enabled', true))
  const contentMode = ref<ContentMode>(readStorage<ContentMode>('fm_content_mode', 'fluid'))
  const tabs = ref<AppTabItem[]>([
    { path: '/dashboard/workbench', title: '工作台', closable: false },
  ])

  const isDark = computed(() => theme.value === 'dark')

  watch(theme, (value) => localStorage.setItem('fm_theme', JSON.stringify(value)), { immediate: true })
  watch(sidebarCollapsed, (value) => localStorage.setItem('fm_sidebar_collapsed', JSON.stringify(value)), { immediate: true })
  watch(tabbarEnabled, (value) => localStorage.setItem('fm_tabbar_enabled', JSON.stringify(value)), { immediate: true })
  watch(contentMode, (value) => localStorage.setItem('fm_content_mode', JSON.stringify(value)), { immediate: true })

  const setTheme = (value: ThemeMode) => {
    theme.value = value
  }

  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
  }

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const toggleTabbar = (value?: boolean) => {
    tabbarEnabled.value = typeof value === 'boolean' ? value : !tabbarEnabled.value
  }

  const setContentMode = (value: ContentMode) => {
    contentMode.value = value
  }

  const addTab = (tab: AppTabItem) => {
    const index = tabs.value.findIndex((item) => item.path === tab.path)
    if (index >= 0) {
      tabs.value[index] = { ...tabs.value[index], title: tab.title }
      return
    }
    tabs.value.push(tab)
  }

  const removeTab = (path: string) => {
    tabs.value = tabs.value.filter((item) => item.path !== path || !item.closable)
  }

  return {
    theme,
    isDark,
    sidebarCollapsed,
    tabbarEnabled,
    contentMode,
    tabs,
    setTheme,
    toggleTheme,
    toggleSidebar,
    toggleTabbar,
    setContentMode,
    addTab,
    removeTab,
  }
})
