<template>
  <view class="registration-list-page">
    <!-- 页面头部 -->
    <view class="page-header">
      <text class="page-title">我的挂号</text>
      <view class="refresh-btn" @click="manualRefresh">
        <text class="refresh-icon">🔄</text>
        <text class="refresh-text">刷新</text>
      </view>
    </view>

    <!-- 就诊提示 -->
    <view class="visit-tips" v-if="hasPendingRegistration">
      <text class="tips-icon">📢</text>
      <text class="tips-text">请留意叫号，过号需重新排队</text>
    </view>

    <!-- 挂号列表 - 按医生分组 -->
    <scroll-view class="registration-list" scroll-y @scrolltolower="loadMore" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <!-- 今日就诊卡片 -->
      <view v-for="(item, index) in registrationList" :key="item.id" class="doctor-queue-card" :class="{ 'active': item.status === REGISTRATION_STATUS.PENDING }">
        <!-- 医生信息头部 -->
        <view class="doctor-header">
          <view class="doctor-info">
            <image :src="item.doctorAvatar || '/static/default-avatar.png'" class="doctor-avatar" mode="aspectFill" />
            <view class="doctor-meta">
              <view class="name-row">
                <text class="doctor-name">{{ item.doctorName }}</text>
                <text class="doctor-title">{{ item.doctorTitle }}</text>
              </view>
              <text class="dept-name">{{ item.deptName }}</text>
            </view>
          </view>
          <view class="status-badge" :style="{ background: getStatusColor(item.status) }">
            {{ getStatusText(item.status) }}
          </view>
        </view>

        <!-- 排队信息区域 - 只有已挂号(排队中)状态显示 -->
        <view class="queue-section" v-if="item.status === REGISTRATION_STATUS.PENDING || item.status === REGISTRATION_STATUS.CHECKED_IN">
          <view class="queue-header">
            <text class="queue-title">当前排队情况</text>
            <text class="queue-time">{{ item.regTime }}</text>
          </view>

          <!-- 排队进度 -->
          <view class="queue-progress">
            <view class="progress-numbers">
              <view class="number-box current">
                <text class="number-label">正在就诊</text>
                <text class="number-value">{{ item.currentNumber || '-' }}</text>
              </view>
              <view class="progress-arrow">→</view>
              <view class="number-box mine">
                <text class="number-label">我的号码</text>
                <text class="number-value">{{ item.queueNumber || '-' }}</text>
              </view>
            </view>

            <view class="progress-bar">
              <view class="progress-track"></view>
              <view class="progress-fill" :style="{ width: getProgressWidth(item) + '%' }"></view>
            </view>
          </view>

          <!-- 前方等待人数 -->
          <view class="waiting-info" :class="{ 'urgent': item.aheadCount <= 3 && item.aheadCount > 0 }">
            <view class="waiting-count">
              <text class="count-icon">👥</text>
              <text class="count-text">前方还有</text>
              <text class="count-number">{{ item.aheadCount || 0 }}</text>
              <text class="count-text">人</text>
            </view>
            <view class="estimated-time" v-if="item.estimatedWaitTime">
              <text class="time-icon">⏱️</text>
              <text class="time-text">预计等待 {{ item.estimatedWaitTime }} 分钟</text>
            </view>
          </view>

          <!-- 即将就诊提醒 -->
          <view class="urgent-notice" v-if="item.aheadCount <= 2 && item.aheadCount > 0">
            <text class="notice-icon">⚠️</text>
            <text class="notice-text">即将轮到您，请前往诊室门口等候</text>
          </view>

          <!-- 操作按钮 -->
          <view class="action-bar">
            <button class="btn-cancel" @click="handleCancel(item)">取消挂号</button>
            <button class="btn-detail" @click="handleView(item)">查看详情</button>
          </view>
        </view>

        <!-- 非排队状态显示简要信息 -->
        <view class="simple-info" v-else>
          <view class="info-row">
            <text class="label">就诊时间</text>
            <text class="value">{{ item.regDate }} {{ item.regTime }}</text>
          </view>
          <view class="info-row">
            <text class="label">就诊人</text>
            <text class="value">{{ item.patientName }}</text>
          </view>
          <view class="info-row">
            <text class="label">挂号费用</text>
            <text class="value price">¥{{ item.regFee }}</text>
          </view>
          <view class="action-bar">
            <button class="btn-detail" @click="handleView(item)">查看详情</button>
          </view>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />
      
      <empty-data
        v-if="registrationList.length === 0 && !loading"
        text="暂无挂号记录"
        subtext="可以去预约医生哦"
        :show-button="true"
        button-text="去挂号"
        @click="goToDoctor"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onPullDownRefresh, onShow, onHide } from '@dcloudio/uni-app'
import EmptyData from '@/components/empty-data.vue'
import { getMyRegistrationList, cancelRegistration } from '@/api/registration.js'
import { REGISTRATION_STATUS, REGISTRATION_STATUS_TEXT } from '@/utils/constants.js'
import { showConfirm, showSuccess } from '@/utils/index.js'

const loading = ref(false)
const refreshing = ref(false)
const registrationList = ref([])
const pageNum = ref(1)
const pageSize = 10
const loadStatus = ref('more')
const autoRefreshTimer = ref(null)

// 计算是否有待就诊的挂号
const hasPendingRegistration = computed(() => {
  return registrationList.value.some(item => item.status === REGISTRATION_STATUS.PENDING)
})

onMounted(() => {
  console.log('挂号列表页面 onMounted')
  const clinicId = uni.getStorageSync('clinicId')
  console.log('当前诊所ID:', clinicId)
  if (clinicId) {
    loadRegistrations()
  } else {
    console.log('没有诊所ID，不加载数据')
    uni.showToast({ title: '请先选择诊所', icon: 'none' })
  }
})

// 页面显示时启动自动刷新
onShow(() => {
  console.log('挂号列表页面 onShow')
  const clinicId = uni.getStorageSync('clinicId')
  if (clinicId && registrationList.value.length > 0) {
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

onPullDownRefresh(() => {
  pageNum.value = 1
  loadRegistrations().finally(() => {
    uni.stopPullDownRefresh()
  })
})

// 开始自动刷新
const startAutoRefresh = () => {
  stopAutoRefresh()
  autoRefreshTimer.value = setInterval(() => {
    loadRegistrations(false)
  }, 30000) // 30秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  pageNum.value = 1
  loadRegistrations().finally(() => {
    refreshing.value = false
  })
}

// 手动刷新
const manualRefresh = () => {
  uni.showLoading({ title: '刷新中...' })
  pageNum.value = 1
  loadRegistrations(false).finally(() => {
    uni.hideLoading()
    uni.showToast({ title: '已更新', icon: 'success', duration: 1000 })
  })
}

// 计算进度条宽度
const getProgressWidth = (item) => {
  if (!item.queueNumber || !item.currentNumber) return 0
  const progress = (item.currentNumber / item.queueNumber) * 100
  return Math.min(progress, 95)
}

// 获取状态文本
const getStatusText = (status) => {
  return REGISTRATION_STATUS_TEXT[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status) => {
  const map = {
    [REGISTRATION_STATUS.PENDING]: 'linear-gradient(135deg, #00b894 0%, #00cec9 100%)',
    [REGISTRATION_STATUS.CHECKED_IN]: 'linear-gradient(135deg, #1890ff 0%, #36cfc9 100%)',
    [REGISTRATION_STATUS.IN_PROGRESS]: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
    [REGISTRATION_STATUS.COMPLETED]: '#999',
    [REGISTRATION_STATUS.CANCELLED]: '#ff4d4f',
    [REGISTRATION_STATUS.REFUNDED]: '#ff7875',
    [REGISTRATION_STATUS.MISSED]: '#faad14'
  }
  return map[status] || '#999'
}

const loadRegistrations = async (showLoading = true) => {
  console.log('开始加载挂号列表, showLoading:', showLoading)
  if (loading.value && showLoading) {
    console.log('正在加载中，跳过')
    return
  }
  if (showLoading) loading.value = true

  try {
    const params = {
      pageNum: pageNum.value,
      pageSize
    }
    console.log('请求参数:', params)

    const res = await getMyRegistrationList(params)
    console.log('挂号列表响应:', res)
    const list = res.data.list || []

    // 后端已返回实时排队数据，直接使用
    const listWithQueueInfo = list.map((item) => {
      // 确保排队中的记录有默认显示值
      if (item.status === REGISTRATION_STATUS.PENDING || item.status === REGISTRATION_STATUS.CHECKED_IN) {
        return {
          ...item,
          currentNumber: item.currentNumber ?? 0,
          queueNumber: item.queueNo,
          aheadCount: item.aheadCount ?? 0,
          estimatedWaitTime: item.expectedWaitTime ?? 0
        }
      }
      return item
    })

    // 排序：排队中的在前面，然后按时间排序
    listWithQueueInfo.sort((a, b) => {
      if (a.status === REGISTRATION_STATUS.PENDING && b.status !== REGISTRATION_STATUS.PENDING) return -1
      if (a.status !== REGISTRATION_STATUS.PENDING && b.status === REGISTRATION_STATUS.PENDING) return 1
      return new Date(b.regDate + ' ' + b.regTime) - new Date(a.regDate + ' ' + a.regTime)
    })

    if (pageNum.value === 1) {
      registrationList.value = listWithQueueInfo
    } else {
      registrationList.value.push(...listWithQueueInfo)
    }

    loadStatus.value = list.length < pageSize ? 'noMore' : 'more'
  } catch (error) {
    console.error('加载挂号列表失败:', error)
    if (showLoading) {
      uni.showToast({ title: '加载失败', icon: 'none' })
    }
  } finally {
    if (showLoading) loading.value = false
  }
}

const loadMore = () => {
  if (loadStatus.value === 'noMore' || loading.value) return
  pageNum.value++
  loadRegistrations()
}

const handleCancel = async (item) => {
  const confirmed = await showConfirm(`确定取消 ${item.doctorName} 医生的挂号吗？`, '取消挂号')
  if (!confirmed) return

  try {
    await cancelRegistration({ id: item.id })
    showSuccess('取消成功')
    loadRegistrations()
  } catch (error) {
    console.error('取消挂号失败:', error)
  }
}

const handleView = (item) => {
  console.log('点击查看详情:', item)
  console.log('跳转URL:', `/pages/registration/detail?id=${item.id}`)
  
  if (!item.id) {
    uni.showToast({ title: '挂号ID不存在', icon: 'none' })
    return
  }
  
  uni.navigateTo({
    url: `/pages/registration/detail?id=${item.id}`,
    success: () => {
      console.log('页面跳转成功')
    },
    fail: (err) => {
      console.error('页面跳转失败:', err)
      uni.showToast({ title: '跳转失败', icon: 'none' })
    }
  })
}

const goToDoctor = () => {
  uni.switchTab({ url: '/pages/doctor/list' })
}
</script>

<style lang="scss" scoped>
.registration-list-page {
  min-height: 100vh;
  background: #f5f7fa;
}

// 页面头部
.page-header {
  background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
  padding: 80rpx 30rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .page-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #fff;
  }

  .refresh-btn {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.2);
    padding: 12rpx 24rpx;
    border-radius: 32rpx;

    &:active {
      background: rgba(255, 255, 255, 0.3);
    }

    .refresh-icon {
      font-size: 24rpx;
      margin-right: 8rpx;
    }

    .refresh-text {
      font-size: 24rpx;
      color: #fff;
    }
  }
}

// 就诊提示
.visit-tips {
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, #fff9e6 0%, #fff5d6 100%);
  padding: 20rpx 30rpx;
  margin: 0 30rpx;
  border-radius: 0 0 16rpx 16rpx;
  border: 1rpx solid #ffe58f;
  border-top: none;

  .tips-icon {
    font-size: 32rpx;
    margin-right: 12rpx;
  }

  .tips-text {
    font-size: 26rpx;
    color: #d48806;
    font-weight: 500;
  }
}

// 挂号列表
.registration-list {
  height: calc(100vh - 180rpx);
  padding: 20rpx 30rpx;
}

// 医生排队卡片
.doctor-queue-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  &.active {
    border: 2rpx solid #00b894;
    background: linear-gradient(180deg, #f8fffe 0%, #fff 100%);
  }
}

// 医生头部
.doctor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 20rpx;

  .doctor-info {
    display: flex;
    align-items: center;

    .doctor-avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
      margin-right: 20rpx;
      background: #f0f0f0;
      border: 4rpx solid #fff;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
    }

    .doctor-meta {
      .name-row {
        display: flex;
        align-items: center;
        margin-bottom: 8rpx;

        .doctor-name {
          font-size: 32rpx;
          font-weight: 600;
          color: #333;
          margin-right: 12rpx;
        }

        .doctor-title {
          font-size: 24rpx;
          color: #00b894;
          background: rgba(0, 184, 148, 0.1);
          padding: 4rpx 12rpx;
          border-radius: 8rpx;
        }
      }

      .dept-name {
        font-size: 26rpx;
        color: #666;
      }
    }
  }

  .status-badge {
    padding: 8rpx 20rpx;
    border-radius: 24rpx;
    font-size: 24rpx;
    color: #fff;
    font-weight: 500;
  }
}

// 排队区域
.queue-section {
  .queue-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .queue-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #333;
    }

    .queue-time {
      font-size: 24rpx;
      color: #999;
    }
  }
}

// 排队进度
.queue-progress {
  background: linear-gradient(135deg, #f8fffe 0%, #e8f5f0 100%);
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;

  .progress-numbers {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 60rpx;
    margin-bottom: 24rpx;

    .number-box {
      text-align: center;

      &.current {
        .number-value {
          background: #f5f5f5;
          color: #999;
        }
      }

      &.mine {
        .number-value {
          background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
          color: #fff;
          box-shadow: 0 4rpx 16rpx rgba(0, 184, 148, 0.3);
        }
      }

      .number-label {
        display: block;
        font-size: 24rpx;
        color: #666;
        margin-bottom: 12rpx;
      }

      .number-value {
        display: block;
        width: 120rpx;
        height: 120rpx;
        line-height: 120rpx;
        border-radius: 50%;
        font-size: 48rpx;
        font-weight: bold;
      }
    }

    .progress-arrow {
      font-size: 48rpx;
      color: #00b894;
      opacity: 0.5;
    }
  }

  .progress-bar {
    position: relative;
    height: 16rpx;
    background: rgba(0, 184, 148, 0.1);
    border-radius: 8rpx;
    overflow: hidden;

    .progress-track {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
    }

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #00b894 0%, #00cec9 100%);
      border-radius: 8rpx;
      transition: width 0.5s ease;
    }
  }
}

// 前方等待信息
.waiting-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;

  &.urgent {
    background: rgba(255, 107, 107, 0.1);
    border: 1rpx solid rgba(255, 107, 107, 0.2);
  }

  .waiting-count {
    display: flex;
    align-items: center;

    .count-icon {
      font-size: 32rpx;
      margin-right: 8rpx;
    }

    .count-text {
      font-size: 28rpx;
      color: #666;
    }

    .count-number {
      font-size: 40rpx;
      font-weight: bold;
      color: #00b894;
      margin: 0 8rpx;
    }
  }

  .estimated-time {
    display: flex;
    align-items: center;

    .time-icon {
      font-size: 28rpx;
      margin-right: 8rpx;
    }

    .time-text {
      font-size: 26rpx;
      color: #ff6b6b;
      font-weight: 500;
    }
  }
}

// 紧急提醒
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

// 简要信息（非排队状态）
.simple-info {
  padding: 20rpx 0;

  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;

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

      &.price {
        color: #ff6b6b;
        font-weight: 500;
      }
    }
  }
}

// 操作按钮
.action-bar {
  display: flex;
  gap: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;

  .btn-cancel,
  .btn-detail {
    flex: 1;
    margin: 0;
    padding: 20rpx 0;
    font-size: 28rpx;
    border-radius: 32rpx;
    line-height: 1.5;
    font-weight: 500;

    &::after {
      border: none;
    }
  }

  .btn-cancel {
    background: #f5f5f5;
    color: #666;
  }

  .btn-detail {
    background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
    color: #fff;
    box-shadow: 0 4rpx 16rpx rgba(0, 184, 148, 0.3);
  }
}
</style>
