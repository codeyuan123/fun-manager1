<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addWatchApi, removeWatchApi, searchFundApi, watchlistApi } from '../../api/modules'
import type { FundSearchItem, WatchlistItem } from '../../types/api'
import { clsByNumber, money, percent } from '../../utils/format'

const loading = ref(false)
const keyword = ref('')
const candidates = ref<FundSearchItem[]>([])
const watchlist = ref<WatchlistItem[]>([])

const load = async () => {
  loading.value = true
  try {
    const resp = await watchlistApi()
    watchlist.value = resp.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '自选加载失败')
  } finally {
    loading.value = false
  }
}

const search = async () => {
  if (!keyword.value.trim()) {
    candidates.value = []
    return
  }
  try {
    const resp = await searchFundApi(keyword.value.trim())
    candidates.value = resp.data.data
  } catch (error: any) {
    ElMessage.error(error?.message || '基金搜索失败')
  }
}

const add = async (fundCode: string) => {
  try {
    await addWatchApi(fundCode)
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

load()
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="page-actions">
      <el-input v-model="keyword" placeholder="输入基金代码或名称" style="max-width: 300px" @keyup.enter="search" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="load">刷新自选</el-button>
    </div>

    <el-table :data="candidates" size="small" style="margin-bottom: 18px">
      <el-table-column prop="fundCode" label="代码" width="120" />
      <el-table-column prop="fundName" label="基金名称" />
      <el-table-column prop="fundType" label="类型" width="120" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="add(row.fundCode)">加入</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-table :data="watchlist">
      <el-table-column prop="fundCode" label="代码" width="120" />
      <el-table-column prop="fundName" label="基金名称" />
      <el-table-column prop="fundType" label="类型" width="120" />
      <el-table-column label="估值">
        <template #default="{ row }">{{ money(row.estimateNav) }}</template>
      </el-table-column>
      <el-table-column label="涨跌幅">
        <template #default="{ row }">
          <span :class="clsByNumber(row.estimateGrowthRate)">{{ percent((row.estimateGrowthRate || 0) / 100) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="estimateTime" label="估值时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="remove(row.fundCode)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
