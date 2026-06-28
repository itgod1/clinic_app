/**
 * Dify AI API 封装
 * 用于 AI 问诊功能
 */

// Dify 配置 - 从环境变量读取
const DIFY_CONFIG = {
  // Dify API 基础地址
  baseURL: import.meta.env.VITE_DIFY_BASE_URL || 'https://api.dify.ai/v1',
  // 你的 Dify Chat 应用 API Key
  apiKey: import.meta.env.VITE_DIFY_API_KEY || '',
  // 应用类型: 'chat' 或 'completion'
  appType: 'chat'
}

/**
 * 发送消息到 Dify (非流式)
 * @param {string} query - 用户输入的消息
 * @param {string} conversationId - 会话ID，首次对话可为空
 * @param {object} inputs - 应用输入参数
 * @returns {Promise} - 返回 Dify 响应
 */
export const sendMessage = (query, conversationId = '', inputs = {}) => {
  return new Promise((resolve, reject) => {
    const data = {
      inputs,
      query,
      response_mode: 'blocking', // blocking 或 streaming
      conversation_id: conversationId,
      user: getUserId() // 用户标识
    }

    uni.request({
      url: `${DIFY_CONFIG.baseURL}/chat-messages`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${DIFY_CONFIG.apiKey}`
      },
      data,
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(res.data?.message || '请求失败'))
        }
      },
      fail: reject
    })
  })
}

/**
 * 发送消息到 Dify (流式)
 * 使用小程序的 requestTask 实现流式响应
 * @param {string} query - 用户输入的消息
 * @param {string} conversationId - 会话ID
 * @param {object} callbacks - 回调函数 { onMessage, onComplete, onError }
 * @param {object} inputs - 应用输入参数
 */
export const sendMessageStream = (query, conversationId = '', callbacks = {}, inputs = {}) => {
  const { onMessage, onComplete, onError } = callbacks

  const data = {
    inputs,
    query,
    response_mode: 'streaming',
    conversation_id: conversationId,
    user: getUserId()
  }

  const requestTask = uni.request({
    url: `${DIFY_CONFIG.baseURL}/chat-messages`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${DIFY_CONFIG.apiKey}`,
      'Accept': 'text/event-stream'
    },
    data,
    enableChunked: true, // 开启流式传输
    success: (res) => {
      // 流式响应处理
      if (res.statusCode === 200) {
        onComplete && onComplete(res.data)
      } else {
        onError && onError(new Error(res.data?.message || '请求失败'))
      }
    },
    fail: (err) => {
      onError && onError(err)
    }
  })

  // 监听数据接收 - 微信小程序支持
  if (requestTask.onChunkReceived) {
    requestTask.onChunkReceived((response) => {
      const uint8Array = new Uint8Array(response.data)
      const text = uint8ArrayToString(uint8Array)
      parseStreamData(text, onMessage)
    })
  }

  return requestTask
}

/**
 * 获取会话列表
 */
export const getConversations = () => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${DIFY_CONFIG.baseURL}/conversations`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${DIFY_CONFIG.apiKey}`
      },
      data: {
        user: getUserId(),
        limit: 20
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(res.data?.message || '获取会话列表失败'))
        }
      },
      fail: reject
    })
  })
}

/**
 * 获取会话历史消息
 * @param {string} conversationId - 会话ID
 */
export const getConversationHistory = (conversationId) => {
  return new Promise((resolve, reject) => {
    if (!conversationId) {
      resolve({ data: [] })
      return
    }

    uni.request({
      url: `${DIFY_CONFIG.baseURL}/messages`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${DIFY_CONFIG.apiKey}`
      },
      data: {
        user: getUserId(),
        conversation_id: conversationId,
        limit: 50
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(res.data?.message || '获取历史消息失败'))
        }
      },
      fail: reject
    })
  })
}

/**
 * 上传文件 (用于图片等多模态输入)
 * @param {string} filePath - 文件路径
 */
export const uploadFile = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${DIFY_CONFIG.baseURL}/files/upload`,
      filePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${DIFY_CONFIG.apiKey}`
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (res.statusCode === 201 || res.statusCode === 200) {
          resolve(data)
        } else {
          reject(new Error(data?.message || '上传失败'))
        }
      },
      fail: reject
    })
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
      return `user_${user.userId || user.id || 'anonymous'}`
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
 * Uint8Array 转字符串
 */
function uint8ArrayToString(uint8Array) {
  const decoder = new TextDecoder('utf-8')
  return decoder.decode(uint8Array)
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
  sendMessage,
  sendMessageStream,
  getConversations,
  getConversationHistory,
  uploadFile
}
