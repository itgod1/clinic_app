<template>
  <view class="ai-consult-page">
    <!-- 顶部提示栏 -->
    <view class="header-banner">
      <view class="banner-content">
        <text class="banner-icon">🤖</text>
        <view class="banner-text">
          <text class="banner-title">AI 智能问诊</text>
          <text class="banner-subtitle">AI 助手提供初步症状分析和就医建议</text>
        </view>
      </view>
      <text class="banner-notice">⚠️ 仅供参考，不能替代专业医生诊断</text>
    </view>

    <!-- 聊天区域 -->
    <scroll-view 
      class="chat-container" 
      scroll-y 
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
      @scrolltoupper="loadMoreHistory"
    >
      <view class="chat-list">
        <!-- 欢迎消息 -->
        <view class="message-item system">
          <view class="message-content">
            <text class="message-text">您好！我是 AI 健康助手，请问您今天有什么不适或健康方面的问题？</text>
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
            <!-- 加载动画 -->
            <view v-if="msg.loading" class="loading-dots">
              <view class="dot"></view>
              <view class="dot"></view>
              <view class="dot"></view>
            </view>
            <text v-else class="message-text">{{ msg.content }}</text>
            
            <!-- 消息时间 -->
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
    <view class="input-area" :style="{ paddingBottom: safeBottom + 'px' }">
      <view class="input-wrapper">
        <input
          v-model="inputMessage"
          class="message-input"
          type="text"
          placeholder="请输入您的症状或问题..."
          confirm-type="send"
          :disabled="isLoading"
          @confirm="sendMessageHandler"
        />
        <view 
          class="send-btn"
          :class="{ active: inputMessage.trim() && !isLoading }"
          @click="sendMessageHandler"
        >
          <text class="send-icon">➤</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { sendMessage, getConversationHistory } from '@/api/dify.js'

// 数据
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const scrollTop = ref(0)
const conversationId = ref('')
const safeBottom = ref(0)

// 快捷问题
const quickQuestions = [
  '头痛是什么原因？',
  '感冒发热怎么办？',
  '口腔溃疡怎么缓解？',
  '牙痛需要看医生吗？',
  '皮肤过敏怎么处理？'
]

// 是否显示快捷问题（没有消息时显示）
const showQuickQuestions = computed(() => messages.value.length === 0)

onLoad(() => {
  // 获取安全区域高度
  const systemInfo = uni.getSystemInfoSync()
  safeBottom.value = systemInfo.safeAreaInsets?.bottom || 0
  
  // 可以尝试恢复之前的会话
  // loadConversationHistory()
})

/**
 * 发送消息处理
 */
const sendMessageHandler = async () => {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value) return

  // 添加用户消息
  addMessage('user', message)
  inputMessage.value = ''
  
  // 发送给 AI
  await sendToAI(message)
}

/**
 * 发送快捷问题
 */
const sendQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessageHandler()
}

/**
 * 发送消息到 AI
 */
const sendToAI = async (message) => {
  isLoading.value = true
  
  // 添加加载中的 AI 消息
  const loadingIndex = messages.value.length
  addMessage('assistant', '', true)
  
  try {
    const response = await sendMessage(message, conversationId.value)
    
    // 保存会话ID
    if (response.conversation_id) {
      conversationId.value = response.conversation_id
    }
    
    // 更新 AI 回复
    messages.value[loadingIndex] = {
      role: 'assistant',
      content: response.answer || '抱歉，我没有理解您的问题，请重新描述一下。',
      time: formatTime()
    }
  } catch (error) {
    console.error('AI 请求失败:', error)
    messages.value[loadingIndex] = {
      role: 'assistant',
      content: '抱歉，服务暂时不可用，请稍后再试。',
      time: formatTime()
    }
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

/**
 * 添加消息到列表
 */
const addMessage = (role, content, loading = false) => {
  messages.value.push({
    role,
    content,
    loading,
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
 * 加载更多历史消息
 */
const loadMoreHistory = async () => {
  if (!conversationId.value) return
  // 可以在这里实现加载更多历史消息的逻辑
}

/**
 * 加载会话历史
 */
const loadConversationHistory = async () => {
  if (!conversationId.value) return
  
  try {
    const res = await getConversationHistory(conversationId.value)
    if (res.data && res.data.length > 0) {
      // 转换历史消息格式
      messages.value = res.data.map(msg => ({
        role: msg.role,
        content: msg.content,
        time: formatTime(new Date(msg.created_at))
      }))
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
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
