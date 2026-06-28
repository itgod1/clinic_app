import request from '@/utils/request.js'

// 获取就诊记录列表（小程序端-只返回当前用户的）
export const getMedicalRecordList = (params) => {
  return request({
    url: '/miniapp/medical/record/list',
    method: 'GET',
    params
  })
}

// 获取就诊记录详情
export const getMedicalRecordDetail = (id) => {
  return request({
    url: `/miniapp/medical/record/${id}`,
    method: 'GET'
  })
}

// 获取处方列表
export const getPrescriptionList = (params) => {
  return request({
    url: '/admin/prescription/list',
    method: 'GET',
    params
  })
}

// 获取处方详情
export const getPrescriptionDetail = (id) => {
  return request({
    url: `/admin/prescription/${id}`,
    method: 'GET'
  })
}
