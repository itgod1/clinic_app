<template>
  <view class="about-page">
    <view class="logo-section">
      <view class="logo-box">
        <text class="logo-icon">🏥</text>
      </view>
      <text class="app-name">智慧诊所</text>
      <text class="app-version">Version 1.0.0</text>
    </view>

    <view class="info-section">
      <view class="info-item">
        <text class="label">官方网站</text>
        <text class="value">www.clinic.com</text>
      </view>
      <view class="info-item">
        <text class="label">客服电话</text>
        <text class="value">400-123-4567</text>
      </view>
      <view class="info-item">
        <text class="label">工作时间</text>
        <text class="value">周一至周日 8:00-20:00</text>
      </view>
    </view>

    <view class="copyright">
      <text>© 2024 智慧诊所 版权所有</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
  background: #fff;

  .logo-box {
    width: 160rpx;
    height: 160rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 30rpx;

    .logo-icon {
      font-size: 80rpx;
    }
  }

  .app-name {
    font-size: 40rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 12rpx;
  }

  .app-version {
    font-size: 26rpx;
    color: #999;
  }
}

.info-section {
  margin-top: 20rpx;
  background: #fff;
  padding: 0 30rpx;

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 30rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .label {
      font-size: 30rpx;
      color: #666;
    }

    .value {
      font-size: 30rpx;
      color: #333;
    }
  }
}

.copyright {
  text-align: center;
  padding: 60rpx 30rpx;

  text {
    font-size: 26rpx;
    color: #999;
  }
}
</style>
