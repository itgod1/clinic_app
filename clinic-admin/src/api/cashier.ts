import { get, post } from '@/utils/request'

export interface UnpaidPrescription {
  prescriptionId: number
  prescriptionNo: string
  createTime: string
  doctorName: string
  diagnosis: string
  totalAmount: number
  discountAmount: number
  actualAmount: number
  paymentStatus: number
  statusName: string
  patientId: number
  patientName: string
}

export interface UnpaidQuery {
  clinicId: number
  pageNum?: number
  pageSize?: number
}

export const getUnpaidList = (params: UnpaidQuery) =>
  get<{ total: number; list: UnpaidPrescription[] }>('/admin/cashier/unpaid', params)

export const cashierPay = (data: {
  patientId: number
  orderId: number
  paymentMethod: number
  operatorId: number
  remark?: string
}) => post('/admin/cashier/pay', data)

export const cashierRefund = (data: {
  orderId: number
  operatorId: number
  refundReason: string
  refundAmount: number
}) => post('/admin/cashier/refund', data)

export const cashierDiscount = (data: {
  orderId: number
  discountAmount: number
  discountReason: string
  operatorId: number
}) => post('/admin/cashier/discount', data)