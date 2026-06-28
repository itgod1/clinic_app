import { get, post, put, del } from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
  loginType: number
}

export interface LoginVO {
  token: string
  expireTime: number
  userInfo: UserInfo
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  clinicId: number
  clinicName: string
  roleId: number
  roleName: string
  avatar?: string
}

export interface CaptchaVO {
  captchaId: string
  captchaImage: string
}

export interface RegisterParams {
  username: string
  password: string
  confirmPassword: string
  realName?: string
  phone?: string
  email?: string
  clinicId?: number
  deptId?: number
}

export const login = (data: LoginParams) => post<LoginVO>('/auth/login', data)

export const logout = () => post('/auth/logout')

export const getCaptcha = () => get<CaptchaVO>('/auth/captcha')

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  post('/auth/updatePassword', data)

export const register = (data: RegisterParams) => post('/auth/register', data)