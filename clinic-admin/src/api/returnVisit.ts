import { get, post } from '@/utils/request'

export interface ReturnVisitPlan {
  id?: number
  clinicId: number
  patientId: number
  patientName: string
  patientPhone?: string
  registrationId?: number
  medicalRecordId?: number
  visitItem?: string
  planDate: string
  planTime?: string
  visitType?: string
  visitTypeName?: string
  contentTemplate?: string
  assigneeId?: number
  assigneeName?: string
  status?: string
  statusName?: string
  priority?: number
  createdAt?: string
  createdAtStr?: string
}

export interface ReturnVisitRecord {
  id?: number
  planId?: number
  clinicId: number
  patientId: number
  patientName: string
  patientPhone?: string
  registrationId?: number
  medicalRecordId?: number
  doctorId?: number
  doctorName?: string
  visitDate?: string
  visitType?: string
  visitTypeName?: string
  content?: string
  result?: string
  resultName?: string
  satisfaction?: number
  nextPlanId?: number
  recordUrl?: string
  remark?: string
  createdAt?: string
  createdAtStr?: string
}

export interface ReturnVisitTemplate {
  id?: number
  clinicId?: number
  name: string
  visitType?: string
  visitTypeName?: string
  contentTemplate?: string
  resultOptions?: string
  sortOrder?: number
  status?: string
}

export interface ReturnVisitRule {
  id?: number
  clinicId: number
  ruleName: string
  conditionType?: string
  conditionValue?: string
  assigneeId?: number
  assigneeName?: string
  assigneeRole?: string
  priority?: number
  autoCreate?: number
  status?: string
}

export interface PlanQuery {
  clinicId: number
  status?: string
  startDate?: string
  endDate?: string
  keyword?: string
  assigneeId?: number
  pageNum?: number
  pageSize?: number
}

export interface RecordQuery {
  clinicId: number
  patientId?: number
  doctorId?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface PlanStatistics {
  pending: number
  executed: number
  cancelled: number
  todayTotal: number
  overdue: number
}

export const getPlanList = (params: PlanQuery) =>
  get('/admin/returnVisit/plan/list', params)

export const getPlanById = (id: number) =>
  get<ReturnVisitPlan>(`/admin/returnVisit/plan/${id}`)

export const createPlan = (data: ReturnVisitPlan) =>
  post('/admin/returnVisit/plan/create', data)

export const updatePlan = (data: ReturnVisitPlan) =>
  post('/admin/returnVisit/plan/update', data)

export const cancelPlan = (id: number) =>
  post('/admin/returnVisit/plan/cancel', { id })

export const getTodayPlans = (clinicId: number) =>
  get<ReturnVisitPlan[]>('/admin/returnVisit/plan/today', { clinicId })

export const getOverduePlans = (clinicId: number) =>
  get<ReturnVisitPlan[]>('/admin/returnVisit/plan/overdue', { clinicId })

export const getPlanStatistics = (clinicId: number) =>
  get<PlanStatistics>('/admin/returnVisit/plan/statistics', { clinicId })

export const getRecordList = (params: RecordQuery) =>
  get('/admin/returnVisit/record/list', params)

export const getRecordById = (id: number) =>
  get<ReturnVisitRecord>(`/admin/returnVisit/record/${id}`)

export const createRecord = (data: ReturnVisitRecord) =>
  post('/admin/returnVisit/record/create', data)

export const getTemplateList = (clinicId?: number) =>
  get<ReturnVisitTemplate[]>('/admin/returnVisit/template/list', { clinicId })

export const createTemplate = (data: ReturnVisitTemplate) =>
  post('/admin/returnVisit/template/create', data)

export const updateTemplate = (data: ReturnVisitTemplate) =>
  post('/admin/returnVisit/template/update', data)

export const deleteTemplate = (id: number) =>
  post('/admin/returnVisit/template/delete', { id })

export const getRuleList = (clinicId: number) =>
  get<ReturnVisitRule[]>('/admin/returnVisit/rule/list', { clinicId })

export const createRule = (data: ReturnVisitRule) =>
  post('/admin/returnVisit/rule/create', data)

export const updateRule = (data: ReturnVisitRule) =>
  post('/admin/returnVisit/rule/update', data)

export const deleteRule = (id: number) =>
  post('/admin/returnVisit/rule/delete', { id })

export const toggleRule = (id: number) =>
  post('/admin/returnVisit/rule/toggle', { id })

// ========== 复诊提醒相关接口 ==========

export interface RevisitPlanQuery {
  clinicId: number
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface RevisitPlan extends ReturnVisitPlan {
  planType?: string
  recoveryDays?: number
  originalTreatment?: string
  remindedAt?: string
  remindedTo?: number
  remindCount?: number
  appointedAt?: string
  appointedBy?: number
  appointedByName?: string
  appointmentNote?: string
  actualDate?: string
  daysUntilDue?: number
}

export interface RevisitStatistics {
  pending: number
  reminded: number
  appointed: number
  executed: number
  cancelled: number
  todayDue: number
  overdue: number
}

export const getRevisitList = (params: RevisitPlanQuery) =>
  get('/admin/returnVisit/revisit/list', params)

export const getRevisitDashboard = (clinicId: number) =>
  get('/admin/returnVisit/revisit/dashboard', { clinicId })

export const getTodayDueRevisits = (clinicId: number) =>
  get<RevisitPlan[]>('/admin/returnVisit/revisit/todayDue', { clinicId })

export const getRemindedRevisits = (clinicId: number) =>
  get<RevisitPlan[]>('/admin/returnVisit/revisit/reminded', { clinicId })

export const getAppointedRevisits = (clinicId: number) =>
  get<RevisitPlan[]>('/admin/returnVisit/revisit/appointed', { clinicId })

export const getRevisitStatistics = (clinicId: number) =>
  get<RevisitStatistics>('/admin/returnVisit/revisit/statistics', { clinicId })

export const createRevisitPlan = (data: Partial<RevisitPlan>) =>
  post('/admin/returnVisit/revisit/create', data)

export const markRevisitAsReminded = (id: number, doctorId: number) =>
  post(`/admin/returnVisit/revisit/remind/${id}?doctorId=${doctorId}`)

export const markRevisitAsAppointed = (id: number, doctorId: number, doctorName: string, note?: string) =>
  post(`/admin/returnVisit/revisit/appoint/${id}`, { doctorId, doctorName, note })

export const markRevisitAsCompleted = (id: number, doctorId: number, note?: string) =>
  post(`/admin/returnVisit/revisit/complete/${id}`, { doctorId, note })

export const createRevisitFromRecord = (params: {
  medicalRecordId: number
  patientId: number
  patientName: string
  patientPhone: string
  originalTreatment: string
  followUpItem: string
  followUpDesc?: string
  recoveryDays: number
  planDate: string
}) =>
  post('/admin/returnVisit/revisit/createFromRecord', params)

export const getPatientRevisitPlans = (clinicId: number, patientId: number) =>
  get<RevisitPlan[]>(`/admin/returnVisit/revisit/patient/${patientId}`, { clinicId })
