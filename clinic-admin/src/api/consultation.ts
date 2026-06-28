import { get, post } from '@/utils/request'

export interface ConsultationRecord {
  id?: number
  recordNo: string
  recordType: number
  recordTypeName: string
  patientId?: number
  patientName: string
  patientPhone: string
  doctorId?: number
  doctorName: string
  deptId?: number
  deptName: string
  visitDate: string
  consultationContent: string
  designRequirements: string
  suggestion: string
  status: number
  statusName: string
  attachUrls: string
  notes: string
  remindCount: number
  remindedAt: string
  nextVisitDate: string
  createdAt: string
  updatedAt: string
}

export interface ConsultationQuery {
  clinicId: number
  recordType?: number
  doctorId?: number
  startDate?: string
  endDate?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export const getConsultationList = (params: ConsultationQuery) =>
  get<{ total: number; list: ConsultationRecord[] }>('/admin/consultation/list', params)

export const getConsultationDetail = (id: number) =>
  get<ConsultationRecord>(`/admin/consultation/${id}`)

export const createConsultation = (data: Partial<ConsultationRecord>) =>
  post('/admin/consultation/create', data)

export const updateConsultation = (data: Partial<ConsultationRecord>) =>
  post('/admin/consultation/update', data)

export const deleteConsultation = (id: number) =>
  post(`/admin/consultation/delete?id=${id}`)

export const getUpcomingRevisits = (params: { clinicId: number; doctorId?: number }) =>
  get<ConsultationRecord[]>('/admin/consultation/upcoming-revisits', params)

export const remindConsultation = (id: number) =>
  post(`/admin/consultation/remind/${id}`)

export const completeConsultation = (id: number) =>
  post(`/admin/consultation/complete/${id}`)
