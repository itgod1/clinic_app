import request from '@/utils/request.js'

// 获取挂号列表
export const getRegistrationList = (params) => {
  return request({
    url: '/admin/registration/list',
    method: 'GET',
    params
  })
}

// 获取当前用户的挂号列表（小程序端）
export const getMyRegistrationList = (params) => {
  return request({
    url: '/miniapp/registration/my-list',
    method: 'GET',
    params
  })
}

// 获取挂号详情
export const getRegistrationDetail = (id) => {
  return request({
    url: `/admin/registration/${id}`,
    method: 'GET'
  })
}

// 创建挂号
export const createRegistration = (data) => {
  return request({
    url: '/admin/registration/create',
    method: 'POST',
    data
  })
}

// 取消挂号
export const cancelRegistration = (data) => {
  return request({
    url: '/admin/registration/cancel',
    method: 'POST',
    data
  })
}

// 获取挂号排队信息
export const getRegistrationQueueInfo = (id) => {
  return request({
    url: `/miniapp/registration/${id}/queue`,
    method: 'GET'
  })
}
