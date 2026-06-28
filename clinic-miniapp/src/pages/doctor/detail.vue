<template>
  <view class="doctor-detail-page">
    <!-- 医生基本信息 -->
    <view class="doctor-header">
      <view class="header-bg"></view>
      <view class="doctor-card">
        <image
          :src="doctor.avatarUrl || '/static/default-avatar.png'"
          class="doctor-avatar"
          mode="aspectFill"
        />
        <view class="doctor-info">
          <view class="name-section">
            <text class="name">{{ doctor.doctorName }}</text>
            <text class="title">{{ doctor.position }}</text>
          </view>
          <text class="department">{{ doctor.deptName }}</text>
          <view class="stats">
            <view class="stat-item">
              <text class="number">{{ doctor.registrationCount || 0 }}</text>
              <text class="label">接诊次数</text>
            </view>
            <view class="stat-item">
              <text class="number">{{ doctor.goodRate || '100%' }}</text>
              <text class="label">好评率</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 医生简介 -->
    <view class="section">
      <view class="section-title">医生简介</view>
      <text class="intro-text">{{ doctor.introduction || '暂无简介' }}</text>
    </view>

    <!-- 擅长领域 -->
    <view class="section" v-if="doctor.specialty">
      <view class="section-title">擅长领域</view>
      <view class="tag-list">
        <text
          v-for="(tag, index) in specialtyList"
          :key="index"
          class="tag"
        >{{ tag }}</text>
      </view>
    </view>

    <!-- 排班信息 -->
    <view class="section schedule-section">
      <view class="section-title">预约挂号</view>

      <!-- 日期选择 -->
      <scroll-view class="date-list" scroll-x scroll-with-animation>
        <view
          v-for="date in dateList"
          :key="date.date"
          :class="['date-item', selectedDate === date.date ? 'active' : '']"
          @click="selectDate(date)"
        >
          <text class="day">{{ date.day }}</text>
          <text class="date">{{ date.date.slice(5) }}</text>
        </view>
      </scroll-view>

      <!-- 时间段选择 -->
      <view class="time-section">
        <view class="time-title">上午</view>
        <view class="time-grid">
          <view
            v-for="time in morningSlots"
            :key="time"
            :class="['time-item', selectedTime === time ? 'active' : '', !isTimeAvailable(time) ? 'disabled' : '']"
            @click="selectTime(time)"
          >
            {{ time }}
          </view>
        </view>

        <view class="time-title">下午</view>
        <view class="time-grid">
          <view
            v-for="time in afternoonSlots"
            :key="time"
            :class="['time-item', selectedTime === time ? 'active' : '', !isTimeAvailable(time) ? 'disabled' : '']"
            @click="selectTime(time)"
          >
            {{ time }}
          </view>
        </view>
      </view>
    </view>

    <!-- 底部按钮 -->
    <view class="footer">
      <view class="fee-info">
        <text class="label">挂号费</text>
        <text class="price">¥{{ doctor.regFee || 0 }}</text>
      </view>
      <button
        class="submit-btn"
        :disabled="!canSubmit"
        @click="submitRegistration"
      >
        立即预约
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDoctorDetail } from '@/api/miniapp/doctor.js'
import { getDoctorSchedule } from '@/api/miniapp/schedule.js'
import { generateDateList } from '@/utils/index.js'

const doctor = ref({})
const dateList = ref([])
const selectedDate = ref('')
const selectedTime = ref('')
const scheduleData = ref({})

const morningSlots = ['08:00-08:30', '08:30-09:00', '09:00-09:30', '09:30-10:00', '10:00-10:30', '10:30-11:00', '11:00-11:30']
const afternoonSlots = ['14:00-14:30', '14:30-15:00', '15:00-15:30', '15:30-16:00', '16:00-16:30', '16:30-17:00']

const specialtyList = computed(() => {
  if (!doctor.value.specialty) return []
  return doctor.value.specialty.split(/[,，、]/).filter(Boolean)
})

const canSubmit = computed(() => {
  return selectedDate.value && selectedTime.value && isTimeAvailable(selectedTime.value)
})

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const doctorId = currentPage.options.id

  if (doctorId) {
    loadDoctorDetail(doctorId)
    initDateList()
  }
})

const initDateList = () => {
  dateList.value = generateDateList(7)
  if (dateList.value.length > 0) {
    selectDate(dateList.value[0])
  }
}

const loadDoctorDetail = async (id) => {
  try {
    const res = await getDoctorDetail(id)
    doctor.value = res.data || {}
  } catch (error) {
    console.error('加载医生详情失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const loadSchedule = async () => {
  if (!doctor.value.id || !selectedDate.value) return

  try {
    const res = await getDoctorSchedule({
      doctorId: doctor.value.id,
      scheduleDate: selectedDate.value
    })
    scheduleData.value = res.data || {}
  } catch (error) {
    console.error('加载排班失败:', error)
  }
}

const selectDate = (date) => {
  selectedDate.value = date.date
  selectedTime.value = ''
  loadSchedule()
}

const selectTime = (time) => {
  if (!isTimeAvailable(time)) return
  selectedTime.value = time
}

const isTimeAvailable = (time) => {
  // 这里可以根据scheduleData判断时间段是否可预约
  // 简化处理：默认都可预约
  return true
}

const submitRegistration = () => {
  const params = {
    doctorId: doctor.value.id,
    doctorName: doctor.value.doctorName,
    doctorTitle: doctor.value.position,
    departmentName: doctor.value.deptName,
    deptId: doctor.value.deptId,
    regDate: selectedDate.value,
    regTime: selectedTime.value,
    regFee: doctor.value.regFee
  }

  uni.navigateTo({
    url: `/pages/registration/confirm?data=${encodeURIComponent(JSON.stringify(params))}`
  })
}
</script>

<style lang="scss" scoped>
.doctor-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 140rpx;
}

.doctor-header {
  position: relative;

  .header-bg {
    height: 200rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  .doctor-card {
    position: relative;
    margin: -80rpx 30rpx 0;
    background: #fff;
    border-radius: 20rpx;
    padding: 30rpx;
    display: flex;
    box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);

    .doctor-avatar {
      width: 160rpx;
      height: 160rpx;
      border-radius: 80rpx;
      margin-right: 24rpx;
      background: #f5f5f5;
    }

    .doctor-info {
      flex: 1;

      .name-section {
        display: flex;
        align-items: center;
        margin-bottom: 12rpx;

        .name {
          font-size: 40rpx;
          font-weight: bold;
          color: #333;
          margin-right: 16rpx;
        }

        .title {
          font-size: 24rpx;
          color: #1890ff;
          background: rgba(24, 144, 255, 0.1);
          padding: 4rpx 16rpx;
          border-radius: 8rpx;
        }
      }

      .department {
        font-size: 28rpx;
        color: #666;
        display: block;
        margin-bottom: 20rpx;
      }

      .stats {
        display: flex;
        gap: 40rpx;

        .stat-item {
          text-align: center;

          .number {
            display: block;
            font-size: 36rpx;
            font-weight: bold;
            color: #333;
          }

          .label {
            display: block;
            font-size: 24rpx;
            color: #999;
            margin-top: 4rpx;
          }
        }
      }
    }
  }
}

.section {
  margin: 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;

  .section-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 20rpx;
    padding-left: 20rpx;
    border-left: 8rpx solid #667eea;
  }

  .intro-text {
    font-size: 28rpx;
    color: #666;
    line-height: 1.8;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .tag {
      font-size: 26rpx;
      color: #667eea;
      background: rgba(102, 126, 234, 0.1);
      padding: 12rpx 24rpx;
      border-radius: 8rpx;
    }
  }
}

.schedule-section {
  .date-list {
    white-space: nowrap;
    margin-bottom: 30rpx;

    .date-item {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      padding: 20rpx 30rpx;
      margin-right: 16rpx;
      background: #f5f5f5;
      border-radius: 16rpx;
      min-width: 120rpx;

      .day {
        font-size: 26rpx;
        color: #666;
        margin-bottom: 8rpx;
      }

      .date {
        font-size: 24rpx;
        color: #999;
      }

      &.active {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

        .day,
        .date {
          color: #fff;
        }
      }

      &:last-child {
        margin-right: 0;
      }
    }
  }

  .time-section {
    .time-title {
      font-size: 28rpx;
      color: #666;
      margin-bottom: 16rpx;
    }

    .time-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16rpx;
      margin-bottom: 30rpx;

      .time-item {
        text-align: center;
        padding: 20rpx 0;
        font-size: 26rpx;
        color: #333;
        background: #f5f5f5;
        border-radius: 12rpx;

        &.active {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #fff;
        }

        &.disabled {
          color: #ccc;
          background: #f9f9f9;
        }
      }
    }
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

  .fee-info {
    .label {
      font-size: 26rpx;
      color: #666;
      margin-right: 12rpx;
    }

    .price {
      font-size: 40rpx;
      font-weight: bold;
      color: #ff6b6b;
    }
  }

  .submit-btn {
    width: 280rpx;
    height: 80rpx;
    line-height: 80rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 30rpx;
    border-radius: 40rpx;

    &::after {
      border: none;
    }

    &[disabled] {
      opacity: 0.5;
    }
  }
}
</style>
