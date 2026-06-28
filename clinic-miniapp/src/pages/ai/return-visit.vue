<template>
  <view class="ai-return-visit-page">
    <!-- 顶部提示栏 -->
    <view class="header-banner">
      <view class="banner-content">
        <text class="banner-icon">🏥</text>
        <view class="banner-text">
          <text class="banner-title">AI 健康回访</text>
          <text class="banner-subtitle">了解您的恢复情况，提供专业建议</text>
        </view>
      </view>
    </view>

    <!-- 患者信息卡片 -->
    <view v-if="patientInfo.name" class="patient-card">
      <view class="patient-header">
        <view class="patient-avatar">
          <text>{{ patientInfo.gender === '男' ? '👨' : '👩' }}</text>
        </view>
        <view class="patient-info">
          <text class="patient-name">{{ patientInfo.name }}</text>
          <text class="patient-detail">{{ patientInfo.gender }} · {{ patientInfo.age }}岁</text>
        </view>
      </view>
      <view class="visit-info">
        <view class="info-item">
          <text class="info-label">就诊项目</text>
          <text class="info-value">{{ visitItem || '牙科治疗' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">就诊时间</text>
          <text class="info-value">{{ visitDate }}</text>
        </view>
      </view>
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
            <text class="message-text">您好！我是 AI 回访助手，今天来了解一下您的恢复情况。请问您最近感觉怎么样？</text>
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

    <!-- 底部输入区域 -->
    <view class="input-area" :style="{ paddingBottom: safeBottom + 'px' }">
      <view class="input-wrapper">
        <input
          v-model="inputMessage"
          class="message-input"
          type="text"
          placeholder="请输入您的回复..."
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
import { recordTaskOpen, getPatientProfile } from '@/api/miniapp/ai-return-visit.js'
import { sendChatflowMessage } from '@/api/dify-chatflow.js'
import { getPatientList } from '@/api/miniapp/patient.js'

// 数据
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const scrollTop = ref(0)
const conversationId = ref('')
const safeBottom = ref(0)

// 患者信息
const patientInfo = ref({})
const visitItem = ref('')
const visitDate = ref('')

// 页面参数
const pageParams = ref({
  patientId: '',
  planId: '',
  taskId: '',
  from: ''
})

onLoad((options) => {
  console.log('AI回访页面参数:', options)
  
  // 保存页面参数
  pageParams.value = {
    patientId: options.patient_id || options.patientId || '',
    planId: options.plan_id || options.planId || '',
    taskId: options.task_id || options.taskId || '',
    from: options.from || 'miniapp'
  }
  
  // 获取安全区域高度
  const systemInfo = uni.getSystemInfoSync()
  safeBottom.value = systemInfo.safeAreaInsets?.bottom || 0
  
  // 初始化页面
  initPage()
})

/**
 * 初始化页面
 */
const initPage = async () => {
  try {
    // 检查必要参数
    if (!pageParams.value.patientId) {
      // 尝试从本地缓存获取 patientId
      const userInfoStr = uni.getStorageSync('userInfo')
      const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null
      console.log('userInfo:', userInfo)
      let patientId = userInfo?.patientId || userInfo?.patient?.id
      console.log('从缓存获取的 patientId:', patientId)

      // 如果还是没有，通过接口获取默认患者
      if (!patientId) {
        try {
          const patientRes = await getPatientList({ page: 1, pageSize: 1 })
          console.log('getPatientList 响应:', patientRes)
          if (patientRes.code === 200 && patientRes.data && patientRes.data.length > 0) {
            console.log('第一个患者数据:', patientRes.data[0])
            patientId = patientRes.data[0].id || patientRes.data[0].patientId
            console.log('通过接口获取默认患者ID:', patientId)
          }
        } catch (e) {
          console.error('获取患者列表失败:', e)
        }
      }

      if (patientId) {
        pageParams.value.patientId = patientId
        console.log('最终使用的 patientId:', pageParams.value.patientId)
      } else {
        uni.showToast({ title: '缺少患者信息', icon: 'none' })
        return
      }
    }

    // 加载患者信息
    await loadPatientInfo()

    // 记录任务打开
    if (pageParams.value.taskId) {
      recordTaskOpened()
    }
  } catch (error) {
    console.error('初始化页面失败:', error)
  }
}

/**
 * 加载患者信息
 */
const loadPatientInfo = async () => {
  try {
    const res = await getPatientProfile(pageParams.value.patientId)
    console.log('获取患者档案响应:', res)
    if (res.data) {
      patientInfo.value = {
        name: res.data.patientName || res.data.patient_name || '患者',
        gender: res.data.genderText || res.data.gender || '女',
        age: res.data.age || 35
      }
      visitItem.value = res.data.visitItem || res.data.visit_item || '牙科治疗'
      visitDate.value = res.data.visitDate || res.data.visit_date || ''
    }
  } catch (error) {
    console.error('获取患者信息失败:', error)
  }
}

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
 * 发送消息到 AI
 */
const sendToAI = async (message) => {
  isLoading.value = true
  
  // 添加加载中的 AI 消息
  const loadingIndex = messages.value.length
  addMessage('assistant', '', true)
  
  try {
    // 准备 inputs 数据
    const inputs = {
      patient_id: pageParams.value.patientId,
      patient_name: patientInfo.value.name,
      patient_gender: patientInfo.value.gender,
      patient_age: patientInfo.value.age,
      plan_id: pageParams.value.planId,
      task_id: pageParams.value.taskId,
      visit_item: visitItem.value,
      visit_date: visitDate.value
    }
    
    // 调用 Dify Chatflow API
    const res = await sendChatflowMessage(message, conversationId.value, inputs)
    
    console.log('AI 响应:', JSON.stringify(res))
    
    // 解析响应数据
    const responseData = (res && res.code === 200 && res.data) ? res.data : res
    
    console.log('解析后:', JSON.stringify(responseData))
    console.log('answer:', responseData ? responseData.answer : 'undefined')
    
    // 保存会话ID
    if (responseData && responseData.conversation_id) {
      conversationId.value = responseData.conversation_id
    }
    
    // 更新 AI 回复
    messages.value[loadingIndex] = {
      role: 'assistant',
      content: (responseData && responseData.answer) ? responseData.answer : '抱歉，我没有理解您的回复，请重新描述一下。',
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
 * 记录任务已打开
 */
const recordTaskOpened = async () => {
  try {
    await recordTaskOpen(pageParams.value.taskId)
  } catch (error) {
    console.error('记录任务打开失败:', error)
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
.ai-return-visit-page {
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
}

// 患者信息卡片
.patient-card {
  margin: 20rpx 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  
  .patient-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;
    
    .patient-avatar {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      background: linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 20rpx;
      font-size: 40rpx;
    }
    
    .patient-info {
      .patient-name {
        display: block;
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
        margin-bottom: 6rpx;
      }
      
      .patient-detail {
        display: block;
        font-size: 24rpx;
        color: #999;
      }
    }
  }
  
  .visit-info {
    display: flex;
    gap: 40rpx;
    
    .info-item {
      display: flex;
      flex-direction: column;
      
      .info-label {
        font-size: 22rpx;
        color: #999;
        margin-bottom: 4rpx;
      }
      
      .info-value {
        font-size: 26rpx;
        color: #333;
        font-weight: 500;
      }
    }
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

// 底部输入区域
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
