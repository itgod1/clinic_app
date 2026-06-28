import request from '@/utils/request.js'

/**
 * AI回访相关API
 */

/**
 * 记录任务已打开
 * @param {string|number} taskId - 任务ID
 */
export const recordTaskOpen = (taskId) => {
  return request({
    url: '/ai/return-visit/task/open',
    method: 'POST',
    params: { taskId }
  })
}

/**
 * 获取患者档案
 * @param {string|number} patientId - 患者ID
 */
export const getPatientProfile = (patientId) => {
  return request({
    url: `/ai/return-visit/patient/${patientId}/profile`,
    method: 'GET'
  })
}

/**
 * 获取患者就诊历史
 * @param {string|number} patientId - 患者ID
 * @param {number} limit - 限制数量
 */
export const getPatientHistory = (patientId, limit = 3) => {
  return request({
    url: `/ai/return-visit/patient/${patientId}/history`,
    method: 'GET',
    params: { limit }
  })
}

/**
 * 开始AI回访对话
 * @param {Object} data - 对话数据
 */
export const startConversation = (data) => {
  return request({
    url: '/ai/return-visit/conversation/start',
    method: 'POST',
    data
  })
}

/**
 * 保存对话消息
 * @param {string} conversationId - 对话ID
 * @param {Object} data - 消息数据
 */
export const saveMessage = (conversationId, data) => {
  return request({
    url: `/ai/return-visit/conversation/${conversationId}/message`,
    method: 'POST',
    data
  })
}

/**
 * 完成对话
 * @param {string} conversationId - 对话ID
 * @param {Object} data - 完成数据
 */
export const completeConversation = (conversationId, data) => {
  return request({
    url: `/ai/return-visit/conversation/${conversationId}/complete`,
    method: 'POST',
    data
  })
}
