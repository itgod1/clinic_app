import request from '@/utils/request.js'

// 获取就诊人列表（小程序专用 - 只返回当前用户的就诊人）
export const getPatientList = (params) => {
  return request({
    url: '/miniapp/patient/list',
    method: 'GET',
    params
  })
}

// 获取就诊人详情
export const getPatientDetail = (id) => {
  return request({
    url: `/miniapp/patient/${id}`,
    method: 'GET'
  })
}

// 创建就诊人
export const createPatient = (data) => {
  return request({
    url: '/miniapp/patient/create',
    method: 'POST',
    data
  })
}

// 更新就诊人
export const updatePatient = (data) => {
  return request({
    url: '/miniapp/patient/update',
    method: 'POST',
    data
  })
}

// 删除就诊人
export const deletePatient = (params) => {
  return request({
    url: '/miniapp/patient/delete',
    method: 'POST',
    params
  })
}
