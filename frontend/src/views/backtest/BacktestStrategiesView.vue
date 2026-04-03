<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { runBacktestStrategiesApi, searchFundApi } from '../../api/modules'
import { useEChart } from '../../composables/useEChart'
import type { BacktestResult, FundSearchItem } from '../../types/api'
import { exportCsv } from '../../utils/export'
import { money, percent } from '../../utils/format'
import { strategyDefinitions, strategyMap } from './strategyDefs'

interface SearchOption extends FundSearchItem {
  value: string
}

const loading = ref(false)
const fundCode = ref('161725')
const fundName = ref('')
const dateRange = ref<[string, string]>(['2024-01-01', '2026-04-03'])
const executionMode = ref('next_trade_day')
const initialCapital = ref(10000)
const selectedStrategies = ref<string[]>(['lump_sum', 'dca_monthly'])
const results = ref<BacktestResult[]>([])

const chart = useEChart()
const strategyParams = reactive<Record<string, Record<string, number | string>>>({})

const selectedDefinitions = computed(() =>
  selectedStrategies.value.map((code) => strategyMap[code]).filter(Boolean),
)

const ensureParams = () => {
  for (const definition of selectedDefinitions.value) {
    if (!strategyParams[definition.code]) {
      strategyParams[definition.code] = { ...definition.defaults }
    }
  }
}

const fetchSuggestions = async (query: string, callback: (items: SearchOption[]) => void) => {
  const keyword = query.trim()
  if (!keyword) {
    callback([])
    return
  }
  try {
    const response = await searchFundApi(keyword)
    callback(
      response.data.data.slice(0, 10).map((item) => ({
        ...item,
        value: `${item.fundCode} ${item.fundName}`,
      })),
    )
  } catch {
    callback([])
  }
}

const selectFund = (item: SearchOption) => {
  fundCode.value = item.fundCode
  fundName.value = item.fundName
}

const renderChart = async () => {
  const chartItems = results.value.slice(0, 4)
  if (!chartItems.length) return
  const xAxis = chartItems[0].series.map((item) => item.date)
  await chart.setOption({
    animationDuration: 400,
    tooltip: { trigger: 'axis' },
    grid: { left: 20, right: 16, top: 24, bottom: 24 },
    legend: { top: 0 },
    xAxis: { type: 'category', data: xAxis },
    yAxis: { type: 'value' },
    series: chartItems.map((item) => ({
      type: 'line',
      name: item.strategyName,
      smooth: true,
      showSymbol: false,
      data: item.series.map((point) => Number(point.portfolioValue)),
    })),
  })
}

const run = async () => {
  if (!fundCode.value.trim()) {
    ElMessage.warning('请输入基金代码')
    return
  }
  if (selectedStrategies.value.length === 0) {
    ElMessage.warning('至少选择一个策略')
    return
  }
  loading.value = true
  try {
    ensureParams()
    const response = await runBacktestStrategiesApi({
      fundCode: fundCode.value.trim(),
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      executionMode: executionMode.value,
      initialCapital: Number(initialCapital.value),
      strategyCodes: selectedStrategies.value,
      strategyParams: Object.fromEntries(selectedStrategies.value.map((code) => [code, strategyParams[code] || {}])),
    })
    results.value = response.data.data
    await nextTick()
    await renderChart()
    ElMessage.success('回测完成')
  } catch (error: any) {
    ElMessage.error(error?.message || '回测失败')
  } finally {
    loading.value = false
  }
}

const exportResult = () => {
  if (!results.value.length) return
  exportCsv(
    `backtest-strategies-${fundCode.value}.csv`,
    ['策略', '累计收益率', '年化收益率', '最大回撤', '夏普', '期末市值', '总投入'],
    results.value.map((item) => [
      item.strategyName,
      percent(item.summary.cumulativeReturnRate),
      percent(item.summary.annualizedReturnRate),
      percent(item.summary.maxDrawdownRate),
      item.summary.sharpeRatio.toFixed(4),
      money(item.summary.endingValue),
      money(item.summary.totalInvested),
    ]),
  )
}

const handleResize = () => {
  chart.resize()
}

watch(selectedStrategies, ensureParams, { immediate: true })

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
        <h2>策略回测</h2>
      </div>
      <div class="headline-actions">
        <el-button type="primary" :loading="loading" @click="run">开始回测</el-button>
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
            <el-autocomplete
              v-model="fundCode"
              class="trade-search"
              :fetch-suggestions="fetchSuggestions"
              placeholder="代码 / 名称"
              @select="selectFund"
            />
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
            <el-select v-model="selectedStrategies" multiple collapse-tags class="w-full">
              <el-option v-for="item in strategyDefinitions" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="strategy-form-grid">
          <article v-for="definition in selectedDefinitions" :key="definition.code" class="candidate-card">
            <strong>{{ definition.name }}</strong>
            <div class="strategy-param-list">
              <template v-for="field in definition.fields" :key="field.key">
                <el-input-number
                  v-if="field.type === 'number'"
                  v-model="strategyParams[definition.code][field.key] as number"
                  :min="field.min"
                  :step="field.step || 1"
                  class="w-full"
                />
                <el-select
                  v-else
                  v-model="strategyParams[definition.code][field.key] as string | number"
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
        <el-table-column prop="strategyName" label="策略" min-width="140" />
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
