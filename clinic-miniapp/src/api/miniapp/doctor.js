import request from '@/utils/request.js'

/**
 * 小程序 - 医生相关接口
 * 对应后端: MiniappController
 */

// 获取医生列表
export const getDoctorList = (params) => {
  return request({
    url: '/miniapp/doctors',
    method: 'GET',
    params
  })
}

// 获取医生详情
export const getDoctorDetail = (id) => {
  return request({
    url: `/miniapp/doctors/${id}`,
    method: 'GET'
  })
}
