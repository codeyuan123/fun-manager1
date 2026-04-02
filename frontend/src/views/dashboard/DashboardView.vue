<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { distributionApi, overviewApi, rankingApi, trendApi } from '../../api/modules'
import type { DashboardOverview, DistributionItem, RankingPayload, TrendPoint } from '../../types/api'
import { clsByNumber, money, percent } from '../../utils/format'

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

const trendRef = ref<HTMLDivElement>()
const distributionRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let distributionChart: echarts.ECharts | null = null

const renderCharts = () => {
  if (trendRef.value) {
    trendChart ??= echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trend.value.map((it) => it.date) },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'line',
          smooth: true,
          data: trend.value.map((it) => Number(it.profit)),
          areaStyle: {},
        },
      ],
    })
  }
  if (distributionRef.value) {
    distributionChart ??= echarts.init(distributionRef.value)
    distributionChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['38%', '72%'],
          data: distribution.value.map((it) => ({ name: it.category, value: Number(it.marketValue) })),
        },
      ],
    })
  }
}

const load = async () => {
  loading.value = true
  try {
    const [a, b, c, d] = await Promise.all([overviewApi(), trendApi(), distributionApi(), rankingApi()])
    overview.value = a.data.data
    trend.value = b.data.data
    distribution.value = c.data.data
    ranking.value = d.data.data
    await nextTick()
    renderCharts()
  } catch (error: any) {
    ElMessage.error(error?.message || '看板加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', () => {
    trendChart?.resize()
    distributionChart?.resize()
  })
})
</script>

<template>
  <div v-loading="loading">
    <section class="kpi-grid">
      <article class="kpi-card">
        <h3>总成本</h3>
        <p>{{ money(overview.totalCost) }}</p>
      </article>
      <article class="kpi-card">
        <h3>当前市值</h3>
        <p>{{ money(overview.totalMarketValue) }}</p>
      </article>
      <article class="kpi-card">
        <h3>累计收益</h3>
        <p :class="clsByNumber(overview.totalEstimatedProfit)">{{ money(overview.totalEstimatedProfit) }}</p>
      </article>
      <article class="kpi-card">
        <h3>累计收益率</h3>
        <p :class="clsByNumber(overview.totalEstimatedProfitRate)">{{ percent(overview.totalEstimatedProfitRate) }}</p>
      </article>
      <article class="kpi-card">
        <h3>今日预估收益</h3>
        <p :class="clsByNumber(overview.totalTodayProfit)">{{ money(overview.totalTodayProfit) }}</p>
      </article>
      <article class="kpi-card">
        <h3>持仓基金数</h3>
        <p>{{ overview.fundCount }}</p>
      </article>
    </section>

    <section class="chart-grid">
      <div class="panel">
        <h3>近 7 天收益趋势</h3>
        <div ref="trendRef" class="chart-box" />
      </div>
      <div class="panel">
        <h3>基金类型分布</h3>
        <div ref="distributionRef" class="chart-box" />
      </div>
    </section>

    <section class="chart-grid">
      <div class="panel">
        <h3>收益 Top 5</h3>
        <el-table :data="ranking.profitTop" size="small">
          <el-table-column prop="fundCode" label="代码" width="100" />
          <el-table-column prop="fundName" label="基金" />
          <el-table-column label="收益">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="收益率">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="panel">
        <h3>亏损 Top 5</h3>
        <el-table :data="ranking.lossTop" size="small">
          <el-table-column prop="fundCode" label="代码" width="100" />
          <el-table-column prop="fundName" label="基金" />
          <el-table-column label="收益">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfit)">{{ money(row.estimatedProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="收益率">
            <template #default="{ row }">
              <span :class="clsByNumber(row.estimatedProfitRate)">{{ percent(row.estimatedProfitRate) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>
