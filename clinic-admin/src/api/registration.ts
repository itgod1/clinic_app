import { get, post } from '@/utils/request'

export interface Registration {
  id: number
  clinicId: number
  patientId: number
  patientName: string
  patientPhone: string
  doctorId: number
  doctorName: string
  deptId: number
  deptName: string
  regNo: string
  regDate: string
  regTime: string
  queueNo: number
  visitType: number
  visitTypeName: string
  status: number
  statusName: string
  chiefComplaint: string
}

export interface RegistrationQuery {
  clinicId: number
  regDate?: string
  doctorId?: number
  status?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface RegistrationListVO {
  total: number
  list: Registration[]
}

export const getRegistrationList = (params: RegistrationQuery) =>
  get<RegistrationListVO>('/admin/registration/list', params)

export const cancelRegistration = (data: { id: number; refundReason?: string }) =>
  post('/admin/registration/cancel', data)

export const changeRegistration = (data: {
  regId: number
  newDoctorId: number
  newScheduleId: number
  operatorId: number
}) => post('/admin/registration/change', data)

export const createRegistration = (data: Partial<Registration>) =>
  post('/admin/registration/create', data)