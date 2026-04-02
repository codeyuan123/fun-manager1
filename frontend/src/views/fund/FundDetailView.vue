<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import FundTradeDialog from '../../components/fund/FundTradeDialog.vue'
import {
  addWatchApi,
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

const route = useRoute()
const router = useRouter()

const code = computed(() => String(route.params.code || ''))
const loading = ref(false)
const navRange = ref<NavRange>('6m')
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
  detail.value.holderStructure.categories.map((dateLabel, index) => ({
    date: dateLabel,
    items: detail.value.holderStructure.series.map((series) => ({
      name: series.name || '占比',
      value: Number(series.data[index] || 0),
    })),
  })),
)

const managerRadar = computed<FundPerformanceRadar>(() => {
  const firstManager = detail.value.managers[0]
  const source = firstManager?.power || detail.value.performanceRadar
  const categories = (source.categories || [])
    .map((label) => String(label || '').trim())
    .filter(Boolean)

  if (!categories.length) {
    return {
      average: source.average,
      categories: ['鏆傛棤鏁版嵁'],
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

const renderCharts = async () => {
  await navChart.setOption({
    animationDuration: 400,
    grid: { left: 20, right: 12, top: 20, bottom: 20 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: navHistory.value.map((item) => item.navDate.slice(5)),
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
        data: navHistory.value.map((item) => Number(item.unitNav || 0)),
      },
    ],
  })

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
      data: detail.value.assetAllocation.categories,
      axisLabel: { color: '#86909c' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf2f8' } },
      axisLabel: { color: '#86909c' },
    },
    series: detail.value.assetAllocation.series.map((series, index) => ({
      type: index === detail.value.assetAllocation.series.length - 1 ? 'line' : 'bar',
      smooth: true,
      name: series.name || `系列${index + 1}`,
      data: series.data.map((value) => Number(value || 0)),
      itemStyle: {
        color: ['#1677ff', '#52c41a', '#faad14', '#722ed1'][index % 4],
      },
      lineStyle: {
        color: ['#1677ff', '#52c41a', '#faad14', '#722ed1'][index % 4],
      },
    })),
  })

  await scaleChart.setOption({
    animationDuration: 400,
    tooltip: { trigger: 'axis' },
    grid: { left: 20, right: 12, top: 20, bottom: 20 },
    xAxis: {
      type: 'category',
      data: detail.value.scaleTrend.map((item) => item.date),
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
        data: detail.value.scaleTrend.map((item) => Number(item.value || 0)),
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
    await renderCharts()
  } catch (error: any) {
    ElMessage.error(error?.message || '净值加载失败')
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
        <div class="panel-head">
          <h3>净值走势</h3>
          <div class="range-switch">
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
        <div :ref="navChart.elementRef" class="chart-box chart-box-lg" />
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
