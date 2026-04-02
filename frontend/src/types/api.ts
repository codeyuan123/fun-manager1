export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface LoginVO {
  token: string
  username: string
  nickname: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
}

export interface PositionItem {
  fundCode: string
  fundName: string
  fundType: string
  totalShares: number
  currentCost: number
  currentNav: number
  marketValue: number
  estimatedProfit: number
  estimatedProfitRate: number
  todayProfit: number
  lastTradeDate: string
}

export interface TransactionItem {
  id: number
  transactionType: string
  tradeDate: string
  amount: number
  shares: number
  fee: number
  nav: number
  remark: string
  createdAt: string
}

export interface WatchlistItem {
  fundCode: string
  fundName: string
  fundType: string
  estimateNav: number
  estimateGrowthRate: number
  estimateTime: string
}

export interface DashboardOverview {
  totalCost: number
  totalMarketValue: number
  totalEstimatedProfit: number
  totalEstimatedProfitRate: number
  totalTodayProfit: number
  fundCount: number
}

export interface TrendPoint {
  date: string
  profit: number
}

export interface DistributionItem {
  category: string
  cost: number
  marketValue: number
}

export interface RankingItem {
  fundCode: string
  fundName: string
  estimatedProfit: number
  estimatedProfitRate: number
}

export interface RankingPayload {
  profitTop: RankingItem[]
  lossTop: RankingItem[]
}

export interface FundSearchItem {
  fundCode: string
  fundName: string
  fundType: string
}

export interface FundEstimate {
  fundCode: string
  fundName: string
  estimateNav: number
  estimateGrowthRate: number
  estimateTime: string
}

export interface FundReturnStat {
  label: string
  value: number | null
}

export interface FundNavPoint {
  navDate: string
  unitNav: number
  accumulatedNav: number | null
  dailyGrowthRate: number | null
}

export interface FundHoldingItem {
  stockCode: string
  stockName: string
  navRatio: number | null
  holdingShares: number | null
  holdingMarketValue: number | null
  reportDate: string | null
}

export interface FundChartSeries {
  name: string | null
  data: Array<number | null>
}

export interface FundChartBlock {
  categories: string[]
  series: FundChartSeries[]
}

export interface FundScalePoint {
  date: string
  value: number | null
  mom: string | null
}

export interface FundPerformanceRadar {
  average: string | null
  categories: string[]
  data: Array<number | null>
}

export interface FundManagerCard {
  id: string | null
  name: string | null
  avatar: string | null
  star: number | null
  workTime: string | null
  fundSize: string | null
  power: FundPerformanceRadar
  profitComparison: FundChartBlock
}

export interface FundPeerReference {
  fundCode: string
  fundName: string
  returnRate: number | null
}

export interface FundDetail {
  fundCode: string
  fundName: string
  fundType: string
  riskLevel: string | null
  managementCompany: string | null
  latestNav: number | null
  latestNavDate: string | null
  estimateNav: number | null
  estimateGrowthRate: number | null
  estimateTime: string | null
  sourceRate: number | null
  currentRate: number | null
  minPurchaseAmount: number | null
  returnStats: FundReturnStat[]
  performanceRadar: FundPerformanceRadar
  managers: FundManagerCard[]
  assetAllocation: FundChartBlock
  holderStructure: FundChartBlock
  scaleTrend: FundScalePoint[]
  sameTypeReferences: FundPeerReference[]
}
