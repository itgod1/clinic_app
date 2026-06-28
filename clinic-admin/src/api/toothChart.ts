import { get, post } from '@/utils/request'

export interface ToothChartRecord {
  id?: number
  clinicId?: number
  medicalRecordId: number
  toothNumber: string
  conditionType: string
  surface?: string
  note?: string
}

export const getToothChart = (medicalRecordId: number) =>
  get<ToothChartRecord[]>(`/admin/tooth-chart/${medicalRecordId}`)

export const saveToothChart = (data: { medicalRecordId: number; clinicId: number; records: ToothChartRecord[] }) =>
  post('/admin/tooth-chart/save', data)
