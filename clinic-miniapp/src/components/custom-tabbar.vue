<template>
  <view class="custom-tabbar">
    <view 
      v-for="(item, index) in tabList" 
      :key="index"
      :class="['tab-item', current === index ? 'active' : '']"
      @click="switchTab(index)"
    >
      <view class="tab-icon">{{ current === index ? item.selectedIcon : item.icon }}</view>
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  current: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['change'])

const tabList = [
  { 
    pagePath: '/pages/index/index',
    text: '首页', 
    icon: '🏠', 
    selectedIcon: '🏠' 
  },
  { 
    pagePath: '/pages/doctor/list',
    text: '医生', 
    icon: '👨‍⚕️', 
    selectedIcon: '👨‍⚕️' 
  },
  { 
    pagePath: '/pages/registration/list',
    text: '挂号', 
    icon: '📋', 
    selectedIcon: '📋' 
  },
  { 
    pagePath: '/pages/mine/index',
    text: '我的', 
    icon: '👤', 
    selectedIcon: '👤' 
  }
]

const current = ref(props.current)

watch(() => props.current, (newVal) => {
  current.value = newVal
})

const switchTab = (index) => {
  if (current.value === index) return
  
  const url = tabList[index].pagePath
  uni.switchTab({ url })
  
  current.value = index
  emit('change', index)
}
</script>

<style lang="scss" scoped>
.custom-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background: #ffffff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1rpx solid #f0f0f0;
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 999;

  .tab-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    height: 100%;

    &.active {
      .tab-icon {
        transform: scale(1.1);
      }
      .tab-text {
        color: #00b894;
        font-weight: 600;
      }
    }

    &:active {
      opacity: 0.7;
    }

    .tab-icon {
      font-size: 44rpx;
      margin-bottom: 6rpx;
      transition: transform 0.2s;
    }

    .tab-text {
      font-size: 22rpx;
      color: #999;
      transition: color 0.2s;
    }
  }
}
</style>
