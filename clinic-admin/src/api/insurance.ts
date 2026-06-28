import { get, post } from '@/utils/request'

// ============= 医保患者 =============

export interface InsurancePatient {
  id: number
  clinicId: number
  patientId: number
  insuranceCardNo: string
  insuranceType: string
  insuranceTypeName: string
  insuranceCity: string
  insuranceStatus: string
  insuranceStatusName: string
  holderName: string
  holderIdCard: string
  validFrom: string
  validTo: string
  bankName: string
  remark: string
  patientName: string
}

export interface InsurancePatientQuery {
  clinicId: number
  keyword?: string
  insuranceType?: string
  insuranceStatus?: string
  pageNum?: number
  pageSize?: number
}

export interface InsurancePatientListVO {
  total: number
  list: InsurancePatient[]
}

export const getInsurancePatientList = (params: InsurancePatientQuery) =>
  get<InsurancePatientListVO>('/admin/insurance/patient/list', params)

export const getInsurancePatientById = (id: number) =>
  get<InsurancePatient>(`/admin/insurance/patient/${id}`)

export const createInsurancePatient = (data: Partial<InsurancePatient>) =>
  post('/admin/insurance/patient/create', data)

export const updateInsurancePatient = (data: Partial<InsurancePatient>) =>
  post('/admin/insurance/patient/update', data)

export const deleteInsurancePatient = (id: number) =>
  post('/admin/insurance/patient/delete', null, { params: { id } })

// ============= 医保目录 =============

export interface InsuranceCatalog {
  id: number
  clinicId: number
  catalogType: string
  catalogTypeName: string
  itemCode: string
  itemName: string
  specification: string
  manufacturer: string
  unit: string
  unitPrice: number
  selfPayRatio: number
  insuranceCategory: string
  insuranceCategoryName: string
  dosageForm: string
  icdCode: string
  hospitalLevel: string
  dailyLimit: number
  status: number
  effectiveDate: string
  expireDate: string
}

export interface InsuranceCatalogQuery {
  clinicId: number
  catalogType?: string
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface InsuranceCatalogListVO {
  total: number
  list: InsuranceCatalog[]
}

export const getInsuranceCatalogList = (params: InsuranceCatalogQuery) =>
  get<InsuranceCatalogListVO>('/admin/insurance/catalog/list', params)

export const getInsuranceCatalogById = (id: number) =>
  get<InsuranceCatalog>(`/admin/insurance/catalog/${id}`)

export const createInsuranceCatalog = (data: Partial<InsuranceCatalog>) =>
  post('/admin/insurance/catalog/create', data)

export const updateInsuranceCatalog = (data: Partial<InsuranceCatalog>) =>
  post('/admin/insurance/catalog/update', data)

export const deleteInsuranceCatalog = (id: number) =>
  post('/admin/insurance/catalog/delete', null, { params: { id } })

// ============= 医保结算 =============

export interface InsuranceSettlement {
  id: number
  clinicId: number
  orderId: number
  patientId: number
  insurancePatientId: number
  settlementNo: string
  totalAmount: number
  insurancePay: number
  selfPay: number
  accountPay: number
  cashPay: number
  settlementStatus: string
  settlementStatusName: string
  insuranceClaimNo: string
  submitTime: string
  completeTime: string
  errorMessage: string
  operatorId: number
  operatorName: string
  remark: string
  patientName: string
  createdAt: string
}

export interface InsuranceSettlementDetail {
  id: number
  settlementId: number
  catalogItemId: number
  itemCode: string
  itemName: string
  specification: string
  unit: string
  quantity: number
  unitPrice: number
  totalPrice: number
  selfPayRatio: number
  insurancePay: number
  selfPay: number
}

export interface InsuranceSettlementQuery {
  clinicId: number
  settlementStatus?: string
  keyword?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface InsuranceSettlementListVO {
  total: number
  list: InsuranceSettlement[]
}

export interface SettlementItem {
  catalogItemId?: number
  itemCode: string
  itemName: string
  specification?: string
  unit?: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export interface InsuranceSettlementRequest {
  orderId: number
  patientId: number
  insurancePatientId?: number
  totalAmount?: number
  items: SettlementItem[]
}

export interface SettlementPreview {
  eligible: boolean
  insuranceTypeName: string
  insuranceCardNo: string
  patientName: string
  items: SettlementPreviewItem[]
  totalAmount: number
  totalInsurancePay: number
  totalSelfPay: number
}

export interface SettlementPreviewItem {
  itemCode: string
  itemName: string
  quantity: number
  unitPrice: number
  totalPrice: number
  selfPayRatio: number
  insurancePay: number
  selfPay: number
  catalogMatched: boolean
}

export interface SettlementDetail {
  settlement: InsuranceSettlement
  details: InsuranceSettlementDetail[]
}

export const getInsuranceSettlementList = (params: InsuranceSettlementQuery) =>
  get<InsuranceSettlementListVO>('/admin/insurance/settlement/list', params)

export const getInsuranceSettlementDetail = (id: number) =>
  get<SettlementDetail>(`/admin/insurance/settlement/${id}`)

export const createSettlementPreview = (data: InsuranceSettlementRequest) =>
  post<SettlementPreview>('/admin/insurance/settlement/preview', data)

export const submitSettlement = (id: number) =>
  post<InsuranceSettlement>(`/admin/insurance/settlement/submit/${id}`)

export const cancelSettlement = (id: number, reason: string) =>
  post(`/admin/insurance/settlement/cancel/${id}`, null, { params: { reason } })

export const querySettlementFromPlatform = (id: number) =>
  get(`/admin/insurance/settlement/query-platform/${id}`)
