<template>
  <view v-if="canSwitch" class="clinic-switcher" @click="switchClinic">
    <text class="icon">🏥</text>
    <text class="name">{{ clinicName }}</text>
    <text class="arrow">切换</text>
  </view>
  <view v-else class="clinic-switcher readonly">
    <text class="icon">🏥</text>
    <text class="name">{{ clinicName }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const clinicName = computed(() => {
  return uni.getStorageSync('clinicName') || '未选择诊所'
})

const clinicList = computed(() => {
  try {
    return JSON.parse(uni.getStorageSync('clinicList') || '[]')
  } catch {
    return []
  }
})

const canSwitch = computed(() => clinicList.value.length > 1)

const switchClinic = () => {
  uni.navigateTo({ url: '/pages/clinic/select' })
}
</script>

<style lang="scss" scoped>
.clinic-switcher {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 24rpx;
  background: rgba(24, 144, 255, 0.1);
  border-radius: 32rpx;
  
  &.readonly {
    background: #f5f5f5;
  }
  
  .icon {
    font-size: 28rpx;
    margin-right: 8rpx;
  }
  
  .name {
    font-size: 26rpx;
    color: #1890ff;
    max-width: 200rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .arrow {
    font-size: 22rpx;
    color: #1890ff;
    margin-left: 8rpx;
    padding: 4rpx 12rpx;
    background: rgba(24, 144, 255, 0.15);
    border-radius: 12rpx;
  }
}
</style>
