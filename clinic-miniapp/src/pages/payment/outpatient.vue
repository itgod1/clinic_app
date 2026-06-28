<template>
  <view class="payment-page">
    <!-- 状态筛选 -->
    <view class="filter-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-item', activeTab === tab.value ? 'active' : '']"
        @click="selectTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 缴费列表 -->
    <scroll-view class="payment-list" scroll-y @scrolltolower="loadMore">
      <view
        v-for="item in paymentList"
        :key="item.id"
        class="payment-card"
        @click="goToDetail(item)"
      >
        <view class="card-header">
          <view class="prescription-no">处方号: {{ item.prescriptionNo }}</view>
          <view :class="['status-tag', getStatusClass(item.paymentStatus)]">
            {{ item.statusName || getStatusText(item.paymentStatus) }}
          </view>
        </view>

        <view class="card-body">
          <view class="info-row">
            <text class="label">就诊人:</text>
            <text class="value">{{ item.patientName }}</text>
          </view>
          <view class="info-row">
            <text class="label">医生:</text>
            <text class="value">{{ item.doctorName }}</text>
          </view>
          <view class="info-row">
            <text class="label">科室:</text>
            <text class="value">{{ item.departmentName }}</text>
          </view>
          <view class="info-row">
            <text class="label">诊断:</text>
            <text class="value diagnosis">{{ item.diagnosis || '-' }}</text>
          </view>
        </view>

        <view class="card-footer">
          <view class="amount-info">
            <text class="amount-label">应缴金额:</text>
            <text class="amount-value">¥{{ item.totalAmount || '0.00' }}</text>
          </view>
          <view class="action-btns">
            <button
              v-if="item.paymentStatus === 0"
              class="btn-pay"
              @click.stop="goToPay(item)"
            >
              立即缴费
            </button>
            <button
              v-else
              class="btn-detail"
              @click.stop="goToDetail(item)"
            >
              查看详情
            </button>
          </view>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />

      <empty-data
        v-if="paymentList.length === 0 && !loading"
        :text="emptyText"
        subtext="暂无相关费用记录"
        :show-button="activeTab === 0"
        button-text="去挂号"
        @click="goToDoctor"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import EmptyData from '@/components/empty-data.vue'
import { getUnpaidList, getPaymentHistory } from '@/api/payment.js'

const loading = ref(false)
const paymentList = ref([])
const activeTab = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loadStatus = ref('more')

const tabs = [
  { label: '待缴费', value: 0 },
  { label: '已缴费', value: 1 }
]

const emptyText = computed(() => {
  const map = {
    0: '暂无待缴费项目',
    1: '暂无缴费记录'
  }
  return map[activeTab.value]
})

onMounted(() => {
  loadPayments()
})

onShow(() => {
  // 页面显示时刷新数据
  pageNum.value = 1
  loadPayments()
})

onPullDownRefresh(() => {
  pageNum.value = 1
  loadPayments().finally(() => {
    uni.stopPullDownRefresh()
  })
})

const loadPayments = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const clinicId = uni.getStorageSync('clinicId')
    const userInfo = uni.getStorageSync('userInfo')

    if (!clinicId || !userInfo) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    const patientInfo = JSON.parse(userInfo)

    const params = {
      clinicId,
      userId: patientInfo.id,
      pageNum: pageNum.value,
      pageSize
    }

    let res
    if (activeTab.value === 0) {
      res = await getUnpaidList(params)
    } else {
      res = await getPaymentHistory(params)
    }

    const list = res.data?.list || []

    if (pageNum.value === 1) {
      paymentList.value = list
    } else {
      paymentList.value.push(...list)
    }

    loadStatus.value = list.length < pageSize ? 'noMore' : 'more'
  } catch (error) {
    console.error('加载缴费列表失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const selectTab = (value) => {
  activeTab.value = value
  pageNum.value = 1
  loadPayments()
}

const loadMore = () => {
  if (loadStatus.value === 'noMore' || loading.value) return
  pageNum.value++
  loadPayments()
}

const getStatusText = (status) => {
  const map = {
    0: '待缴费',
    1: '已支付',
    2: '已退款'
  }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = {
    0: 'status-unpaid',
    1: 'status-paid',
    2: 'status-refund'
  }
  return map[status] || ''
}

const goToPay = (item) => {
  uni.navigateTo({
    url: `/pages/payment/detail?id=${item.id}&action=pay`
  })
}

const goToDetail = (item) => {
  uni.navigateTo({
    url: `/pages/payment/detail?id=${item.id}`
  })
}

const goToDoctor = () => {
  uni.switchTab({ url: '/pages/doctor/list' })
}
</script>

<style lang="scss" scoped>
.payment-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.filter-tabs {
  display: flex;
  background: #fff;
  padding: 0 30rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 30rpx 0;
    font-size: 30rpx;
    color: #666;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 60rpx;
        height: 4rpx;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 2rpx;
      }
    }
  }
}

.payment-list {
  height: calc(100vh - 100rpx);
  padding: 20rpx 30rpx;
}

.payment-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 30rpx;
    border-bottom: 1rpx solid #f5f5f5;

    .prescription-no {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }

    .status-tag {
      font-size: 24rpx;
      padding: 6rpx 16rpx;
      border-radius: 8rpx;

      &.status-unpaid {
        background: #fff2f0;
        color: #ff4d4f;
      }

      &.status-paid {
        background: #f6ffed;
        color: #52c41a;
      }

      &.status-refund {
        background: #f5f5f5;
        color: #999;
      }
    }
  }

  .card-body {
    padding: 24rpx 30rpx;

    .info-row {
      display: flex;
      margin-bottom: 16rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        font-size: 28rpx;
        color: #999;
        width: 120rpx;
      }

      .value {
        font-size: 28rpx;
        color: #333;
        flex: 1;

        &.diagnosis {
          color: #666;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 30rpx;
    background: #fafafa;
    border-top: 1rpx solid #f5f5f5;

    .amount-info {
      display: flex;
      align-items: center;

      .amount-label {
        font-size: 28rpx;
        color: #666;
        margin-right: 12rpx;
      }

      .amount-value {
        font-size: 36rpx;
        color: #ff4d4f;
        font-weight: bold;
      }
    }

    .action-btns {
      display: flex;
      gap: 16rpx;

      button {
        margin: 0;
        padding: 0 32rpx;
        height: 64rpx;
        line-height: 64rpx;
        font-size: 28rpx;
        border-radius: 32rpx;
        border: none;

        &::after {
          border: none;
        }
      }

      .btn-pay {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
      }

      .btn-detail {
        background: #f0f0f0;
        color: #666;
      }
    }
  }
}
</style>
