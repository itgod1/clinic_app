import { get, post, del } from '@/utils/request'

export interface Department {
  id: number
  clinicId: number
  deptName: string
  deptCode: string
  deptType: number
  deptTypeName: string
  sortOrder: number
  status: number
  createdAt: string
}

export interface DepartmentQuery {
  clinicId: number
  deptType?: number
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface DepartmentListVO {
  total: number
  list: Department[]
}

export const getDepartmentList = (params: DepartmentQuery) =>
  get<DepartmentListVO>('/admin/department/list', params)

export const createDepartment = (data: Partial<Department>) =>
  post('/admin/department/create', data)

export const updateDepartment = (data: Partial<Department>) =>
  post('/admin/department/update', data)

export const deleteDepartment = (deptId: number) =>
  del(`/admin/department/${deptId}`)