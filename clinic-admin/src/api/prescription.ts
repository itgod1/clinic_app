import { get, post } from '@/utils/request'

export interface Prescription {
  id?: number
  prescriptionId?: number
  prescriptionNo?: string
  prescriptionType: number
  prescriptionTypeName?: string
  registrationId?: number
  patientId?: number
  patientName: string
  doctorId?: number
  doctorName?: string
  deptId?: number
  departmentName?: string
  diagnosis: string
  totalAmount?: number
  discountAmount?: number
  actualAmount?: number
  paymentStatus?: number
  statusName?: string
  items: PrescriptionItem[]
  createTime?: string
}

export interface PrescriptionItem {
  itemId?: number
  itemType: number
  itemTypeName?: string
  itemName: string
  spec?: string
  unit: string
  quantity: number
  unitPrice: number
  subtotal: number
  usage?: string
  frequency?: string
  duration?: string
  remark?: string
}

export interface PrescriptionQuery {
  clinicId: number
  doctorId?: number
  prescriptionType?: number
  paymentStatus?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export const getPrescriptionList = (params: PrescriptionQuery) =>
  get<{ total: number; list: Prescription[] }>('/admin/prescription/list', params)

export const getPrescriptionDetail = (prescriptionId: number) =>
  get<Prescription>(`/admin/prescription/${prescriptionId}`)

export const createPrescription = (data: Prescription) =>
  post<{ prescriptionId: number; prescriptionNo: string }>('/admin/prescription/create', data)

export const refundPrescription = (data: {
  prescriptionId: number
  operatorId: number
  refundReason: string
}) => post('/admin/prescription/refund', data)

export const dispensePrescription = (data: {
  prescriptionId: number
  operatorId: number
  remark?: string
}) => post('/admin/prescription/dispense', data)
