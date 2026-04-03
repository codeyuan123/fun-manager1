<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addWatchApi, refreshEstimatesApi, removeWatchApi, searchFundApi, watchlistApi } from '../../api/modules'
import type { FundSearchItem, WatchlistItem } from '../../types/api'
import {
  clsByNumber,
  estimateConfidenceLabel,
  estimateConfidenceType,
  estimateSourceLabel,
  fundTypeLabel,
  percent,
} from '../../utils/format'

const router = useRouter()
const loading = ref(false)
const searchLoading = ref(false)
const keyword = ref('')
const candidates = ref<FundSearchItem[]>([])
const watchlist = ref<WatchlistItem[]>([])

const watchedCodes = computed(() => new Set(watchlist.value.map((item) => item.fundCode)))

const load = async () => {
  loading.value = true
  try {
    const response = await watchlistApi()
    watchlist.value = response.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '自选加载失败')
  } finally {
    loading.value = false
  }
}

const search = async () => {
  const value = keyword.value.trim()
  if (!value) {
    candidates.value = []
    return
  }
  searchLoading.value = true
  try {
    const response = await searchFundApi(value)
    candidates.value = response.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '搜索失败')
  } finally {
    searchLoading.value = false
  }
}

const add = async (fundCode: string) => {
  try {
    await addWatchApi(fundCode)
    await refreshEstimatesApi([fundCode])
    ElMessage.success('已加入自选')
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '加入失败')
  }
}

const remove = async (fundCode: string) => {
  try {
    await removeWatchApi(fundCode)
    ElMessage.success('已移除')
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '移除失败')
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
        <span class="eyebrow">Watchlist</span>
        <h2>自选基金</h2>
      </div>
      <div class="headline-actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>基金搜索</h3>
      </div>

      <div class="search-strip">
        <el-input v-model="keyword" placeholder="代码 / 名称" clearable @keyup.enter="search" />
        <el-button type="primary" :loading="searchLoading" @click="search">搜索</el-button>
      </div>

      <div class="candidate-grid">
        <article v-for="item in candidates" :key="item.fundCode" class="candidate-card">
          <button class="fund-link-button" @click="openFund(item.fundCode)">
            <strong>{{ item.fundCode }}</strong>
            <span>{{ item.fundName }}</span>
            <em>{{ fundTypeLabel(item.fundType) }}</em>
          </button>
          <el-button
            size="small"
            :type="watchedCodes.has(item.fundCode) ? 'info' : 'primary'"
            @click="watchedCodes.has(item.fundCode) ? remove(item.fundCode) : add(item.fundCode)"
          >
            {{ watchedCodes.has(item.fundCode) ? '移除' : '加入' }}
          </el-button>
        </article>
      </div>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <h3>自选列表</h3>
      </div>

      <el-table :data="watchlist" class="terminal-table clickable-table" size="small" empty-text="暂无自选">
        <el-table-column label="基金" min-width="220">
          <template #default="{ row }">
            <button class="fund-link-button" @click="openFund(row.fundCode)">
              <strong>{{ row.fundCode }}</strong>
              <span>{{ row.fundName }}</span>
              <em>{{ fundTypeLabel(row.fundType) }}</em>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="估值" width="110" align="right">
          <template #default="{ row }">{{ Number(row.estimateNav || 0).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="涨跌" width="120" align="right">
          <template #default="{ row }">
            <span :class="clsByNumber(row.estimateGrowthRate)">{{ percent((row.estimateGrowthRate || 0) / 100) }}</span>
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
        <el-table-column prop="estimateTime" label="时间" width="170" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="danger" plain @click="remove(row.fundCode)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>
