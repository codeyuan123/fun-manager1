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
