/**
 * 智能助手（AgentScope 多 agent）API
 *
 * 对接 clinic-api 代理端点（/miniapp/agent/*）：
 * - chatStream：POST /chat，SSE 分块流式接收（mp-weixin 用 uni.request
 *   enableChunked + onChunkReceived；H5 用 fetch + ReadableStream）
 * - confirmAgent：HITL 写操作确认（REQUIRE_USER_CONFIRM 帧的 replyId
 *   + 原样回传 tool_call）
 * - interruptChat / getHistory：停止回答 / 恢复历史
 *
 * 注意：/chat 成功时 Content-Type 是 text/event-stream。失败走 Result 约定：
 * HTTP 恒 200、错误码在 body（{"code":429,"message":"..."}）——所以收完流
 * 后一个 SSE 帧都没见到时，要把全量响应当 JSON 错误体解析（真实 HTTP 非 200
 * 仍先按状态码分支）。不要给请求加 Accept: text/event-stream——那样失败
 * 分支会被 Spring 内容协商盖成 406，真实错误码丢失。
 */
import request from '@/utils/request.js'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

/**
 * SSE 分块解析：按行切，data: 行取 JSON 事件。
 * 返回未完成的尾部（可能被 chunk 边界切断的半行），调用方须留待下一块拼接。
 */
export function parseSseBuffer(buf, onEvent) {
  const lines = buf.split('\n')
  const rest = lines.pop()
  for (const line of lines) {
    if (!line.startsWith('data:')) continue
    const payload = line.slice(5).trim()
    if (!payload || payload === '[DONE]') continue
    let evt
    try {
      evt = JSON.parse(payload)
    } catch (e) {
      continue // 坏帧丢弃，不中断整条流
    }
    onEvent(evt)
  }
  return rest
}

/**
 * 流式对话。handlers: { onEvent(evt), onDone(), onFail(err) }
 * 事件类型见后端 AgentProxyController：PROXY_SESSION / TEXT_BLOCK_DELTA /
 * REQUIRE_USER_CONFIRM / PROXY_QUIET_END / PROXY_ERROR（其余帧可忽略）。
 */
export function chatStream({ text }, { onEvent, onDone, onFail }) {
  // 不带 Accept: text/event-stream：后端失败分支裸返回 Result POJO，带上它
  // 会被 Spring 内容协商盖成 406、丢掉真实错误码；成功路径 SseEmitter 不看 Accept
  const header = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '')
  }

  // raw 是跨 chunk 半行缓冲；all 攒全量响应文本，失败兜底解析用
  let raw = ''
  let all = ''
  let sawEvent = false

  const handleChunkText = (t) => {
    all += t
    raw = parseSseBuffer(raw + t, (evt) => {
      sawEvent = true
      onEvent(evt)
    })
  }

  // 收尾：一个 SSE 帧都没收到，说明 body 是 Result JSON 错误体（HTTP 200、
  // 错误码在 code/message），解析出 message 走失败；解析不出按正常完成兜底
  const finish = () => {
    if (sawEvent) {
      onDone && onDone()
      return
    }
    let message = ''
    try {
      const j = JSON.parse(all)
      message = (j && j.message) || ''
    } catch (e) { /* 非 JSON，按正常完成处理 */ }
    if (message) onFail && onFail(new Error(message))
    else onDone && onDone()
  }

  // #ifdef H5
  fetch(BASE_URL + '/miniapp/agent/chat', {
    method: 'POST',
    headers: header,
    body: JSON.stringify({ text })
  }).then(async (res) => {
    if (!res.ok) {
      let message = '请求失败(' + res.status + ')'
      try {
        const j = await res.json()
        message = j.message || message
      } catch (e) { /* 保持默认 */ }
      onFail && onFail(new Error(message))
      return
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      handleChunkText(decoder.decode(value, { stream: true }))
    }
    finish()
  }).catch((e) => onFail && onFail(e))
  // #endif

  // #ifndef H5
  const task = uni.request({
    url: BASE_URL + '/miniapp/agent/chat',
    method: 'POST',
    header,
    data: { text },
    enableChunked: true,
    success: (res) => {
      if (res.statusCode === 200) {
        finish()
      } else {
        let message = '请求失败(' + res.statusCode + ')'
        // enableChunked 下错误体可能只到过 onChunkReceived（all），res.data 可能为空
        const bodyText = typeof res.data === 'string' && res.data ? res.data : all
        try {
          const j = bodyText ? JSON.parse(bodyText) : null
          message = (j && (j.message || j.detail)) || message
        } catch (e) { /* 保持默认 */ }
        onFail && onFail(new Error(message))
      }
    },
    fail: (err) => {
      console.error('agent 流式请求失败:', err)
      onFail && onFail(err)
    }
  })
  if (task && task.onChunkReceived) {
    task.onChunkReceived((response) => {
      handleChunkText(new TextDecoder('utf-8').decode(new Uint8Array(response.data)))
    })
  }
  // #endif
}

/** HITL 写操作确认：confirmed=true 执行，false 拒绝。toolCall 原样回传帧内对象 */
export const confirmAgent = (data) => request({
  url: '/miniapp/agent/confirm',
  method: 'POST',
  data
})

/** 停止当前回答 */
export const interruptChat = (sessionId) => request({
  url: '/miniapp/agent/interrupt',
  method: 'POST',
  data: { sessionId }
})

/** 历史消息（进页面恢复会话用）。
 * 注意 GET 的查询参数必须走 params 键——request.js 只把 params 拼进 URL，
 * data 在 GET 下会被丢弃（曾导致 history 请求不带 sessionId 恒返回空） */
export const getHistory = (sessionId, limit = 50) => request({
  url: '/miniapp/agent/history',
  method: 'GET',
  params: { sessionId, limit }
})
