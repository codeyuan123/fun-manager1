<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { runBacktestFundsApi, searchFundApi } from '../../api/modules'
import { useEChart } from '../../composables/useEChart'
import type { BacktestResult, FundSearchItem } from '../../types/api'
import { exportCsv } from '../../utils/export'
import { money, percent } from '../../utils/format'
import { strategyDefinitions, strategyMap } from './strategyDefs'

const loading = ref(false)
const chart = useEChart()
const dateRange = ref<[string, string]>(['2024-01-01', '2026-04-03'])
const executionMode = ref('next_trade_day')
const initialCapital = ref(10000)
const strategyCode = ref('dca_monthly')
const fundCodes = ref<string[]>(['161725', '003095'])
const fundOptions = ref<FundSearchItem[]>([])
const results = ref<BacktestResult[]>([])
const strategyParams = reactive<Record<string, number | string>>({})

const activeStrategy = () => strategyMap[strategyCode.value]

const ensureParams = () => {
  const definition = activeStrategy()
  if (!definition) return
  for (const [key, value] of Object.entries(definition.defaults)) {
    if (strategyParams[key] === undefined) {
      strategyParams[key] = value
    }
  }
}

const remoteSearchFunds = async (query: string) => {
  const keyword = query.trim()
  if (!keyword) {
    fundOptions.value = []
    return
  }
  try {
    const response = await searchFundApi(keyword)
    fundOptions.value = response.data.data.slice(0, 12)
  } catch {
    fundOptions.value = []
  }
}

const renderChart = async () => {
  if (!results.value.length) return
  const xAxis = results.value[0].series.map((item) => item.date)
  await chart.setOption({
    animationDuration: 400,
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 20, right: 16, top: 24, bottom: 24 },
    xAxis: { type: 'category', data: xAxis },
    yAxis: { type: 'value' },
    series: results.value.slice(0, 10).map((item) => ({
      type: 'line',
      name: item.fundName || item.fundCode,
      smooth: true,
      showSymbol: false,
      data: item.series.map((point) => Number(point.portfolioValue)),
    })),
  })
}

const run = async () => {
  if (!fundCodes.value.length) {
    ElMessage.warning('至少选择一只基金')
    return
  }
  loading.value = true
  try {
    ensureParams()
    const response = await runBacktestFundsApi({
      fundCodes: fundCodes.value,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      executionMode: executionMode.value,
      initialCapital: Number(initialCapital.value),
      strategyCode: strategyCode.value,
      strategyParams: { ...strategyParams },
    })
    results.value = response.data.data
    await nextTick()
    await renderChart()
    ElMessage.success('基金对比完成')
  } catch (error: any) {
    ElMessage.error(error?.message || '基金对比失败')
  } finally {
    loading.value = false
  }
}

const exportResult = () => {
  if (!results.value.length) return
  exportCsv(
    `backtest-funds-${strategyCode.value}.csv`,
    ['基金', '策略', '累计收益率', '年化收益率', '最大回撤', '夏普', '期末市值', '总投入'],
    results.value.map((item) => [
      item.fundName || item.fundCode,
      item.strategyName,
      percent(item.summary.cumulativeReturnRate),
      percent(item.summary.annualizedReturnRate),
      percent(item.summary.maxDrawdownRate),
      Number(item.summary.sharpeRatio || 0).toFixed(4),
      money(item.summary.endingValue),
      money(item.summary.totalInvested),
    ]),
  )
}

const handleResize = () => {
  chart.resize()
}

watch(strategyCode, () => ensureParams(), { immediate: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="terminal-page">
    <section class="headline-panel">
      <div>
        <span class="eyebrow">Backtest</span>
        <h2>基金对比</h2>
      </div>
      <div class="headline-actions">
        <el-button type="primary" :loading="loading" @click="run">开始对比</el-button>
        <el-button :disabled="results.length === 0" @click="exportResult">导出 CSV</el-button>
      </div>
    </section>

    <section class="terminal-grid terminal-grid-two backtest-layout">
      <article class="terminal-panel backtest-form-panel">
        <div class="panel-head">
          <h3>参数</h3>
        </div>

        <el-form label-width="96px" class="trade-form">
          <el-form-item label="基金">
            <el-select
              v-model="fundCodes"
              multiple
              filterable
              remote
              reserve-keyword
              clearable
              :remote-method="remoteSearchFunds"
              class="w-full"
            >
              <el-option
                v-for="item in fundOptions"
                :key="item.fundCode"
                :label="`${item.fundCode} ${item.fundName}`"
                :value="item.fundCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="区间">
            <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" class="w-full" />
          </el-form-item>
          <el-form-item label="资金">
            <el-input-number v-model="initialCapital" :min="1000" :step="1000" class="w-full" />
          </el-form-item>
          <el-form-item label="成交模式">
            <el-segmented
              v-model="executionMode"
              :options="[
                { label: 'T+0', value: 'same_day' },
                { label: 'T+1', value: 'next_trade_day' },
              ]"
            />
          </el-form-item>
          <el-form-item label="策略">
            <el-select v-model="strategyCode" class="w-full">
              <el-option v-for="item in strategyDefinitions" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="strategy-form-grid" v-if="activeStrategy()">
          <article class="candidate-card">
            <strong>{{ activeStrategy()?.name }}</strong>
            <div class="strategy-param-list">
              <template v-for="field in activeStrategy()?.fields || []" :key="field.key">
                <el-input-number
                  v-if="field.type === 'number'"
                  v-model="strategyParams[field.key] as number"
                  :min="field.min"
                  :step="field.step || 1"
                  class="w-full"
                />
                <el-select
                  v-else
                  v-model="strategyParams[field.key] as string | number"
                  class="w-full"
                >
                  <el-option
                    v-for="option in field.options || []"
                    :key="option.value"
                    :label="`${field.label}: ${option.label}`"
                    :value="option.value"
                  />
                </el-select>
              </template>
            </div>
          </article>
        </div>
      </article>

      <article class="terminal-panel backtest-result-panel">
        <div class="panel-head">
          <h3>收益曲线</h3>
        </div>
        <div :ref="chart.elementRef" class="chart-box-market-main" />
      </article>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>结果</h3>
      </div>
      <el-table :data="results" class="terminal-table" size="small" empty-text="暂无回测结果">
        <el-table-column prop="fundName" label="基金" min-width="180" />
        <el-table-column prop="strategyName" label="策略" min-width="120" />
        <el-table-column label="累计收益率" width="120" align="right">
          <template #default="{ row }">{{ percent(row.summary.cumulativeReturnRate) }}</template>
        </el-table-column>
        <el-table-column label="年化收益率" width="120" align="right">
          <template #default="{ row }">{{ percent(row.summary.annualizedReturnRate) }}</template>
        </el-table-column>
        <el-table-column label="最大回撤" width="120" align="right">
          <template #default="{ row }">{{ percent(row.summary.maxDrawdownRate) }}</template>
        </el-table-column>
        <el-table-column label="夏普" width="100" align="right">
          <template #default="{ row }">{{ Number(row.summary.sharpeRatio || 0).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="卡玛" width="100" align="right">
          <template #default="{ row }">{{ Number(row.summary.calmarRatio || 0).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="索提诺" width="100" align="right">
          <template #default="{ row }">{{ Number(row.summary.sortinoRatio || 0).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="胜率" width="100" align="right">
          <template #default="{ row }">{{ percent(row.summary.winRate) }}</template>
        </el-table-column>
        <el-table-column label="期末市值" width="120" align="right">
          <template #default="{ row }">{{ money(row.summary.endingValue) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>
