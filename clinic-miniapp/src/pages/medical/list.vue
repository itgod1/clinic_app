<template>
  <view class="medical-list-page">
    <!-- 统计卡片 -->
    <view class="stats-card">
      <view class="stat-item">
        <text class="stat-num">{{ recordList.length }}</text>
        <text class="stat-label">就诊次数</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-num">{{ getCurrentYearCount() }}</text>
        <text class="stat-label">本年就诊</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-num">{{ getLastVisitDate() }}</text>
        <text class="stat-label">最近就诊</text>
      </view>
    </view>

    <!-- 记录列表 -->
    <scroll-view class="record-list" scroll-y @scrolltolower="loadMore">
      <view
        v-for="(record, index) in recordList"
        :key="record.id"
        class="record-card"
        @click="viewDetail(record)"
      >
        <!-- 卡片头部 -->
        <view class="card-header">
          <view class="visit-date">
            <text class="date-day">{{ getDay(record.visitDate) }}</text>
            <text class="date-month">{{ getMonth(record.visitDate) }}月</text>
          </view>
          <view class="visit-info">
            <text class="record-no">就诊号：{{ record.recordNo }}</text>
            <text class="visit-type">{{ record.visitTypeName || '门诊' }}</text>
          </view>
          <view class="status-tag" :class="getStatusClass(record.status)">
            {{ getStatusText(record.status) }}
          </view>
        </view>

        <!-- 卡片主体 -->
        <view class="card-body">
          <view class="doctor-info">
            <view class="avatar-wrap">
              <text class="avatar-icon">👨‍⚕️</text>
            </view>
            <view class="doctor-detail">
              <text class="doctor-name">{{ record.doctorName || '未知医生' }}</text>
              <text class="dept-name">{{ record.departmentName || '未知科室' }}</text>
            </view>
          </view>

          <view class="info-section" v-if="record.chiefComplaint">
            <view class="section-title">
              <text class="title-icon">📝</text>
              <text>主诉</text>
            </view>
            <text class="section-content">{{ record.chiefComplaint }}</text>
          </view>

          <view class="info-section" v-if="record.diagnosis && record.diagnosis !== '暂无诊断'">
            <view class="section-title">
              <text class="title-icon">🔍</text>
              <text>诊断</text>
            </view>
            <text class="section-content diagnosis">{{ record.diagnosis }}</text>
          </view>

          <view class="info-section" v-if="record.treatment">
            <view class="section-title">
              <text class="title-icon">💊</text>
              <text>治疗方案</text>
            </view>
            <text class="section-content">{{ record.treatment }}</text>
          </view>

          <view class="info-section" v-if="record.medicalAdvice">
            <view class="section-title">
              <text class="title-icon">⚕️</text>
              <text>医嘱</text>
            </view>
            <text class="section-content">{{ record.medicalAdvice }}</text>
          </view>
        </view>

        <!-- 卡片底部 -->
        <view class="card-footer">
          <text class="view-detail">查看详情</text>
          <text class="arrow-icon">›</text>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />
      <empty-data
        v-if="recordList.length === 0 && !loading"
        text="暂无就诊记录"
        subtext="您还没有就诊记录，请先预约挂号"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import EmptyData from '@/components/empty-data.vue'
import { getMedicalRecordList } from '@/api/medical.js'

const loading = ref(false)
const recordList = ref([])
const pageNum = ref(1)
const pageSize = 10
const loadStatus = ref('more')

onMounted(() => {
  loadRecords()
})

onPullDownRefresh(() => {
  pageNum.value = 1
  loadRecords().finally(() => {
    uni.stopPullDownRefresh()
  })
})

const loadRecords = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const clinicId = uni.getStorageSync('clinicId')
    const res = await getMedicalRecordList({
      clinicId,
      pageNum: pageNum.value,
      pageSize
    })
    const list = res.data.list || []
    
    // 转换后端字段为前端期望的格式
    const formattedList = list.map(item => ({
      id: item.id,
      recordNo: item.recordNo || '',
      visitDate: item.visitDate || (item.createdAt ? item.createdAt.split('T')[0] : ''),
      status: 2,
      doctorName: item.doctorName || '',
      doctorTitle: '',
      departmentName: item.deptName || '',
      diagnosis: item.diagnosis || '',
      chiefComplaint: item.chiefComplaint || '',
      treatment: item.treatment || '',
      medicalAdvice: item.medicalAdvice || '',
      visitTypeName: item.visitTypeName || '门诊',
      prescriptionId: null
    }))

    if (pageNum.value === 1) {
      recordList.value = formattedList
    } else {
      recordList.value.push(...formattedList)
    }

    loadStatus.value = list.length < pageSize ? 'noMore' : 'more'
  } catch (error) {
    console.error('加载就诊记录失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (loadStatus.value === 'noMore' || loading.value) return
  pageNum.value++
  loadRecords()
}

const getStatusText = (status) => {
  const map = {
    0: '待就诊',
    1: '就诊中',
    2: '已完成',
    3: '已取消'
  }
  return map[status] || '已完成'
}

const getStatusClass = (status) => {
  const map = {
    0: 'pending',
    1: 'in-progress',
    2: 'completed',
    3: 'cancelled'
  }
  return map[status] || 'completed'
}

const getDay = (dateStr) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return date.getDate().toString().padStart(2, '0')
}

const getMonth = (dateStr) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return (date.getMonth() + 1).toString()
}

const getCurrentYearCount = () => {
  const currentYear = new Date().getFullYear()
  return recordList.value.filter(r => {
    if (!r.visitDate) return false
    return new Date(r.visitDate).getFullYear() === currentYear
  }).length
}

const getLastVisitDate = () => {
  if (recordList.value.length === 0) return '-'
  const lastRecord = recordList.value[0]
  if (!lastRecord.visitDate) return '-'
  const date = new Date(lastRecord.visitDate)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const viewDetail = (record) => {
  uni.navigateTo({
    url: `/pages/medical/detail?id=${record.id}`
  })
}
</script>

<style lang="scss" scoped>
.medical-list-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f9f6 0%, #f5f7fa 100%);
}

// 统计卡片
.stats-card {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: linear-gradient(135deg, #00b894 0%, #00d4a8 100%);
  margin: 20rpx 30rpx;
  padding: 30rpx;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 184, 148, 0.2);

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-num {
      font-size: 40rpx;
      font-weight: bold;
      color: #fff;
      margin-bottom: 8rpx;
    }

    .stat-label {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.9);
    }
  }

  .stat-divider {
    width: 2rpx;
    height: 60rpx;
    background: rgba(255, 255, 255, 0.3);
  }
}

// 记录列表
.record-list {
  height: calc(100vh - 200rpx);
  padding: 0 30rpx 30rpx;

  .record-card {
    background: #fff;
    border-radius: 20rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
    overflow: hidden;

    .card-header {
      display: flex;
      align-items: center;
      padding: 24rpx 30rpx;
      background: linear-gradient(135deg, #f8fffe 0%, #e8f5f0 100%);
      border-bottom: 1rpx solid rgba(0, 184, 148, 0.1);

      .visit-date {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 80rpx;
        height: 80rpx;
        background: linear-gradient(135deg, #00b894 0%, #00d4a8 100%);
        border-radius: 16rpx;
        margin-right: 20rpx;

        .date-day {
          font-size: 32rpx;
          font-weight: bold;
          color: #fff;
          line-height: 1;
        }

        .date-month {
          font-size: 20rpx;
          color: rgba(255, 255, 255, 0.9);
          margin-top: 4rpx;
        }
      }

      .visit-info {
        flex: 1;
        display: flex;
        flex-direction: column;

        .record-no {
          font-size: 24rpx;
          color: #999;
          margin-bottom: 8rpx;
        }

        .visit-type {
          font-size: 26rpx;
          color: #00b894;
          font-weight: 500;
        }
      }

      .status-tag {
        font-size: 22rpx;
        padding: 6rpx 16rpx;
        border-radius: 20rpx;
        font-weight: 500;

        &.pending {
          background: #e6f7ff;
          color: #1890ff;
        }

        &.in-progress {
          background: #f6ffed;
          color: #52c41a;
        }

        &.completed {
          background: #00b894;
          color: #fff;
        }

        &.cancelled {
          background: #fff1f0;
          color: #ff4d4f;
        }
      }
    }

    .card-body {
      padding: 24rpx 30rpx;

      .doctor-info {
        display: flex;
        align-items: center;
        margin-bottom: 24rpx;
        padding-bottom: 20rpx;
        border-bottom: 1rpx dashed #eee;

        .avatar-wrap {
          width: 80rpx;
          height: 80rpx;
          background: linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 20rpx;

          .avatar-icon {
            font-size: 44rpx;
          }
        }

        .doctor-detail {
          display: flex;
          flex-direction: column;

          .doctor-name {
            font-size: 30rpx;
            font-weight: 600;
            color: #333;
            margin-bottom: 6rpx;
          }

          .dept-name {
            font-size: 24rpx;
            color: #00b894;
          }
        }
      }

      .info-section {
        margin-bottom: 20rpx;

        &:last-child {
          margin-bottom: 0;
        }

        .section-title {
          display: flex;
          align-items: center;
          margin-bottom: 12rpx;

          .title-icon {
            font-size: 28rpx;
            margin-right: 8rpx;
          }

          text {
            font-size: 26rpx;
            color: #666;
            font-weight: 500;
          }
        }

        .section-content {
          font-size: 28rpx;
          color: #333;
          line-height: 1.6;
          padding-left: 36rpx;

          &.diagnosis {
            color: #00b894;
            font-weight: 500;
          }
        }
      }
    }

    .card-footer {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20rpx 30rpx;
      background: #f8fffe;
      border-top: 1rpx solid rgba(0, 184, 148, 0.1);

      .view-detail {
        font-size: 26rpx;
        color: #00b894;
        font-weight: 500;
      }

      .arrow-icon {
        font-size: 32rpx;
        color: #00b894;
        margin-left: 8rpx;
      }
    }
  }
}
</style>
