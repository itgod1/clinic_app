import request from '@/utils/request.js'

// 获取当前用户的就诊人列表
export const getPatientList = (params) => {
  return request({
    url: '/miniapp/patient/list',
    method: 'GET',
    params
  })
}

// 获取就诊人详情
export const getPatientDetail = (id, params) => {
  return request({
    url: `/miniapp/patient/${id}`,
    method: 'GET',
    params
  })
}

// 添加就诊人
export const createPatient = (data, params) => {
  return request({
    url: '/miniapp/patient/create',
    method: 'POST',
    data,
    params
  })
}

// 更新就诊人
export const updatePatient = (data, params) => {
  return request({
    url: '/miniapp/patient/update',
    method: 'POST',
    data,
    params
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
