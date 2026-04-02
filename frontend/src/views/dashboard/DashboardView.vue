<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { distributionApi, overviewApi, rankingApi, trendApi } from '../../api/modules'
import { useEChart } from '../../composables/useEChart'
import type { DashboardOverview, DistributionItem, RankingPayload, TrendPoint } from '../../types/api'
import { clsByNumber, money, percent } from '../../utils/format'

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

const trendChart = useEChart()
const distributionChart = useEChart()

const openFund = (fundCode: string) => {
  router.push(`/fund/${fundCode}`)
}

const renderCharts = async () => {
  const isUp = Number(overview.value.totalEstimatedProfit) >= 0
  const lineColor = isUp ? '#ff6b6b' : '#2bc37c'
  await trendChart.setOption({
    animationDuration: 500,
    grid: { left: 28, right: 16, top: 28, bottom: 26 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#08111d',
      borderColor: 'rgba(125, 211, 252, 0.2)',
      textStyle: { color: '#d7e6f5' },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((item) => item.date.slice(5)),
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
        lineStyle: { color: lineColor, width: 2.5 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${lineColor}55` },
              { offset: 1, color: `${lineColor}05` },
            ],
          },
        },
        data: trend.value.map((item) => Number(item.profit)),
      },
    ],
  })

  await distributionChart.setOption({
    animationDuration: 600,
    tooltip: {
      trigger: 'item',
      backgroundColor: '#08111d',
      borderColor: 'rgba(125, 211, 252, 0.2)',
      textStyle: { color: '#d7e6f5' },
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8aaad0' },
      icon: 'circle',
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '74%'],
        center: ['50%', '46%'],
        padAngle: 3,
        itemStyle: {
          borderColor: '#08111d',
          borderWidth: 3,
        },
        label: { color: '#d7e6f5', formatter: '{b}\n{d}%' },
        data: distribution.value.map((item, index) => ({
          name: item.category,
          value: Number(item.marketValue),
          itemStyle: {
            color: ['#7dd3fc', '#38bdf8', '#f59e0b', '#a78bfa', '#22c55e', '#fb7185'][index % 6],
          },
        })),
      },
    ],
  })
}

const load = async () => {
  loading.value = true
  try {
    const [overviewResp, trendResp, distributionResp, rankingResp] = await Promise.all([
      overviewApi(),
      trendApi(),
      distributionApi(),
      rankingApi(),
    ])
    overview.value = overviewResp.data.data
    trend.value = trendResp.data.data
    distribution.value = distributionResp.data.data
    ranking.value = rankingResp.data.data
    await renderCharts()
  } catch (error: any) {
    ElMessage.error(error?.message || '资产看板加载失败')
  } finally {
    loading.value = false
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
        <span class="eyebrow">实时资产</span>
        <h2>基于真实持仓重算收益、仓位暴露和强弱分布。</h2>
      </div>
      <div class="headline-chip">
        <span>持有基金数</span>
        <strong>{{ overview.fundCount }}</strong>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>累计投入</span>
        <strong>{{ money(overview.totalCost) }}</strong>
      </article>
      <article class="metric-card">
        <span>当前市值</span>
        <strong>{{ money(overview.totalMarketValue) }}</strong>
      </article>
      <article class="metric-card">
        <span>浮动盈亏</span>
        <strong :class="clsByNumber(overview.totalEstimatedProfit)">{{ money(overview.totalEstimatedProfit) }}</strong>
      </article>
      <article class="metric-card">
        <span>浮动收益率</span>
        <strong :class="clsByNumber(overview.totalEstimatedProfitRate)">{{ percent(overview.totalEstimatedProfitRate) }}</strong>
      </article>
      <article class="metric-card">
        <span>今日预估</span>
        <strong :class="clsByNumber(overview.totalTodayProfit)">{{ money(overview.totalTodayProfit) }}</strong>
      </article>
      <article class="metric-card">
        <span>状态</span>
        <strong>{{ Number(overview.totalEstimatedProfit) >= 0 ? '偏强' : '回撤中' }}</strong>
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">收益</span>
            <h3>近 30 日浮盈曲线</h3>
          </div>
        </div>
        <div :ref="trendChart.elementRef" class="chart-box" />
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">仓位</span>
            <h3>按基金类型统计市值</h3>
          </div>
        </div>
        <div :ref="distributionChart.elementRef" class="chart-box" />
      </article>
    </section>

    <section class="terminal-grid terminal-grid-two">
      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">盈利榜</span>
            <h3>收益贡献最高的持仓</h3>
          </div>
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
          <el-table-column label="盈亏" width="140" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="收益率" width="120" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="terminal-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">回撤榜</span>
            <h3>回撤最大的持仓</h3>
          </div>
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
          <el-table-column label="盈亏" width="140" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="收益率" width="120" align="right">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>
  </div>
</template>
