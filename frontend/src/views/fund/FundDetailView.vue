<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FundTradeDialog from '../../components/fund/FundTradeDialog.vue'
import {
  addWatchApi,
  estimateHistoryApi,
  fundDetailApi,
  fundHoldingsApi,
  fundNavHistoryApi,
  refreshEstimatesApi,
  removeWatchApi,
  watchlistApi,
} from '../../api/modules'
import { useEChart } from '../../composables/useEChart'
import type {
  FundDetail,
  FundEstimateHistoryPoint,
  FundHoldingItem,
  FundNavPoint,
  FundPerformanceRadar,
  WatchlistItem,
} from '../../types/api'
import {
  clsByNumber,
  estimateConfidenceLabel,
  estimateConfidenceType,
  estimateSourceLabel,
  fundTypeLabel,
  money,
  navRangeLabel,
  percent,
  ratioPercent,
  returnLabel,
} from '../../utils/format'

type NavRange = '1m' | '3m' | '6m' | '1y' | 'max'
type DetailChartMode = 'range' | 'intraday'

interface TrendChartPoint {
  axisLabel: string
  tooltipLabel: string
  value: number
}

const route = useRoute()
const router = useRouter()

const code = computed(() => String(route.params.code || ''))
const loading = ref(false)
const navRange = ref<NavRange>('6m')
const chartMode = ref<DetailChartMode>('range')
const currentDate = new Date()
const holdingYear = ref(currentDate.getFullYear())
const holdingQuarter = ref(Math.floor(currentDate.getMonth() / 3) + 1)

const detail = ref<FundDetail>({
  fundCode: '',
  fundName: '',
  fundType: '',
  riskLevel: null,
  managementCompany: null,
  latestNav: null,
  latestNavDate: null,
  estimateNav: null,
  estimateGrowthRate: null,
  estimateTime: null,
  estimateSource: null,
  estimateConfidence: null,
  holdingCoverageRate: null,
  quotedCoverageRate: null,
  estimateUpdatedAt: null,
  sourceRate: null,
  currentRate: null,
  minPurchaseAmount: null,
  returnStats: [],
  performanceRadar: { average: null, categories: [], data: [] },
  managers: [],
  assetAllocation: { categories: [], series: [] },
  holderStructure: { categories: [], series: [] },
  scaleTrend: [],
  sameTypeReferences: [],
})

const navHistory = ref<FundNavPoint[]>([])
const intradayHistory = ref<FundEstimateHistoryPoint[]>([])
const holdings = ref<FundHoldingItem[]>([])
const watchlist = ref<WatchlistItem[]>([])

const tradeVisible = ref(false)
const tradeMode = ref<'BUY' | 'SELL'>('BUY')

const navChart = useEChart()
const radarChart = useEChart()
const allocationChart = useEChart()
const scaleChart = useEChart()

const yearOptions = computed(() => Array.from({ length: 6 }, (_, index) => currentDate.getFullYear() - index))
const quarterOptions = [
  { value: 1, label: 'Q1' },
  { value: 2, label: 'Q2' },
  { value: 3, label: 'Q3' },
  { value: 4, label: 'Q4' },
]
const watchSet = computed(() => new Set(watchlist.value.map((item) => item.fundCode)))
const isWatched = computed(() => watchSet.value.has(code.value))

const holderRows = computed(() =>
  (detail.value.holderStructure?.categories || []).map((dateLabel, index) => ({
    date: dateLabel,
    items: (detail.value.holderStructure?.series || []).map((series) => ({
      name: series.name || '占比',
      value: Number(series.data[index] || 0),
    })),
  })),
)

const managerRadar = computed<FundPerformanceRadar>(() => {
  const firstManager = detail.value.managers[0]
  const source = firstManager?.power || detail.value.performanceRadar || { average: null, categories: [], data: [] }
  const categories = (source.categories || [])
    .map((label) => String(label || '').trim())
    .filter(Boolean)

  if (!categories.length) {
    return {
      average: source.average,
      categories: ['暂无数据'],
      data: [0],
    }
  }

  const normalizedData = categories.map((_, index) => {
    const value = Number(source.data?.[index] ?? 0)
    return Number.isFinite(value) ? value : 0
  })

  return {
    average: source.average,
    categories,
    data: normalizedData,
  }
})

const todayDate = computed(() => dayjs().format('YYYY-MM-DD'))

const navGrowthSeries = computed<TrendChartPoint[]>(() => {
  const validPoints = navHistory.value
    .map((item) => ({
      navDate: String(item.navDate || ''),
      unitNav: Number(item.unitNav),
    }))
    .filter((item) => item.navDate && Number.isFinite(item.unitNav) && item.unitNav > 0)

  if (!validPoints.length) {
    return []
  }

  const baseNav = validPoints[0].unitNav
  return validPoints.map((item) => ({
    axisLabel: item.navDate.slice(5),
    tooltipLabel: item.navDate,
    value: Number((((item.unitNav / baseNav) - 1) * 100).toFixed(2)),
  }))
})

const intradayGrowthSeries = computed<TrendChartPoint[]>(() =>
  intradayHistory.value
    .map((item) => ({
      estimateTime: String(item.estimateTime || ''),
      value: Number(item.estimateGrowthRate),
    }))
    .filter((item) => item.estimateTime && Number.isFinite(item.value))
    .map((item) => ({
      axisLabel: dayjs(item.estimateTime).format('HH:mm'),
      tooltipLabel: dayjs(item.estimateTime).format('YYYY-MM-DD HH:mm'),
      value: Number(item.value.toFixed(2)),
    })),
)

const activeTrendSeries = computed(() =>
  chartMode.value === 'range' ? navGrowthSeries.value : intradayGrowthSeries.value,
)
const activeTrendHasData = computed(() => activeTrendSeries.value.length > 0)
const activeTrendTitle = computed(() => (chartMode.value === 'range' ? '涨幅走势' : '今日走势图'))
const activeTrendSubtitle = computed(() =>
  chartMode.value === 'range'
    ? '按区间首个净值点换算累计涨幅'
    : `基于 ${todayDate.value} 的估值分时涨幅`,
)
const activeTrendEmptyText = computed(() =>
  chartMode.value === 'range' ? '暂无区间涨幅数据' : '今日暂无分时数据，可稍后刷新',
)

const loadWatchlist = async () => {
  const response = await watchlistApi()
  watchlist.value = response.data.data
}

const loadNavHistory = async () => {
  const response = await fundNavHistoryApi(code.value, navRange.value)
  navHistory.value = response.data.data
}

const loadHoldings = async () => {
  const response = await fundHoldingsApi(code.value, holdingYear.value, holdingQuarter.value)
  holdings.value = response.data.data
}

const loadIntradayHistory = async () => {
  const response = await estimateHistoryApi(code.value, todayDate.value)
  intradayHistory.value = response.data.data
}

const resolveTrendPalette = (lastValue: number) => {
  if (lastValue > 0) {
    return {
      line: '#ff4d4f',
      fillTop: 'rgba(255, 77, 79, 0.26)',
      fillBottom: 'rgba(255, 77, 79, 0.05)',
    }
  }

  if (lastValue < 0) {
    return {
      line: '#52c41a',
      fillTop: 'rgba(82, 196, 26, 0.24)',
      fillBottom: 'rgba(82, 196, 26, 0.05)',
    }
  }

  return {
    line: '#1677ff',
    fillTop: 'rgba(22, 119, 255, 0.24)',
    fillBottom: 'rgba(22, 119, 255, 0.05)',
  }
}

const toNumberOrZero = (value: unknown): number => {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

const renderTrendChart = async () => {
  const dataset = activeTrendSeries.value
  if (!dataset.length) {
    return
  }

  await nextTick()
  const palette = resolveTrendPalette(dataset[dataset.length - 1]?.value ?? 0)

  await navChart.setOption({
    animationDuration: 400,
    grid: { left: 20, right: 20, top: 28, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const current = Array.isArray(params) ? params[0] : params
        const point = dataset[current?.dataIndex ?? -1]
        if (!point) {
          return ''
        }

        return `${point.tooltipLabel}<br/>${activeTrendTitle.value}: <span style="color:${palette.line}">${point.value.toFixed(2)}%</span>`
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dataset.map((item) => item.axisLabel),
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
        lineStyle: { color: palette.line, width: 2.5 },
        itemStyle: { color: palette.line },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: palette.fillTop },
              { offset: 1, color: palette.fillBottom },
            ],
          },
        },
        data: dataset.map((item) => item.value),
      },
    ],
  })

  navChart.resize()
}

const renderCharts = async () => {
  const allocationCategories = detail.value.assetAllocation?.categories || []
  const allocationSeriesRaw = detail.value.assetAllocation?.series || []
  const safeAllocationSeries = allocationSeriesRaw
    .filter((series) => Array.isArray(series?.data))
    .map((series, index) => ({
      type: index === allocationSeriesRaw.length - 1 ? 'line' : 'bar',
      smooth: true,
      name: series.name || `系列${index + 1}`,
      data: (series.data || []).map((value) => toNumberOrZero(value)),
      itemStyle: {
        color: ['#1677ff', '#52c41a', '#faad14', '#722ed1'][index % 4],
      },
      lineStyle: {
        color: ['#1677ff', '#52c41a', '#faad14', '#722ed1'][index % 4],
      },
    }))
  const hasAllocationData = safeAllocationSeries.length > 0 && allocationCategories.length > 0

  const scaleSource = detail.value.scaleTrend || []
  const scaleCategories = scaleSource.length ? scaleSource.map((item) => item.date || '--') : ['--']
  const scaleValues = scaleSource.length ? scaleSource.map((item) => toNumberOrZero(item.value)) : [0]

  await radarChart.setOption({
    animationDuration: 400,
    radar: {
      indicator: managerRadar.value.categories.map((label) => ({ name: label, max: 100 })),
      splitLine: { lineStyle: { color: '#e7edf4' } },
      splitArea: { areaStyle: { color: ['#fbfdff', '#f5f8fc'] } },
      axisName: { color: '#86909c' },
    },
    series: [
      {
        type: 'radar',
        symbol: 'none',
        areaStyle: { color: 'rgba(22, 119, 255, 0.18)' },
        lineStyle: { color: '#1677ff' },
        data: [
          {
            value: managerRadar.value.data.map((value) => Number(value || 0)),
            name: '综合评分',
          },
        ],
      },
    ],
  })

  await allocationChart.setOption({
    animationDuration: 400,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#86909c' } },
    grid: { left: 20, right: 12, top: 42, bottom: 20 },
    xAxis: {
      type: 'category',
      data: hasAllocationData ? allocationCategories : ['--'],
      axisLabel: { color: '#86909c' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf2f8' } },
      axisLabel: { color: '#86909c' },
    },
    series: hasAllocationData
      ? safeAllocationSeries
      : [
          {
            type: 'bar',
            name: '暂无数据',
            data: [0],
            itemStyle: { color: 'rgba(134, 144, 156, 0.4)' },
          },
        ],
  })

  await scaleChart.setOption({
    animationDuration: 400,
    tooltip: { trigger: 'axis' },
    grid: { left: 20, right: 12, top: 20, bottom: 20 },
    xAxis: {
      type: 'category',
      data: scaleCategories,
      axisLabel: { color: '#86909c' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf2f8' } },
      axisLabel: { color: '#86909c' },
    },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        data: scaleValues,
        itemStyle: { color: '#13c2c2' },
      },
    ],
  })
}

const load = async () => {
  if (!code.value) return
  loading.value = true
  try {
    const [detailResp] = await Promise.all([fundDetailApi(code.value)])
    detail.value = detailResp.data.data
    await Promise.all([loadNavHistory(), loadHoldings(), loadWatchlist()])
    await renderCharts()
    if (chartMode.value === 'range') {
      await renderTrendChart()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '基金详情加载失败')
  } finally {
    loading.value = false
  }
}

const reloadNav = async (range: NavRange) => {
  navRange.value = range
  try {
    await loadNavHistory()
    if (chartMode.value === 'range') {
      await renderTrendChart()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '涨幅数据加载失败')
  }
}

const reloadHoldings = async () => {
  try {
    await loadHoldings()
  } catch (error: any) {
    ElMessage.error(error?.message || '持仓加载失败')
  }
}

const toggleWatch = async () => {
  try {
    if (isWatched.value) {
      await removeWatchApi(code.value)
      ElMessage.success('已移除自选')
    } else {
      await addWatchApi(code.value)
      ElMessage.success('已加入自选')
    }
    await loadWatchlist()
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const refreshEstimate = async () => {
  try {
    await refreshEstimatesApi([code.value])
    await load()
    if (chartMode.value === 'intraday') {
      try {
        await loadIntradayHistory()
        await renderTrendChart()
      } catch (error: any) {
        intradayHistory.value = []
        ElMessage.error(error?.message || '今日分时加载失败')
      }
    }
    ElMessage.success('估值已刷新')
  } catch (error: any) {
    ElMessage.error(error?.message || '估值刷新失败')
  }
}

const openTrade = (mode: 'BUY' | 'SELL') => {
  tradeMode.value = mode
  tradeVisible.value = true
}

const openPeer = (fundCode: string) => {
  router.push(`/fund/${fundCode}`)
}

const switchChartMode = async (mode: DetailChartMode) => {
  if (chartMode.value === mode) {
    return
  }

  chartMode.value = mode

  if (mode === 'range') {
    await renderTrendChart()
    return
  }

  try {
    await loadIntradayHistory()
    await renderTrendChart()
  } catch (error: any) {
    intradayHistory.value = []
    ElMessage.error(error?.message || '今日分时加载失败')
  }
}

const handleResize = () => {
  navChart.resize()
  radarChart.resize()
  allocationChart.resize()
  scaleChart.resize()
}

watch(
  () => code.value,
  () => {
    navRange.value = '6m'
    chartMode.value = 'range'
    intradayHistory.value = []
    load()
  },
  { immediate: true },
)

watch(
  () => [holdingYear.value, holdingQuarter.value] as const,
  () => {
    if (code.value) {
      reloadHoldings()
    }
  },
)

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="terminal-page" v-loading="loading">
    <section class="headline-panel headline-panel-detail">
      <div>
        <span class="eyebrow">Fund</span>
        <h2>{{ detail.fundName || code }} <small>{{ detail.fundCode }}</small></h2>
        <p class="terminal-subline">
          <span>{{ fundTypeLabel(detail.fundType) }}</span>
          <span>{{ detail.managementCompany || '--' }}</span>
        </p>
      </div>
      <div class="headline-actions">
        <el-button :type="isWatched ? 'info' : 'primary'" @click="toggleWatch">
          {{ isWatched ? '已加入自选' : '加入自选' }}
        </el-button>
        <el-button @click="refreshEstimate">刷新估值</el-button>
        <el-button type="primary" @click="openTrade('BUY')">买入</el-button>
        <el-button type="danger" plain @click="openTrade('SELL')">卖出</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>估值</span>
        <strong>{{ money(detail.estimateNav) }}</strong>
      </article>
      <article class="metric-card">
        <span>涨跌</span>
        <strong :class="clsByNumber((detail.estimateGrowthRate || 0) / 100)">
          {{ percent((detail.estimateGrowthRate || 0) / 100) }}
        </strong>
      </article>
      <article class="metric-card">
        <span>净值</span>
        <strong>{{ money(detail.latestNav) }}</strong>
      </article>
      <article class="metric-card">
        <span>费率</span>
        <strong>{{ detail.currentRate == null ? '--' : `${detail.currentRate}%` }}</strong>
      </article>
      <article class="metric-card">
        <span>起购</span>
        <strong>{{ detail.minPurchaseAmount == null ? '--' : money(detail.minPurchaseAmount) }}</strong>
      </article>
      <article class="metric-card">
        <span>评分</span>
        <strong>{{ detail.performanceRadar.average || '--' }}</strong>
      </article>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>估值说明</h3>
      </div>
      <div class="candidate-grid">
        <article class="candidate-card">
          <div>
            <strong>来源</strong>
            <span>{{ detail.estimateSource === 'self_holdings' ? '基于最近披露持仓近似估值' : '当前按第三方估值展示' }}</span>
          </div>
          <el-tag :type="detail.estimateSource === 'self_holdings' ? 'success' : 'info'">
            {{ estimateSourceLabel(detail.estimateSource) }}
          </el-tag>
        </article>
        <article class="candidate-card">
          <div>
            <strong>可信度</strong>
            <span>
              持仓覆盖 {{ ratioPercent(detail.holdingCoverageRate) }} / 行情覆盖 {{ ratioPercent(detail.quotedCoverageRate) }}
              <template v-if="detail.estimateUpdatedAt"> / {{ detail.estimateUpdatedAt }}</template>
            </span>
          </div>
          <el-tag :type="estimateConfidenceType(detail.estimateConfidence)">
            {{ estimateConfidenceLabel(detail.estimateConfidence) }}
          </el-tag>
        </article>
      </div>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel terminal-panel-large">
        <div class="panel-head detail-trend-head">
          <div class="detail-trend-copy">
            <h3>{{ activeTrendTitle }}</h3>
            <p>{{ activeTrendSubtitle }}</p>
          </div>
          <div class="detail-trend-actions">
            <div class="detail-trend-switch">
              <button :class="{ active: chartMode === 'range' }" @click="switchChartMode('range')">区间涨幅</button>
              <button :class="{ active: chartMode === 'intraday' }" @click="switchChartMode('intraday')">
                今日涨幅
              </button>
            </div>
            <div v-if="chartMode === 'range'" class="range-switch">
              <button
                v-for="item in ['1m', '3m', '6m', '1y', 'max']"
                :key="item"
                :class="{ active: navRange === item }"
                @click="reloadNav(item as NavRange)"
              >
                {{ navRangeLabel(item) }}
              </button>
            </div>
          </div>
        </div>
        <div class="detail-chart-stage">
          <div v-show="activeTrendHasData" :ref="navChart.elementRef" class="chart-box chart-box-lg" />
          <div v-if="!activeTrendHasData" class="detail-chart-empty">
            <el-empty :description="activeTrendEmptyText" />
          </div>
        </div>
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <h3>经理雷达</h3>
        </div>
        <div :ref="radarChart.elementRef" class="chart-box" />
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <h3>阶段收益</h3>
        </div>
        <div class="stat-strip">
          <div v-for="item in detail.returnStats" :key="item.label" class="stat-pill">
            <span>{{ returnLabel(item.label) }}</span>
            <strong :class="clsByNumber((item.value || 0) / 100)">
              {{ item.value == null ? '--' : percent((item.value || 0) / 100) }}
            </strong>
          </div>
        </div>
        <div class="holder-list">
          <div v-for="row in holderRows" :key="row.date" class="holder-row">
            <div class="holder-date">{{ row.date }}</div>
            <div class="holder-bars">
              <div v-for="item in row.items" :key="`${row.date}-${item.name}`" class="holder-bar">
                <label>{{ item.name }}</label>
                <div>
                  <span :style="{ width: `${Math.min(item.value, 100)}%` }" />
                </div>
                <strong>{{ item.value.toFixed(2) }}%</strong>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <h3>资产配置</h3>
        </div>
        <div :ref="allocationChart.elementRef" class="chart-box" />
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <h3>规模变化</h3>
        </div>
        <div :ref="scaleChart.elementRef" class="chart-box" />
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <h3>基金经理</h3>
        </div>
        <div class="manager-grid">
          <article
            v-for="(manager, index) in detail.managers"
            :key="manager.id || manager.name || `manager-${index}`"
            class="manager-card"
          >
            <div class="manager-head">
              <img v-if="manager.avatar" :src="manager.avatar" :alt="manager.name || '经理'" />
              <div>
                <strong>{{ manager.name || '未知经理' }}</strong>
                <span>{{ manager.workTime || '--' }}</span>
              </div>
            </div>
            <div class="manager-meta">
              <span>在管规模</span>
              <strong>{{ manager.fundSize || '--' }}</strong>
            </div>
            <div class="manager-stars">
              <span v-for="star in 5" :key="star" :class="{ active: star <= Number(manager.star || 0) }">●</span>
            </div>
          </article>
        </div>
      </article>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>季度持仓</h3>
        <div class="selector-row">
          <el-select v-model="holdingYear" style="width: 100px">
            <el-option v-for="year in yearOptions" :key="year" :label="year" :value="year" />
          </el-select>
          <el-select v-model="holdingQuarter" style="width: 90px">
            <el-option
              v-for="option in quarterOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
      </div>

      <el-table :data="holdings" class="terminal-table" size="small" empty-text="暂无季度持仓">
        <el-table-column label="股票" min-width="220">
          <template #default="{ row }">
            <div class="stock-cell">
              <strong>{{ row.stockCode }}</strong>
              <span>{{ row.stockName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="占比" width="100" align="right">
          <template #default="{ row }">{{ row.navRatio == null ? '--' : `${Number(row.navRatio).toFixed(2)}%` }}</template>
        </el-table-column>
        <el-table-column label="股数" width="110" align="right">
          <template #default="{ row }">{{ row.holdingShares == null ? '--' : Number(row.holdingShares).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="市值" width="130" align="right">
          <template #default="{ row }">{{ money(row.holdingMarketValue) }}</template>
        </el-table-column>
        <el-table-column prop="reportDate" label="报告期" width="110" />
      </el-table>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>同类基金</h3>
      </div>
      <div class="peer-grid">
        <button
          v-for="item in detail.sameTypeReferences"
          :key="item.fundCode"
          class="peer-card"
          @click="openPeer(item.fundCode)"
        >
          <strong>{{ item.fundCode }}</strong>
          <span>{{ item.fundName }}</span>
          <em :class="clsByNumber((item.returnRate || 0) / 100)">
            {{ item.returnRate == null ? '--' : percent((item.returnRate || 0) / 100) }}
          </em>
        </button>
      </div>
    </section>

    <FundTradeDialog
      v-model="tradeVisible"
      :mode="tradeMode"
      :fund-code="detail.fundCode"
      :fund-name="detail.fundName"
      :nav="detail.estimateNav || detail.latestNav"
      @success="load"
    />
  </div>
</template>

<style scoped>
.detail-trend-head {
  align-items: flex-start;
  flex-wrap: wrap;
}

.detail-trend-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-trend-copy p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--fm-text-muted);
}

.detail-trend-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
}

.detail-trend-switch {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid var(--fm-border);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.detail-trend-switch button {
  border: 0;
  background: transparent;
  color: var(--fm-text-muted);
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: transform 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.detail-trend-switch button:hover {
  color: var(--fm-text-main);
  transform: translateY(-1px);
}

.detail-trend-switch button.active {
  color: #fff;
  background: linear-gradient(135deg, var(--fm-primary), #49a2ff);
  box-shadow: 0 10px 22px rgba(22, 119, 255, 0.24);
}

.detail-chart-stage {
  min-height: clamp(300px, 32vw, 420px);
}

.detail-chart-empty {
  min-height: clamp(300px, 32vw, 420px);
  display: grid;
  place-items: center;
  border-radius: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.12);
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.06), transparent 58%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), rgba(255, 255, 255, 0.01));
}

.detail-chart-empty :deep(.el-empty__description p) {
  color: var(--fm-text-muted);
}

@media (max-width: 960px) {
  .detail-trend-actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}
</style>
