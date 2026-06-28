import { get, post } from '@/utils/request'

export interface OperationLog {
  id: number
  clinicId: number
  userId: number
  username: string
  realName: string
  module: string
  operationType: number
  operationTypeName?: string
  description: string
  requestMethod: string
  requestUrl: string
  requestParams?: string
  responseData?: string
  ipAddress: string
  location?: string
  userAgent?: string
  status: number
  errorMsg?: string
  executionTime?: number
  operationTime: string
  createdAt: string
}

export interface OperationLogQuery {
  clinicId: number
  module?: string
  operationType?: number
  username?: string
  startTime?: string
  endTime?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

// 操作类型选项
export const operationTypeOptions = [
  { label: '新增', value: 1 },
  { label: '修改', value: 2 },
  { label: '删除', value: 3 },
  { label: '查询', value: 4 },
  { label: '导出', value: 5 },
  { label: '登录', value: 6 },
  { label: '登出', value: 7 },
  { label: '其他', value: 8 },
  { label: '审核', value: 9 },
  { label: '导入', value: 10 },
  { label: '打印', value: 11 },
  { label: '支付', value: 12 },
  { label: '退费', value: 13 }
]

// 获取操作类型名称
export const getOperationTypeName = (type: number) => {
  const item = operationTypeOptions.find(o => o.value === type)
  return item?.label || '其他'
}

// 分页查询操作日志
export const getOperationLogList = (params: OperationLogQuery) =>
  get<{ total: number; list: OperationLog[] }>('/admin/operation-log/list', params)

// 批量删除日志
export const deleteOperationLogs = (ids: number[]) =>
  post('/admin/operation-log/delete', ids)

// 清理日志
export const cleanOperationLogs = (days: number) =>
  post(`/admin/operation-log/clean?days=${days}`)
