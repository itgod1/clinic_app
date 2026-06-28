import { get, post } from '@/utils/request'

export interface RechargeRecord {
  id: number
  patientId: number
  patientName: string
  amount: number
  giftAmount: number
  paymentMethod: number
  paymentMethodName: string
  remark: string
  operatorId: number
  operatorName: string
  createdAt: string
}

export interface RechargeParams {
  patientId: number
  amount: number
  giftAmount?: number
  paymentMethod: number
  remark?: string
}

export const rechargeMember = (data: RechargeParams) =>
  post('/admin/member/recharge', data)

export const getRechargeRecords = (patientId: number) =>
  get<RechargeRecord[]>(`/admin/member/recharge/records/${patientId}`)
