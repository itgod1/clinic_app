import request from '@/utils/request.js'

// 小程序登录
export const miniappLogin = (data) => {
  return request({
    url: '/auth/miniapp/login',
    method: 'POST',
    data
  })
}

// 账号密码登录
export const login = (data) => {
  return request({
    url: '/auth/login',
    method: 'POST',
    data
  })
}

// 登出
export const logout = () => {
  return request({
    url: '/auth/logout',
    method: 'POST'
  })
}

// 获取用户信息
export const getUserInfo = () => {
  return request({
    url: '/auth/info',
    method: 'GET'
  })
}

// 获取用户关联的诊所列表
export const getUserClinics = () => {
  return request({
    url: '/auth/clinics',
    method: 'GET'
  })
}
