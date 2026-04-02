<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addWatchApi, removeWatchApi, searchFundApi, watchlistApi } from '../../api/modules'
import type { FundSearchItem, WatchlistItem } from '../../types/api'
import { clsByNumber, fundTypeLabel, money, percent } from '../../utils/format'

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
    ElMessage.error(error?.message || '自选列表加载失败')
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
    ElMessage.error(error?.message || '基金搜索失败')
  } finally {
    searchLoading.value = false
  }
}

const add = async (fundCode: string) => {
  try {
    await addWatchApi(fundCode)
    ElMessage.success('已加入自选')
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '加入自选失败')
  }
}

const remove = async (fundCode: string) => {
  try {
    await removeWatchApi(fundCode)
    ElMessage.success('已移除自选')
    await load()
  } catch (error: any) {
    ElMessage.error(error?.message || '移除自选失败')
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
        <span class="eyebrow">自选池</span>
        <h2>跟踪你关注的基金，并快速跳转到完整详情页。</h2>
      </div>
      <div class="headline-actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">基金搜索</span>
          <h3>搜索公募基金</h3>
        </div>
      </div>

      <div class="search-strip">
        <el-input v-model="keyword" placeholder="输入基金代码或名称" clearable @keyup.enter="search" />
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
            {{ watchedCodes.has(item.fundCode) ? '移除' : '加入自选' }}
          </el-button>
        </article>
      </div>
    </section>

    <section class="terminal-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">实时跟踪</span>
          <h3>自选基金行情</h3>
        </div>
      </div>

      <el-table :data="watchlist" class="terminal-table clickable-table" size="small" empty-text="暂无自选基金">
        <el-table-column label="基金" min-width="240">
          <template #default="{ row }">
            <button class="fund-link-button" @click="openFund(row.fundCode)">
              <strong>{{ row.fundCode }}</strong>
              <span>{{ row.fundName }}</span>
              <em>{{ fundTypeLabel(row.fundType) }}</em>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="预估净值" width="130" align="right">
          <template #default="{ row }">{{ money(row.estimateNav) }}</template>
        </el-table-column>
        <el-table-column label="预估涨跌" width="140" align="right">
          <template #default="{ row }">
            <span :class="clsByNumber(row.estimateGrowthRate)">{{ percent((row.estimateGrowthRate || 0) / 100) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="danger" plain @click="remove(row.fundCode)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>
