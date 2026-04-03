export type StrategyFieldType = 'number' | 'select'

export interface StrategyFieldOption {
  label: string
  value: string | number
}

export interface StrategyField {
  key: string
  label: string
  type: StrategyFieldType
  min?: number
  step?: number
  options?: StrategyFieldOption[]
}

export interface StrategyDefinition {
  code: string
  name: string
  fields: StrategyField[]
  defaults: Record<string, number | string>
}

export const strategyDefinitions: StrategyDefinition[] = [
  {
    code: 'lump_sum',
    name: '一次性买入',
    fields: [],
    defaults: {},
  },
  {
    code: 'dca_daily',
    name: '日定投',
    fields: [{ key: 'periodicAmount', label: '每日金额', type: 'number', min: 1, step: 100 }],
    defaults: { periodicAmount: 100 },
  },
  {
    code: 'dca_weekly',
    name: '周定投',
    fields: [
      { key: 'periodicAmount', label: '每周金额', type: 'number', min: 1, step: 100 },
      {
        key: 'weekday',
        label: '星期',
        type: 'select',
        options: [
          { label: '周一', value: 1 },
          { label: '周二', value: 2 },
          { label: '周三', value: 3 },
          { label: '周四', value: 4 },
          { label: '周五', value: 5 },
        ],
      },
    ],
    defaults: { periodicAmount: 500, weekday: 1 },
  },
  {
    code: 'dca_monthly',
    name: '月定投',
    fields: [
      { key: 'periodicAmount', label: '每月金额', type: 'number', min: 1, step: 100 },
      { key: 'dayOfMonth', label: '扣款日', type: 'number', min: 1, step: 1 },
    ],
    defaults: { periodicAmount: 1000, dayOfMonth: 8 },
  },
  {
    code: 'drawdown_add',
    name: '回撤加仓',
    fields: [
      { key: 'baseAmount', label: '基础月投', type: 'number', min: 1, step: 100 },
      { key: 'extraAmount', label: '额外加仓', type: 'number', min: 1, step: 100 },
      { key: 'drawdownThreshold', label: '回撤阈值', type: 'number', min: 0.01, step: 0.01 },
      { key: 'drawdownWindowDays', label: '回撤窗口', type: 'number', min: 5, step: 5 },
      { key: 'dayOfMonth', label: '扣款日', type: 'number', min: 1, step: 1 },
    ],
    defaults: { baseAmount: 1000, extraAmount: 2000, drawdownThreshold: 0.1, drawdownWindowDays: 60, dayOfMonth: 8 },
  },
  {
    code: 'ma_timing',
    name: '均线择时',
    fields: [{ key: 'maPeriod', label: '均线天数', type: 'number', min: 5, step: 1 }],
    defaults: { maPeriod: 20 },
  },
  {
    code: 'nav_percentile_dca',
    name: '净值分位定投',
    fields: [
      { key: 'baseAmount', label: '基础月投', type: 'number', min: 1, step: 100 },
      { key: 'windowDays', label: '分位窗口', type: 'number', min: 30, step: 10 },
      { key: 'mediumPercentile', label: '中位阈值', type: 'number', min: 0.01, step: 0.01 },
      { key: 'deepPercentile', label: '深位阈值', type: 'number', min: 0.01, step: 0.01 },
      { key: 'mediumMultiplier', label: '中位倍数', type: 'number', min: 1, step: 1 },
      { key: 'deepMultiplier', label: '深位倍数', type: 'number', min: 1, step: 1 },
      { key: 'dayOfMonth', label: '扣款日', type: 'number', min: 1, step: 1 },
    ],
    defaults: {
      baseAmount: 1000,
      windowDays: 250,
      mediumPercentile: 0.3,
      deepPercentile: 0.15,
      mediumMultiplier: 2,
      deepMultiplier: 3,
      dayOfMonth: 8,
    },
  },
  {
    code: 'grid_add',
    name: '网格加仓',
    fields: [
      { key: 'baseAmount', label: '基准建仓', type: 'number', min: 1, step: 100 },
      { key: 'gridStep', label: '网格跌幅', type: 'number', min: 0.01, step: 0.01 },
      { key: 'gridAmount', label: '每格加仓', type: 'number', min: 1, step: 100 },
      { key: 'maxGrids', label: '最大档数', type: 'number', min: 1, step: 1 },
    ],
    defaults: { baseAmount: 10000, gridStep: 0.05, gridAmount: 2000, maxGrids: 5 },
  },
]

export const strategyMap = Object.fromEntries(strategyDefinitions.map((item) => [item.code, item]))
