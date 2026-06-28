import { get, post, put, del } from '@/utils/request'

export interface Doctor {
  id: number
  clinicId: number
  deptId: number
  deptName: string
  doctorName: string
  doctorCode: string
  title: number
  titleName: string
  specialty: string
  phone: string
  avatarUrl: string
  introduction: string
  workStatus: number
  serviceCount: number
  rating: number
  status: number
}

export interface DoctorQuery {
  clinicId: number
  deptId?: number
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface DoctorListVO {
  total: number
  records: Doctor[]
}

export const getDoctorList = (params: DoctorQuery) =>
  get<DoctorListVO>('/admin/doctor/list', params)

export const getDoctorInfo = (id: number) =>
  get<Doctor>(`/admin/doctor/${id}`)

export const createDoctor = (data: Partial<Doctor>) =>
  post('/admin/doctor/create', data)

export const updateDoctor = (data: Partial<Doctor>) =>
  post('/admin/doctor/update', data)

export const deleteDoctor = (id: number) =>
  post('/admin/doctor/delete', { doctorId: id })