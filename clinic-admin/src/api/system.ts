import { get, post, put, del } from '@/utils/request'

export interface SysUser {
  id: number
  clinicId: number
  username: string
  realName: string
  phone: string
  email: string
  roleId: number
  roleName: string
  deptId: number
  deptName: string
  post: string
  status: number
  statusName: string
  lastLoginTime: string
  createTime: string
}

export interface SysUserQuery {
  clinicId: number
  roleId?: number
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SysUserListVO {
  total: number
  list: SysUser[]
}

export const getSysUserList = (params: SysUserQuery) =>
  get<SysUserListVO>('/admin/user/list', params)

export const getSysUserInfo = (id: number) =>
  get<SysUser>(`/admin/user/${id}`)

export const createSysUser = (data: Partial<SysUser>) =>
  post('/admin/user/create', data)

export const updateSysUser = (data: Partial<SysUser>) =>
  post('/admin/user/update', data)

export const deleteSysUser = (id: number) =>
  post('/admin/user/delete', { userId: id })

export const resetPassword = (data: { userId: number; newPassword: string }) =>
  post('/admin/user/resetPassword', data)

export const changePassword = (data: { oldPassword: string; newPassword: string }) =>
  post('/admin/user/changePassword', data)