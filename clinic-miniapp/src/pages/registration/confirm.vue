<template>
  <view class="registration-confirm-page">
    <!-- 医生信息 -->
    <view class="card doctor-card">
      <view class="card-title">预约医生</view>
      <view class="doctor-info">
        <view class="info-row">
          <text class="label">医生姓名</text>
          <text class="value">{{ regData.doctorName }} {{ regData.doctorTitle }}</text>
        </view>
        <view class="info-row">
          <text class="label">所属科室</text>
          <text class="value">{{ regData.departmentName }}</text>
        </view>
        <view class="info-row">
          <text class="label">挂号费用</text>
          <text class="value price">¥{{ regData.regFee }}</text>
        </view>
      </view>
    </view>

    <!-- 预约时间选择 -->
    <view class="card time-card">
      <view class="card-title">选择预约时间</view>
      
      <!-- 日期选择 -->
      <view class="date-section">
        <text class="section-label">预约日期</text>
        <scroll-view class="date-list" scroll-x>
          <view
            v-for="date in dateList"
            :key="date.value"
            :class="['date-item', selectedDate === date.value ? 'active' : '']"
            @click="selectDate(date.value)"
          >
            <text class="date-week">{{ date.week }}</text>
            <text class="date-day">{{ date.day }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- 时段选择 -->
      <view class="time-section">
        <text class="section-label">就诊时段</text>
        
        <!-- 上午时段 -->
        <view class="time-period">
          <text class="period-title">上午</text>
          <view class="time-grid">
            <view
              v-for="time in morningSlots"
              :key="time"
              :class="['time-slot', selectedTime === time ? 'active' : '', !isTimeAvailable(time) ? 'disabled' : '']"
              @click="isTimeAvailable(time) && selectTime(time)"
            >
              {{ time }}
            </view>
          </view>
        </view>
        
        <!-- 下午时段 -->
        <view class="time-period">
          <text class="period-title">下午</text>
          <view class="time-grid">
            <view
              v-for="time in afternoonSlots"
              :key="time"
              :class="['time-slot', selectedTime === time ? 'active' : '', !isTimeAvailable(time) ? 'disabled' : '']"
              @click="isTimeAvailable(time) && selectTime(time)"
            >
              {{ time }}
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 就诊人选择 -->
    <view class="card patient-card">
      <view class="card-title">
        <text>就诊人信息</text>
        <text class="add-btn" @click="addPatient">+ 添加</text>
      </view>

      <view v-if="selectedPatient.id" class="patient-info">
        <view class="info-row">
          <text class="label">姓名</text>
          <text class="value">{{ selectedPatient.patientName || '未获取' }}</text>
        </view>
        <view class="info-row">
          <text class="label">电话</text>
          <text class="value">{{ selectedPatient.phone || '未获取' }}</text>
        </view>
        <view class="info-row">
          <text class="label">性别</text>
          <text class="value">{{ selectedPatient.gender === 1 ? '男' : '女' }}</text>
        </view>
        <view class="info-row">
          <text class="label">年龄</text>
          <text class="value">{{ selectedPatient.age }}岁</text>
        </view>
        <view class="info-row">
          <text class="label">手机号</text>
          <text class="value">{{ selectedPatient.phone }}</text>
        </view>
      </view>

      <view v-else class="patient-selector" @click="selectPatient">
        <text class="placeholder">请选择就诊人</text>
        <text class="arrow">></text>
      </view>
    </view>

    <!-- 就诊类型 -->
    <view class="card type-card">
      <view class="card-title">就诊类型</view>
      <view class="type-selector">
        <view
          :class="['type-item', visitType === 1 ? 'active' : '']"
          @click="visitType = 1"
        >
          <text class="type-name">初诊</text>
          <text class="type-desc">首次就诊</text>
        </view>
        <view
          :class="['type-item', visitType === 2 ? 'active' : '']"
          @click="visitType = 2"
        >
          <text class="type-name">复诊</text>
          <text class="type-desc">再次就诊</text>
        </view>
      </view>
    </view>

    <!-- 病情描述 -->
    <view class="card desc-card">
      <view class="card-title">病情描述（选填）</view>
      <textarea
        v-model="chiefComplaint"
        class="desc-textarea"
        placeholder="请简单描述您的症状，便于医生提前了解..."
        maxlength="200"
      />
      <text class="word-count">{{ chiefComplaint.length }}/200</text>
    </view>

    <!-- 底部按钮 -->
    <view class="footer">
      <view class="total-fee">
        <text class="label">合计:</text>
        <text class="price">¥{{ regData.regFee }}</text>
      </view>
      <button
        class="submit-btn"
        :disabled="!canSubmit"
        :loading="submitting"
        @click="submit"
      >
        确认预约
      </button>
    </view>

    <!-- 就诊人选择弹窗 -->
    <uni-popup ref="patientPopup" type="bottom">
      <view class="patient-popup">
        <view class="popup-header">
          <text class="title">选择就诊人</text>
          <text class="close" @click="closePatientPopup">✕</text>
        </view>
        <scroll-view class="patient-list" scroll-y>
          <view
            v-for="patient in patientList"
            :key="patient.id"
            :class="['patient-item', selectedPatient.id === patient.id ? 'selected' : '']"
            @click="confirmSelectPatient(patient)"
          >
            <view class="patient-main">
              <text class="name">{{ patient.patientName }}</text>
              <text class="info">{{ patient.gender === 1 ? '男' : '女' }} | {{ patient.age }}岁</text>
            </view>
            <text class="phone">{{ patient.phone }}</text>
            <text v-if="selectedPatient.id === patient.id" class="check">✓</text>
          </view>
        </scroll-view>
        <view class="popup-footer">
          <button class="add-btn" @click="addPatient">+ 添加就诊人</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createRegistration } from '@/api/registration.js'
import { getPatientList } from '@/api/patient.js'
import { getDoctorSchedule } from '@/api/miniapp/schedule.js'
import { showSuccess, showError } from '@/utils/index.js'

const regData = ref({})
const patientList = ref([])
const selectedPatient = ref({})
const visitType = ref(1)
const chiefComplaint = ref('')
const submitting = ref(false)
const patientPopup = ref(null)

// 日期和时段选择
const dateList = ref([])
const selectedDate = ref('')
const selectedTime = ref('')

// 详细时段
const morningSlots = ['08:00-08:30', '08:30-09:00', '09:00-09:30', '09:30-10:00', '10:00-10:30', '10:30-11:00', '11:00-11:30']
const afternoonSlots = ['14:00-14:30', '14:30-15:00', '15:00-15:30', '15:30-16:00', '16:00-16:30', '16:30-17:00']

// 时段可用状态（上午/下午）
const timeSlots = ref([
  { name: '上午', range: '08:00-12:00', value: '08:00-12:00', available: false },
  { name: '下午', range: '14:00-18:00', value: '14:00-18:00', available: false }
])

// 存储详细的排班数据
const scheduleData = ref([])

// 判断时段是否可预约
const isTimeAvailable = (time) => {
  // 根据排班数据判断时段是否可用
  if (!scheduleData.value || scheduleData.value.length === 0) {
    return false
  }
  
  // 解析时段的开始时间
  const startTime = time.split('-')[0]
  const [hour, minute] = startTime.split(':').map(Number)
  const slotStartMinutes = hour * 60 + minute
  
  // 查找包含该时段的排班
  const schedule = scheduleData.value.find(s => {
    if (!s.timeSlotStart || !s.timeSlotEnd) return false
    
    // 解析排班开始时间
    const scheduleStartHour = parseInt(s.timeSlotStart.substring(0, 2))
    const scheduleStartMinute = parseInt(s.timeSlotStart.substring(3, 5))
    const scheduleStartMinutes = scheduleStartHour * 60 + scheduleStartMinute
    
    // 解析排班结束时间
    const scheduleEndHour = parseInt(s.timeSlotEnd.substring(0, 2))
    const scheduleEndMinute = parseInt(s.timeSlotEnd.substring(3, 5))
    const scheduleEndMinutes = scheduleEndHour * 60 + scheduleEndMinute
    
    // 检查时段是否在排班范围内
    return slotStartMinutes >= scheduleStartMinutes && slotStartMinutes < scheduleEndMinutes
  })
  
  // 如果有排班且号源大于0，则可用
  return schedule && schedule.remainingQuota > 0
}

const canSubmit = computed(() => {
  return selectedPatient.value.id && selectedDate.value && selectedTime.value && !submitting.value
})

onMounted(() => {
  // 获取页面参数
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const data = currentPage.options.data

  if (data) {
    try {
      regData.value = JSON.parse(decodeURIComponent(data))
    } catch (e) {
      console.error('解析参数失败:', e)
    }
  }

  // 生成未来7天的日期列表
  generateDateList()
  
  loadPatients()
})

// 生成日期列表
const generateDateList = () => {
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const list = []
  const today = new Date()
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + i)
    
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const dateStr = `${year}-${month}-${day}`
    
    list.push({
      value: dateStr,
      week: i === 0 ? '今天' : weekDays[date.getDay()],
      day: `${month}-${day}`
    })
  }
  
  dateList.value = list
  // 默认选中今天
  selectedDate.value = list[0]?.value || ''
  // 加载今天的排班
  loadSchedule()
}

// 选择日期
const selectDate = (date) => {
  selectedDate.value = date
  selectedTime.value = ''
  loadSchedule()
}

// 选择时段
const selectTime = (time) => {
  selectedTime.value = time
}

// 加载医生排班
const loadSchedule = async () => {
  if (!regData.value.doctorId || !selectedDate.value) return

  try {
    const clinicId = uni.getStorageSync('clinicId')
    const res = await getDoctorSchedule({
      clinicId,
      doctorId: regData.value.doctorId,
      startDate: selectedDate.value,
      endDate: selectedDate.value
    })

    const schedules = res.data || []

    // 存储详细排班数据
    scheduleData.value = schedules

    // 重置时段可用状态
    timeSlots.value[0].available = false
    timeSlots.value[1].available = false

    // 根据排班更新时段可用状态
    schedules.forEach(schedule => {
      const startHour = parseInt(schedule.timeSlotStart?.substring(0, 2) || '0')
      const remaining = schedule.remainingQuota || 0

      if (startHour < 12) {
        // 上午
        timeSlots.value[0].available = remaining > 0
      } else {
        // 下午
        timeSlots.value[1].available = remaining > 0
      }
    })

    console.log('排班数据:', schedules)
  } catch (error) {
    console.error('加载排班失败:', error)
    scheduleData.value = []
  }
}

const loadPatients = async () => {
  try {
    const clinicId = uni.getStorageSync('clinicId')
    if (!clinicId) {
      uni.showToast({ title: '请先选择诊所', icon: 'none' })
      return
    }
    const res = await getPatientList({ pageSize: 100, clinicId })
    // 新接口直接返回数组
    patientList.value = Array.isArray(res.data) ? res.data : (res.data.list || [])

    // 默认选中第一个
    if (patientList.value.length > 0 && !selectedPatient.value.id) {
      selectedPatient.value = patientList.value[0]
    }
  } catch (error) {
    console.error('加载就诊人列表失败:', error)
  }
}

const selectPatient = () => {
  if (patientList.value.length === 0) {
    addPatient()
    return
  }
  patientPopup.value?.open()
}

const confirmSelectPatient = (patient) => {
  selectedPatient.value = patient
  closePatientPopup()
}

const closePatientPopup = () => {
  patientPopup.value?.close()
}

const addPatient = () => {
  uni.navigateTo({
    url: '/pages/patient/edit?from=registration'
  })
}

const submit = async () => {
  if (!canSubmit.value) return

  submitting.value = true
  uni.showLoading({ title: '提交中...' })

  // 调试输出
  console.log('selectedPatient:', selectedPatient.value)
  console.log('patientName:', selectedPatient.value.patientName)
  console.log('phone:', selectedPatient.value.phone)

  try {
    await createRegistration({
      doctorId: regData.value.doctorId,
      deptId: regData.value.deptId,
      patientId: selectedPatient.value.id,
      patientName: selectedPatient.value.patientName,
      phone: selectedPatient.value.phone,
      regDate: selectedDate.value,
      regTime: selectedTime.value,
      visitType: visitType.value,
      chiefComplaint: chiefComplaint.value
    })

    uni.showToast({
      title: '预约成功',
      icon: 'success',
      duration: 1500,
      success: () => {
        setTimeout(() => {
          uni.switchTab({
            url: '/pages/registration/list',
            success: () => {
              console.log('跳转成功')
            },
            fail: (err) => {
              console.error('跳转失败:', err)
              uni.showToast({ title: '跳转失败', icon: 'none' })
            }
          })
        }, 1500)
      }
    })
  } catch (error) {
    showError(error.message || '预约失败')
  } finally {
    submitting.value = false
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.registration-confirm-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .card-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 24rpx;
    padding-left: 20rpx;
    border-left: 8rpx solid #667eea;

    .add-btn {
      font-size: 28rpx;
      color: #667eea;
      font-weight: normal;
    }
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;

    &:last-child {
      margin-bottom: 0;
    }

    .label {
      font-size: 28rpx;
      color: #999;
    }

    .value {
      font-size: 28rpx;
      color: #333;

      &.highlight {
        color: #667eea;
        font-weight: 500;
      }

      &.price {
        color: #ff6b6b;
        font-size: 36rpx;
        font-weight: bold;
      }
    }
  }
}

.patient-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40rpx 30rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  border: 2rpx dashed #ddd;

  .placeholder {
    font-size: 30rpx;
    color: #999;
  }

  .arrow {
    font-size: 32rpx;
    color: #999;
  }
}

.type-selector {
  display: flex;
  gap: 20rpx;

  .type-item {
    flex: 1;
    text-align: center;
    padding: 30rpx;
    background: #f5f5f5;
    border-radius: 12rpx;
    border: 3rpx solid transparent;

    .type-name {
      display: block;
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 8rpx;
    }

    .type-desc {
      display: block;
      font-size: 24rpx;
      color: #999;
    }

    &.active {
      background: rgba(102, 126, 234, 0.1);
      border-color: #667eea;

      .type-name {
        color: #667eea;
      }
    }
  }
}

// 日期选择样式
.date-section {
  margin-bottom: 30rpx;

  .section-label {
    display: block;
    font-size: 28rpx;
    color: #666;
    margin-bottom: 20rpx;
  }

  .date-list {
    white-space: nowrap;

    .date-item {
      display: inline-block;
      text-align: center;
      padding: 20rpx 30rpx;
      margin-right: 16rpx;
      background: #f5f5f5;
      border-radius: 12rpx;
      border: 3rpx solid transparent;

      .date-week {
        display: block;
        font-size: 24rpx;
        color: #999;
        margin-bottom: 8rpx;
      }

      .date-day {
        display: block;
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
      }

      &.active {
        background: rgba(102, 126, 234, 0.1);
        border-color: #667eea;

        .date-week, .date-day {
          color: #667eea;
        }
      }
    }
  }
}

// 时段选择样式
.time-section {
  .section-label {
    display: block;
    font-size: 28rpx;
    color: #666;
    margin-bottom: 20rpx;
  }

  .time-period {
    margin-bottom: 30rpx;

    .period-title {
      font-size: 28rpx;
      color: #666;
      margin-bottom: 16rpx;
    }

    .time-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16rpx;

      .time-slot {
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

.desc-textarea {
  width: 100%;
  height: 200rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
  position: relative;
  z-index: 10;
}

.word-count {
  display: block;
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
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

  .total-fee {
    .label {
      font-size: 28rpx;
      color: #666;
      margin-right: 12rpx;
    }

    .price {
      font-size: 44rpx;
      font-weight: bold;
      color: #ff6b6b;
    }
  }

  .submit-btn {
    width: 280rpx;
    height: 88rpx;
    line-height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;

    &::after {
      border: none;
    }

    &[disabled] {
      opacity: 0.5;
    }
  }
}

.patient-popup {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 70vh;

  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .title {
      font-size: 34rpx;
      font-weight: 600;
      color: #333;
    }

    .close {
      font-size: 40rpx;
      color: #999;
      padding: 10rpx;
    }
  }

  .patient-list {
    max-height: 50vh;

    .patient-item {
      display: flex;
      align-items: center;
      padding: 30rpx;
      border-bottom: 1rpx solid #f5f5f5;

      &.selected {
        background: rgba(102, 126, 234, 0.05);
      }

      .patient-main {
        flex: 1;

        .name {
          display: block;
          font-size: 32rpx;
          font-weight: 500;
          color: #333;
          margin-bottom: 8rpx;
        }

        .info {
          display: block;
          font-size: 26rpx;
          color: #999;
        }
      }

      .phone {
        font-size: 26rpx;
        color: #666;
        margin-right: 20rpx;
      }

      .check {
        font-size: 32rpx;
        color: #667eea;
        font-weight: bold;
      }
    }
  }

  .popup-footer {
    padding: 20rpx 30rpx;
    border-top: 1rpx solid #f0f0f0;

    .add-btn {
      width: 100%;
      height: 88rpx;
      line-height: 88rpx;
      background: #f5f5f5;
      color: #667eea;
      font-size: 30rpx;
      border-radius: 44rpx;

      &::after {
        border: none;
      }
    }
  }
}
</style>
