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
import { clsByNumber, fundTypeLabel, money, navRangeLabel, percent, returnLabel } from '../../utils/format'

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
  { value: 1, label: '一季度' },
  { value: 2, label: '二季度' },
  { value: 3, label: '三季度' },
  { value: 4, label: '四季度' },
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
  return firstManager?.power || detail.value.performanceRadar
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
  const navDates = navHistory.value.map((item) => item.navDate.slice(5))
  const navValues = navHistory.value.map((item) => Number(item.unitNav || 0))
  await navChart.setOption({
    animationDuration: 500,
    grid: { left: 28, right: 18, top: 24, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#08111d',
      borderColor: 'rgba(125, 211, 252, 0.2)',
      textStyle: { color: '#d7e6f5' },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: navDates,
      axisLine: { lineStyle: { color: 'rgba(138,170,208,0.18)' } },
      axisLabel: { color: '#8aaad0' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(138,170,208,0.08)' } },
      axisLabel: { color: '#8aaad0' },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { color: '#7dd3fc', width: 2.5 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(125, 211, 252, 0.28)' },
              { offset: 1, color: 'rgba(125, 211, 252, 0.03)' },
            ],
          },
        },
        data: navValues,
      },
    ],
  })

  await radarChart.setOption({
    animationDuration: 500,
    radar: {
      indicator: managerRadar.value.categories.map((label) => ({ name: label, max: 100 })),
      splitLine: { lineStyle: { color: 'rgba(138,170,208,0.16)' } },
      splitArea: { areaStyle: { color: ['rgba(125, 211, 252, 0.02)', 'rgba(125, 211, 252, 0.04)'] } },
      axisName: { color: '#8aaad0' },
    },
    series: [
      {
        type: 'radar',
        symbol: 'none',
        areaStyle: { color: 'rgba(245, 158, 11, 0.26)' },
        lineStyle: { color: '#f59e0b' },
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
    animationDuration: 500,
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#08111d',
      borderColor: 'rgba(125, 211, 252, 0.2)',
      textStyle: { color: '#d7e6f5' },
    },
    legend: {
      top: 0,
      textStyle: { color: '#8aaad0' },
    },
    grid: { left: 28, right: 18, top: 46, bottom: 24 },
    xAxis: {
      type: 'category',
      data: detail.value.assetAllocation.categories,
      axisLine: { lineStyle: { color: 'rgba(138,170,208,0.18)' } },
      axisLabel: { color: '#8aaad0' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(138,170,208,0.08)' } },
      axisLabel: { color: '#8aaad0' },
    },
    series: detail.value.assetAllocation.series.map((series, index) => ({
      type: index === detail.value.assetAllocation.series.length - 1 ? 'line' : 'bar',
      smooth: true,
      name: series.name || `系列${index + 1}`,
      data: series.data.map((value) => Number(value || 0)),
      itemStyle: {
        color: ['#7dd3fc', '#22c55e', '#f59e0b', '#a78bfa'][index % 4],
      },
      lineStyle: {
        color: ['#7dd3fc', '#22c55e', '#f59e0b', '#a78bfa'][index % 4],
      },
    })),
  })

  await scaleChart.setOption({
    animationDuration: 500,
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#08111d',
      borderColor: 'rgba(125, 211, 252, 0.2)',
      textStyle: { color: '#d7e6f5' },
    },
    grid: { left: 28, right: 18, top: 22, bottom: 26 },
    xAxis: {
      type: 'category',
      data: detail.value.scaleTrend.map((item) => item.date),
      axisLabel: { color: '#8aaad0' },
      axisLine: { lineStyle: { color: 'rgba(138,170,208,0.18)' } },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(138,170,208,0.08)' } },
      axisLabel: { color: '#8aaad0' },
    },
    series: [
      {
        type: 'bar',
        barWidth: 20,
        data: detail.value.scaleTrend.map((item) => Number(item.value || 0)),
        itemStyle: { color: '#f97316' },
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
    ElMessage.error(error?.message || '净值走势加载失败')
  }
}

const reloadHoldings = async () => {
  try {
    await loadHoldings()
  } catch (error: any) {
    ElMessage.error(error?.message || '季度持仓加载失败')
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
    ElMessage.error(error?.message || '自选操作失败')
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
        <span class="eyebrow">基金详情</span>
        <h2>{{ detail.fundName || code }} <small>{{ detail.fundCode }}</small></h2>
        <p class="terminal-subline">
          {{ fundTypeLabel(detail.fundType) }}
          <span>·</span>
          <span>{{ detail.managementCompany || '基金公司待补充' }}</span>
        </p>
      </div>
      <div class="headline-actions">
        <el-button :type="isWatched ? 'info' : 'primary'" @click="toggleWatch">
          {{ isWatched ? '移除自选' : '加入自选' }}
        </el-button>
        <el-button type="primary" @click="openTrade('BUY')">买入</el-button>
        <el-button type="danger" plain @click="openTrade('SELL')">卖出</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>预估净值</span>
        <strong>{{ money(detail.estimateNav) }}</strong>
      </article>
      <article class="metric-card">
        <span>预估涨跌</span>
        <strong :class="clsByNumber((detail.estimateGrowthRate || 0) / 100)">
          {{ percent((detail.estimateGrowthRate || 0) / 100) }}
        </strong>
      </article>
      <article class="metric-card">
        <span>最新净值</span>
        <strong>{{ money(detail.latestNav) }}</strong>
      </article>
      <article class="metric-card">
        <span>申购费率</span>
        <strong>{{ detail.currentRate == null ? '--' : `${detail.currentRate}%` }}</strong>
      </article>
      <article class="metric-card">
        <span>起购金额</span>
        <strong>{{ detail.minPurchaseAmount == null ? '--' : money(detail.minPurchaseAmount) }}</strong>
      </article>
      <article class="metric-card">
        <span>综合评分</span>
        <strong>{{ detail.performanceRadar.average || '--' }}</strong>
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel terminal-panel-large">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">净值走势</span>
            <h3>历史单位净值</h3>
          </div>
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
          <div>
            <span class="panel-kicker">能力雷达</span>
            <h3>基金经理能力视图</h3>
          </div>
        </div>
        <div :ref="radarChart.elementRef" class="chart-box" />
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">阶段收益</span>
            <h3>区间表现</h3>
          </div>
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
          <div>
            <span class="panel-kicker">资产配置</span>
            <h3>资产结构趋势</h3>
          </div>
        </div>
        <div :ref="allocationChart.elementRef" class="chart-box" />
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">基金规模</span>
            <h3>基金规模变化</h3>
          </div>
        </div>
        <div :ref="scaleChart.elementRef" class="chart-box" />
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">基金经理</span>
            <h3>现任经理信息</h3>
          </div>
        </div>
        <div class="manager-grid">
          <article
            v-for="(manager, index) in detail.managers"
            :key="manager.id || manager.name || `manager-${index}`"
            class="manager-card"
          >
            <div class="manager-head">
              <img v-if="manager.avatar" :src="manager.avatar" :alt="manager.name || '基金经理'" />
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
        <div>
          <span class="panel-kicker">季度持仓</span>
          <h3>重仓股明细</h3>
        </div>
        <div class="selector-row">
          <el-select v-model="holdingYear" style="width: 120px">
            <el-option v-for="year in yearOptions" :key="year" :label="year" :value="year" />
          </el-select>
          <el-select v-model="holdingQuarter" style="width: 120px">
            <el-option
              v-for="option in quarterOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
      </div>

      <el-table :data="holdings" class="terminal-table" size="small" empty-text="该季度暂无持仓">
        <el-table-column label="股票" min-width="220">
          <template #default="{ row }">
            <div class="stock-cell">
              <strong>{{ row.stockCode }}</strong>
              <span>{{ row.stockName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="净值占比" width="120" align="right">
          <template #default="{ row }">{{ row.navRatio == null ? '--' : `${Number(row.navRatio).toFixed(2)}%` }}</template>
        </el-table-column>
        <el-table-column label="持股数" width="120" align="right">
          <template #default="{ row }">{{ row.holdingShares == null ? '--' : Number(row.holdingShares).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="持仓市值" width="150" align="right">
          <template #default="{ row }">{{ money(row.holdingMarketValue) }}</template>
        </el-table-column>
        <el-table-column prop="reportDate" label="报告期" width="120" />
      </el-table>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">同类参考</span>
          <h3>同类基金参考</h3>
        </div>
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
