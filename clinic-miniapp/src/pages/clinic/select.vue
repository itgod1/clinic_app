<template>
  <view class="select-clinic-page">
    <!-- 返回按钮 -->
    <view class="nav-back" @click="goBack">
      <text class="nav-back-icon">←</text>
    </view>
    
    <view class="page-header">
      <view class="title-section">
        <text class="main-title">选择就诊诊所</text>
        <text class="sub-title">请选择您要就诊的诊所</text>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="searchKeyword"
          placeholder="搜索诊所名称"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text v-if="searchKeyword" class="clear-icon" @click="clearSearch">✕</text>
      </view>
    </view>

    <scroll-view class="clinic-list" scroll-y @scrolltolower="loadMore">
      <!-- 当前选择 -->
      <view v-if="currentClinic.id" class="current-clinic">
        <text class="section-title">当前选择</text>
        <view class="clinic-card selected">
          <view class="card-main">
            <view class="clinic-logo" :style="{ background: getGradient(currentClinic.id) }">
              <text class="logo-text">{{ getFirstChar(currentClinic.clinicName) }}</text>
            </view>
            <view class="clinic-info">
              <view class="info-header">
                <text class="clinic-name">{{ currentClinic.clinicName }}</text>
                <view class="tags">
                  <text class="tag current">当前</text>
                </view>
              </view>
              <text class="clinic-address">
                <text class="icon">📍</text>
                {{ currentClinic.address || '暂无地址信息' }}
              </text>
              <text class="clinic-phone" v-if="currentClinic.contactPhone">
                <text class="icon">📞</text>
                {{ currentClinic.contactPhone }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 诊所列表 -->
      <text class="section-title">{{ searchKeyword ? '搜索结果' : '推荐诊所' }}</text>

      <view v-if="loading && clinicList.length === 0" class="loading-state">
        <text>加载中...</text>
      </view>

      <view v-else-if="clinicList.length === 0" class="empty-state">
        <text class="empty-icon">🏥</text>
        <text class="empty-text">暂无诊所</text>
        <text v-if="searchKeyword" class="empty-hint">试试其他关键词</text>
      </view>

      <view
        v-for="clinic in clinicList"
        v-else
        :key="clinic.id"
        :class="['clinic-card', currentClinic.id === clinic.id ? 'disabled' : '']"
        @click="selectClinic(clinic)"
      >
        <view class="card-main">
          <view class="clinic-logo" :style="{ background: getGradient(clinic.id) }">
            <text class="logo-text">{{ getFirstChar(clinic.clinicName) }}</text>
          </view>
          <view class="clinic-info">
            <view class="info-header">
              <text class="clinic-name">{{ clinic.clinicName }}</text>
              <view class="tags">
                <text
                  class="tag status"
                  :class="getStatusClass(clinic.businessStatus)"
                >
                  {{ getStatusText(clinic.businessStatus) }}
                </text>
              </view>
            </view>
            <text class="clinic-address">
              <text class="icon">📍</text>
              {{ clinic.address || '暂无地址信息' }}
            </text>
            <text class="clinic-phone" v-if="clinic.contactPhone">
              <text class="icon">📞</text>
              {{ clinic.contactPhone }}
            </text>
          </view>
        </view>

        <view v-if="currentClinic.id === clinic.id" class="card-footer">
          <text class="selected-text">✓ 当前诊所</text>
        </view>
        <view v-else class="card-footer select-hint">
          <text class="select-text">点击选择</text>
        </view>
      </view>

      <view v-if="loadingMore" class="loading-more">
        <text>加载更多...</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getClinicList as getAvailableClinics, switchClinic as switchClinicApi } from '@/api/miniapp/clinic.js'

const clinicList = ref([])
const currentClinic = ref({})
const searchKeyword = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)

// 渐变色数组
const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
]

const getGradient = (id) => {
  return gradients[(id % gradients.length)]
}

const getFirstChar = (name) => {
  return name ? name.charAt(0) : '医'
}

// 获取状态样式类
const getStatusClass = (status) => {
  // 1:营业中 2:休息中 3:停业
  if (status === 1) return 'open'
  return 'close'
}

// 获取状态文本
const getStatusText = (status) => {
  // 1:营业中 2:休息中 3:停业
  switch (status) {
    case 1: return '营业中'
    case 2: return '休息中'
    case 3: return '停业'
    default: return '休息中'
  }
}

onMounted(() => {
  // 加载当前选择的诊所
  const clinicId = uni.getStorageSync('clinicId')
  const clinicName = uni.getStorageSync('clinicName')
  if (clinicId && clinicName) {
    currentClinic.value = { id: parseInt(clinicId), clinicName }
  }
  loadClinicList()
})

const loadClinicList = async (isLoadMore = false) => {
  if (loading.value || loadingMore.value) return

  if (!isLoadMore) {
    loading.value = true
    page.value = 1
  } else {
    if (!hasMore.value) return
    loadingMore.value = true
  }

  try {
    const res = await getAvailableClinics({
      page: page.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    })

    const list = res.data?.records || res.data?.list || res.data || []
    const total = res.data?.total || list.length

    if (isLoadMore) {
      clinicList.value = [...clinicList.value, ...list]
    } else {
      clinicList.value = list
    }

    hasMore.value = clinicList.value.length < total
    page.value++
  } catch (error) {
    console.error('加载诊所列表失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  loadClinicList(true)
}

const goBack = () => {
  // 判断是否有上一页，如果没有则返回首页
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

const handleSearch = () => {
  loadClinicList(false)
}

const clearSearch = () => {
  searchKeyword.value = ''
  loadClinicList(false)
}

const selectClinic = async (clinic) => {
  if (currentClinic.value.id === clinic.id) {
    uni.showToast({ title: '已经是当前诊所', icon: 'none' })
    return
  }

  uni.showModal({
    title: '确认选择',
    content: `确定选择「${clinic.clinicName}」作为就诊诊所吗？`,
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '保存中...' })

        try {
          // 1. 先保存到本地（确保无论接口成功与否，本地都有值）
          uni.setStorageSync('clinicId', clinic.id)
          uni.setStorageSync('clinicName', clinic.clinicName)

          // 2. 调用后端接口同步更新（如果已登录）
          const token = uni.getStorageSync('token')
          if (token) {
            await switchClinicApi(clinic.id)
          }

          uni.hideLoading()
          uni.showToast({
            title: '选择成功',
            icon: 'success',
            success: () => {
              setTimeout(() => {
                uni.switchTab({ url: '/pages/index/index' })
              }, 1500)
            }
          })
        } catch (error) {
          uni.hideLoading()
          console.error('切换诊所失败:', error)
          // 即使接口失败，本地已保存，仍然可以继续使用
          uni.showToast({
            title: '选择成功',
            icon: 'success',
            success: () => {
              setTimeout(() => {
                uni.switchTab({ url: '/pages/index/index' })
              }, 1500)
            }
          })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.select-clinic-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  position: relative;
}

.nav-back {
  position: absolute;
  top: 100rpx;
  left: 30rpx;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  z-index: 100;

  &:active {
    background: rgba(255, 255, 255, 0.4);
  }

  .nav-back-icon {
    font-size: 36rpx;
    color: #fff;
    font-weight: bold;
  }
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 30rpx 40rpx;

  .title-section {
    text-align: center;

    .main-title {
      display: block;
      font-size: 44rpx;
      font-weight: bold;
      color: #fff;
      margin-bottom: 16rpx;
    }

    .sub-title {
      display: block;
      font-size: 28rpx;
      color: rgba(255, 255, 255, 0.8);
    }
  }
}

.search-section {
  background: #fff;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .search-box {
    display: flex;
    align-items: center;
    background: #f5f7fa;
    border-radius: 36rpx;
    padding: 16rpx 24rpx;

    .search-icon {
      font-size: 32rpx;
      margin-right: 16rpx;
      color: #999;
    }

    .search-input {
      flex: 1;
      font-size: 28rpx;
      color: #333;
    }

    .clear-icon {
      font-size: 28rpx;
      color: #999;
      padding: 8rpx;
    }
  }
}

.clinic-list {
  flex: 1;
  padding: 30rpx;

  .section-title {
    display: block;
    font-size: 28rpx;
    color: #999;
    margin-bottom: 20rpx;
    padding-left: 10rpx;
  }

  .current-clinic {
    margin-bottom: 40rpx;
  }
}

.loading-state,
.empty-state,
.loading-more {
  text-align: center;
  padding: 60rpx 0;
  color: #999;
  font-size: 28rpx;
}

.empty-state {
  .empty-icon {
    display: block;
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .empty-text {
    display: block;
    font-size: 32rpx;
    color: #666;
    margin-bottom: 10rpx;
  }

  .empty-hint {
    display: block;
    font-size: 26rpx;
    color: #999;
  }
}

.clinic-card {
  background: #fff;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  border: 3rpx solid transparent;
  transition: all 0.3s;

  &.selected {
    border-color: #52c41a;
    box-shadow: 0 4rpx 24rpx rgba(82, 196, 26, 0.2);
  }

  &.disabled {
    opacity: 0.7;
  }

  &:active {
    transform: scale(0.98);
  }

  .card-main {
    display: flex;
    padding: 30rpx;

    .clinic-logo {
      width: 120rpx;
      height: 120rpx;
      border-radius: 60rpx;
      margin-right: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .logo-text {
        font-size: 48rpx;
        font-weight: bold;
        color: #fff;
      }
    }

    .clinic-info {
      flex: 1;
      min-width: 0;

      .info-header {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12rpx;
        margin-bottom: 16rpx;

        .clinic-name {
          font-size: 34rpx;
          font-weight: 600;
          color: #333;
        }

        .tags {
          display: flex;
          gap: 8rpx;

          .tag {
            font-size: 22rpx;
            padding: 4rpx 14rpx;
            border-radius: 8rpx;

            &.current {
              background: #f6ffed;
              color: #52c41a;
            }

            &.status {
              &.open {
                background: #f6ffed;
                color: #52c41a;
              }

              &.close {
                background: #fff1f0;
                color: #ff4d4f;
              }
            }
          }
        }
      }

      .clinic-address,
      .clinic-phone {
        display: block;
        font-size: 26rpx;
        color: #666;
        margin-bottom: 8rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        .icon {
          margin-right: 8rpx;
        }
      }
    }
  }

  .card-footer {
    padding: 16rpx 30rpx;
    text-align: center;

    &.select-hint {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    }

    .selected-text {
      font-size: 28rpx;
      color: #52c41a;
      font-weight: 500;
    }

    .select-text {
      font-size: 28rpx;
      color: #667eea;
      font-weight: 500;
    }
  }
}
</style>
