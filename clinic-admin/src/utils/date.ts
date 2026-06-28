import dayjs from 'dayjs'

export const formatDate = (date: string | Date | number, format: string = 'YYYY-MM-DD'): string => {
  if (!date) return ''
  return dayjs(date).format(format)
}

export const formatDateTime = (date: string | Date | number, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!date) return ''
  return dayjs(date).format(format)
}

export const formatTime = (date: string | Date | number, format: string = 'HH:mm'): string => {
  if (!date) return ''
  return dayjs(date).format(format)
}

export const parseDate = (date: string | Date | number): Date => {
  return dayjs(date).toDate()
}

export const getDateRange = (days: number): [string, string] => {
  const end = dayjs().format('YYYY-MM-DD')
  const start = dayjs().subtract(days, 'day').format('YYYY-MM-DD')
  return [start, end]
}

export const isToday = (date: string | Date | number): boolean => {
  return dayjs(date).isSame(dayjs(), 'day')
}

export const isSameDay = (date1: any, date2: any): boolean => {
  return dayjs(date1).isSame(dayjs(date2), 'day')
}