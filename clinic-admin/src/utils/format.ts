export const formatMoney = (amount: number | string, decimals: number = 2): string => {
  if (amount === null || amount === undefined || amount === '') return '¥0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '¥0.00'
  return `¥${num.toFixed(decimals)}`
}

export const parseMoney = (money: string): number => {
  if (!money) return 0
  const num = parseFloat(money.replace(/[^0-9.-]/g, ''))
  return isNaN(num) ? 0 : num
}

export const formatNumber = (num: number | string, decimals: number = 0): string => {
  if (num === null || num === undefined || num === '') return '0'
  const n = typeof num === 'string' ? parseFloat(num) : num
  if (isNaN(n)) return '0'
  return n.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export const formatPercent = (num: number | string, decimals: number = 0): string => {
  if (num === null || num === undefined || num === '') return '0%'
  const n = typeof num === 'string' ? parseFloat(num) : num
  if (isNaN(n)) return '0%'
  return `${(n * 100).toFixed(decimals)}%`
}

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}