import { get, post } from '@/utils/request'

export interface Schedule {
  id?: number
  clinicId: number
  doctorId: number
  scheduleDate: string
  amIsWork: boolean
  amStartTime?: string
  amEndTime?: string
  pmIsWork: boolean
  pmStartTime?: string
  pmEndTime?: string
  limitNum: number
}

export interface ScheduleQuery {
  clinicId: number
  doctorId?: number
  startDate: string
  endDate: string
}

export const getScheduleList = (params: ScheduleQuery) =>
  get<Schedule[]>('/admin/schedule/list', params)

export const batchSetSchedule = (data: {
  clinicId: number
  doctorId: number
  schedules: Schedule[]
}) => post('/admin/schedule/batchSet', data)
