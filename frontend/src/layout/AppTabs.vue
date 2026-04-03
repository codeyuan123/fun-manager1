<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const activeTab = computed(() => route.path)
const tabs = computed(() => appStore.tabs)

const onTabClick = (path: string | number) => {
  router.push(String(path))
}

const onTabRemove = (path: string | number) => {
  const nextPath = String(path)
  const currentIndex = tabs.value.findIndex((item) => item.path === nextPath)
  appStore.removeTab(nextPath)
  if (route.path !== nextPath) return

  const fallback = tabs.value[currentIndex - 1] || tabs.value[currentIndex + 1] || tabs.value[0]
  router.push(fallback?.path || '/dashboard/workbench')
}
</script>

<template>
  <div class="app-tabbar" v-if="appStore.tabbarEnabled">
    <el-tabs
      :model-value="activeTab"
      type="card"
      class="app-tabbar-tabs"
      @tab-click="(pane: { paneName?: string | number }) => onTabClick(pane.paneName || '')"
      @tab-remove="onTabRemove"
    >
      <el-tab-pane
        v-for="item in tabs"
        :key="item.path"
        :label="item.title"
        :name="item.path"
        :closable="item.closable"
      />
    </el-tabs>
  </div>
</template>
