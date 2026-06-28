import request from '@/utils/request.js'

// 获取待缴费列表
export const getUnpaidList = (params) => {
  return request({
    url: '/miniapp/payments/unpaid',
    method: 'GET',
    params
  })
}

// 获取缴费记录列表
export const getPaymentHistory = (params) => {
  return request({
    url: '/miniapp/payments/history',
    method: 'GET',
    params
  })
}

// 获取处方详情
export const getPaymentDetail = (id) => {
  return request({
    url: `/miniapp/payments/${id}`,
    method: 'GET'
  })
}

// 支付处方
export const payPrescription = (data) => {
  return request({
    url: '/miniapp/payments/pay',
    method: 'POST',
    data
  })
}
