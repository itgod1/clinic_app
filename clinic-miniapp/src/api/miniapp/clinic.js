import request from '@/utils/request.js'

/**
 * 小程序 - 诊所相关接口
 * 对应后端: MiniappController
 */

// 获取可用诊所列表
export const getClinicList = (params) => {
  return request({
    url: '/miniapp/clinics',
    method: 'GET',
    params
  })
}

// 获取诊所详情
export const getClinicDetail = (id) => {
  return request({
    url: `/miniapp/clinics/${id}`,
    method: 'GET'
  })
}

// 获取科室列表
export const getDepartmentList = (params) => {
  return request({
    url: '/miniapp/departments',
    method: 'GET',
    params
  })
}

// 切换当前诊所
export const switchClinic = (clinicId) => {
  return request({
    url: '/miniapp/clinic/switch',
    method: 'POST',
    params: { clinicId }
  })
}
