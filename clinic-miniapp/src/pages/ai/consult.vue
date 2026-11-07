<template>
  <view class="ai-consult-page">
    <!-- 顶部提示栏 -->
    <view class="header-banner">
      <view class="banner-content">
        <text class="banner-icon">🤖</text>
        <view class="banner-text">
          <text class="banner-title">诊所智能助手</text>
          <text class="banner-subtitle">预约挂号、排班、药单、病历，一句话办理</text>
        </view>
      </view>
      <text class="banner-notice">ℹ️ 涉及预约/取消的操作会先向您确认再执行</text>
    </view>

    <!-- 聊天区域 -->
    <scroll-view
      class="chat-container"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
    >
      <view class="chat-list">
        <!-- 欢迎消息 -->
        <view class="message-item system">
          <view class="message-content">
            <text class="message-text">您好！我是诊所智能助手，可以帮您查排班、约医生、取消预约、看药单和病历。请问需要什么帮助？</text>
          </view>
        </view>

        <!-- 消息列表 -->
        <view
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="[msg.role]"
        >
          <!-- AI 头像 -->
          <view v-if="msg.role === 'assistant'" class="avatar ai-avatar">
            <text>🤖</text>
          </view>

          <view class="message-content">
            <!-- 思考与工具过程面板：流式展示 agent 在想什么、调了哪些工具 -->
            <view v-if="(msg.steps && msg.steps.length) || msg.thinking" class="process-panel">
              <view class="process-header" @click="toggleProcess(msg)">
                <text class="process-title">{{ processTitle(msg) }}</text>
                <text v-if="msg.steps && msg.steps.length" class="process-meta">{{ msg.steps.length }} 个工具 {{ msg.processOpen ? '▾' : '▸' }}</text>
              </view>
              <view v-if="msg.processOpen" class="process-body">
                <text v-if="msg.thinking" class="thinking-text">{{ thinkingView(msg) }}</text>
                <view v-for="s in (msg.steps || [])" :key="s.id" class="tool-step">
                  <text class="tool-state">{{ s.done ? '✅' : '🔄' }}</text>
                  <view class="tool-info">
                    <text class="tool-name">{{ s.label }}</text>
                    <text v-if="s.argLine" class="tool-args">{{ s.argLine }}</text>
                  </view>
                </view>
              </view>
            </view>

            <!-- 加载动画（没有任何过程和文字时） -->
            <view v-if="msg.loading && !msg.content && !msg.thinking && !(msg.steps && msg.steps.length)" class="loading-dots">
              <view class="dot"></view>
              <view class="dot"></view>
            </view>
            <text v-if="msg.content" class="message-text" user-select>{{ msg.content }}</text>

            <!-- HITL 写操作确认卡片 -->
            <view v-if="msg.type === 'confirm'" class="confirm-card">
              <view class="confirm-title">⚠️ {{ toolLabel(msg.toolCall) }} · 等您确认</view>
              <view class="confirm-rows">
                <view v-for="row in toolRows(msg.toolCall)" :key="row.label" class="confirm-row">
                  <text class="row-label">{{ row.label }}</text>
                  <text class="row-value">{{ row.value }}</text>
                </view>
              </view>
              <view v-if="!msg.decided" class="confirm-btns">
                <view class="confirm-btn ok" @click="decide(msg, true)">同意执行</view>
                <view class="confirm-btn no" @click="decide(msg, false)">拒绝</view>
              </view>
              <text v-else class="confirm-done">
                {{ msg.confirmed ? '✅ 您已同意，正在执行…' : '🚫 您已拒绝，不会执行' }}
              </text>
            </view>

            <!-- 出错重试 -->
            <text v-if="msg.error" class="retry-btn" @click="retry">点击重试</text>

            <text v-if="msg.time" class="message-time">{{ msg.time }}</text>
          </view>

          <!-- 用户头像 -->
          <view v-if="msg.role === 'user'" class="avatar user-avatar">
            <text>👤</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 快捷问题 -->
    <view v-if="showQuickQuestions" class="quick-questions">
      <scroll-view scroll-x class="quick-scroll">
        <view class="quick-list">
          <view
            v-for="(q, index) in quickQuestions"
            :key="index"
            class="quick-item"
            @click="sendQuickQuestion(q)"
          >
            <text class="quick-text">{{ q }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 底部输入区域 -->
    <view class="input-area" :style="{ paddingBottom: safeBottom + 20 + 'rpx' }">
      <view class="input-wrapper">
        <input
          v-model="inputMessage"
          class="message-input"
          type="text"
          placeholder="如：帮我约明天上午王建国医生"
          confirm-type="send"
          :disabled="state !== 'idle'"
          @confirm="sendMessageHandler"
        />
        <!-- 发送 / 停止 -->
        <view
          v-if="state !== 'sending'"
          class="send-btn"
          :class="{ active: inputMessage.trim() && state === 'idle' }"
          @click="sendMessageHandler"
        >
          <text class="send-icon">➤</text>
        </view>
        <view v-else class="send-btn active stop-btn" @click="stopAnswering">
          <text class="send-icon">⏹</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { chatStream, confirmAgent, interruptChat, getHistory } from '@/api/agent.js'

// ---- 状态机：idle 可输入 | sending 等回答 | confirming 等患者确认写操作 ----
const state = ref('idle')
const messages = ref([])
const inputMessage = ref('')
const scrollTop = ref(0)
const sessionId = ref('')
const safeBottom = ref(0)
let lastText = ''

// 快捷问题
const quickQuestions = [
  '王建国医生最近有排班吗',
  '帮我查我的挂号记录',
  '张三的药单有哪些',
  '帮我取消张三的预约'
]

// 是否显示快捷问题（没有消息且空闲时显示）
const showQuickQuestions = computed(() => messages.value.length === 0 && state.value === 'idle')

// ---- 确认卡片文案 ----
const TOOL_LABELS = {
  list_departments: '查科室',
  list_doctors: '查医生',
  list_schedules: '查排班',
  list_my_registrations: '查我的挂号',
  get_queue_status: '查候诊队列',
  create_registration: '创建挂号',
  cancel_registration: '取消挂号',
  list_prescriptions: '查药单',
  get_prescription: '查药单详情',
  list_medical_records: '查病历',
  get_medical_record: '查病历详情',
  list_patients: '查就诊人',
  // leader 侧框架工具（不是业务工具，但流里/历史里会出现）
  TeamSay: '派单给业务专员'
}
const ROW_LABELS = [
  ['regDate', '就诊日期'],
  ['regTime', '就诊时间'],
  ['doctorId', '医生'],
  ['patientId', '就诊人ID'],
  ['deptId', '科室ID'],
  ['reason', '原因']
]
const toolLabel = (tc) => TOOL_LABELS[tc?.name] || tc?.name || '操作'
const toolRows = (tc) => {
  const input = tc?.input || {}
  const rows = []
  for (const [key, label] of ROW_LABELS) {
    if (input[key] !== undefined && input[key] !== null && input[key] !== '') {
      rows.push({ label, value: String(input[key]) })
    }
  }
  return rows.length ? rows : [{ label: '操作', value: toolLabel(tc) }]
}

// ---- 思考/工具过程面板 ----
// 气泡统一工厂：历史消息/用户消息没有这些字段，模板里全部做了空值保护
const newBubble = () => ({
  role: 'assistant',
  content: '',
  loading: true,
  thinking: '',
  steps: [],
  speaker: '',
  processOpen: true
})

const toggleProcess = (msg) => {
  msg.processOpen = !msg.processOpen
}

const processTitle = (msg) => {
  if (!msg.loading) return '🧠 思考过程'
  return msg.speaker ? `🧠 ${msg.speaker} · 思考中…` : '🧠 思考中…'
}

// 流式时只显示思考尾部（思考可能很长，最新的在末尾）；回答结束后展开全文
const thinkingView = (msg) => {
  const t = msg.thinking || ''
  if (msg.loading && t.length > 240) return '…' + t.slice(-240)
  return t
}

// 工具入参摘要：解析累计的参数 JSON，取前几个键值；解析不了就截断原文
const summarizeArgs = (raw) => {
  let line = ''
  try {
    const obj = JSON.parse(raw || '{}')
    line = Object.entries(obj)
      .slice(0, 3)
      .map(([k, v]) => `${k}=${String(v).slice(0, 20)}`)
      .join('  ')
  } catch (e) {
    line = (raw || '').slice(0, 60)
  }
  return line.length > 80 ? line.slice(0, 80) + '…' : line
}

onLoad(() => {
  // 获取安全区域高度
  const systemInfo = uni.getSystemInfoSync()
  safeBottom.value = systemInfo.safeAreaInsets?.bottom || 0

  loadHistory()
})

/**
 * 登录态预检：真 JWT 是 3 段 2 个点；storage 里残留旧 mock 假 token 时
 * 每个请求都会被后端 401 但前端无感知，必须拦在发送前并引导重新登录
 */
const ensureLogin = () => {
  const token = uni.getStorageSync('token') || ''
  if (token && token.split('.').length === 3) return true
  uni.showModal({
    title: '请先登录',
    content: '登录态无效（可能是测试缓存残留），请到登录页点「一键登录测试患者张三（开发）」重新登录',
    confirmText: '去登录',
    success: (r) => {
      if (r.confirm) uni.reLaunch({ url: '/pages/login/index' })
    }
  })
  return false
}

/**
 * 发送消息处理
 */
const sendMessageHandler = () => {
  const message = inputMessage.value.trim()
  if (!message || state.value !== 'idle') return
  if (!ensureLogin()) return
  lastText = message
  inputMessage.value = ''
  addMessage('user', message)
  sendToAgent(message)
}

/**
 * 发送快捷问题
 */
const sendQuickQuestion = (question) => {
  if (state.value !== 'idle') return
  if (!ensureLogin()) return
  lastText = question
  addMessage('user', question)
  sendToAgent(question)
}

// ---- 流式增量节流渲染（200ms 一批，避免 setData 洪峰） ----
let pendingDelta = ''
let pendingThink = ''
let flushTimer = null
let curBubble = null

const flushDelta = () => {
  if (!curBubble) return
  if (pendingThink) {
    curBubble.thinking += pendingThink
    pendingThink = ''
  }
  if (pendingDelta) {
    curBubble.content += pendingDelta
    curBubble.time = formatTime()
    pendingDelta = ''
  }
  scrollToBottom()
}

/**
 * SSE 事件分发：PROXY_SESSION / REPLY_START / THINKING_BLOCK_DELTA /
 * TOOL_CALL_* / TOOL_RESULT_END / TEXT_BLOCK_DELTA / REQUIRE_USER_CONFIRM /
 * PROXY_QUIET_END / PROXY_ERROR（HINT_BLOCK 等内部帧不展示）
 */
const onEvent = (evt) => {
  const type = evt.type || ''
  if (type === 'PROXY_SESSION') {
    sessionId.value = evt.session_id || sessionId.value
    if (sessionId.value) uni.setStorageSync('agentSessionId', sessionId.value)
  } else if (type === 'REPLY_START') {
    // 一条流里可能有多个 run（总助手 → 业务专员 → 总助手汇总），记下当前说话者
    if (curBubble && evt.name) curBubble.speaker = evt.name
  } else if (type === 'THINKING_BLOCK_DELTA') {
    pendingThink += evt.delta || ''
    if (!flushTimer) {
      flushTimer = setTimeout(() => {
        flushTimer = null
        flushDelta()
      }, 200)
    }
  } else if (type === 'TOOL_CALL_START') {
    if (curBubble) {
      curBubble.steps.push({
        id: evt.tool_call_id,
        name: evt.tool_call_name,
        label: TOOL_LABELS[evt.tool_call_name] || evt.tool_call_name || '工具',
        rawArgs: '',
        argLine: '',
        done: false
      })
      scrollToBottom()
    }
  } else if (type === 'TOOL_CALL_DELTA') {
    if (curBubble) {
      const s = curBubble.steps.find((x) => x.id === evt.tool_call_id)
      if (s) s.rawArgs += evt.delta || ''
    }
  } else if (type === 'TOOL_CALL_END') {
    if (curBubble) {
      const s = curBubble.steps.find((x) => x.id === evt.tool_call_id)
      if (s && !s.argLine) s.argLine = summarizeArgs(s.rawArgs)
    }
  } else if (type === 'TOOL_RESULT_END') {
    if (curBubble) {
      const s = curBubble.steps.find((x) => x.id === evt.tool_call_id)
      if (s) s.done = true
      scrollToBottom()
    }
  } else if (type === 'TEXT_BLOCK_DELTA') {
    pendingDelta += evt.delta || evt.text || ''
    if (!flushTimer) {
      flushTimer = setTimeout(() => {
        flushTimer = null
        flushDelta()
      }, 200)
    }
  } else if (type === 'REQUIRE_USER_CONFIRM') {
    flushDelta()
    messages.value.push({
      role: 'assistant',
      type: 'confirm',
      replyId: evt.reply_id,
      toolCall: (evt.tool_calls && evt.tool_calls[0]) || null,
      toolCalls: evt.tool_calls || [],
      decided: false,
      confirmed: false,
      time: formatTime()
    })
    state.value = 'confirming'
    scrollToBottom()
  } else if (type === 'PROXY_QUIET_END') {
    finishRound()
  } else if (type === 'PROXY_ERROR') {
    failCurrent(evt.message || '出错了')
  }
}

const finishRound = () => {
  flushDelta()
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }
  if (curBubble) {
    curBubble.loading = false
    if (curBubble.content) curBubble.time = formatTime()
  }
  curBubble = null
  if (state.value !== 'confirming') state.value = 'idle'
  scrollToBottom()
}

const failCurrent = (message) => {
  flushDelta()
  if (curBubble) {
    curBubble.loading = false
    curBubble.error = true
    curBubble.content += (curBubble.content ? '\n\n' : '') + '⚠️ ' + message
    curBubble.time = formatTime()
  }
  state.value = 'idle'
  scrollToBottom()
}

/**
 * 发送消息到智能助手（SSE 流式）
 */
const sendToAgent = (message) => {
  state.value = 'sending'
  curBubble = newBubble()
  messages.value.push(curBubble)
  scrollToBottom()

  chatStream({ text: message }, {
    onEvent,
    onDone: finishRound,
    onFail: (err) => failCurrent(err.message || '连接失败，请检查网络')
  })
}

const retry = () => {
  if (lastText && state.value === 'idle') {
    addMessage('user', lastText)
    sendToAgent(lastText)
  }
}

/**
 * HITL 确认：confirmed=true 执行 / false 拒绝；确认后答复继续走原 SSE 流
 */
const decide = (msg, confirmed) => {
  if (msg.decided || state.value !== 'confirming') return
  msg.decided = true
  msg.confirmed = confirmed
  curBubble = newBubble()
  messages.value.push(curBubble)
  state.value = 'sending'
  scrollToBottom()

  confirmAgent({
    replyId: msg.replyId,
    confirmed,
    toolCalls: msg.toolCalls,
    sessionId: sessionId.value
  }).catch(() => {
    uni.showToast({ title: '确认发送失败，请重试', icon: 'none' })
    msg.decided = false // 还原卡片让用户再点
    state.value = 'confirming'
  })
}

/**
 * 停止回答
 */
const stopAnswering = () => {
  interruptChat(sessionId.value).catch(() => {})
  finishRound()
}

/**
 * 添加消息到列表
 */
const addMessage = (role, content) => {
  messages.value.push({
    role,
    content,
    time: formatTime()
  })
  scrollToBottom()
}

/**
 * 滚动到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    const query = uni.createSelectorQuery()
    query.select('.chat-list').boundingClientRect()
    query.exec((res) => {
      if (res[0]) {
        scrollTop.value = res[0].height
      }
    })
  })
}

/**
 * 恢复历史会话（尽力渲染；后端代理重启后会开新会话，旧历史丢失属预期）
 */
const loadHistory = async () => {
  const sid = uni.getStorageSync('agentSessionId')
  if (!sid) return
  try {
    const res = await getHistory(sid)
    const list = res && res.data
    if (!Array.isArray(list)) return
    for (const m of list) {
      const role = m.role === 'user' ? 'user' : 'assistant'
      let content = ''
      // 存储消息里 thinking / tool_call 块都在（流式帧只是没有回放）——
      // 历史气泡同样渲染思考与工具步骤；历史条目多，面板默认收起
      let thinking = ''
      const steps = []
      if (typeof m.content === 'string') {
        content = m.content
      } else if (Array.isArray(m.content)) {
        const texts = []
        for (const b of m.content) {
          if (!b || typeof b !== 'object') continue
          if (b.type === 'text' && b.text) {
            texts.push(b.text)
          } else if (b.type === 'thinking' && b.thinking) {
            thinking += b.thinking
          } else if (b.type === 'tool_call' && b.name) {
            steps.push({
              id: b.id || '',
              name: b.name,
              label: TOOL_LABELS[b.name] || b.name,
              argLine: summarizeArgs(typeof b.input === 'string'
                ? b.input : JSON.stringify(b.input || {})),
              done: true
            })
          }
        }
        content = texts.join('')
      }
      content = String(content || '').trim()
      if (content || thinking || steps.length) {
        messages.value.push({ role, content, time: '', thinking, steps, processOpen: false })
      }
    }
    if (messages.value.length) scrollToBottom()
  } catch (e) {
    // 历史恢复失败不阻塞新对话
  }
}

/**
 * 格式化时间
 */
const formatTime = (date = new Date()) => {
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}
</script>

<style lang="scss" scoped>
.ai-consult-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

// 顶部提示栏
.header-banner {
  background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
  padding: 20rpx 30rpx;
  padding-top: 80rpx;

  .banner-content {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;

    .banner-icon {
      font-size: 60rpx;
      margin-right: 20rpx;
    }

    .banner-text {
      flex: 1;

      .banner-title {
        display: block;
        font-size: 36rpx;
        font-weight: 600;
        color: #fff;
      }

      .banner-subtitle {
        display: block;
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.9);
        margin-top: 4rpx;
      }
    }
  }

  .banner-notice {
    display: block;
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.85);
    background: rgba(0, 0, 0, 0.15);
    padding: 8rpx 16rpx;
    border-radius: 8rpx;
  }
}

// 聊天区域
.chat-container {
  flex: 1;
  padding: 20rpx 30rpx;
  overflow-y: auto;
}

.chat-list {
  .message-item {
    display: flex;
    margin-bottom: 30rpx;

    &.system {
      justify-content: center;

      .message-content {
        background: rgba(0, 184, 148, 0.1);
        border: 1rpx solid rgba(0, 184, 148, 0.2);
        max-width: 90%;
      }
    }

    &.user {
      justify-content: flex-end;

      .message-content {
        background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
        color: #fff;
        border-radius: 24rpx 24rpx 4rpx 24rpx;
        margin-right: 16rpx;
      }
    }

    &.assistant {
      justify-content: flex-start;

      .message-content {
        background: #fff;
        border-radius: 24rpx 24rpx 24rpx 4rpx;
        margin-left: 16rpx;
        box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
      }
    }

    .avatar {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      font-size: 40rpx;

      &.ai-avatar {
        background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%);
      }

      &.user-avatar {
        background: linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%);
      }
    }

    .message-content {
      max-width: 70%;
      padding: 24rpx;

      .message-text {
        font-size: 28rpx;
        line-height: 1.6;
        color: inherit;
        word-break: break-all;
        white-space: pre-wrap;
      }

      .message-time {
        display: block;
        font-size: 20rpx;
        color: #999;
        margin-top: 12rpx;
        text-align: right;
      }
    }
  }
}

// 加载动画
.loading-dots {
  display: flex;
  gap: 12rpx;
  padding: 12rpx;

  .dot {
    width: 16rpx;
    height: 16rpx;
    background: #00b894;
    border-radius: 50%;
    animation: bounce 1.4s ease-in-out infinite both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

// 思考与工具过程面板
.process-panel {
  margin-bottom: 12rpx;
  border: 1rpx solid #e3e9f0;
  background: #f8fafc;
  border-radius: 12rpx;
  overflow: hidden;

  .process-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12rpx 16rpx;

    .process-title {
      font-size: 24rpx;
      color: #5f6b7a;
      font-weight: 500;
    }

    .process-meta {
      font-size: 22rpx;
      color: #9aa7b5;
    }
  }

  .process-body {
    padding: 0 16rpx 12rpx;

    .thinking-text {
      display: block;
      font-size: 22rpx;
      line-height: 1.5;
      color: #8a95a5;
      white-space: pre-wrap;
      word-break: break-all;
      border-left: 4rpx solid #dfe6ee;
      padding-left: 12rpx;
      margin-bottom: 10rpx;
    }

    .tool-step {
      display: flex;
      align-items: flex-start;
      padding: 6rpx 0;

      .tool-state {
        font-size: 22rpx;
        margin-right: 10rpx;
        flex-shrink: 0;
      }

      .tool-info {
        flex: 1;
        min-width: 0;

        .tool-name {
          font-size: 24rpx;
          color: #4a5568;
          font-weight: 500;
        }

        .tool-args {
          display: block;
          font-size: 20rpx;
          color: #9aa7b5;
          word-break: break-all;
        }
      }
    }
  }
}

// HITL 确认卡片
.confirm-card {
  margin-top: 12rpx;
  border: 2rpx solid #ffcc80;
  background: #fff8e1;
  border-radius: 12rpx;
  padding: 20rpx;

  .confirm-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #e65100;
    margin-bottom: 12rpx;
  }

  .confirm-rows {
    margin-bottom: 16rpx;

    .confirm-row {
      display: flex;
      justify-content: space-between;
      padding: 6rpx 0;

      .row-label {
        font-size: 24rpx;
        color: #8d6e63;
      }

      .row-value {
        font-size: 24rpx;
        color: #333;
        font-weight: 500;
      }
    }
  }

  .confirm-btns {
    display: flex;
    gap: 16rpx;

    .confirm-btn {
      flex: 1;
      text-align: center;
      padding: 14rpx 0;
      border-radius: 10rpx;
      font-size: 26rpx;

      &.ok {
        background: #00b894;
        color: #fff;
      }

      &.no {
        background: #fff;
        color: #e53935;
        border: 2rpx solid #e53935;
      }
    }
  }

  .confirm-done {
    font-size: 24rpx;
    color: #6d4c41;
  }
}

// 出错重试
.retry-btn {
  display: inline-block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #00b894;
  text-decoration: underline;
}

// 快捷问题
.quick-questions {
  padding: 20rpx 30rpx;
  background: #fff;
  border-top: 1rpx solid #eee;

  .quick-scroll {
    white-space: nowrap;
  }

  .quick-list {
    display: flex;
    gap: 16rpx;
  }

  .quick-item {
    background: rgba(0, 184, 148, 0.1);
    border: 1rpx solid rgba(0, 184, 148, 0.2);
    border-radius: 32rpx;
    padding: 16rpx 28rpx;

    &:active {
      background: rgba(0, 184, 148, 0.2);
    }

    .quick-text {
      font-size: 26rpx;
      color: #00b894;
    }
  }
}

// 输入区域
.input-area {
  background: #fff;
  border-top: 1rpx solid #eee;
  padding: 20rpx 30rpx;

  .input-wrapper {
    display: flex;
    align-items: center;
    background: #f5f7fa;
    border-radius: 40rpx;
    padding: 12rpx 12rpx 12rpx 24rpx;

    .message-input {
      flex: 1;
      height: 64rpx;
      font-size: 28rpx;
      color: #333;

      &::placeholder {
        color: #999;
      }
    }

    .send-btn {
      width: 64rpx;
      height: 64rpx;
      border-radius: 50%;
      background: #ddd;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-left: 16rpx;
      transition: all 0.3s;

      &.active {
        background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
      }

      &.stop-btn {
        background: #e53935;
      }

      &:active {
        transform: scale(0.95);
      }

      .send-icon {
        font-size: 28rpx;
        color: #fff;
        margin-left: 4rpx;
      }
    }
  }
}
</style>
