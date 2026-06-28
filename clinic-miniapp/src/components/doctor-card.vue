<template>
  <view class="doctor-card" @click="handleClick">
    <image 
      :src="doctor.avatarUrl || '/static/default-avatar.png'" 
      class="avatar" 
      mode="aspectFill"
    />
    <view class="info">
      <view class="header">
        <text class="name">{{ doctor.doctorName }}</text>
        <text class="title">{{ doctor.position }}</text>
      </view>
      <text class="department">{{ doctor.deptName }}</text>
      <text class="intro" v-if="doctor.introduction">{{ doctor.introduction }}</text>
      <view class="footer">
        <text class="fee">挂号费: ¥{{ doctor.regFee || 0 }}</text>
        <text class="status" :class="statusClass">{{ statusText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  doctor: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

// 根据排班状态判断
const statusClass = computed(() => {
  // hasSchedule 为 true 表示有可预约排班
  if (props.doctor.hasSchedule === true) return 'online'
  return 'offline'
})

const statusText = computed(() => {
  if (props.doctor.hasSchedule === true) return '可预约'
  return '休息中'
})

const handleClick = () => {
  emit('click', props.doctor)
}
</script>

<style lang="scss" scoped>
.doctor-card {
  display: flex;
  padding: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 60rpx;
    margin-right: 24rpx;
    background: #f0f0f0;
  }
  
  .info {
    flex: 1;
    min-width: 0;
    
    .header {
      display: flex;
      align-items: center;
      margin-bottom: 8rpx;
      
      .name {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
        margin-right: 16rpx;
      }
      
      .title {
        font-size: 24rpx;
        color: #1890ff;
        background: rgba(24, 144, 255, 0.1);
        padding: 4rpx 16rpx;
        border-radius: 8rpx;
      }
    }
    
    .department {
      font-size: 26rpx;
      color: #666;
      display: block;
      margin-bottom: 8rpx;
    }
    
    .intro {
      font-size: 24rpx;
      color: #999;
      display: block;
      margin-bottom: 12rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      
      .fee {
        font-size: 26rpx;
        color: #ff6b6b;
        font-weight: 500;
      }
      
      .status {
        font-size: 24rpx;
        padding: 6rpx 16rpx;
        border-radius: 8rpx;
        
        &.online {
          color: #52c41a;
          background: rgba(82, 196, 26, 0.1);
        }
        
        &.offline {
          color: #999;
          background: #f5f5f5;
        }
      }
    }
  }
}
</style>
