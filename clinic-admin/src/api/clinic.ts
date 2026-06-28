import { get, post } from '@/utils/request'

export interface ClinicInfo {
  id: number
  clinicName: string
  clinicCode: string
  address: string
  province: string
  city: string
  district: string
  contactPerson: string
  contactPhone: string
  logoUrl: string
  licenseNo: string
  licenseUrl: string
  businessStatus: number
  serviceStart: string
  serviceEnd: string
  description: string
  latitude?: number
  longitude?: number
  createdAt?: string
}

export interface ClinicListParams {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

// 获取诊所列表
export const getClinicList = (params: ClinicListParams) => get('/admin/clinic/list', params)

// 获取所有营业中的诊所
export const getActiveClinicList = () => get('/admin/clinic/activeList')

// 获取诊所详情
export const getClinicDetail = (id: number) => get(`/admin/clinic/${id}`)

// 创建诊所
export const createClinic = (data: Partial<ClinicInfo>) => post('/admin/clinic/create', data)

// 更新诊所
export const updateClinic = (data: Partial<ClinicInfo>) => post('/admin/clinic/update', data)

// 删除诊所
export const deleteClinic = (id: number) => post('/admin/clinic/delete', { id })

// 检查诊所编码是否存在
export const checkClinicCode = (clinicCode: string) => get('/admin/clinic/checkCode', { clinicCode })

// 兼容原有接口
export const updateClinicInfo = (data: Partial<ClinicInfo>) => post('/admin/clinic/update', data)
