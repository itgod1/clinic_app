import { get, post } from '@/utils/request'

export interface Patient {
  id: number
  clinicId: number
  patientName: string
  patientCode: string
  phone: string
  gender: number
  genderName: string
  age: number
  birthday: string
  memberLevel: number
  memberLevelName: string
  balance: number
  points: number
  registerSource: number
  lastVisitDate: string
  visitCount: number
  status: number
  allergyHistory: string
  medicalHistory: string
}

export interface PatientQuery {
  clinicId: number
  keyword?: string
  memberLevel?: number
  registerSource?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface PatientListVO {
  total: number
  list: Patient[]
}

export const getPatientList = (params: PatientQuery) =>
  get<PatientListVO>('/admin/patient/list', params)

export const getPatientInfo = (id: number) =>
  get<Patient>(`/admin/patient/${id}`)

export const updatePatientStatus = (data: { patientId: number; status: number }) =>
  post('/admin/patient/updateStatus', data)

export const createPatient = (data: Partial<Patient>) =>
  post('/admin/patient/create', data)

export const updatePatient = (data: Partial<Patient>) =>
  post('/admin/patient/update', data)

export const deletePatient = (patientId: number) =>
  post('/admin/patient/delete', null, { params: { patientId } })