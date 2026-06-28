<template>
  <view class="payment-cashier" v-if="visible" style="border: 5px solid red;">
    <view class="cashier-mask" @click="close"></view>
    <view class="cashier-content">
      <view class="cashier-header">
        <text class="close-btn" @click="close">✕</text>
        <text class="title">选择支付方式</text>
        <view class="placeholder"></view>
      </view>

      <view class="cashier-body">
        <!-- 支付金额 -->
        <view class="pay-amount">
          <text class="currency">¥</text>
          <text class="amount">{{ amount }}</text>
        </view>

        <!-- 支付方式列表 -->
        <view class="pay-methods">
          <view 
            v-for="method in payMethods" 
            :key="method.value"
            :class="['pay-method-item', selectedMethod === method.value ? 'active' : '']"
            @click="selectMethod(method.value)"
          >
            <view class="method-icon" :style="{ background: method.bgColor }">
              <text class="icon-text">{{ method.icon }}</text>
            </view>
            <view class="method-info">
              <text class="method-name">{{ method.name }}</text>
              <text class="method-desc" v-if="method.desc">{{ method.desc }}</text>
            </view>
            <view class="method-check">
              <text class="check-icon" v-if="selectedMethod === method.value">✓</text>
            </view>
          </view>
        </view>
      </view>

      <view class="cashier-footer">
        <button class="pay-btn" :disabled="!selectedMethod" @click="confirmPay">
          确认支付 ¥{{ amount }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  amount: {
    type: [Number, String],
    default: '0.00'
  },
  defaultMethod: {
    type: Number,
    default: 2 // 默认微信支付
  }
})

const emit = defineEmits(['close', 'confirm'])

const selectedMethod = ref(props.defaultMethod)

// 支付方式列表 - 与后端保持一致
// 1现金 2微信 3支付宝 4银行卡 5会员卡
const payMethods = [
  {
    value: 2,
    name: '微信支付',
    icon: '💬',
    bgColor: '#07c160',
    desc: '推荐使用微信支付'
  },
  {
    value: 3,
    name: '支付宝',
    icon: '🔷',
    bgColor: '#1677ff',
    desc: ''
  },
  {
    value: 4,
    name: '银行卡',
    icon: '💳',
    bgColor: '#ff9500',
    desc: '支持储蓄卡/信用卡'
  },
  {
    value: 5,
    name: '会员卡',
    icon: '💎',
    bgColor: '#ffd700',
    desc: '使用会员卡余额支付'
  },
  {
    value: 1,
    name: '现金支付',
    icon: '💵',
    bgColor: '#ff6b6b',
    desc: '到院后现场支付'
  }
]

watch(() => props.visible, (newVal) => {
  if (newVal) {
    selectedMethod.value = props.defaultMethod
  }
})

const close = () => {
  emit('close')
}

const selectMethod = (value) => {
  selectedMethod.value = value
}

const confirmPay = () => {
  if (!selectedMethod.value) {
    uni.showToast({ title: '请选择支付方式', icon: 'none' })
    return
  }
  emit('confirm', selectedMethod.value)
}
</script>

<style lang="scss" scoped>
.payment-cashier {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;

  .cashier-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
  }

  .cashier-content {
    position: relative;
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    animation: slideUp 0.3s ease;
  }

  @keyframes slideUp {
    from {
      transform: translateY(100%);
    }
    to {
      transform: translateY(0);
    }
  }

  .cashier-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .close-btn {
      font-size: 36rpx;
      color: #999;
      padding: 10rpx;
    }

    .title {
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
    }

    .placeholder {
      width: 56rpx;
    }
  }

  .cashier-body {
    flex: 1;
    overflow-y: auto;
    padding: 40rpx 30rpx;

    .pay-amount {
      text-align: center;
      margin-bottom: 50rpx;

      .currency {
        font-size: 40rpx;
        color: #333;
        font-weight: bold;
      }

      .amount {
        font-size: 80rpx;
        color: #333;
        font-weight: bold;
      }
    }

    .pay-methods {
      .pay-method-item {
        display: flex;
        align-items: center;
        padding: 30rpx;
        margin-bottom: 20rpx;
        background: #f8f9fa;
        border-radius: 16rpx;
        border: 2rpx solid transparent;
        transition: all 0.3s;

        &.active {
          background: #f0f9f6;
          border-color: #00b894;
        }

        &:active {
          opacity: 0.8;
        }

        .method-icon {
          width: 80rpx;
          height: 80rpx;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 24rpx;

          .icon-text {
            font-size: 40rpx;
          }
        }

        .method-info {
          flex: 1;

          .method-name {
            display: block;
            font-size: 30rpx;
            color: #333;
            font-weight: 500;
            margin-bottom: 6rpx;
          }

          .method-desc {
            display: block;
            font-size: 24rpx;
            color: #999;
          }
        }

        .method-check {
          width: 48rpx;
          height: 48rpx;
          border-radius: 50%;
          border: 2rpx solid #ddd;
          display: flex;
          align-items: center;
          justify-content: center;

          .check-icon {
            font-size: 28rpx;
            color: #00b894;
            font-weight: bold;
          }
        }

        &.active .method-check {
          border-color: #00b894;
          background: #00b894;

          .check-icon {
            color: #fff;
          }
        }
      }
    }
  }

  .cashier-footer {
    padding: 30rpx;
    border-top: 1rpx solid #f0f0f0;

    .pay-btn {
      width: 100%;
      height: 90rpx;
      line-height: 90rpx;
      background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
      color: #fff;
      font-size: 32rpx;
      font-weight: 600;
      border-radius: 45rpx;
      border: none;

      &::after {
        border: none;
      }

      &:disabled {
        background: #ccc;
      }

      &:active {
        opacity: 0.9;
      }
    }
  }
}
</style>
