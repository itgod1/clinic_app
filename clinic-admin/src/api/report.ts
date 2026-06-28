import { get } from '@/utils/request'

export interface DashboardStats {
  todayDate: string
  todayRegCount: number
  todayRegChange: number
  waitingCount: number
  waitingChange: number
  unpaidCount: number
  todayRevenue: number
  todayRevenueChange: number
  weekRegTrend: { date: string; count: number }[]
  weekRevenueTrend: { date: string; amount: number }[]
  todayTopDoctors: { doctorName: string; count: number; revenue: number }[]
  todayTopItems: { itemName: string; count: number; amount: number }[]
  lowStockItems: { itemName: string; stock: number; lowStockAlert: number }[]
}

export interface DailyReport {
  reportDate: string
  regCount: number
  regCancelCount: number
  actualVisitCount: number
  newPatientCount: number
  oldPatientCount: number
  prescriptionCount: number
  prescriptionAmount: number
  discountAmount: number
  actualAmount: number
  cashAmount: number
  wechatAmount: number
  alipayAmount: number
  cardAmount: number
  revenueDetails: { category: string; count: number; amount: number }[]
}

export interface MonthlyReport {
  year: number
  month: number
  regCount: number
  regCancelCount: number
  actualVisitCount: number
  newPatientCount: number
  oldPatientCount: number
  prescriptionCount: number
  prescriptionAmount: number
  discountAmount: number
  actualAmount: number
}

export const getDashboardStats = (clinicId: number) =>
  get<DashboardStats>('/admin/report/dashboard', { clinicId })

export const getDailyReport = (params: { clinicId: number; date: string }) =>
  get<DailyReport>('/admin/report/daily', params)

export const getMonthlyReport = (params: { clinicId: number; year: number; month: number }) =>
  get<MonthlyReport>('/admin/report/monthly', params)

export const getDeptRanking = (params: {
  clinicId: number
  startDate: string
  endDate: string
}) => get<any>('/admin/report/deptRanking', params)

export const getDoctorPerformance = (params: {
  clinicId: number
  startDate: string
  endDate: string
  doctorId?: number
}) => get<any>('/admin/report/doctorPerformance', params)

export const getMedicineConsume = (params: {
  clinicId: number
  startDate: string
  endDate: string
}) => get<any>('/admin/report/medicineConsume', params)