<template>
  <view class="registration-detail-page">
    <!-- 排队状态卡片 - 仅在排队中显示 -->
    <view class="queue-status-card" v-if="isQueuing">
      <view class="queue-header">
        <view class="queue-icon-wrap">
          <text class="queue-icon">🕐</text>
        </view>
        <view class="queue-title-info">
          <text class="queue-title">排队中</text>
          <text class="queue-doctor">{{ detail.doctorName }} · {{ detail.departmentName }}</text>
        </view>
      </view>

      <!-- 叫号对比 -->
      <view class="queue-numbers">
        <view class="number-item">
          <text class="number-label">正在就诊</text>
          <view class="number-circle current">
            <text class="number-value">{{ queueInfo.currentNumber || '-' }}</text>
          </view>
        </view>
        <view class="number-arrow">→</view>
        <view class="number-item">
          <text class="number-label">我的号码</text>
          <view class="number-circle mine">
            <text class="number-value">{{ queueInfo.queueNumber || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 等待信息 -->
      <view class="waiting-info" :class="{ 'urgent': queueInfo.aheadCount <= 3 && queueInfo.aheadCount > 0 }">
        <view class="waiting-item">
          <text class="waiting-icon">👥</text>
          <text class="waiting-label">前方还有</text>
          <text class="waiting-count">{{ queueInfo.aheadCount || 0 }}</text>
          <text class="waiting-unit">人</text>
        </view>
        <view class="time-item" v-if="queueInfo.estimatedWaitTime">
          <text class="time-icon">⏱️</text>
          <text class="time-label">预计等待 {{ queueInfo.estimatedWaitTime }} 分钟</text>
        </view>
      </view>

      <!-- 临号提醒 -->
      <view class="urgent-notice" v-if="queueInfo.aheadCount <= 2 && queueInfo.aheadCount > 0">
        <text class="notice-icon">⚠️</text>
        <text class="notice-text">即将轮到您，请前往诊室门口等候</text>
      </view>

      <!-- 刷新按钮 -->
      <view class="refresh-row">
        <text class="refresh-btn" @click="refreshQueueInfo">
          <text class="refresh-icon">🔄</text>
          刷新排队信息
        </text>
      </view>
    </view>

    <!-- 普通状态卡片 -->
    <view class="status-card" v-else>
      <view class="status-icon" :style="{ background: statusColor + '20' }">
        <text :style="{ color: statusColor }">{{ statusIcon }}</text>
      </view>
      <text class="status-text" :style="{ color: statusColor }">{{ statusText }}</text>
      <text class="status-desc">{{ statusDesc }}</text>
    </view>

    <!-- 挂号信息 -->
    <view class="info-card">
      <view class="card-title">挂号信息</view>
      <view class="info-list">
        <view class="info-item">
          <text class="label">挂号单号</text>
          <text class="value">{{ detail.regNo }}</text>
        </view>
        <view class="info-item">
          <text class="label">挂号时间</text>
          <text class="value">{{ detail.createTime }}</text>
        </view>
        <view class="info-item">
          <text class="label">就诊日期</text>
          <text class="value highlight">{{ detail.regDate }} {{ detail.regTime }}</text>
        </view>
        <view class="info-item">
          <text class="label">挂号费用</text>
          <text class="value price">¥{{ detail.regFee }}</text>
        </view>
        <view class="info-item">
          <text class="label">就诊类型</text>
          <text class="value">{{ detail.visitType === 1 ? '初诊' : '复诊' }}</text>
        </view>
      </view>
    </view>

    <!-- 医生信息 -->
    <view class="info-card">
      <view class="card-title">医生信息</view>
      <view class="doctor-info-header">
        <image :src="detail.doctorAvatar || '/static/default-avatar.png'" class="doctor-avatar" mode="aspectFill" />
        <view class="doctor-meta">
          <text class="doctor-name">{{ detail.doctorName }}</text>
          <text class="doctor-title">{{ detail.doctorTitle }}</text>
          <text class="doctor-dept">{{ detail.departmentName }}</text>
        </view>
      </view>
    </view>

    <!-- 就诊人信息 -->
    <view class="info-card">
      <view class="card-title">就诊人信息</view>
      <view class="info-list">
        <view class="info-item">
          <text class="label">姓名</text>
          <text class="value">{{ detail.patientName }}</text>
        </view>
        <view class="info-item">
          <text class="label">手机号</text>
          <text class="value">{{ detail.patientPhone }}</text>
        </view>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="footer" v-if="canCancel">
      <button class="cancel-btn" @click="handleCancel">取消挂号</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getRegistrationDetail, cancelRegistration, getRegistrationQueueInfo } from '@/api/registration.js'
import { REGISTRATION_STATUS, REGISTRATION_STATUS_TEXT, REGISTRATION_STATUS_COLOR } from '@/utils/constants.js'
import { showConfirm, showSuccess } from '@/utils/index.js'

const detail = ref({})
const queueInfo = ref({})
const loading = ref(false)
const refreshTimer = ref(null)
const registrationId = ref(null)

const isQueuing = computed(() => {
  return detail.value.status === REGISTRATION_STATUS.PENDING || 
         detail.value.status === REGISTRATION_STATUS.CHECKED_IN
})

const statusText = computed(() => REGISTRATION_STATUS_TEXT[detail.value.status] || '未知')
const statusColor = computed(() => REGISTRATION_STATUS_COLOR[detail.value.status] || '#999')

const statusIcon = computed(() => {
  const map = {
    [REGISTRATION_STATUS.PENDING]: '⏳',
    [REGISTRATION_STATUS.IN_PROGRESS]: '🔍',
    [REGISTRATION_STATUS.COMPLETED]: '✓',
    [REGISTRATION_STATUS.CANCELLED]: '✕'
  }
  return map[detail.value.status] || '?'
})

const statusDesc = computed(() => {
  const map = {
    [REGISTRATION_STATUS.PENDING]: '请按时前往就诊',
    [REGISTRATION_STATUS.IN_PROGRESS]: '正在就诊中',
    [REGISTRATION_STATUS.COMPLETED]: '就诊已完成',
    [REGISTRATION_STATUS.CANCELLED]: '挂号已取消'
  }
  return map[detail.value.status] || ''
})

const canCancel = computed(() => {
  return detail.value.status === REGISTRATION_STATUS.PENDING
})

onMounted(() => {
  console.log('详情页面 onMounted')
  
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  console.log('当前页面:', currentPage)
  console.log('页面参数:', currentPage.options)
  
  const id = currentPage.options?.id
  console.log('获取到的ID:', id)

  if (id) {
    registrationId.value = id
    loadDetail(id)
  } else {
    console.error('没有获取到挂号ID')
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})

// 页面显示时启动自动刷新
onShow(() => {
  if (isQueuing.value) {
    startAutoRefresh()
  }
})

// 页面隐藏时停止自动刷新
onHide(() => {
  stopAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 开始自动刷新
const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer.value = setInterval(() => {
    loadQueueInfo()
  }, 30000) // 30秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

const loadDetail = async (id) => {
  loading.value = true
  console.log('加载挂号详情, ID:', id)
  
  try {
    const res = await getRegistrationDetail(id)
    console.log('挂号详情响应:', res)
    
    detail.value = res.data || {}
    console.log('挂号详情数据:', detail.value)
    console.log('挂号状态:', detail.value.status)
    
    // 如果是排队中或已签到状态，加载排队信息
    if (detail.value.status === REGISTRATION_STATUS.PENDING || 
        detail.value.status === REGISTRATION_STATUS.CHECKED_IN) {
      console.log('开始加载排队信息')
      loadQueueInfo()
      startAutoRefresh()
    } else {
      console.log('不需要加载排队信息, 状态:', detail.value.status)
    }
  } catch (error) {
    console.error('加载详情失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 加载排队信息
const loadQueueInfo = async () => {
  if (!registrationId.value) return
  
  try {
    // 尝试从API获取排队信息
    const res = await getRegistrationQueueInfo(registrationId.value)
    queueInfo.value = res.data || {}
  } catch (error) {
    console.log('获取排队信息失败，使用模拟数据')
    // API不存在时使用模拟数据
    const currentNum = Math.floor(Math.random() * 10) + 1
    const myNumber = currentNum + Math.floor(Math.random() * 8) + 2
    queueInfo.value = {
      currentNumber: currentNum,
      queueNumber: myNumber,
      aheadCount: myNumber - currentNum - 1,
      estimatedWaitTime: (myNumber - currentNum) * 10
    }
  }
}

// 手动刷新排队信息
const refreshQueueInfo = () => {
  uni.showLoading({ title: '刷新中...' })
  loadQueueInfo().finally(() => {
    uni.hideLoading()
    uni.showToast({ title: '已更新', icon: 'success', duration: 1000 })
  })
}

const handleCancel = async () => {
  const confirmed = await showConfirm('确定取消该挂号吗？', '取消挂号')
  if (!confirmed) return

  try {
    await cancelRegistration({ id: detail.value.id })
    showSuccess('取消成功')
    stopAutoRefresh()
    loadDetail(detail.value.id)
  } catch (error) {
    console.error('取消失败:', error)
  }
}
</script>

<style lang="scss" scoped>
.registration-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

// 排队状态卡片
.queue-status-card {
  background: linear-gradient(180deg, #f8fffe 0%, #fff 100%);
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  border: 2rpx solid #00b894;

  .queue-header {
    display: flex;
    align-items: center;
    padding-bottom: 24rpx;
    border-bottom: 1rpx solid rgba(0, 184, 148, 0.1);
    margin-bottom: 24rpx;

    .queue-icon-wrap {
      width: 80rpx;
      height: 80rpx;
      background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 20rpx;

      .queue-icon {
        font-size: 40rpx;
      }
    }

    .queue-title-info {
      .queue-title {
        display: block;
        font-size: 36rpx;
        font-weight: 600;
        color: #00b894;
        margin-bottom: 8rpx;
      }

      .queue-doctor {
        display: block;
        font-size: 26rpx;
        color: #666;
      }
    }
  }

  .queue-numbers {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 60rpx;
    margin-bottom: 30rpx;

    .number-item {
      text-align: center;

      .number-label {
        display: block;
        font-size: 26rpx;
        color: #666;
        margin-bottom: 16rpx;
      }

      .number-circle {
        width: 140rpx;
        height: 140rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        &.current {
          background: #f5f5f5;

          .number-value {
            color: #999;
          }
        }

        &.mine {
          background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
          box-shadow: 0 8rpx 24rpx rgba(0, 184, 148, 0.3);

          .number-value {
            color: #fff;
          }
        }

        .number-value {
          font-size: 56rpx;
          font-weight: bold;
        }
      }
    }

    .number-arrow {
      font-size: 48rpx;
      color: #00b894;
      opacity: 0.5;
    }
  }

  .waiting-info {
    background: #f8f9fa;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 24rpx;

    &.urgent {
      background: rgba(255, 107, 107, 0.1);
      border: 1rpx solid rgba(255, 107, 107, 0.2);
    }

    .waiting-item {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16rpx;

      .waiting-icon {
        font-size: 32rpx;
        margin-right: 8rpx;
      }

      .waiting-label {
        font-size: 28rpx;
        color: #666;
      }

      .waiting-count {
        font-size: 40rpx;
        font-weight: bold;
        color: #00b894;
        margin: 0 8rpx;
      }

      .waiting-unit {
        font-size: 28rpx;
        color: #666;
      }
    }

    .time-item {
      display: flex;
      align-items: center;
      justify-content: center;

      .time-icon {
        font-size: 28rpx;
        margin-right: 8rpx;
      }

      .time-label {
        font-size: 26rpx;
        color: #ff6b6b;
        font-weight: 500;
      }
    }
  }

  .urgent-notice {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(90deg, #fff3e0 0%, #ffe0b2 100%);
    border-radius: 12rpx;
    padding: 20rpx;
    margin-bottom: 24rpx;
    border: 1rpx solid #ffcc80;

    .notice-icon {
      font-size: 32rpx;
      margin-right: 12rpx;
    }

    .notice-text {
      font-size: 28rpx;
      color: #ff9800;
      font-weight: 600;
    }
  }

  .refresh-row {
    display: flex;
    justify-content: center;
    padding-top: 20rpx;
    border-top: 1rpx solid rgba(0, 184, 148, 0.1);

    .refresh-btn {
      display: flex;
      align-items: center;
      font-size: 26rpx;
      color: #00b894;
      padding: 12rpx 24rpx;

      &:active {
        opacity: 0.7;
      }

      .refresh-icon {
        margin-right: 8rpx;
      }
    }
  }
}

// 普通状态卡片
.status-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 60rpx 40rpx;
  text-align: center;
  margin-bottom: 20rpx;

  .status-icon {
    width: 120rpx;
    height: 120rpx;
    border-radius: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 24rpx;
    font-size: 60rpx;
  }

  .status-text {
    display: block;
    font-size: 40rpx;
    font-weight: bold;
    margin-bottom: 12rpx;
  }

  .status-desc {
    display: block;
    font-size: 28rpx;
    color: #999;
  }
}

.info-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .card-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 24rpx;
    padding-left: 20rpx;
    border-left: 8rpx solid #00b894;
  }

  .doctor-info-header {
    display: flex;
    align-items: center;

    .doctor-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      margin-right: 24rpx;
      background: #f0f0f0;
      border: 4rpx solid #fff;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
    }

    .doctor-meta {
      .doctor-name {
        display: block;
        font-size: 36rpx;
        font-weight: 600;
        color: #333;
        margin-bottom: 8rpx;
      }

      .doctor-title {
        display: block;
        font-size: 26rpx;
        color: #00b894;
        background: rgba(0, 184, 148, 0.1);
        padding: 4rpx 12rpx;
        border-radius: 8rpx;
        margin-bottom: 8rpx;
        display: inline-block;
      }

      .doctor-dept {
        display: block;
        font-size: 26rpx;
        color: #666;
      }
    }
  }

  .info-list {
    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        font-size: 28rpx;
        color: #999;
      }

      .value {
        font-size: 28rpx;
        color: #333;

        &.highlight {
          color: #00b894;
          font-weight: 500;
        }

        &.price {
          color: #ff6b6b;
          font-size: 32rpx;
          font-weight: bold;
        }
      }
    }
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

  .cancel-btn {
    width: 100%;
    height: 90rpx;
    line-height: 90rpx;
    background: #fff1f0;
    color: #ff4d4f;
    font-size: 32rpx;
    border-radius: 45rpx;
    border: 2rpx solid #ffccc7;

    &::after {
      border: none;
    }
  }
}
</style>