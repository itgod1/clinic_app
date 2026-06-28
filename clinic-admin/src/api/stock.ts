import { get, post } from '@/utils/request'

export interface StockItem {
  id: number
  itemCode: string
  itemName: string
  itemType: number
  itemTypeName: string
  categoryId: number
  unit: string
  spec: string
  manufacturer: string
  barcode: string
  purchasePrice: number
  retailPrice: number
  memberPrice: number
  stock: number
  lowStockAlert: number
  isLowStock: boolean
  status: number
  lastInTime: string
  lastOutTime: string
}

export interface StockQuery {
  clinicId: number
  itemType?: number
  keyword?: string
  status?: number
  lowStockAlert?: boolean
  pageNum?: number
  pageSize?: number
}

export interface StockRecord {
  id: number
  clinicId: number
  itemId: number
  itemName: string
  batchNo: string
  quantity: number
  unitPrice: number
  totalAmount: number
  supplier: string
  operationType: number
  operationTypeName: string
  relatedOrderNo: string
  operatorId: number
  operatorName: string
  remark: string
  createdAt: string
}

export const getStockList = (params: StockQuery) =>
  get<{ total: number; list: StockItem[] }>('/admin/item/list', params)

export const getStockDetail = (itemId: number) =>
  get<StockItem>(`/admin/item/${itemId}`)

export const createStockItem = (data: Partial<StockItem>) =>
  post('/admin/item/create', data)

export const updateStockItem = (data: Partial<StockItem>) =>
  post('/admin/item/update', data)

export const updateItemPrice = (data: {
  itemId: number
  retailPrice: number
  memberPrice: number
  reason: string
}) => post('/admin/item/updatePrice', data)

export const stockIn = (data: {
  clinicId: number
  itemId: number
  batchNo: string
  quantity: number
  unitPrice: number
  totalAmount: number
  supplier: string
  operatorId: number
  remark?: string
}) => post('/admin/stock/in', data)

export const stockOut = (data: {
  clinicId: number
  itemId: number
  quantity: number
  operationType: number
  relatedOrderNo?: string
  operatorId: number
  remark?: string
}) => post('/admin/stock/out', data)

export const getStockInList = (params: {
  clinicId: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}) => get<{ total: number; list: StockRecord[] }>('/admin/stock/inList', params)

export const getStockOutList = (params: {
  clinicId: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}) => get<{ total: number; list: StockRecord[] }>('/admin/stock/outList', params)

export const getStockQuery = (params: StockQuery) =>
  get<{ total: number; list: StockItem[] }>('/admin/stock/query', params)

export const stockCheck = (data: {
  clinicId: number
  operatorId: number
  items: { itemId: number; actualStock: number; remark?: string }[]
}) => post('/admin/stock/check', data)