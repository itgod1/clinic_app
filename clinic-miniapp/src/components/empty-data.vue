<template>
  <view class="empty-data" :style="{ paddingTop: top + 'rpx', paddingBottom: bottom + 'rpx' }">
    <image 
      :src="image || '/static/empty.png'" 
      class="empty-image" 
      mode="aspectFit"
    />
    <text class="empty-text">{{ text }}</text>
    <text class="empty-subtext" v-if="subtext">{{ subtext }}</text>
    <button 
      v-if="showButton" 
      class="empty-btn"
      @click="handleClick"
    >{{ buttonText }}</button>
  </view>
</template>

<script setup>
const props = defineProps({
  text: {
    type: String,
    default: '暂无数据'
  },
  subtext: {
    type: String,
    default: ''
  },
  image: {
    type: String,
    default: ''
  },
  showButton: {
    type: Boolean,
    default: false
  },
  buttonText: {
    type: String,
    default: '去逛逛'
  },
  top: {
    type: Number,
    default: 100
  },
  bottom: {
    type: Number,
    default: 100
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  emit('click')
}
</script>

<style lang="scss" scoped>
.empty-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  .empty-image {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 30rpx;
  }
  
  .empty-text {
    font-size: 30rpx;
    color: #999;
    margin-bottom: 12rpx;
  }
  
  .empty-subtext {
    font-size: 26rpx;
    color: #bbb;
    margin-bottom: 30rpx;
  }
  
  .empty-btn {
    margin: 0;
    padding: 16rpx 48rpx;
    background: #1890ff;
    color: #fff;
    font-size: 28rpx;
    border-radius: 32rpx;
    line-height: 1.5;
    
    &::after {
      border: none;
    }
  }
}
</style>
