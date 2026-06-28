<template>
  <view class="registration-item" :class="{ 'queuing': isQueuing }">
    <!-- 排队状态头部 - 仅在待就诊时显示 -->
    <view class="queue-header" v-if="isQueuing">
      <view class="queue-status">
        <view class="queue-icon">🕐</view>
        <view class="queue-info">
          <text class="queue-title">正在排队中</text>
          <text class="queue-doctor">{{ data.doctorName }} · {{ data.deptName }}</text>
        </view>
      </view>
      <view class="queue-badge" v-if="data.queueNumber">
        <text class="queue-label">排队号</text>
        <text class="queue-num">{{ data.queueNumber }}</text>
      </view>
    </view>

    <!-- 普通头部 -->
    <view class="header" v-else>
      <text class="reg-no">挂号单: {{ data.regNo }}</text>
      <text class="status" :style="{ color: statusColor }">{{ statusText }}</text>
    </view>
    
    <!-- 排队信息卡片 - 仅在待就诊时显示 -->
    <view class="queue-card" v-if="isQueuing">
      <view class="queue-progress">
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: queueProgress + '%' }"></view>
        </view>
        <view class="progress-text">
          <text class="waiting-count">
            <text class="highlight">{{ data.aheadCount || 0 }}</text>
            人排队中
          </text>
          <text class="estimated-time" v-if="data.estimatedWaitTime">
            预计等待 {{ data.estimatedWaitTime }} 分钟
          </text>
        </view>
      </view>
      
      <view class="queue-stats">
        <view class="stat-item">
          <text class="stat-value current">{{ data.currentNumber || '-' }}</text>
          <text class="stat-label">当前叫号</text>
        </view>
        <view class="stat-arrow">→</view>
        <view class="stat-item">
          <text class="stat-value mine">{{ data.queueNumber || '-' }}</text>
          <text class="stat-label">我的号码</text>
        </view>
      </view>

      <view class="queue-notice" v-if="data.aheadCount <= 3 && data.aheadCount > 0">
        <text class="notice-icon">⚠️</text>
        <text class="notice-text">即将轮到您，请做好准备</text>
      </view>
    </view>
    
    <!-- 基本信息 -->
    <view class="body" :class="{ 'queue-body': isQueuing }">
      <view class="info-row">
        <text class="label">就诊医生</text>
        <text class="value">{{ data.doctorName }} {{ data.doctorTitle }}</text>
      </view>
      <view class="info-row">
        <text class="label">就诊时间</text>
        <text class="value">{{ data.regDate }} {{ data.regTime }}</text>
      </view>
      <view class="info-row">
        <text class="label">就诊人</text>
        <text class="value">{{ data.patientName }}</text>
      </view>
      <view class="info-row">
        <text class="label">挂号费用</text>
        <text class="value price">¥{{ data.regFee }}</text>
      </view>
    </view>
    
    <!-- 操作按钮 -->
    <view class="footer" v-if="showActions">
      <view class="action-left" v-if="isQueuing">
        <text class="refresh-text" @click="handleRefresh">刷新状态</text>
      </view>
      <view class="action-right">
        <button 
          v-if="canCancel" 
          class="btn cancel"
          @click="handleCancel"
        >取消挂号</button>
        <button 
          v-if="canView" 
          class="btn view"
          @click="handleView"
        >查看详情</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { 
  REGISTRATION_STATUS, 
  REGISTRATION_STATUS_TEXT, 
  REGISTRATION_STATUS_COLOR 
} from '@/utils/constants.js'

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  showActions: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['cancel', 'view', 'refresh'])

const statusText = computed(() => {
  return REGISTRATION_STATUS_TEXT[props.data.status] || '未知'
})

const statusColor = computed(() => {
  return REGISTRATION_STATUS_COLOR[props.data.status] || '#999'
})

const isQueuing = computed(() => {
  return props.data.status === REGISTRATION_STATUS.PENDING
})

// 排队进度百分比
const queueProgress = computed(() => {
  if (!props.data.queueNumber || !props.data.currentNumber) return 0
  const total = props.data.queueNumber
  const current = props.data.currentNumber
  const progress = (current / total) * 100
  return Math.min(progress, 95) // 最高显示95%
})

const canCancel = computed(() => {
  return props.data.status === REGISTRATION_STATUS.PENDING
})

const canView = computed(() => {
  return true
})

const handleCancel = () => {
  emit('cancel', props.data)
}

const handleView = () => {
  emit('view', props.data)
}

const handleRefresh = () => {
  emit('refresh', props.data)
}
</script>

<style lang="scss" scoped>
.registration-item {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  
  &.queuing {
    border: 2rpx solid #00b894;
    background: linear-gradient(180deg, #f8fffe 0%, #fff 100%);
  }
  
  // 排队状态头部
  .queue-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid rgba(0, 184, 148, 0.1);
    margin-bottom: 20rpx;
    
    .queue-status {
      display: flex;
      align-items: center;
      
      .queue-icon {
        width: 64rpx;
        height: 64rpx;
        background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 36rpx;
        margin-right: 16rpx;
        animation: pulse 2s infinite;
      }
      
      @keyframes pulse {
        0%, 100% { transform: scale(1); }
        50% { transform: scale(1.05); }
      }
      
      .queue-info {
        .queue-title {
          display: block;
          font-size: 32rpx;
          font-weight: 600;
          color: #00b894;
          margin-bottom: 4rpx;
        }
        
        .queue-doctor {
          display: block;
          font-size: 24rpx;
          color: #666;
        }
      }
    }
    
    .queue-badge {
      background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
      border-radius: 16rpx;
      padding: 12rpx 24rpx;
      text-align: center;
      
      .queue-label {
        display: block;
        font-size: 20rpx;
        color: rgba(255, 255, 255, 0.9);
        margin-bottom: 4rpx;
      }
      
      .queue-num {
        display: block;
        font-size: 40rpx;
        font-weight: bold;
        color: #fff;
      }
    }
  }
  
  // 普通头部
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;
    margin-bottom: 20rpx;
    
    .reg-no {
      font-size: 26rpx;
      color: #666;
    }
    
    .status {
      font-size: 26rpx;
      font-weight: 500;
    }
  }
  
  // 排队信息卡片
  .queue-card {
    background: linear-gradient(135deg, #e8f5f0 0%, #f0f9f6 100%);
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 24rpx;
    
    .queue-progress {
      margin-bottom: 24rpx;
      
      .progress-bar {
        height: 12rpx;
        background: rgba(0, 184, 148, 0.15);
        border-radius: 6rpx;
        overflow: hidden;
        margin-bottom: 16rpx;
        
        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #00b894 0%, #00cec9 100%);
          border-radius: 6rpx;
          transition: width 0.5s ease;
        }
      }
      
      .progress-text {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .waiting-count {
          font-size: 26rpx;
          color: #666;
          
          .highlight {
            font-size: 36rpx;
            font-weight: bold;
            color: #00b894;
            margin-right: 8rpx;
          }
        }
        
        .estimated-time {
          font-size: 24rpx;
          color: #ff6b6b;
          background: rgba(255, 107, 107, 0.1);
          padding: 6rpx 16rpx;
          border-radius: 8rpx;
        }
      }
    }
    
    .queue-stats {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 40rpx;
      padding: 20rpx 0;
      border-top: 1rpx solid rgba(0, 184, 148, 0.1);
      
      .stat-item {
        text-align: center;
        
        .stat-value {
          display: block;
          width: 100rpx;
          height: 100rpx;
          line-height: 100rpx;
          border-radius: 50%;
          font-size: 48rpx;
          font-weight: bold;
          margin-bottom: 12rpx;
          
          &.current {
            background: #f5f5f5;
            color: #999;
          }
          
          &.mine {
            background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
            color: #fff;
            box-shadow: 0 4rpx 16rpx rgba(0, 184, 148, 0.3);
          }
        }
        
        .stat-label {
          font-size: 24rpx;
          color: #666;
        }
      }
      
      .stat-arrow {
        font-size: 40rpx;
        color: #00b894;
        opacity: 0.5;
      }
    }
    
    .queue-notice {
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255, 193, 7, 0.1);
      border-radius: 12rpx;
      padding: 16rpx;
      margin-top: 20rpx;
      
      .notice-icon {
        font-size: 32rpx;
        margin-right: 12rpx;
      }
      
      .notice-text {
        font-size: 26rpx;
        color: #ff9800;
        font-weight: 500;
      }
    }
  }
  
  // 基本信息
  .body {
    &.queue-body {
      background: rgba(255, 255, 255, 0.6);
      border-radius: 12rpx;
      padding: 20rpx;
    }
    
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
  .footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 24rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #f0f0f0;
    
    .action-left {
      .refresh-text {
        font-size: 26rpx;
        color: #00b894;
        padding: 12rpx 24rpx;
        
        &:active {
          opacity: 0.7;
        }
      }
    }
    
    .action-right {
      display: flex;
      gap: 20rpx;
    }
    
    .btn {
      margin: 0;
      padding: 16rpx 36rpx;
      font-size: 26rpx;
      border-radius: 32rpx;
      line-height: 1.5;
      font-weight: 500;
      
      &::after {
        border: none;
      }
      
      &.cancel {
        background: #f5f5f5;
        color: #666;
      }
      
      &.view {
        background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
        color: #fff;
        box-shadow: 0 4rpx 16rpx rgba(0, 184, 148, 0.3);
      }
    }
  }
}
</style>
