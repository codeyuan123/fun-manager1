<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { buyApi, estimateApi, searchFundApi, sellApi } from '../../api/modules'
import type { FundSearchItem } from '../../types/api'
import { fundTypeLabel } from '../../utils/format'

interface FundOption extends FundSearchItem {
  value: string
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    mode: 'BUY' | 'SELL'
    fundCode?: string
    fundName?: string
    nav?: number | null
  }>(),
  {
    fundCode: '',
    fundName: '',
    nav: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const loading = computed(() => false)

const form = reactive({
  fundCode: '',
  fundName: '',
  amount: 1000,
  shares: undefined as number | undefined,
  fee: 0,
  nav: 1,
  tradeDate: '',
  remark: '',
})

const reset = () => {
  form.fundCode = props.fundCode
  form.fundName = props.fundName
  form.amount = 1000
  form.shares = undefined
  form.fee = 0
  form.nav = props.nav && props.nav > 0 ? props.nav : 1
  form.tradeDate = ''
  form.remark = ''
}

watch(
  () => [props.modelValue, props.fundCode, props.fundName, props.nav] as const,
  ([visible]) => {
    if (visible) {
      reset()
    }
  },
  { immediate: true },
)

const fetchSuggestions = async (query: string, callback: (items: FundOption[]) => void) => {
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

const selectFund = async (option: FundOption) => {
  form.fundCode = option.fundCode
  form.fundName = option.fundName
  await fillEstimate()
}

const fillEstimate = async () => {
  if (!form.fundCode.trim()) return
  try {
    const response = await estimateApi(form.fundCode.trim())
    form.nav = Number(response.data.data.estimateNav || form.nav || 1)
    if (!form.fundName) {
      form.fundName = response.data.data.fundName
    }
  } catch {
    ElMessage.warning('获取估值失败，请手动确认净值')
  }
}

const submit = async () => {
  if (!form.fundCode.trim()) {
    ElMessage.warning('请输入基金代码')
    return
  }
  try {
    const payload = {
      ...form,
      fundCode: form.fundCode.trim(),
      fundName: form.fundName.trim() || undefined,
      tradeDate: form.tradeDate || undefined,
    }
    if (props.mode === 'BUY') {
      await buyApi(payload)
      ElMessage.success('买入记录已保存')
    } else {
      await sellApi(payload)
      ElMessage.success('卖出记录已保存')
    }
    dialogVisible.value = false
    emit('success')
  } catch (error: any) {
    ElMessage.error(error?.message || '交易提交失败')
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="props.mode === 'BUY' ? '新增买入' : '新增卖出'"
    width="560px"
    class="terminal-dialog"
  >
    <el-form label-width="112px" class="trade-form">
      <el-form-item label="基金搜索">
        <el-autocomplete
          v-model="form.fundCode"
          class="trade-search"
          :fetch-suggestions="fetchSuggestions"
          placeholder="输入基金代码或名称"
          @select="selectFund"
          @blur="fillEstimate"
        >
          <template #default="{ item }">
            <div class="trade-suggestion">
              <strong>{{ item.fundCode }}</strong>
              <span>{{ item.fundName }}</span>
              <em>{{ fundTypeLabel(item.fundType) }}</em>
            </div>
          </template>
        </el-autocomplete>
      </el-form-item>
      <el-form-item label="基金名称">
        <el-input v-model="form.fundName" placeholder="估值获取成功后自动带出" />
      </el-form-item>
      <el-form-item label="交易金额">
        <el-input-number v-model="form.amount" :min="0.01" :step="100" :precision="2" class="w-full" />
      </el-form-item>
      <el-form-item label="成交净值">
        <el-input-number v-model="form.nav" :min="0.000001" :step="0.01" :precision="6" class="w-full" />
      </el-form-item>
      <el-form-item label="成交份额">
        <el-input-number v-model="form.shares" :min="0.0001" :step="10" :precision="4" class="w-full" />
      </el-form-item>
      <el-form-item label="手续费">
        <el-input-number v-model="form.fee" :min="0" :step="1" :precision="2" class="w-full" />
      </el-form-item>
      <el-form-item label="交易日期">
        <el-date-picker v-model="form.tradeDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" maxlength="100" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submit">
          {{ props.mode === 'BUY' ? '确认买入' : '确认卖出' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>
