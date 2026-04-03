<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { distributionApi, overviewApi, positionsApi, rankingApi, refreshEstimatesApi, trendApi } from '../../api/modules'
import { useEChart } from '../../composables/useEChart'
import type { DashboardOverview, DistributionItem, PositionItem, RankingPayload, TrendPoint } from '../../types/api'
import { clsByNumber, estimateSourceLabel, money, percent } from '../../utils/format'

const router = useRouter()
const loading = ref(false)

const overview = ref<DashboardOverview>({
  totalCost: 0,
  totalMarketValue: 0,
  totalEstimatedProfit: 0,
  totalEstimatedProfitRate: 0,
  totalTodayProfit: 0,
  fundCount: 0,
})
const trend = ref<TrendPoint[]>([])
const distribution = ref<DistributionItem[]>([])
const ranking = ref<RankingPayload>({ profitTop: [], lossTop: [] })
const positions = ref<PositionItem[]>([])

const trendChart = useEChart()
const distributionChart = useEChart()

const openFund = (fundCode: string) => {
  router.push(`/fund/${fundCode}`)
}

const renderCharts = async () => {
  const isProfit = Number(overview.value.totalEstimatedProfit) >= 0
  const lineColor = isProfit ? '#ff4d4f' : '#52c41a'

  await trendChart.setOption({
    animationDuration: 400,
    grid: { left: 20, right: 12, top: 20, bottom: 20 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((item) => item.date.slice(5)),
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
        lineStyle: { color: lineColor, width: 2.5 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${lineColor}30` },
              { offset: 1, color: `${lineColor}08` },
            ],
          },
        },
        data: trend.value.map((item) => Number(item.profit)),
      },
    ],
  })

  await distributionChart.setOption({
    animationDuration: 500,
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      textStyle: { color: '#86909c' },
    },
    series: [
      {
        type: 'pie',
        radius: ['56%', '74%'],
        center: ['50%', '42%'],
        data: distribution.value.map((item, index) => ({
          name: item.category,
          value: Number(item.marketValue),
          itemStyle: {
            color: ['#1677ff', '#13c2c2', '#722ed1', '#faad14', '#52c41a', '#ff7a45'][index % 6],
          },
        })),
      },
    ],
  })
}

const load = async () => {
  loading.value = true
  try {
    const [overviewResp, trendResp, distributionResp, rankingResp, positionsResp] = await Promise.all([
      overviewApi(),
      trendApi(),
      distributionApi(),
      rankingApi(),
      positionsApi(),
    ])
    overview.value = overviewResp.data.data
    trend.value = trendResp.data.data
    distribution.value = distributionResp.data.data
    ranking.value = rankingResp.data.data
    positions.value = positionsResp.data.data
    await renderCharts()
  } catch (error: any) {
    ElMessage.error(error?.message || '工作台加载失败')
  } finally {
    loading.value = false
  }
}

const refreshEstimates = async () => {
  try {
    const response = await refreshEstimatesApi()
    ElMessage.success(`刷新完成：${response.data.data.successCount}/${response.data.data.fundCount}`)
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '估值刷新失败')
  }
}

const handleResize = () => {
  trendChart.resize()
  distributionChart.resize()
}

onMounted(() => {
  load()
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
        <span class="eyebrow">Workbench</span>
        <h2>基金工作台</h2>
      </div>
      <div class="headline-actions">
        <el-button type="primary" @click="refreshEstimates">刷新估值</el-button>
        <el-button @click="router.push('/fund/positions')">持仓</el-button>
        <el-button @click="router.push('/fund/watchlist')">自选</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>总成本</span>
        <strong>{{ money(overview.totalCost) }}</strong>
      </article>
      <article class="metric-card">
        <span>总市值</span>
        <strong>{{ money(overview.totalMarketValue) }}</strong>
      </article>
      <article class="metric-card">
        <span>累计收益</span>
        <strong :class="clsByNumber(overview.totalEstimatedProfit)">{{ money(overview.totalEstimatedProfit) }}</strong>
      </article>
      <article class="metric-card">
        <span>收益率</span>
        <strong :class="clsByNumber(overview.totalEstimatedProfitRate)">{{ percent(overview.totalEstimatedProfitRate) }}</strong>
      </article>
      <article class="metric-card">
        <span>今日收益</span>
        <strong :class="clsByNumber(overview.totalTodayProfit)">{{ money(overview.totalTodayProfit) }}</strong>
      </article>
      <article class="metric-card">
        <span>持仓数</span>
        <strong>{{ overview.fundCount }}</strong>
      </article>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>估值来源</h3>
      </div>
      <div class="candidate-grid">
        <article class="candidate-card">
          <div>
            <strong>自算基金</strong>
            <span>{{ positions.filter((item) => item.estimateSource === 'self_holdings').length }}</span>
          </div>
          <el-tag type="success">{{ estimateSourceLabel('self_holdings') }}</el-tag>
        </article>
        <article class="candidate-card">
          <div>
            <strong>第三方基金</strong>
            <span>{{ positions.filter((item) => item.estimateSource === 'third_party').length }}</span>
          </div>
          <el-tag type="info">{{ estimateSourceLabel('third_party') }}</el-tag>
        </article>
      </div>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <h3>收益趋势</h3>
        </div>
        <div :ref="trendChart.elementRef" class="chart-box" />
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <h3>类型分布</h3>
        </div>
        <div :ref="distributionChart.elementRef" class="chart-box" />
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <h3>盈利前五</h3>
        </div>
        <el-table :data="ranking.profitTop" class="terminal-table clickable-table" size="small" empty-text="暂无数据">
          <el-table-column label="基金" min-width="220">
            <template #default="{ row }">
              <button class="fund-link-button" @click="openFund(row.fundCode)">
                <strong>{{ row.fundCode }}</strong>
                <span>{{ row.fundName }}</span>
              </button>
            </template>
          </el-table-column>
          <el-table-column label="收益" width="130" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="收益率" width="110" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <h3>回撤前五</h3>
        </div>
        <el-table :data="ranking.lossTop" class="terminal-table clickable-table" size="small" empty-text="暂无数据">
          <el-table-column label="基金" min-width="220">
            <template #default="{ row }">
              <button class="fund-link-button" @click="openFund(row.fundCode)">
                <strong>{{ row.fundCode }}</strong>
                <span>{{ row.fundName }}</span>
              </button>
            </template>
          </el-table-column>
          <el-table-column label="收益" width="130" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="收益率" width="110" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>
  </div>
</template>
