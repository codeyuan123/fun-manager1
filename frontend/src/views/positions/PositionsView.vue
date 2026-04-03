<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FundTradeDialog from '../../components/fund/FundTradeDialog.vue'
import { positionsApi, refreshEstimatesApi, transactionsApi } from '../../api/modules'
import type { PositionItem, TransactionItem } from '../../types/api'
import {
  clsByNumber,
  estimateConfidenceLabel,
  estimateConfidenceType,
  estimateSourceLabel,
  fundTypeLabel,
  money,
  percent,
  ratioPercent,
  tradeTypeLabel,
} from '../../utils/format'

const router = useRouter()
const loading = ref(false)
const positions = ref<PositionItem[]>([])

const tradeVisible = ref(false)
const tradeMode = ref<'BUY' | 'SELL'>('BUY')
const activePosition = ref<Partial<PositionItem>>({})

const txDialogVisible = ref(false)
const txList = ref<TransactionItem[]>([])
const currentFundCode = ref('')

const summary = computed(() => {
  const totalCost = positions.value.reduce((sum, item) => sum + Number(item.currentCost || 0), 0)
  const totalValue = positions.value.reduce((sum, item) => sum + Number(item.marketValue || 0), 0)
  const totalPnl = positions.value.reduce((sum, item) => sum + Number(item.estimatedProfit || 0), 0)
  return {
    totalCost,
    totalValue,
    totalPnl,
    totalRate: totalCost ? totalPnl / totalCost : 0,
  }
})

const load = async () => {
  loading.value = true
  try {
    const response = await positionsApi()
    positions.value = response.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '持仓加载失败')
  } finally {
    loading.value = false
  }
}

const openTrade = (mode: 'BUY' | 'SELL', row?: PositionItem) => {
  tradeMode.value = mode
  activePosition.value = row || {}
  tradeVisible.value = true
}

const showTransactions = async (fundCode: string) => {
  currentFundCode.value = fundCode
  txDialogVisible.value = true
  try {
    const response = await transactionsApi(fundCode)
    txList.value = response.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '流水加载失败')
  }
}

const refreshEstimates = async () => {
  try {
    const response = await refreshEstimatesApi(positions.value.map((item) => item.fundCode))
    ElMessage.success(`刷新完成：${response.data.data.successCount}/${response.data.data.fundCount}`)
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '估值刷新失败')
  }
}

const openFund = (fundCode: string) => {
  router.push(`/fund/${fundCode}`)
}

load()
</script>

<template>
  <div class="terminal-page" v-loading="loading">
    <section class="headline-panel">
      <div>
        <span class="eyebrow">Positions</span>
        <h2>持仓管理</h2>
      </div>
      <div class="headline-actions">
        <el-button type="primary" @click="openTrade('BUY')">新增买入</el-button>
        <el-button @click="refreshEstimates">刷新估值</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
    </section>

    <section class="metric-grid metric-grid-compact">
      <article class="metric-card">
        <span>总成本</span>
        <strong>{{ money(summary.totalCost) }}</strong>
      </article>
      <article class="metric-card">
        <span>总市值</span>
        <strong>{{ money(summary.totalValue) }}</strong>
      </article>
      <article class="metric-card">
        <span>总收益</span>
        <strong :class="clsByNumber(summary.totalPnl)">{{ money(summary.totalPnl) }}</strong>
      </article>
      <article class="metric-card">
        <span>收益率</span>
        <strong :class="clsByNumber(summary.totalRate)">{{ percent(summary.totalRate) }}</strong>
      </article>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>持仓列表</h3>
      </div>

      <el-table :data="positions" class="terminal-table clickable-table" size="small" empty-text="暂无持仓">
        <el-table-column label="基金" min-width="220">
          <template #default="{ row }">
            <button class="fund-link-button" @click="openFund(row.fundCode)">
              <strong>{{ row.fundCode }}</strong>
              <span>{{ row.fundName }}</span>
              <em>{{ fundTypeLabel(row.fundType) }}</em>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="份额" width="100" align="right">
          <template #default="{ row }">{{ Number(row.totalShares).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="成本" width="120" align="right">
          <template #default="{ row }">{{ money(row.currentCost) }}</template>
        </el-table-column>
        <el-table-column label="净值" width="100" align="right">
          <template #default="{ row }">{{ Number(row.currentNav).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="市值" width="120" align="right">
          <template #default="{ row }">{{ money(row.marketValue) }}</template>
        </el-table-column>
        <el-table-column label="收益" width="120" align="right">
          <template #default="{ row }">
            <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收益率" width="110" align="right">
          <template #default="{ row }">
            <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.estimateSource === 'self_holdings' ? 'success' : 'info'">
              {{ estimateSourceLabel(row.estimateSource) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可信度" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="estimateConfidenceType(row.estimateConfidence)">
              {{ estimateConfidenceLabel(row.estimateConfidence) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="持仓覆盖" width="110" align="right">
          <template #default="{ row }">{{ ratioPercent(row.holdingCoverageRate) }}</template>
        </el-table-column>
        <el-table-column label="行情覆盖" width="110" align="right">
          <template #default="{ row }">{{ ratioPercent(row.quotedCoverageRate) }}</template>
        </el-table-column>
        <el-table-column label="今日" width="110" align="right">
          <template #default="{ row }">
            <span :class="clsByNumber(row.todayProfit)">{{ money(row.todayProfit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="showTransactions(row.fundCode)">流水</el-button>
              <el-button size="small" type="primary" @click="openTrade('BUY', row)">买入</el-button>
              <el-button size="small" type="danger" plain @click="openTrade('SELL', row)">卖出</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <FundTradeDialog
      v-model="tradeVisible"
      :mode="tradeMode"
      :fund-code="activePosition.fundCode"
      :fund-name="activePosition.fundName"
      :nav="activePosition.currentNav"
      @success="load"
    />

    <el-dialog v-model="txDialogVisible" width="900px" title="交易流水" class="terminal-dialog">
      <el-table :data="txList" class="terminal-table" size="small" empty-text="暂无记录">
        <el-table-column label="方向" width="80">
          <template #default="{ row }">{{ tradeTypeLabel(row.transactionType) }}</template>
        </el-table-column>
        <el-table-column prop="tradeDate" label="日期" width="110" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="份额" width="110" align="right">
          <template #default="{ row }">{{ Number(row.shares).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="净值" width="100" align="right">
          <template #default="{ row }">{{ Number(row.nav).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="费用" width="100" align="right">
          <template #default="{ row }">{{ money(row.fee) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <span class="mono-label">{{ currentFundCode }}</span>
          <el-button @click="txDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
