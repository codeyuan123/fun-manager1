<script setup lang="ts">
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { changePasswordApi } from '../api/modules'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const reset = () => {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

const submit = async () => {
  if (!form.currentPassword || !form.newPassword || !form.confirmPassword) {
    ElMessage.warning('请完整填写密码信息')
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(form.newPassword)) {
    ElMessage.warning('新密码至少 8 位，且包含字母和数字')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  try {
    await changePasswordApi({ ...form })
    visible.value = false
    reset()
    emit('success')
  } catch (error: any) {
    ElMessage.error(error?.message || '修改密码失败')
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="修改密码" width="460px">
    <el-form label-width="100px" class="trade-form">
      <el-form-item label="当前密码">
        <el-input v-model="form.currentPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">确认修改</el-button>
      </div>
    </template>
  </el-dialog>
</template>
