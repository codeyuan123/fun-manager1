<script setup lang="ts">
defineProps<{
  modelValue: boolean
  remainingSeconds: number
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  continue: []
  logout: []
}>()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="登录即将超时"
    width="420px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <p>检测到长时间无操作，{{ remainingSeconds }} 秒后将自动退出登录。</p>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('logout')">退出登录</el-button>
        <el-button type="primary" @click="$emit('continue')">继续会话</el-button>
      </div>
    </template>
  </el-dialog>
</template>
