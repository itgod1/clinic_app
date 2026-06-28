<template>
  <view class="index-page">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="header-top">
        <view class="location" @click="switchClinic">
          <text class="location-icon">📍</text>
          <text class="location-text">{{ clinicName || '选择诊所' }}</text>
          <text class="location-arrow">▼</text>
        </view>
        <view class="search-box" @click="goToSearch">
          <text class="search-icon">🔍</text>
          <text class="search-placeholder">搜索医生、科室</text>
        </view>
        <view class="header-right">
          <text class="scan-icon">📷</text>
        </view>
      </view>
    </view>

    <!-- 轮播图 -->
    <view class="banner-section">
      <swiper class="banner-swiper" :indicator-dots="true" :autoplay="true" :interval="3000" :duration="500" circular indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#fff">
        <swiper-item v-for="(item, index) in bannerList" :key="index" @click="onBannerClick(item)">
          <view class="banner-item" :style="{ background: item.bgGradient }">
            <!-- 装饰背景图案 -->
            <view class="banner-pattern" :style="{ backgroundImage: item.pattern }"></view>
            <view class="banner-content">
              <view class="banner-text">
                <text class="banner-title">{{ item.title }}</text>
                <text class="banner-subtitle">{{ item.subtitle }}</text>
                <view class="banner-btn">
                  <text>{{ item.btnText }}</text>
                  <text class="btn-arrow">→</text>
                </view>
              </view>
              <view class="banner-icon-wrap" :style="{ background: item.iconBg }">
                <text class="banner-emoji">{{ item.emoji }}</text>
              </view>
            </view>
          </view>
        </swiper-item>
      </swiper>
    </view>

    <!-- 快捷功能入口 -->
    <view class="quick-actions">
      <view class="action-grid">
        <view class="action-item" v-for="(item, index) in quickActions" :key="index" @click="handleActionClick(item)">
          <view class="action-icon" :style="{ background: item.bgColor }">
            <text class="icon-emoji">{{ item.icon }}</text>
          </view>
          <text class="action-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <!-- 推荐医生 -->
    <view class="section doctor-section">
      <view class="section-header">
        <view class="section-title-wrap">
          <view class="title-line"></view>
          <text class="section-title">推荐医生</text>
        </view>
        <text class="more" @click="goToDoctors">查看更多 ></text>
      </view>
      <scroll-view class="doctor-scroll" scroll-x>
        <view class="doctor-list-horizontal">
          <view class="doctor-card-mini" v-for="doctor in doctorList" :key="doctor.id" @click="goToDoctorDetail(doctor)">
            <view class="doctor-avatar-wrap">
              <image :src="doctor.avatarUrl || '/static/default-avatar.png'" class="doctor-avatar-mini" mode="aspectFill" />
              <view class="online-tag" v-if="doctor.hasSchedule">可预约</view>
            </view>
            <text class="doctor-name">{{ doctor.doctorName }}</text>
            <text class="doctor-title">{{ doctor.position }}</text>
            <text class="doctor-dept">{{ doctor.deptName }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 诊所信息卡片 -->
    <view class="section clinic-section" v-if="clinicInfo.name">
      <view class="section-header">
        <view class="section-title-wrap">
          <view class="title-line"></view>
          <text class="section-title">诊所信息</text>
        </view>
      </view>
      <view class="clinic-card">
        <view class="clinic-header">
          <view class="clinic-logo-wrap">
            <text class="clinic-logo-emoji">🏥</text>
          </view>
          <view class="clinic-info">
            <text class="clinic-name">{{ clinicInfo.name }}</text>
            <text class="clinic-slogan">{{ clinicInfo.slogan || '专业口腔医疗服务' }}</text>
          </view>
        </view>
        <view class="clinic-features">
          <view class="feature-item">
            <text class="feature-num">10+</text>
            <text class="feature-label">专业医生</text>
          </view>
          <view class="feature-divider"></view>
          <view class="feature-item">
            <text class="feature-num">5</text>
            <text class="feature-label">特色科室</text>
          </view>
          <view class="feature-divider"></view>
          <view class="feature-item">
            <text class="feature-num">8年</text>
            <text class="feature-label">服务经验</text>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getDoctorList as getPublicDoctorList } from '@/api/miniapp/doctor.js'
import { getClinicDetail as getPublicClinicDetail } from '@/api/miniapp/clinic.js'
import { getPatientList } from '@/api/miniapp/patient.js'

// 数据
const clinicInfo = ref({})
const doctorList = ref([])
const loading = ref(false)
const selectedClinicId = ref(null)

// 轮播图数据 - 使用与导航栏一致的绿色主题
const bannerList = ref([
  {
    bgGradient: 'linear-gradient(135deg, #00b894 0%, #00d4a8 100%)',
    pattern: 'radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 50%)',
    iconBg: 'rgba(255,255,255,0.15)',
    emoji: '👨‍⚕️',
    title: '专家坐诊',
    subtitle: '三甲医院专家定期坐诊',
    btnText: '立即预约',
    url: '/pages/doctor/list'
  },
  {
    bgGradient: 'linear-gradient(135deg, #00a884 0%, #00b894 100%)',
    pattern: 'radial-gradient(circle at 20% 80%, rgba(255,255,255,0.1) 0%, transparent 50%)',
    iconBg: 'rgba(255,255,255,0.15)',
    emoji: '📅',
    title: '在线挂号',
    subtitle: '提前预约 无需排队',
    btnText: '去挂号',
    url: '/pages/registration/dept-select'
  },
  {
    bgGradient: 'linear-gradient(135deg, #00c8a0 0%, #00d4b8 100%)',
    pattern: 'radial-gradient(circle at 70% 70%, rgba(255,255,255,0.1) 0%, transparent 50%)',
    iconBg: 'rgba(255,255,255,0.15)',
    emoji: '🎁',
    title: '优惠活动',
    subtitle: '洁牙套餐限时特惠中',
    btnText: '查看详情',
    url: '/pages/index/index'
  }
])

// 快捷功能 - 使用emoji图标
const quickActions = ref([
  { icon: '📋', label: '预约挂号', bgColor: 'linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%)', needLogin: true, needClinic: true, url: '/pages/registration/dept-select' },
  { icon: '💳', label: '门诊缴费', bgColor: 'linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)', needLogin: true, needClinic: true, url: '/pages/payment/outpatient' },
  { icon: '📄', label: '就诊记录', bgColor: 'linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%)', needLogin: true, needClinic: true, url: '/pages/medical/list' },
  { icon: '📍', label: '来院导航', bgColor: 'linear-gradient(135deg, #fce4ec 0%, #f8bbd9 100%)', needLogin: false, needClinic: true, url: '' },
  { icon: '🤖', label: 'AI问诊', bgColor: 'linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%)', needLogin: false, needClinic: false, url: '/pages/ai/consult', type: 'consult' },
  { icon: '📋', label: 'AI回访', bgColor: 'linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%)', needLogin: true, needClinic: false, url: '/pages/ai/return-visit', type: 'return-visit' },
  { icon: '💬', label: '联系客服', bgColor: 'linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 100%)', needLogin: false, needClinic: false, url: '' },
  { icon: '🏷️', label: '优惠活动', bgColor: 'linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%)', needLogin: false, needClinic: false, url: '' },
  { icon: '📰', label: '健康资讯', bgColor: 'linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)', needLogin: false, needClinic: false, url: '' }
])

// 计算属性
const clinicName = computed(() => {
  if (selectedClinicId.value) {
    return uni.getStorageSync('clinicName') || '选择诊所'
  }
  return '选择诊所'
})

const isLoggedIn = computed(() => {
  return !!uni.getStorageSync('token')
})

onMounted(() => {
  updateClinicStatus()
  loadPublicData()
  if (isLoggedIn.value) {
    loadPrivateData()
  }
})

onShow(() => {
  const oldClinicId = selectedClinicId.value
  updateClinicStatus()
  const newClinicId = selectedClinicId.value
  if (oldClinicId !== newClinicId) {
    loadDoctors()
  }
})

onPullDownRefresh(() => {
  loadPublicData().finally(() => {
    uni.stopPullDownRefresh()
  })
})

const loadPublicData = async () => {
  loading.value = true
  try {
    const clinicId = uni.getStorageSync('clinicId')
    if (clinicId) {
      const res = await getPublicClinicDetail(clinicId)
      clinicInfo.value = res.data || {}
    }
    await loadDoctors()
  } catch (error) {
    console.error('加载公开数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadPrivateData = async () => {}

const loadDoctors = async () => {
  try {
    const clinicId = uni.getStorageSync('clinicId')
    const params = { pageSize: 6 }
    if (clinicId) {
      params.clinicId = clinicId
    }
    const res = await getPublicDoctorList(params)
    doctorList.value = res.data?.records || res.data?.list || res.data || []
  } catch (error) {
    console.error('加载医生列表失败:', error)
    doctorList.value = []
  }
}

const hasSelectedClinic = computed(() => {
  return !!selectedClinicId.value
})

const updateClinicStatus = () => {
  const clinicId = uni.getStorageSync('clinicId')
  selectedClinicId.value = clinicId ? parseInt(clinicId) : null
}

const handleActionClick = (item) => {
  if (item.needClinic !== false && !hasSelectedClinic.value) {
    uni.showModal({
      title: '提示',
      content: '请先选择就诊诊所',
      confirmText: '去选择',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/clinic/select' })
        }
      }
    })
    return
  }

  const token = uni.getStorageSync('token')
  const userInfo = uni.getStorageSync('userInfo')
  
  if (item.needLogin && !token) {
    uni.showModal({
      title: '提示',
      content: '你还没有登录哦...',
      confirmText: '马上登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/login/index' })
        }
      }
    })
    return
  }
  
  // AI回访特殊处理：需要传递patientId
  if (item.type === 'return-visit') {
    handleAIReturnVisit()
    return
  }
  
  if (item.url) {
    if (item.url.startsWith('/pages')) {
      uni.navigateTo({ url: item.url })
    } else {
      uni.switchTab({ url: item.url })
    }
  } else {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}

const onBannerClick = (item) => {
  if (!hasSelectedClinic.value) {
    uni.showModal({
      title: '提示',
      content: '请先选择就诊诊所',
      confirmText: '去选择',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/clinic/select' })
        }
      }
    })
    return
  }
  if (item.url) {
    uni.navigateTo({ url: item.url })
  }
}

const goToSearch = () => {
  uni.showToast({ title: '搜索功能开发中', icon: 'none' })
}

const switchClinic = () => {
  uni.navigateTo({ url: '/pages/clinic/select' })
}

const goToDoctors = () => {
  uni.switchTab({ url: '/pages/doctor/list' })
}

const goToDoctorDetail = (doctor) => {
  if (!hasSelectedClinic.value) {
    uni.showModal({
      title: '提示',
      content: '请先选择就诊诊所',
      confirmText: '去选择',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/clinic/select' })
        }
      }
    })
    return
  }
  uni.navigateTo({ url: `/pages/doctor/detail?id=${doctor.id}` })
}

/**
 * 处理AI回访点击
 * 先尝试从userInfo获取patientId，如果没有则调用就诊人列表接口
 */
const handleAIReturnVisit = async () => {
  uni.showLoading({ title: '加载中...' })
  
  try {
    // 1. 先尝试从userInfo获取patientId（注意：不能用userInfo.id，那是userId）
    const userInfoStr = uni.getStorageSync('userInfo')
    const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null
    let patientId = userInfo?.patientId || userInfo?.patient?.id
    
    // 2. 如果userInfo中没有，调用就诊人列表接口
    if (!patientId) {
      const res = await getPatientList()
      const patients = res.data || []
      
      if (patients.length === 0) {
        uni.hideLoading()
        uni.showModal({
          title: '提示',
          content: '您还没有绑定就诊人，请先绑定',
          confirmText: '去绑定',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/patient/list' })
            }
          }
        })
        return
      }
      
      // 3. 如果有多个就诊人，让用户选择
      if (patients.length > 1) {
        uni.hideLoading()
        showPatientSelector(patients)
        return
      }
      
      // 只有一个就诊人，直接使用（使用id字段，这是patientId，不是userId）
      patientId = patients[0].id
    }
    
    uni.hideLoading()
    
    // 4. 跳转到AI回访页面
    uni.navigateTo({ 
      url: `/pages/ai/return-visit?patient_id=${patientId}&from=home`
    })
    
  } catch (error) {
    uni.hideLoading()
    console.error('获取就诊人信息失败:', error)
    uni.showToast({ title: '加载失败，请重试', icon: 'none' })
  }
}

/**
 * 显示就诊人选择弹窗
 */
const showPatientSelector = (patients) => {
  const patientNames = patients.map((p, index) => 
    `${p.patientName || p.name} (${p.gender === 'MALE' || p.gender === '男' ? '男' : '女'} ${p.age || ''}岁)`
  )
  
  uni.showActionSheet({
    title: '请选择要回访的就诊人',
    itemList: patientNames,
    success: (res) => {
      const selectedPatient = patients[res.tapIndex]
      // 使用id字段（patientId），不是userId
      const patientId = selectedPatient.id
      
      // 跳转到AI回访页面，带上患者信息
      uni.navigateTo({ 
        url: `/pages/ai/return-visit?patient_id=${patientId}&patientName=${selectedPatient.patientName || selectedPatient.name}&patientGender=${selectedPatient.gender}&patientAge=${selectedPatient.age}&from=home`
      })
    },
    fail: (res) => {
      console.log('用户取消选择')
    }
  })
}
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fffe 0%, #f0f9f6 100%);
  padding-bottom: 40rpx;
}

// 顶部导航
.header-section {
  background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
  padding: 20rpx 20rpx 50rpx;
  padding-top: 100rpx;
  
  .header-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
    
    .location {
      display: flex;
      align-items: center;
      color: #fff;
      
      .location-icon {
        font-size: 24rpx;
        margin-right: 6rpx;
      }
      
      .location-text {
        font-size: 26rpx;
        max-width: 140rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-weight: 500;
      }
      
      .location-arrow {
        font-size: 18rpx;
        margin-left: 6rpx;
        opacity: 0.8;
      }
    }
    
    .search-box {
      flex: 1;
      display: flex;
      align-items: center;
      background: rgba(255, 255, 255, 0.25);
      border-radius: 32rpx;
      padding: 10rpx 24rpx;
      height: 64rpx;
      backdrop-filter: blur(10rpx);
      
      .search-icon {
        font-size: 26rpx;
        margin-right: 12rpx;
        opacity: 0.9;
      }
      
      .search-placeholder {
        font-size: 26rpx;
        color: rgba(255, 255, 255, 0.85);
      }
    }
    
    .header-right {
      .scan-icon {
        font-size: 36rpx;
        color: #fff;
      }
    }
  }
}

// 轮播图
.banner-section {
  margin: -30rpx 0 30rpx;
  position: relative;
  z-index: 1;
  
  .banner-swiper {
    height: 240rpx;
    overflow: hidden;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
    
    .banner-item {
      width: 100%;
      height: 100%;
      position: relative;
      overflow: hidden;
      
      .banner-pattern {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        pointer-events: none;
      }
      
      .banner-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 32rpx 36rpx;
        height: 100%;
        box-sizing: border-box;
        
        .banner-text {
          flex: 1;
          
          .banner-title {
            display: block;
            font-size: 42rpx;
            font-weight: bold;
            color: #fff;
            margin-bottom: 12rpx;
            text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
          }
          
          .banner-subtitle {
            display: block;
            font-size: 26rpx;
            color: rgba(255, 255, 255, 0.95);
            margin-bottom: 24rpx;
          }
          
          .banner-btn {
            display: inline-flex;
            align-items: center;
            background: #fff;
            color: #00b894;
            font-size: 26rpx;
            font-weight: 600;
            padding: 14rpx 28rpx;
            border-radius: 32rpx;
            box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
            
            .btn-arrow {
              margin-left: 8rpx;
              font-size: 24rpx;
            }
          }
        }
        
        .banner-icon-wrap {
          width: 140rpx;
          height: 140rpx;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          margin-left: 20rpx;
          
          .banner-emoji {
            font-size: 80rpx;
            filter: drop-shadow(0 4rpx 12rpx rgba(0, 0, 0, 0.1));
          }
        }
      }
    }
  }
}

// 快捷功能
.quick-actions {
  margin: 0 30rpx 30rpx;
  
  .action-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24rpx;
    background: #fff;
    border-radius: 24rpx;
    padding: 32rpx 24rpx;
    box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
    
    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .action-icon {
        width: 96rpx;
        height: 96rpx;
        border-radius: 28rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16rpx;
        box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
        
        .icon-emoji {
          font-size: 48rpx;
        }
      }
      
      .action-label {
        font-size: 24rpx;
        color: #333;
        font-weight: 500;
      }
    }
  }
}

// 区块样式
.section {
  margin: 0 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .section-title-wrap {
      display: flex;
      align-items: center;
      
      .title-line {
        width: 6rpx;
        height: 32rpx;
        background: linear-gradient(180deg, #00b894 0%, #00cec9 100%);
        border-radius: 4rpx;
        margin-right: 16rpx;
      }
      
      .section-title {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
      }
    }
    
    .more {
      font-size: 26rpx;
      color: #999;
    }
  }
}

// 医生横向滚动
.doctor-scroll {
  white-space: nowrap;
  
  .doctor-list-horizontal {
    display: flex;
    gap: 24rpx;
    
    .doctor-card-mini {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      width: 160rpx;
      text-align: center;
      
      .doctor-avatar-wrap {
        position: relative;
        margin-bottom: 16rpx;
        
        .doctor-avatar-mini {
          width: 120rpx;
          height: 120rpx;
          border-radius: 50%;
          background: linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%);
          border: 4rpx solid #fff;
          box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
        }
        
        .online-tag {
          position: absolute;
          bottom: 0;
          right: 0;
          background: #00b894;
          color: #fff;
          font-size: 18rpx;
          padding: 4rpx 12rpx;
          border-radius: 12rpx;
          border: 2rpx solid #fff;
        }
      }
      
      .doctor-name {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
        margin-bottom: 6rpx;
      }
      
      .doctor-title {
        font-size: 22rpx;
        color: #00b894;
        margin-bottom: 4rpx;
        font-weight: 500;
      }
      
      .doctor-dept {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
}

// 诊所信息卡片
.clinic-section {
  .clinic-card {
    background: linear-gradient(135deg, #f8fffe 0%, #e8f5f0 100%);
    border-radius: 20rpx;
    padding: 30rpx;
    border: 1rpx solid rgba(0, 184, 148, 0.1);
    
    .clinic-header {
      display: flex;
      align-items: center;
      margin-bottom: 30rpx;
      padding-bottom: 24rpx;
      border-bottom: 1rpx solid rgba(0, 184, 148, 0.1);
      
      .clinic-logo-wrap {
        width: 80rpx;
        height: 80rpx;
        border-radius: 20rpx;
        margin-right: 20rpx;
        background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        
        .clinic-logo-emoji {
          font-size: 44rpx;
        }
      }
      
      .clinic-info {
        .clinic-name {
          display: block;
          font-size: 32rpx;
          font-weight: 600;
          color: #333;
          margin-bottom: 8rpx;
        }
        
        .clinic-slogan {
          display: block;
          font-size: 24rpx;
          color: #666;
        }
      }
    }
    
    .clinic-features {
      display: flex;
      justify-content: space-around;
      align-items: center;
      
      .feature-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        
        .feature-num {
          font-size: 36rpx;
          font-weight: bold;
          color: #00b894;
          margin-bottom: 8rpx;
        }
        
        .feature-label {
          font-size: 24rpx;
          color: #666;
        }
      }
      
      .feature-divider {
        width: 2rpx;
        height: 60rpx;
        background: rgba(0, 184, 148, 0.2);
      }
    }
  }
}
</style>
