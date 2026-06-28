<template>
  <view class="login-page">
    <view class="login-bg">
      <view class="circle circle-1"></view>
      <view class="circle circle-2"></view>
      <view class="circle circle-3"></view>
    </view>
    
    <view class="login-content">
      <view class="logo-section">
        <view class="logo-box">
          <text class="logo-icon">🏥</text>
        </view>
        <text class="app-name">智慧诊所</text>
        <text class="app-slogan">在线预约挂号，便捷就医体验</text>
      </view>
      
      <view class="login-section">
        <!-- 微信一键登录（真机使用） -->
        <button
          class="wechat-btn"
          open-type="getPhoneNumber"
          @getphonenumber="handleGetPhoneNumber"
        >
          <text class="icon">💬</text>
          <text>微信一键登录</text>
        </button>

        <!-- 模拟器测试按钮（仅在开发工具显示） -->
        <button
          v-if="isDevTools"
          class="wechat-btn test-btn"
          @click="handleDevLogin"
        >
          <text class="icon">�</text>
          <text>模拟登录（开发测试）</text>
        </button>
        
        <!-- 账号密码登录（可选） -->
        <view class="divider">
          <text class="line"></text>
          <text class="text">或</text>
          <text class="line"></text>
        </view>
        
        <button class="account-btn" @click="showAccountLogin = true">
          账号密码登录
        </button>
      </view>
      
      <view class="agreement">
        <checkbox :checked="agreed" @click="agreed = !agreed" color="#1890ff" />
        <text class="text">
          我已阅读并同意
          <text class="link" @click="openAgreement">《用户协议》</text>
          和
          <text class="link" @click="openPrivacy">《隐私政策》</text>
        </text>
      </view>
    </view>
    
    <!-- 账号密码登录弹窗 -->
    <uni-popup ref="accountPopup" type="center">
      <view class="account-login-popup">
        <view class="popup-header">
          <text class="title">账号登录</text>
          <text class="close" @click="closeAccountLogin">✕</text>
        </view>
        
        <view class="form">
          <view class="input-group">
            <text class="label">用户名</text>
            <input 
              v-model="accountForm.username"
              placeholder="请输入用户名"
              class="input"
            />
          </view>
          
          <view class="input-group">
            <text class="label">密码</text>
            <input 
              v-model="accountForm.password"
              placeholder="请输入密码"
              password
              class="input"
            />
          </view>
          
          <button class="submit-btn" @click="handleAccountLogin">登录</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { miniappLogin, login } from '@/api/auth.js'

const agreed = ref(false)
const showAccountLogin = ref(false)
const accountForm = ref({
  username: '',
  password: ''
})
const accountPopup = ref(null)
const isDevTools = ref(false)

// 检测是否在开发者工具中
onMounted(() => {
  const systemInfo = uni.getSystemInfoSync()
  isDevTools.value = systemInfo.platform === 'devtools'
})

// 开发工具模拟登录
const handleDevLogin = async () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  uni.showLoading({ title: '模拟登录中...' })

  try {
    // 模拟登录数据
    const mockUserInfo = {
      id: 1,
      username: 'test_user',
      realName: '测试用户',
      phone: '13800138000',
      avatar: ''
    }

    // 保存登录信息
    uni.setStorageSync('token', 'mock_token_' + Date.now())
    uni.setStorageSync('userInfo', JSON.stringify(mockUserInfo))

    uni.showToast({ title: '模拟登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1500)
  } catch (error) {
    uni.showToast({ title: '登录失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 微信手机号授权登录
const handleGetPhoneNumber = async (e) => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  if (e.detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({ title: '请授权手机号', icon: 'none' })
    return
  }

  uni.showLoading({ title: '登录中...' })

  try {
    // 获取微信登录code
    const loginRes = await uni.login({ provider: 'weixin' })

    // 调用登录接口
    const res = await miniappLogin({
      code: loginRes.code,
      encryptedData: e.detail.encryptedData,
      iv: e.detail.iv
    })
    
    // 保存登录信息
    console.log('登录返回数据:', res)
    console.log('token:', res.data.token)
    uni.setStorageSync('token', res.data.token)
    uni.setStorageSync('userInfo', JSON.stringify(res.data.userInfo))
    
    // 验证保存成功
    const savedToken = uni.getStorageSync('token')
    console.log('保存后的token:', savedToken)
    
    // 处理多诊所逻辑
    handleLoginSuccess(res.data)
  } catch (error) {
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 账号密码登录
const handleAccountLogin = async () => {
  if (!accountForm.value.username || !accountForm.value.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  
  uni.showLoading({ title: '登录中...' })
  
  try {
    const res = await login({
      username: accountForm.value.username,
      password: accountForm.value.password
    })
    
    // 保存登录信息
    uni.setStorageSync('token', res.data.token)
    uni.setStorageSync('userInfo', JSON.stringify(res.data.userInfo))
    
    // 处理多诊所逻辑
    handleLoginSuccess(res.data)
    
    closeAccountLogin()
  } catch (error) {
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 登录成功处理
const handleLoginSuccess = (data) => {
  // 患者可以自由选择诊所，不需要预先关联
  // 直接跳转到首页，在"我的"页面提供选择诊所功能
  uni.switchTab({ url: '/pages/index/index' })
}

const openAgreement = () => {
  uni.navigateTo({ url: '/pages/webview/index?url=agreement' })
}

const openPrivacy = () => {
  uni.navigateTo({ url: '/pages/webview/index?url=privacy' })
}

const closeAccountLogin = () => {
  accountPopup.value?.close()
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  
  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    
    &.circle-1 {
      width: 600rpx;
      height: 600rpx;
      top: -200rpx;
      right: -200rpx;
    }
    
    &.circle-2 {
      width: 400rpx;
      height: 400rpx;
      bottom: 100rpx;
      left: -150rpx;
    }
    
    &.circle-3 {
      width: 300rpx;
      height: 300rpx;
      bottom: 300rpx;
      right: -100rpx;
    }
  }
}

.login-content {
  position: relative;
  z-index: 1;
  padding: 80rpx 60rpx;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.logo-section {
  text-align: center;
  margin-top: 100rpx;
  margin-bottom: 120rpx;
  
  .logo-box {
    width: 160rpx;
    height: 160rpx;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 40rpx;
    margin: 0 auto 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10rpx);
    
    .logo-icon {
      font-size: 80rpx;
    }
  }
  
  .app-name {
    display: block;
    font-size: 48rpx;
    font-weight: bold;
    color: #fff;
    margin-bottom: 16rpx;
  }
  
  .app-slogan {
    display: block;
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.login-section {
  margin-top: auto;
  margin-bottom: 60rpx;
  
  .wechat-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16rpx;
    height: 100rpx;
    background: #07c160;
    color: #fff;
    font-size: 32rpx;
    border-radius: 50rpx;
    border: none;
    margin-bottom: 20rpx;

    &::after {
      border: none;
    }

    .icon {
      font-size: 40rpx;
    }

    &.test-btn {
      background: #ff9800;
      font-size: 28rpx;
    }
  }
  
  .divider {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 40rpx;
    
    .line {
      width: 120rpx;
      height: 2rpx;
      background: rgba(255, 255, 255, 0.3);
    }
    
    .text {
      margin: 0 30rpx;
      font-size: 26rpx;
      color: rgba(255, 255, 255, 0.6);
    }
  }
  
  .account-btn {
    height: 100rpx;
    background: rgba(255, 255, 255, 0.2);
    color: #fff;
    font-size: 32rpx;
    border-radius: 50rpx;
    border: 2rpx solid rgba(255, 255, 255, 0.3);
    
    &::after {
      border: none;
    }
  }
}

.agreement {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  
  .text {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.8);
    margin-left: 8rpx;
    line-height: 1.6;
  }
  
  .link {
    color: #fff;
    text-decoration: underline;
  }
}

.account-login-popup {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  
  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40rpx;
    
    .title {
      font-size: 36rpx;
      font-weight: 600;
      color: #333;
    }
    
    .close {
      font-size: 40rpx;
      color: #999;
      padding: 10rpx;
    }
  }
  
  .form {
    .input-group {
      margin-bottom: 30rpx;
      
      .label {
        display: block;
        font-size: 28rpx;
        color: #666;
        margin-bottom: 12rpx;
      }
      
      .input {
        height: 90rpx;
        background: #f5f5f5;
        border-radius: 12rpx;
        padding: 0 24rpx;
        font-size: 30rpx;
      }
    }
    
    .submit-btn {
      height: 90rpx;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      font-size: 32rpx;
      border-radius: 45rpx;
      margin-top: 40rpx;
      
      &::after {
        border: none;
      }
    }
  }
}
</style>
