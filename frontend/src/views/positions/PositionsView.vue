<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { buyApi, estimateApi, positionsApi, sellApi, transactionsApi } from '../../api/modules'
import type { PositionItem, TransactionItem } from '../../types/api'
import { clsByNumber, money, percent } from '../../utils/format'

const loading = ref(false)
const positions = ref<PositionItem[]>([])
const txDialogVisible = ref(false)
const txList = ref<TransactionItem[]>([])
const currentFundCode = ref('')

const tradeDialogVisible = ref(false)
const tradeMode = ref<'BUY' | 'SELL'>('BUY')
const tradeForm = reactive({
  fundCode: '',
  fundName: '',
  amount: 1000,
  shares: undefined as number | undefined,
  fee: 0,
  nav: 1,
  tradeDate: '',
  remark: '',
})

const load = async () => {
  loading.value = true
  try {
    const resp = await positionsApi()
    positions.value = resp.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '持仓加载失败')
  } finally {
    loading.value = false
  }
}

const openTrade = (mode: 'BUY' | 'SELL', row?: PositionItem) => {
  tradeMode.value = mode
  tradeForm.fundCode = row?.fundCode || ''
  tradeForm.fundName = row?.fundName || ''
  tradeForm.amount = 1000
  tradeForm.shares = undefined
  tradeForm.fee = 0
  tradeForm.nav = row?.currentNav || 1
  tradeForm.tradeDate = ''
  tradeForm.remark = ''
  tradeDialogVisible.value = true
}

const fillNav = async () => {
  if (!tradeForm.fundCode) return
  try {
    const resp = await estimateApi(tradeForm.fundCode)
    tradeForm.nav = Number(resp.data.data.estimateNav || 1)
    if (!tradeForm.fundName) tradeForm.fundName = resp.data.data.fundName
  } catch {
    ElMessage.warning('未获取到估值，使用手工填写净值')
  }
}

const submitTrade = async () => {
  const payload = {
    ...tradeForm,
    tradeDate: tradeForm.tradeDate || undefined,
  }
  try {
    if (tradeMode.value === 'BUY') {
      await buyApi(payload)
      ElMessage.success('买入记录已保存')
    } else {
      await sellApi(payload)
      ElMessage.success('卖出记录已保存')
    }
    tradeDialogVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '交易提交失败')
  }
}

const showTransactions = async (fundCode: string) => {
  currentFundCode.value = fundCode
  txDialogVisible.value = true
  try {
    const resp = await transactionsApi(fundCode)
    txList.value = resp.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '交易明细加载失败')
  }
}

load()
</script>

<template>
  <el-card class="page-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="page-toolbar">
        <el-button type="primary" @click="openTrade('BUY')">新增买入</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>

    <el-table :data="positions" stripe>
      <el-table-column prop="fundCode" label="代码" width="100" fixed="left" />
      <el-table-column prop="fundName" label="基金名称" min-width="200" />
      <el-table-column prop="totalShares" label="份额" width="120" />
      <el-table-column label="成本" width="130">
        <template #default="{ row }">{{ money(row.currentCost) }}</template>
      </el-table-column>
      <el-table-column label="市值" width="130">
        <template #default="{ row }">{{ money(row.marketValue) }}</template>
      </el-table-column>
      <el-table-column label="累计收益" width="130">
        <template #default="{ row }">
          <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收益率" width="120">
        <template #default="{ row }">
          <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="今日预估" width="130">
        <template #default="{ row }">
          <span :class="clsByNumber(row.todayProfit)">{{ money(row.todayProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showTransactions(row.fundCode)">明细</el-button>
          <el-button size="small" type="primary" @click="openTrade('BUY', row)">买入</el-button>
          <el-button size="small" type="danger" plain @click="openTrade('SELL', row)">卖出</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="tradeDialogVisible" :title="tradeMode === 'BUY' ? '新增买入' : '新增卖出'" width="560px">
    <el-form label-width="120px">
      <el-form-item label="基金代码">
        <el-input v-model="tradeForm.fundCode" @blur="fillNav" />
      </el-form-item>
      <el-form-item label="基金名称">
        <el-input v-model="tradeForm.fundName" />
      </el-form-item>
      <el-form-item label="交易金额">
        <el-input-number v-model="tradeForm.amount" :precision="2" :min="0.01" :step="100" />
      </el-form-item>
      <el-form-item label="交易净值">
        <el-input-number v-model="tradeForm.nav" :precision="6" :min="0.000001" :step="0.01" />
      </el-form-item>
      <el-form-item label="交易份额">
        <el-input-number v-model="tradeForm.shares" :precision="4" :min="0.0001" :step="10" />
      </el-form-item>
      <el-form-item label="手续费">
        <el-input-number v-model="tradeForm.fee" :precision="2" :min="0" :step="1" />
      </el-form-item>
      <el-form-item label="交易日期">
        <el-date-picker v-model="tradeForm.tradeDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="tradeForm.remark" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="tradeDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitTrade">提交</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="txDialogVisible" :title="`交易明细 - ${currentFundCode}`" width="860px">
    <el-table :data="txList" stripe>
      <el-table-column prop="transactionType" label="类型" width="100" />
      <el-table-column prop="tradeDate" label="交易日期" width="120" />
      <el-table-column label="金额">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="shares" label="份额" />
      <el-table-column prop="nav" label="净值" />
      <el-table-column label="手续费">
        <template #default="{ row }">{{ money(row.fee) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
    </el-table>
  </el-dialog>
</template>
