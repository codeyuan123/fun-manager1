export const money = (value: number | string | null | undefined) => {
  const n = Number(value ?? 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export const percent = (value: number | string | null | undefined) => {
  const n = Number(value ?? 0) * 100
  return `${n.toFixed(2)}%`
}

export const clsByNumber = (value: number | string | null | undefined) => {
  const n = Number(value ?? 0)
  if (n > 0) return 'profit'
  if (n < 0) return 'loss'
  return ''
}

export const tradeTypeLabel = (value: string | null | undefined) => {
  if (value === 'BUY') return '买入'
  if (value === 'SELL') return '卖出'
  return value || '--'
}

export const fundTypeLabel = (value: string | null | undefined) => {
  if (!value || value.toUpperCase() === 'UNKNOWN') return '未知类型'
  return value
}

export const navRangeLabel = (value: string | null | undefined) => {
  switch ((value || '').toLowerCase()) {
    case '1m':
      return '近1月'
    case '3m':
      return '近3月'
    case '6m':
      return '近6月'
    case '1y':
      return '近1年'
    case 'max':
      return '成立以来'
    default:
      return value || '--'
  }
}

export const returnLabel = (value: string | null | undefined) => {
  switch ((value || '').toUpperCase()) {
    case '1M':
      return '近1月'
    case '3M':
      return '近3月'
    case '6M':
      return '近6月'
    case '1Y':
      return '近1年'
    case 'MAX':
      return '成立以来'
    case 'RELATIVE':
      return '同类对比'
    default:
      return value || '--'
  }
}

export const estimateSourceLabel = (value: string | null | undefined) => {
  if (value === 'self_holdings') return '自算'
  if (value === 'third_party') return '第三方'
  return '--'
}

export const estimateConfidenceLabel = (value: string | null | undefined) => {
  if (value === 'high') return '高'
  if (value === 'medium') return '中'
  if (value === 'fallback') return '回退'
  return '--'
}

export const estimateConfidenceType = (value: string | null | undefined) => {
  if (value === 'high') return 'success'
  if (value === 'medium') return 'warning'
  if (value === 'fallback') return 'info'
  return 'info'
}

export const ratioPercent = (value: number | string | null | undefined) => {
  const n = Number(value ?? 0)
  return `${(n * 100).toFixed(2)}%`
}
