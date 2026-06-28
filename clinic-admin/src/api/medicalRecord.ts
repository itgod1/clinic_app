import { get, post } from '@/utils/request'

export interface MedicalRecord {
  id?: number
  recordId?: number
  recordNo: string
  patientId: number
  patientName: string
  patientPhone: string
  registrationId: number
  doctorId: number
  doctorName: string
  deptId: number
  deptName: string
  visitType: number
  visitTypeName: string
  visitDate: string
  chiefComplaint: string
  diagnosis: string
  treatment: string
  medicalAdvice: string
  attachUrls: string
  createTime: string
  updateTime: string
}

export interface MedicalRecordQuery {
  clinicId: number
  patientId?: number
  doctorId?: number
  startDate?: string
  endDate?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export const getMedicalRecordList = (params: MedicalRecordQuery) =>
  get<{ total: number; list: MedicalRecord[] }>('/admin/medicalRecord/list', params)

export const getMedicalRecordDetail = (recordId: number) =>
  get<MedicalRecord>(`/admin/medicalRecord/${recordId}`)

export const createMedicalRecord = (data: Partial<MedicalRecord>) =>
  post('/admin/medicalRecord/create', data)

export const updateMedicalRecord = (data: Partial<MedicalRecord>) =>
  post('/admin/medicalRecord/update', data)

export const deleteMedicalRecord = (recordId: number) =>
  post('/admin/medicalRecord/delete', { recordId })