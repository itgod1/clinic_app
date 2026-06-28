<template>
  <view class="dept-select-page">
    <!-- 顶部导航 -->
    <view class="page-header">
      <view class="header-content">
        <view class="nav-back" @click="goBack">
          <text class="nav-back-icon">←</text>
        </view>
        <text class="page-title">选择科室</text>
        <view class="header-placeholder"></view>
      </view>
    </view>

    <!-- 诊所信息卡片 -->
    <view class="clinic-card" v-if="clinicName">
      <view class="clinic-icon">🏥</view>
      <view class="clinic-info">
        <text class="clinic-label">当前院区</text>
        <text class="clinic-name">{{ clinicName }}</text>
      </view>
      <view class="clinic-arrow">›</view>
    </view>

    <view class="dept-container">
      <!-- 左侧科室列表 -->
      <scroll-view class="dept-sidebar" scroll-y>
        <view
          v-for="dept in departmentList"
          :key="dept.id"
          :class="['dept-item', selectedDeptId === dept.id ? 'active' : '']"
          @click="selectDept(dept)"
        >
          <view class="dept-icon" v-if="selectedDeptId === dept.id">●</view>
          <text class="dept-name">{{ dept.deptName }}</text>
        </view>
      </scroll-view>

      <!-- 右侧医生列表 -->
      <scroll-view class="doctor-list" scroll-y>
        <view v-if="loading" class="loading-state">
          <view class="loading-spinner"></view>
          <text class="loading-text">加载中...</text>
        </view>

        <view v-else-if="doctorList.length === 0" class="empty-state">
          <text class="empty-emoji">👨‍⚕️</text>
          <text class="empty-text">暂无医生</text>
          <text class="empty-subtext">该科室暂时无法预约</text>
        </view>

        <view
          v-for="doctor in doctorList"
          v-else
          :key="doctor.id"
          class="doctor-card"
          @click="selectDoctor(doctor)"
        >
          <view class="doctor-main">
            <view class="avatar-wrap">
              <image :src="doctor.avatarUrl || '/static/default-avatar.png'" class="avatar" mode="aspectFill" />
              <view class="status-badge" v-if="doctor.hasSchedule">可约</view>
            </view>
            <view class="doctor-info">
              <view class="info-header">
                <text class="doctor-name">{{ doctor.doctorName }}</text>
                <view class="doctor-title">{{ doctor.position }}</view>
              </view>
              <view class="doctor-tags" v-if="doctor.specialty">
                <text class="tag" v-for="(tag, idx) in doctor.specialty.split(',').slice(0, 2)" :key="idx">{{ tag }}</text>
              </view>
              <text class="doctor-intro">{{ doctor.introduction || '专业口腔医师，经验丰富' }}</text>
            </view>
          </view>
          <view class="doctor-footer">
            <view class="fee-info">
              <text class="fee-label">挂号费</text>
              <text class="fee-value">¥{{ doctor.regFee || 0 }}</text>
            </view>
            <view 
              class="reg-btn" 
              :class="{ 'disabled': !doctor.hasSchedule }" 
              @click.stop="selectDoctor(doctor)"
            >
              <text class="btn-text">{{ doctor.hasSchedule ? '立即预约' : '已满' }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDepartmentList } from '@/api/miniapp/clinic.js'
import { getDoctorList } from '@/api/miniapp/doctor.js'

const departmentList = ref([])
const doctorList = ref([])
const selectedDeptId = ref(null)
const loading = ref(false)
const clinicName = ref('')

onMounted(() => {
  const clinicId = uni.getStorageSync('clinicId')
  clinicName.value = uni.getStorageSync('clinicName') || '未知诊所'
  if (clinicId) {
    loadDepartments(clinicId)
  } else {
    uni.showToast({ title: '请先选择诊所', icon: 'none' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1500)
  }
})

const loadDepartments = async (clinicId) => {
  try {
    const res = await getDepartmentList({ clinicId, pageSize: 100 })
    departmentList.value = res.data?.list || res.data?.records || []
    if (departmentList.value.length > 0) {
      selectDept(departmentList.value[0])
    }
  } catch (error) {
    console.error('加载科室失败:', error)
    uni.showToast({ title: '加载科室失败', icon: 'none' })
  }
}

const selectDept = async (dept) => {
  selectedDeptId.value = dept.id
  await loadDoctors(dept.id)
}

const loadDoctors = async (deptId) => {
  loading.value = true
  try {
    const clinicId = uni.getStorageSync('clinicId')
    const res = await getDoctorList({ 
      clinicId, 
      deptId,
      pageSize: 100 
    })
    doctorList.value = res.data?.list || res.data?.records || []
  } catch (error) {
    console.error('加载医生失败:', error)
    uni.showToast({ title: '加载医生失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const selectDoctor = (doctor) => {
  if (!doctor.hasSchedule) {
    uni.showToast({ title: '该医生暂无号源', icon: 'none' })
    return
  }
  
  const regData = {
    doctorId: doctor.id,
    doctorName: doctor.doctorName,
    doctorTitle: doctor.position,
    deptId: selectedDeptId.value,
    departmentName: doctor.deptName,
    regDate: new Date().toISOString().split('T')[0],
    regTime: '08:00-12:00',
    regFee: doctor.regFee || 0
  }
  
  uni.navigateTo({
    url: `/pages/registration/confirm?data=${encodeURIComponent(JSON.stringify(regData))}`
  })
}

const goBack = () => {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.dept-select-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

// 顶部导航
.page-header {
  background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
  padding: 80rpx 30rpx 30rpx;

  .header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .nav-back {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10rpx);

    &:active {
      background: rgba(255, 255, 255, 0.35);
    }

    .nav-back-icon {
      font-size: 32rpx;
      color: #fff;
      font-weight: bold;
    }
  }

  .page-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #fff;
  }

  .header-placeholder {
    width: 64rpx;
  }
}

// 诊所信息卡片
.clinic-card {
  margin: 20rpx 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 30rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .clinic-icon {
    width: 72rpx;
    height: 72rpx;
    background: linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%);
    border-radius: 18rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    margin-right: 20rpx;
  }

  .clinic-info {
    flex: 1;

    .clinic-label {
      display: block;
      font-size: 22rpx;
      color: #999;
      margin-bottom: 4rpx;
    }

    .clinic-name {
      display: block;
      font-size: 30rpx;
      color: #333;
      font-weight: 500;
    }
  }

  .clinic-arrow {
    font-size: 36rpx;
    color: #ccc;
  }
}

// 主体容器
.dept-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  margin: 0 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

// 左侧科室列表
.dept-sidebar {
  width: 200rpx;
  background: #f8f9fa;
  height: 100%;
  border-radius: 24rpx 0 0 24rpx;

  .dept-item {
    padding: 32rpx 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    transition: all 0.3s;

    &.active {
      background: #fff;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 6rpx;
        height: 40rpx;
        background: linear-gradient(180deg, #00b894 0%, #00cec9 100%);
        border-radius: 0 4rpx 4rpx 0;
      }

      .dept-icon {
        display: block;
        color: #00b894;
        font-size: 16rpx;
        margin-right: 8rpx;
      }

      .dept-name {
        color: #00b894;
        font-weight: 600;
      }
    }

    .dept-icon {
      display: none;
    }

    .dept-name {
      font-size: 28rpx;
      color: #666;
      text-align: center;
    }
  }
}

// 右侧医生列表
.doctor-list {
  flex: 1;
  background: #fff;
  height: 100%;
  padding: 20rpx;
  border-radius: 0 24rpx 24rpx 0;

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 100rpx 0;

    .loading-spinner {
      width: 48rpx;
      height: 48rpx;
      border: 4rpx solid #f0f0f0;
      border-top-color: #00b894;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 20rpx;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .loading-text {
      font-size: 26rpx;
      color: #999;
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 80rpx 0;

    .empty-emoji {
      font-size: 100rpx;
      margin-bottom: 20rpx;
      opacity: 0.5;
    }

    .empty-text {
      font-size: 30rpx;
      color: #666;
      margin-bottom: 8rpx;
    }

    .empty-subtext {
      font-size: 24rpx;
      color: #999;
    }
  }

  .doctor-card {
    background: #fff;
    border-radius: 20rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    border: 1rpx solid #f0f0f0;
    transition: all 0.3s;

    &:active {
      transform: scale(0.98);
      box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
    }

    .doctor-main {
      display: flex;
      margin-bottom: 20rpx;
      padding-bottom: 20rpx;
      border-bottom: 1rpx solid #f5f5f5;

      .avatar-wrap {
        position: relative;
        margin-right: 20rpx;
        flex-shrink: 0;

        .avatar {
          width: 120rpx;
          height: 120rpx;
          border-radius: 50%;
          background: linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%);
          border: 4rpx solid #fff;
          box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
        }

        .status-badge {
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

      .doctor-info {
        flex: 1;
        min-width: 0;

        .info-header {
          display: flex;
          align-items: center;
          margin-bottom: 12rpx;

          .doctor-name {
            font-size: 32rpx;
            font-weight: 600;
            color: #333;
            margin-right: 16rpx;
          }

          .doctor-title {
            font-size: 22rpx;
            color: #00b894;
            background: rgba(0, 184, 148, 0.1);
            padding: 4rpx 14rpx;
            border-radius: 8rpx;
            font-weight: 500;
          }
        }

        .doctor-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 8rpx;
          margin-bottom: 12rpx;

          .tag {
            font-size: 20rpx;
            color: #666;
            background: #f5f5f5;
            padding: 4rpx 12rpx;
            border-radius: 6rpx;
          }
        }

        .doctor-intro {
          font-size: 24rpx;
          color: #999;
          display: block;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .doctor-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .fee-info {
        display: flex;
        align-items: baseline;

        .fee-label {
          font-size: 24rpx;
          color: #999;
          margin-right: 8rpx;
        }

        .fee-value {
          font-size: 36rpx;
          color: #ff6b6b;
          font-weight: 600;

          &::before {
            content: '¥';
            font-size: 24rpx;
          }
        }
      }

      .reg-btn {
        background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
        color: #fff;
        padding: 16rpx 36rpx;
        border-radius: 32rpx;
        font-size: 26rpx;
        font-weight: 500;
        box-shadow: 0 4rpx 16rpx rgba(0, 184, 148, 0.3);

        &.disabled {
          background: #d9d9d9;
          box-shadow: none;
        }

        &:active {
          opacity: 0.9;
        }

        .btn-text {
          display: block;
        }
      }
    }
  }
}
</style>
