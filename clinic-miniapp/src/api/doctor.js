import request from '@/utils/request.js'

// 获取医生列表（管理端，需要token）
export const getDoctorList = (params) => {
  return request({
    url: '/admin/doctor/list',
    method: 'GET',
    params
  })
}

// 获取医生列表（小程序公开接口，不需要token）
export const getPublicDoctorList = (params) => {
  return request({
    url: '/miniapp/doctors',
    method: 'GET',
    params
  })
}

// 获取医生详情
export const getDoctorDetail = (id) => {
  return request({
    url: `/admin/doctor/${id}`,
    method: 'GET'
  })
}

// 获取医生排班
export const getDoctorSchedule = (params) => {
  return request({
    url: '/admin/schedule/list',
    method: 'GET',
    params
  })
}
