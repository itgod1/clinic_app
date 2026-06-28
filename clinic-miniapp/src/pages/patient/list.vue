<template>
  <view class="patient-list-page">
    <!-- 患者列表 -->
    <scroll-view class="patient-list" scroll-y>
      <view
        v-for="patient in patientList"
        :key="patient.id"
        class="patient-card"
        @click="editPatient(patient)"
      >
        <view class="patient-main">
          <view class="patient-header">
            <text class="name">{{ patient.name }}</text>
            <view class="tags">
              <text class="tag gender" :class="patient.gender === 1 ? 'male' : 'female'">
                {{ patient.gender === 1 ? '男' : '女' }}
              </text>
              <text class="tag age">{{ patient.age }}岁</text>
              <text v-if="patient.isDefault" class="tag default">默认</text>
            </view>
          </view>
          <view class="patient-info">
            <text class="info-item">
              <text class="label">手机号：</text>
              <text class="value">{{ patient.phone }}</text>
            </text>
            <text class="info-item">
              <text class="label">身份证号：</text>
              <text class="value">{{ formatIdCard(patient.idCard) }}</text>
            </text>
          </view>
        </view>
        <text class="arrow">></text>
      </view>
      <empty-data
        v-if="patientList.length === 0 && !loading"
        text="暂无就诊人"
        subtext="请添加就诊人"
        :show-button="true"
        button-text="添加就诊人"
        @click="addPatient"
      />
    </scroll-view>

    <!-- 添加按钮 -->
    <view class="footer">
      <button class="add-btn" @click="addPatient">+ 添加就诊人</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import EmptyData from '@/components/empty-data.vue'
import { getPatientList } from '@/api/patient.js'

const loading = ref(false)
const patientList = ref([])

onMounted(() => {
  loadPatients()
})

onShow(() => {
  loadPatients()
})

onPullDownRefresh(() => {
  loadPatients().finally(() => {
    uni.stopPullDownRefresh()
  })
})

const loadPatients = async () => {
  loading.value = true
  try {
    const clinicId = uni.getStorageSync('clinicId')
    if (!clinicId) {
      uni.showToast({ title: '请先选择诊所', icon: 'none' })
      return
    }
    const res = await getPatientList({ pageSize: 100, clinicId })
    // 新接口直接返回数组
    patientList.value = Array.isArray(res.data) ? res.data : (res.data.list || [])
  } catch (error) {
    console.error('加载就诊人列表失败:', error)
  } finally {
    loading.value = false
  }
}

const formatIdCard = (idCard) => {
  if (!idCard || idCard.length !== 18) return idCard
  return idCard.replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
}

const addPatient = () => {
  uni.navigateTo({ url: '/pages/patient/edit' })
}

const editPatient = (patient) => {
  uni.navigateTo({
    url: `/pages/patient/edit?id=${patient.id}`
  })
}
</script>

<style lang="scss" scoped>
.patient-list-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.patient-list {
  height: calc(100vh - 140rpx);
  padding: 20rpx 30rpx;

  .patient-card {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;

    .patient-main {
      flex: 1;

      .patient-header {
        display: flex;
        align-items: center;
        margin-bottom: 16rpx;

        .name {
          font-size: 36rpx;
          font-weight: 600;
          color: #333;
          margin-right: 20rpx;
        }

        .tags {
          display: flex;
          gap: 12rpx;

          .tag {
            font-size: 22rpx;
            padding: 4rpx 16rpx;
            border-radius: 8rpx;

            &.gender {
              &.male {
                background: #e6f7ff;
                color: #1890ff;
              }

              &.female {
                background: #fff0f6;
                color: #eb2f96;
              }
            }

            &.age {
              background: #f6ffed;
              color: #52c41a;
            }

            &.default {
              background: #fff7e6;
              color: #fa8c16;
            }
          }
        }
      }

      .patient-info {
        .info-item {
          display: block;
          font-size: 26rpx;
          margin-bottom: 8rpx;

          &:last-child {
            margin-bottom: 0;
          }

          .label {
            color: #999;
          }

          .value {
            color: #666;
          }
        }
      }
    }

    .arrow {
      font-size: 32rpx;
      color: #ccc;
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
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

  .add-btn {
    width: 100%;
    height: 90rpx;
    line-height: 90rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 45rpx;

    &::after {
      border: none;
    }
  }
}
</style>
