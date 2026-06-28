/**
 * Dify Chatflow API 封装
 * 用于 AI 回访功能 - 通过后端服务转发，不直接暴露 Dify API
 */

import request from '@/utils/request.js'

/**
 * 发送消息到 Dify Chatflow (通过后端转发)
 * @param {string} query - 用户输入的消息
 * @param {string} conversationId - 会话ID，首次对话可为空
 * @param {object} inputs - Chatflow 输入参数
 * @returns {Promise} - 返回 Dify 响应
 */
export const sendChatflowMessage = (query, conversationId = '', inputs = {}) => {
  const data = {
    query,
    conversation_id: conversationId,
    inputs,
    response_mode: 'blocking',
    user: getUserId()
  }

  console.log('发送 Chatflow 请求(后端转发):', {
    conversation_id: conversationId,
    inputs: Object.keys(inputs)
  })

  return request({
    url: '/ai/dify/chat',
    method: 'POST',
    data
  })
}

/**
 * 发送消息到 Dify Chatflow (流式，通过后端转发)
 * @param {string} query - 用户输入的消息
 * @param {string} conversationId - 会话ID
 * @param {object} callbacks - 回调函数 { onMessage, onComplete, onError }
 * @param {object} inputs - Chatflow 输入参数
 */
export const sendChatflowMessageStream = (query, conversationId = '', callbacks = {}, inputs = {}) => {
  const { onMessage, onComplete, onError } = callbacks

  const data = {
    query,
    conversation_id: conversationId,
    inputs,
    response_mode: 'streaming',
    user: getUserId()
  }

  console.log('发送 Chatflow 流式请求(后端转发):', {
    conversation_id: conversationId
  })

  return new Promise((resolve, reject) => {
    const requestTask = uni.request({
      url: 'http://192.168.1.9:8080/api/ai/dify/chat-stream',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + (uni.getStorageSync('token') || ''),
        'Accept': 'text/event-stream'
      },
      data,
      enableChunked: true,
      success: (res) => {
        if (res.statusCode === 200) {
          onComplete && onComplete(res.data)
        } else {
          onError && onError(new Error(res.data?.message || '请求失败'))
        }
      },
      fail: (err) => {
        console.error('Chatflow 流式请求失败:', err)
        onError && onError(err)
      }
    })

    // 监听数据接收
    if (requestTask.onChunkReceived) {
      requestTask.onChunkReceived((response) => {
        const uint8Array = new Uint8Array(response.data)
        const text = new TextDecoder('utf-8').decode(uint8Array)
        parseStreamData(text, onMessage)
      })
    }

    resolve(requestTask)
  })
}

/**
 * 获取会话列表(通过后端转发)
 */
export const getConversations = () => {
  return request({
    url: '/ai/dify/conversations',
    method: 'GET',
    params: {
      user: getUserId(),
      limit: 20
    }
  })
}

/**
 * 获取会话历史消息(通过后端转发)
 * @param {string} conversationId - 会话ID
 */
export const getConversationHistory = (conversationId) => {
  if (!conversationId) {
    return Promise.resolve({ data: [] })
  }

  return request({
    url: '/ai/dify/messages',
    method: 'GET',
    params: {
      user: getUserId(),
      conversation_id: conversationId,
      limit: 50
    }
  })
}

// ============ 工具函数 ============

/**
 * 获取用户唯一标识
 */
function getUserId() {
  const userInfo = uni.getStorageSync('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      return `user_${user.userId || user.id || user.patientId || 'anonymous'}`
    } catch (e) {
      return 'user_anonymous'
    }
  }
  // 生成匿名用户ID
  let anonymousId = uni.getStorageSync('anonymousUserId')
  if (!anonymousId) {
    anonymousId = `anon_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    uni.setStorageSync('anonymousUserId', anonymousId)
  }
  return anonymousId
}

/**
 * 解析流式数据
 */
function parseStreamData(text, onMessage) {
  const lines = text.split('\n')
  for (const line of lines) {
    if (line.startsWith('data: ')) {
      try {
        const jsonStr = line.slice(6)
        const data = JSON.parse(jsonStr)
        onMessage && onMessage(data)
      } catch (e) {
        // 忽略解析失败的行
      }
    }
  }
}

export default {
  sendChatflowMessage,
  sendChatflowMessageStream,
  getConversations,
  getConversationHistory
}
