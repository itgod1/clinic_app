import request from '@/utils/request.js'

/**
 * 小程序 - 排班相关接口
 * 对应后端: MiniappController
 */

// 获取医生排班列表
export const getDoctorSchedule = (params) => {
  return request({
    url: '/miniapp/schedules',
    method: 'GET',
    params
  })
}
