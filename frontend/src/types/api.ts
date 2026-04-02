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
