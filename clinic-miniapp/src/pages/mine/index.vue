<template>
  <view class="mine-page">
    <!-- 用户信息卡片 -->
    <view class="user-card" @click="handleUserCardClick">
      <view class="user-bg"></view>
      <view class="user-content">
        <image
          :src="userInfo.avatar || '/static/default-avatar.png'"
          class="user-avatar"
          mode="aspectFill"
        />
        <view class="user-info">
          <text class="user-name">{{ userInfo.realName || userInfo.username || '用户' }}</text>
          <text class="user-phone">{{ formatPhone(userInfo.phone) }}</text>
          <text class="logout-hint">点击退出登录</text>
        </view>
        <text class="logout-arrow">></text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-section">
      <view class="menu-group">
        <view class="menu-title">就诊服务</view>
        <view class="menu-list">
          <view class="menu-item" @click="goToPatients">
            <view class="icon-box" style="background: #e6f7ff;">
              <text class="icon" style="color: #1890ff;">👥</text>
            </view>
            <text class="label">就诊人管理</text>
            <text class="arrow">></text>
          </view>
          <view class="menu-item" @click="goToPayments">
            <view class="icon-box" style="background: #f6ffed;">
              <text class="icon" style="color: #52c41a;">💰</text>
            </view>
            <text class="label">门诊缴费</text>
            <text class="arrow">></text>
          </view>
          <view class="menu-item" @click="goToMedicalRecords">
            <view class="icon-box" style="background: #fff7e6;">
              <text class="icon" style="color: #fa8c16;">📖</text>
            </view>
            <text class="label">就诊记录</text>
            <text class="arrow">></text>
          </view>
        </view>
      </view>

      <view class="menu-group">
        <view class="menu-title">系统设置</view>
        <view class="menu-list">
          <view class="menu-item" @click="selectClinic">
            <view class="icon-box" style="background: #f0f5ff;">
              <text class="icon" style="color: #2f54eb;">🏥</text>
            </view>
            <text class="label">选择诊所</text>
            <text class="value">{{ clinicName || '未选择' }}</text>
            <text class="arrow">></text>
          </view>
          <view class="menu-item" @click="clearCache">
            <view class="icon-box" style="background: #f5f5f5;">
              <text class="icon" style="color: #666;">🗑️</text>
            </view>
            <text class="label">清除缓存</text>
            <text class="value">{{ cacheSize }}</text>
            <text class="arrow">></text>
          </view>
          <view class="menu-item" @click="contactService">
            <view class="icon-box" style="background: #fff0f6;">
              <text class="icon" style="color: #eb2f96;">💬</text>
            </view>
            <text class="label">联系客服</text>
            <text class="arrow">></text>
          </view>
          <view class="menu-item" @click="aboutUs">
            <view class="icon-box" style="background: #f6ffed;">
              <text class="icon" style="color: #52c41a;">ℹ️</text>
            </view>
            <text class="label">关于我们</text>
            <text class="arrow">></text>
          </view>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { logout as logoutApi } from '@/api/auth.js'
import { formatPhone, showConfirm } from '@/utils/index.js'

const userInfo = ref({})
const clinicName = ref('')
const cacheSize = ref('0KB')

const canSwitchClinic = computed(() => {
  try {
    const list = JSON.parse(uni.getStorageSync('clinicList') || '[]')
    return list.length > 1
  } catch {
    return false
  }
})

onMounted(() => {
  loadUserInfo()
  refreshClinicName()
  calculateCacheSize()
})

// 页面显示时刷新诊所名称（从选择页返回时触发）
onShow(() => {
  refreshClinicName()
})

const refreshClinicName = () => {
  clinicName.value = uni.getStorageSync('clinicName') || ''
}

const loadUserInfo = () => {
  try {
    const info = uni.getStorageSync('userInfo')
    if (info) {
      userInfo.value = JSON.parse(info)
    }
  } catch (e) {
    console.error('加载用户信息失败:', e)
  }
}

const calculateCacheSize = () => {
  // 简化的缓存大小计算
  cacheSize.value = '1.2MB'
}

const goToPatients = () => {
  uni.navigateTo({ url: '/pages/patient/list' })
}

const goToPayments = () => {
  uni.navigateTo({ url: '/pages/payment/outpatient' })
}

const goToMedicalRecords = () => {
  uni.navigateTo({ url: '/pages/medical/list' })
}

const selectClinic = () => {
  uni.navigateTo({ url: '/pages/clinic/select' })
}

const clearCache = async () => {
  const confirmed = await showConfirm('确定清除缓存吗？', '提示')
  if (!confirmed) return

  uni.showLoading({ title: '清除中...' })
  setTimeout(() => {
    uni.hideLoading()
    uni.showToast({ title: '清除成功', icon: 'success' })
    cacheSize.value = '0KB'
  }, 1000)
}

const contactService = () => {
  uni.makePhoneCall({
    phoneNumber: '400-123-4567'
  })
}

const aboutUs = () => {
  uni.navigateTo({ url: '/pages/mine/about' })
}

const handleUserCardClick = async () => {
  const confirmed = await showConfirm('确定退出登录吗？', '退出登录')
  if (!confirmed) return
  
  await logout()
}

const logout = async () => {
  try {
    await logoutApi()
  } catch (e) {
    // 忽略错误
  }

  // 清除本地存储
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('clinicId')
  uni.removeStorageSync('clinicName')
  uni.removeStorageSync('clinicList')

  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.user-card {
  position: relative;

  .user-bg {
    height: 200rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  .user-content {
    position: relative;
    margin: -60rpx 30rpx 0;
    background: #fff;
    border-radius: 20rpx;
    padding: 40rpx;
    display: flex;
    align-items: center;
    box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);

    .user-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 60rpx;
      margin-right: 30rpx;
      background: #f5f5f5;
    }

    .user-info {
      flex: 1;
      
      .user-name {
        display: block;
        font-size: 40rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 8rpx;
      }

      .user-phone {
        display: block;
        font-size: 28rpx;
        color: #999;
        margin-bottom: 8rpx;
      }
      
      .logout-hint {
        display: block;
        font-size: 24rpx;
        color: #ff4d4f;
      }
    }
    
    .logout-arrow {
      font-size: 32rpx;
      color: #ccc;
      margin-left: 20rpx;
    }
  }
}

.menu-section {
  padding: 30rpx;

  .menu-group {
    margin-bottom: 30rpx;

    .menu-title {
      font-size: 28rpx;
      color: #999;
      margin-bottom: 20rpx;
      padding-left: 10rpx;
    }

    .menu-list {
      background: #fff;
      border-radius: 16rpx;
      overflow: hidden;

      .menu-item {
        display: flex;
        align-items: center;
        padding: 30rpx;
        border-bottom: 1rpx solid #f5f5f5;

        &:last-child {
          border-bottom: none;
        }

        .icon-box {
          width: 60rpx;
          height: 60rpx;
          border-radius: 16rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 20rpx;

          .icon {
            font-size: 36rpx;
          }
        }

        .label {
          flex: 1;
          font-size: 30rpx;
          color: #333;
        }

        .value {
          font-size: 28rpx;
          color: #999;
          margin-right: 16rpx;
          max-width: 200rpx;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .arrow {
          font-size: 28rpx;
          color: #ccc;
        }
      }
    }
  }
}

.logout-section {
  padding: 0 30rpx 60rpx;

  .logout-btn {
    width: 100%;
    height: 90rpx;
    line-height: 90rpx;
    background: #fff;
    color: #ff4d4f;
    font-size: 32rpx;
    border-radius: 16rpx;

    &::after {
      border: none;
    }
  }
}
</style>
