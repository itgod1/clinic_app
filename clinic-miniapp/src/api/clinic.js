import request from '@/utils/request.js'

// 获取诊所详情
export const getClinicDetail = (id) => {
  return request({
    url: `/admin/clinic/${id}`,
    method: 'GET'
  })
}

// 获取诊所列表（超级管理员）
export const getClinicList = (params) => {
  return request({
    url: '/admin/clinic/list',
    method: 'GET',
    params
  })
}

// 获取用户关联的诊所列表
export const getMyClinics = () => {
  return request({
    url: '/auth/clinics',
    method: 'GET'
  })
}

// 获取所有可用诊所列表（患者选择用）
export const getAvailableClinics = (params) => {
  return request({
    url: '/miniapp/clinics',
    method: 'GET',
    params
  })
}

// 获取诊所详情（小程序公开接口）
export const getPublicClinicDetail = (id) => {
  return request({
    url: `/miniapp/clinics/${id}`,
    method: 'GET'
  })
}

// 获取诊所科室列表（管理端，需要token）
export const getDepartmentList = (params) => {
  return request({
    url: '/admin/department/list',
    method: 'GET',
    params
  })
}

// 获取诊所科室列表（小程序公开接口）
export const getPublicDepartmentList = (params) => {
  return request({
    url: '/miniapp/departments',
    method: 'GET',
    params
  })
}
