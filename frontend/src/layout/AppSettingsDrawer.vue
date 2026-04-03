<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '../stores/app'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const appStore = useAppStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})
</script>

<template>
  <el-drawer v-model="visible" title="界面设置" size="320px">
    <div class="settings-block">
      <div class="settings-item">
        <span>主题</span>
        <el-segmented
          :model-value="appStore.theme"
          :options="[
            { label: '浅色', value: 'light' },
            { label: '深色', value: 'dark' },
          ]"
          @change="(value: string | number | boolean) => appStore.setTheme(value as 'light' | 'dark')"
        />
      </div>
      <div class="settings-item">
        <span>内容宽度</span>
        <el-segmented
          :model-value="appStore.contentMode"
          :options="[
            { label: '自适应', value: 'fluid' },
            { label: '定宽', value: 'fixed' },
          ]"
          @change="(value: string | number | boolean) => appStore.setContentMode(value as 'fluid' | 'fixed')"
        />
      </div>
      <div class="settings-item">
        <span>标签页</span>
        <el-switch
          :model-value="appStore.tabbarEnabled"
          @change="(value: string | number | boolean) => appStore.toggleTabbar(Boolean(value))"
        />
      </div>
      <div class="settings-item">
        <span>侧栏折叠</span>
        <el-switch :model-value="appStore.sidebarCollapsed" @change="() => appStore.toggleSidebar()" />
      </div>
    </div>
  </el-drawer>
</template>
