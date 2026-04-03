<script setup lang="ts">
import dayjs from 'dayjs'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { estimateHistoryApi, fundDetailApi, refreshEstimatesApi, searchFundApi } from '../../api/modules'
import { useEChart } from '../../composables/useEChart'
import type { FundDetail, FundEstimateHistoryPoint, FundSearchItem } from '../../types/api'
import {
  estimateConfidenceLabel,
  estimateConfidenceType,
  estimateSourceLabel,
  money,
  percent,
  ratioPercent,
} from '../../utils/format'

interface SearchOption extends FundSearchItem {
  value: string
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const fundCode = ref(String(route.query.code || ''))
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const detail = ref<FundDetail | null>(null)
const history = ref<FundEstimateHistoryPoint[]>([])

const navChart = useEChart()
const rateChart = useEChart()

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

const selectFund = (item: SearchOption | { fundCode: string }) => {
  if (!item.fundCode) return
  fundCode.value = item.fundCode
  router.replace({ path: '/fund/market', query: { code: item.fundCode } })
}

const renderCharts = async () => {
  await navChart.setOption({
    animationDuration: 400,
    grid: { left: 24, right: 16, top: 24, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: history.value.map((item) => dayjs(item.estimateTime).format('HH:mm')),
      axisLabel: { color: '#86909c' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf2f8' } },
      axisLabel: { color: '#86909c' },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { color: '#1677ff', width: 2.5 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22, 119, 255, 0.24)' },
              { offset: 1, color: 'rgba(22, 119, 255, 0.05)' },
            ],
          },
        },
        data: history.value.map((item) => Number(item.estimateNav || 0)),
      },
    ],
  })

  await rateChart.setOption({
    animationDuration: 400,
    grid: { left: 24, right: 16, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: history.value.map((item) => dayjs(item.estimateTime).format('HH:mm')),
      axisLabel: { color: '#86909c' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf2f8' } },
      axisLabel: {
        color: '#86909c',
        formatter: (value: number) => `${value.toFixed(2)}%`,
      },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { color: '#ff4d4f', width: 2 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255, 77, 79, 0.18)' },
              { offset: 1, color: 'rgba(255, 77, 79, 0.04)' },
            ],
          },
        },
        data: history.value.map((item) => Number(item.estimateGrowthRate || 0)),
      },
    ],
  })
}

const load = async () => {
  if (!fundCode.value.trim()) {
    detail.value = null
    history.value = []
    return
  }
  loading.value = true
  try {
    const [detailResp, historyResp] = await Promise.all([
      fundDetailApi(fundCode.value.trim()),
      estimateHistoryApi(fundCode.value.trim(), selectedDate.value),
    ])
    detail.value = detailResp.data.data
    history.value = historyResp.data.data
    await renderCharts()
  } catch (error: any) {
    ElMessage.error(error?.message || '实时行情加载失败')
  } finally {
    loading.value = false
  }
}

const refreshEstimate = async () => {
  if (!fundCode.value.trim()) return
  try {
    await refreshEstimatesApi([fundCode.value.trim()])
    await load()
    ElMessage.success('估值已刷新')
  } catch (error: any) {
    ElMessage.error(error?.message || '估值刷新失败')
  }
}

const handleResize = () => {
  navChart.resize()
  rateChart.resize()
}

watch(
  () => route.query.code,
  (value) => {
    fundCode.value = String(value || '')
    load()
  },
  { immediate: true },
)

watch(selectedDate, () => {
  load()
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="terminal-page" v-loading="loading">
    <section class="headline-panel">
      <div>
        <span class="eyebrow">Market</span>
        <h2>基金实时行情</h2>
      </div>
      <div class="headline-actions">
        <el-autocomplete
          v-model="fundCode"
          class="app-global-search"
          :fetch-suggestions="fetchSuggestions"
          placeholder="搜索基金代码或名称"
          @select="selectFund"
        />
        <el-date-picker v-model="selectedDate" type="date" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="refreshEstimate">刷新估值</el-button>
      </div>
    </section>

    <section class="terminal-panel" v-if="detail">
      <div class="panel-head">
        <h3>{{ detail.fundName }} / {{ detail.fundCode }}</h3>
        <div class="headline-actions">
          <el-tag :type="detail.estimateSource === 'self_holdings' ? 'success' : 'info'">
            {{ estimateSourceLabel(detail.estimateSource) }}
          </el-tag>
          <el-tag :type="estimateConfidenceType(detail.estimateConfidence)">
            {{ estimateConfidenceLabel(detail.estimateConfidence) }}
          </el-tag>
        </div>
      </div>
      <div class="candidate-grid">
        <article class="candidate-card">
          <div>
            <strong>最新估值</strong>
            <span>{{ money(detail.estimateNav) }}</span>
          </div>
        </article>
        <article class="candidate-card">
          <div>
            <strong>涨跌幅</strong>
            <span>{{ percent((detail.estimateGrowthRate || 0) / 100) }}</span>
          </div>
        </article>
        <article class="candidate-card">
          <div>
            <strong>持仓覆盖</strong>
            <span>{{ ratioPercent(detail.holdingCoverageRate) }}</span>
          </div>
        </article>
        <article class="candidate-card">
          <div>
            <strong>行情覆盖</strong>
            <span>{{ ratioPercent(detail.quotedCoverageRate) }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="terminal-panel" v-if="history.length > 0">
      <div class="panel-head">
        <h3>今日估值曲线</h3>
      </div>
      <div :ref="navChart.elementRef" class="chart-box-market-main" />
    </section>

    <section class="terminal-panel" v-if="history.length > 0">
      <div class="panel-head">
        <h3>今日涨跌幅</h3>
      </div>
      <div :ref="rateChart.elementRef" class="chart-box-market-sub" />
    </section>

    <section class="terminal-panel" v-if="detail">
      <div class="panel-head">
        <h3>说明</h3>
      </div>
      <div class="candidate-grid">
        <article class="candidate-card">
          <div>
            <strong>估值时间</strong>
            <span>{{ detail.estimateUpdatedAt || detail.estimateTime || '--' }}</span>
          </div>
        </article>
        <article class="candidate-card">
          <div>
            <strong>来源说明</strong>
            <span>{{ detail.estimateSource === 'self_holdings' ? '按最近披露持仓近似估值' : '第三方估值回退' }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="terminal-panel" v-if="detail && history.length === 0">
      <div class="panel-head">
        <h3>暂无数据</h3>
      </div>
      <el-empty description="今日暂无估值记录，可手动刷新后再查看" />
    </section>
  </div>
</template>
