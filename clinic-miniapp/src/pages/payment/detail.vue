<template>
  <view class="payment-detail-page">
    <!-- 加载中 -->
    <view v-if="loading" class="loading-container">
      <text>加载中...</text>
    </view>

    <template v-else-if="prescription.id">
      <!-- 状态栏 -->
      <view :class="['status-bar', getStatusClass(prescription.paymentStatus)]">
        <text class="status-icon">{{ getStatusIcon(prescription.paymentStatus) }}</text>
        <text class="status-text">{{ getStatusText(prescription.paymentStatus) }}</text>
      </view>

      <!-- 处方信息 -->
      <view class="info-section">
        <view class="section-title">处方信息</view>
        <view class="info-list">
          <view class="info-item">
            <text class="label">处方号</text>
            <text class="value">{{ prescription.prescriptionNo }}</text>
          </view>
          <view class="info-item">
            <text class="label">就诊人</text>
            <text class="value">{{ prescription.patientName }}</text>
          </view>
          <view class="info-item">
            <text class="label">医生</text>
            <text class="value">{{ prescription.doctorName }}</text>
          </view>
          <view class="info-item">
            <text class="label">科室</text>
            <text class="value">{{ prescription.departmentName }}</text>
          </view>
          <view class="info-item">
            <text class="label">诊断</text>
            <text class="value">{{ prescription.diagnosis || '-' }}</text>
          </view>
          <view class="info-item">
            <text class="label">开方时间</text>
            <text class="value">{{ formatTime(prescription.createdAt) }}</text>
          </view>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="info-section">
        <view class="section-title">费用明细</view>
        <view class="info-list">
          <view class="info-item">
            <text class="label">应收金额</text>
            <text class="value">¥{{ prescription.totalAmount || '0.00' }}</text>
          </view>
          <view class="info-item" v-if="prescription.discountAmount && prescription.discountAmount > 0">
            <text class="label">优惠金额</text>
            <text class="value discount">-¥{{ prescription.discountAmount }}</text>
          </view>
          <view class="info-item total">
            <text class="label">实收金额</text>
            <text class="value amount">¥{{ prescription.actualAmount || prescription.totalAmount || '0.00' }}</text>
          </view>
        </view>
      </view>

      <!-- 支付信息 -->
      <view class="info-section" v-if="prescription.paymentStatus === 1">
        <view class="section-title">支付信息</view>
        <view class="info-list">
          <view class="info-item">
            <text class="label">支付方式</text>
            <text class="value">{{ getPayMethodText(prescription.paymentMethod) }}</text>
          </view>
          <view class="info-item" v-if="prescription.payTime">
            <text class="label">支付时间</text>
            <text class="value">{{ formatTime(prescription.payTime) }}</text>
          </view>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="footer-actions" v-if="prescription.paymentStatus === 0">
        <view class="total-amount">
          <text class="label">应付:</text>
          <text class="value">¥{{ prescription.totalAmount || '0.00' }}</text>
        </view>
        <button class="btn-submit" @click="handlePay">立即支付</button>
      </view>
    </template>

    <!-- 空状态 -->
    <empty-data
      v-else
      text="处方不存在"
      subtext="请检查处方信息"
    />

    <!-- 支付收银台 -->
    <payment-cashier
      :visible="showCashier"
      :amount="prescription.actualAmount || prescription.totalAmount || '0.00'"
      :default-method="2"
      @close="closeCashier"
      @confirm="confirmPay"
    />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import EmptyData from '@/components/empty-data.vue'
import PaymentCashier from '@/components/payment-cashier.vue'
import { getPaymentDetail, payPrescription } from '@/api/payment.js'

const loading = ref(true)
const prescription = ref({})
const prescriptionId = ref(null)
const showCashier = ref(false)
const selectedPayMethod = ref(null)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const { id } = currentPage.$page?.options || currentPage.options || {}

  if (id) {
    prescriptionId.value = id
    loadDetail()
  } else {
    loading.value = false
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getPaymentDetail(prescriptionId.value)
    prescription.value = res.data || {}
  } catch (error) {
    console.error('加载处方详情失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
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

const getStatusIcon = (status) => {
  const map = {
    0: '⏰',
    1: '✅',
    2: '↩️'
  }
  return map[status] || '❓'
}

const getPayMethodText = (method) => {
  const map = {
    1: '现金',
    2: '微信支付',
    3: '支付宝',
    4: '银行卡',
    5: '会员卡'
  }
  return map[method] || '未知'
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 显示收银台
const handlePay = () => {
  console.log('点击立即支付')
  console.log('处方金额:', prescription.value.totalAmount)
  if (!prescription.value.totalAmount || prescription.value.totalAmount <= 0) {
    uni.showToast({ title: '支付金额错误', icon: 'none' })
    return
  }
  console.log('显示收银台')
  showCashier.value = true
}

// 关闭收银台
const closeCashier = () => {
  showCashier.value = false
  selectedPayMethod.value = null
}

// 确认支付
const confirmPay = async (paymentMethod) => {
  selectedPayMethod.value = paymentMethod
  showCashier.value = false

  // 现金支付提示到院支付
  if (paymentMethod === 1) {
    uni.showModal({
      title: '现金支付',
      content: '您选择了现金支付，请到医院前台缴费',
      confirmText: '我知道了',
      showCancel: false
    })
    return
  }

  uni.showLoading({ title: '支付中...' })

  try {
    const clinicId = uni.getStorageSync('clinicId')

    // 调用支付接口
    await payPrescription({
      prescriptionId: prescription.value.id,
      clinicId: clinicId,
      paymentMethod: paymentMethod
    })

    uni.hideLoading()
    uni.showToast({ title: '支付成功', icon: 'success' })

    // 刷新页面数据
    setTimeout(() => {
      loadDetail()
    }, 1500)
  } catch (error) {
    uni.hideLoading()
    console.error('支付失败:', error)
    uni.showToast({ title: error.message || '支付失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.payment-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 120rpx;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  color: #999;
  font-size: 28rpx;
}

.status-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 30rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  &.status-unpaid {
    background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  }

  &.status-paid {
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  }

  &.status-refund {
    background: linear-gradient(135deg, #999 0%, #ccc 100%);
  }

  .status-icon {
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .status-text {
    font-size: 36rpx;
    color: #fff;
    font-weight: bold;
  }
}

.info-section {
  margin: 20rpx 30rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;

  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    padding: 30rpx;
    border-bottom: 1rpx solid #f5f5f5;
  }

  .info-list {
    padding: 0 30rpx;

    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx 0;
      border-bottom: 1rpx solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      &.total {
        padding-top: 30rpx;
        margin-top: 10rpx;
        border-top: 2rpx solid #f0f0f0;

        .label {
          font-size: 32rpx;
          font-weight: bold;
        }

        .value {
          font-size: 40rpx;
          color: #ff4d4f;
          font-weight: bold;
        }
      }

      .label {
        font-size: 28rpx;
        color: #999;
      }

      .value {
        font-size: 28rpx;
        color: #333;
        text-align: right;
        flex: 1;
        margin-left: 20rpx;

        &.discount {
          color: #52c41a;
        }

        &.amount {
          color: #ff4d4f;
          font-weight: bold;
        }
      }
    }
  }
}

.footer-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

  .total-amount {
    display: flex;
    align-items: baseline;

    .label {
      font-size: 28rpx;
      color: #666;
      margin-right: 12rpx;
    }

    .value {
      font-size: 44rpx;
      color: #ff4d4f;
      font-weight: bold;
    }
  }

  .btn-submit {
    margin: 0;
    padding: 0 60rpx;
    height: 80rpx;
    line-height: 80rpx;
    font-size: 32rpx;
    border-radius: 40rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    border: none;

    &::after {
      border: none;
    }

    &:active {
      opacity: 0.8;
    }
  }
}
</style>
