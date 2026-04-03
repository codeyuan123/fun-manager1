import http from './http'
import type {
  ApiResponse,
  BacktestResult,
  DashboardOverview,
  DistributionItem,
  EstimateRefreshSummary,
  FundDetail,
  FundEstimate,
  FundEstimateHistoryPoint,
  FundHoldingItem,
  FundNavPoint,
  FundSearchItem,
  LoginVO,
  PositionItem,
  RankingPayload,
  TransactionItem,
  TrendPoint,
  UserInfo,
  WatchlistItem,
} from '../types/api'

export const loginApi = (payload: { username: string; password: string }) =>
  http.post<ApiResponse<LoginVO>>('/auth/login', payload)

export const meApi = () => http.get<ApiResponse<UserInfo>>('/auth/me')

export const changePasswordApi = (payload: {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}) => http.post<ApiResponse<null>>('/auth/change-password', payload)

export const positionsApi = () => http.get<ApiResponse<PositionItem[]>>('/positions')

export const buyApi = (payload: {
  fundCode: string
  fundName?: string
  amount: number
  shares?: number
  fee?: number
  nav: number
  tradeDate?: string
  remark?: string
}) => http.post<ApiResponse<null>>('/positions/transactions/buy', payload)

export const sellApi = (payload: {
  fundCode: string
  fundName?: string
  amount: number
  shares?: number
  fee?: number
  nav: number
  tradeDate?: string
  remark?: string
}) => http.post<ApiResponse<null>>('/positions/transactions/sell', payload)

export const transactionsApi = (fundCode: string) =>
  http.get<ApiResponse<TransactionItem[]>>(`/positions/${fundCode}/transactions`)

export const watchlistApi = () => http.get<ApiResponse<WatchlistItem[]>>('/watchlist')

export const addWatchApi = (fundCode: string) => http.post<ApiResponse<null>>(`/watchlist/${fundCode}`)

export const removeWatchApi = (fundCode: string) => http.delete<ApiResponse<null>>(`/watchlist/${fundCode}`)

export const overviewApi = () => http.get<ApiResponse<DashboardOverview>>('/dashboard/overview')

export const trendApi = () => http.get<ApiResponse<TrendPoint[]>>('/dashboard/trend')

export const distributionApi = () => http.get<ApiResponse<DistributionItem[]>>('/dashboard/distribution')

export const rankingApi = () => http.get<ApiResponse<RankingPayload>>('/dashboard/ranking')

export const refreshEstimatesApi = (fundCodes?: string[]) =>
  http.post<ApiResponse<EstimateRefreshSummary>>('/estimates/refresh', fundCodes?.length ? { fundCodes } : {})

export const searchFundApi = (keyword: string) =>
  http.get<ApiResponse<FundSearchItem[]>>('/funds/search', { params: { keyword } })

export const estimateApi = (fundCode: string) =>
  http.get<ApiResponse<FundEstimate>>(`/funds/${fundCode}/estimate`)

export const estimateHistoryApi = (fundCode: string, date?: string) =>
  http.get<ApiResponse<FundEstimateHistoryPoint[]>>(`/funds/${fundCode}/estimate-history`, {
    params: { date },
  })

export const fundDetailApi = (fundCode: string) =>
  http.get<ApiResponse<FundDetail>>(`/funds/${fundCode}`)

export const fundNavHistoryApi = (fundCode: string, range: '1m' | '3m' | '6m' | '1y' | 'max' = '6m') =>
  http.get<ApiResponse<FundNavPoint[]>>(`/funds/${fundCode}/nav-history`, { params: { range } })

export const fundHoldingsApi = (fundCode: string, year?: number, quarter?: number) =>
  http.get<ApiResponse<FundHoldingItem[]>>(`/funds/${fundCode}/holdings`, {
    params: {
      year,
      quarter,
    },
  })

export const runBacktestStrategiesApi = (payload: {
  fundCode: string
  startDate: string
  endDate: string
  executionMode: string
  initialCapital: number
  strategyCodes: string[]
  strategyParams: Record<string, Record<string, unknown>>
}) => http.post<ApiResponse<BacktestResult[]>>('/backtests/strategies/run', payload)

export const runBacktestFundsApi = (payload: {
  fundCodes: string[]
  startDate: string
  endDate: string
  executionMode: string
  initialCapital: number
  strategyCode: string
  strategyParams: Record<string, unknown>
}) => http.post<ApiResponse<BacktestResult[]>>('/backtests/funds/run', payload)
