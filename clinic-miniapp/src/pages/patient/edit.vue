<template>
  <view class="patient-edit-page">
    <view class="form-section">
      <view class="form-item">
        <text class="label required">姓名</text>
        <input
          v-model="form.patientName"
          class="input"
          placeholder="请输入真实姓名"
          maxlength="20"
        />
      </view>

      <view class="form-item">
        <text class="label required">性别</text>
        <view class="radio-group">
          <view
            :class="['radio-item', form.gender === 1 ? 'active' : '']"
            @click="form.gender = 1"
          >
            <text class="radio-icon">{{ form.gender === 1 ? '●' : '○' }}</text>
            <text>男</text>
          </view>
          <view
            :class="['radio-item', form.gender === 2 ? 'active' : '']"
            @click="form.gender = 2"
          >
            <text class="radio-icon">{{ form.gender === 2 ? '●' : '○' }}</text>
            <text>女</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label required">年龄</text>
        <input
          v-model="form.age"
          class="input"
          type="number"
          placeholder="请输入年龄"
          maxlength="3"
        />
      </view>

      <view class="form-item">
        <text class="label required">手机号</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <view class="form-item">
        <text class="label">身份证号</text>
        <input
          v-model="form.idCard"
          class="input"
          placeholder="请输入身份证号（选填）"
          maxlength="18"
        />
      </view>

      <view class="form-item">
        <text class="label">出生日期</text>
        <picker mode="date" :value="form.birthDate" @change="onBirthDateChange">
          <view class="picker">
            <text :class="['picker-text', !form.birthDate ? 'placeholder' : '']">
              {{ form.birthDate || '请选择出生日期（选填）' }}
            </text>
            <text class="arrow">></text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">地址</text>
        <input
          v-model="form.address"
          class="input"
          placeholder="请输入地址（选填）"
          maxlength="100"
        />
      </view>

      <view class="form-item switch-item">
        <text class="label">设为默认就诊人</text>
        <switch
          :checked="form.isDefault"
          color="#667eea"
          @change="form.isDefault = $event.detail.value"
        />
      </view>
    </view>

    <view class="tips">
      <text class="tip-text">温馨提示：请填写真实信息，以便医生为您提供更好的服务</text>
    </view>

    <view class="footer">
      <button
        class="submit-btn"
        :disabled="!canSubmit"
        :loading="submitting"
        @click="submit"
      >
        {{ isEdit ? '保存' : '添加' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createPatient, updatePatient, getPatientDetail } from '@/api/patient.js'
import { showSuccess, showError } from '@/utils/index.js'

const form = ref({
  patientName: '',
  gender: 1,
  age: '',
  phone: '',
  idCard: '',
  birthDate: '',
  address: '',
  isDefault: false
})

const patientId = ref(null)
const submitting = ref(false)

const isEdit = computed(() => !!patientId.value)

const canSubmit = computed(() => {
  return form.value.patientName &&
    form.value.gender &&
    form.value.age &&
    form.value.phone &&
    !submitting.value
})

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options.id

  if (id) {
    patientId.value = id
    loadPatientDetail(id)
  }
})

const loadPatientDetail = async (id) => {
  try {
    const res = await getPatientDetail(id)
    const data = res.data
    form.value = {
      patientName: data.patientName || data.name,
      gender: data.gender || 1,
      age: data.age ? String(data.age) : '',
      phone: data.phone,
      idCard: data.idCard || '',
      birthDate: data.birthDate || '',
      address: data.address || '',
      isDefault: data.isDefault || false
    }
  } catch (error) {
    console.error('加载就诊人详情失败:', error)
    showError('加载失败')
  }
}

const onBirthDateChange = (e) => {
  form.value.birthDate = e.detail.value
}

const submit = async () => {
  if (!canSubmit.value) return

  // 验证手机号
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    showError('请输入正确的手机号')
    return
  }

  submitting.value = true
  uni.showLoading({ title: '保存中...' })

  try {
    const data = {
      ...form.value,
      age: parseInt(form.value.age)
    }

    if (isEdit.value) {
      await updatePatient({ ...data, id: patientId.value })
    } else {
      await createPatient(data)
    }

    showSuccess(isEdit.value ? '保存成功' : '添加成功')

    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error) {
    showError(error.message || '保存失败')
  } finally {
    submitting.value = false
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.patient-edit-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 140rpx;
}

.form-section {
  background: #fff;
  padding: 0 30rpx;

  .form-item {
    display: flex;
    align-items: center;
    padding: 30rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .label {
      width: 180rpx;
      font-size: 30rpx;
      color: #333;

      &.required::before {
        content: '*';
        color: #ff4d4f;
        margin-right: 8rpx;
      }
    }

    .input {
      flex: 1;
      font-size: 30rpx;
      color: #333;
      text-align: right;
    }

    .radio-group {
      flex: 1;
      display: flex;
      justify-content: flex-end;
      gap: 40rpx;

      .radio-item {
        display: flex;
        align-items: center;
        font-size: 30rpx;
        color: #666;

        .radio-icon {
          font-size: 32rpx;
          margin-right: 12rpx;
          color: #ccc;
        }

        &.active {
          color: #667eea;

          .radio-icon {
            color: #667eea;
          }
        }
      }
    }

    .picker {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: flex-end;

      .picker-text {
        font-size: 30rpx;
        color: #333;

        &.placeholder {
          color: #999;
        }
      }

      .arrow {
        font-size: 28rpx;
        color: #ccc;
        margin-left: 12rpx;
      }
    }

    &.switch-item {
      justify-content: space-between;
    }
  }
}

.tips {
  padding: 30rpx;

  .tip-text {
    font-size: 26rpx;
    color: #999;
    line-height: 1.6;
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

  .submit-btn {
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

    &[disabled] {
      opacity: 0.5;
    }
  }
}
</style>
