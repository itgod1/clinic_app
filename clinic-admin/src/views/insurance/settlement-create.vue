<template>
  <div class="page-container settlement-create-page">
    <el-card class="step-card">
      <template #header>
        <div class="card-header">
          <span class="step-title">医保结算</span>
          <el-tag type="warning" v-if="!previewData">请填写信息后计算费用</el-tag>
          <el-tag type="success" v-else>费用预览就绪</el-tag>
        </div>
      </template>

      <el-form ref="formRef" :model="form" label-width="120px" class="settlement-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="处方/订单ID" prop="orderId">
              <el-input-number v-model="form.orderId" :min="1" style="width: 100%" placeholder="输入订单ID" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="患者ID" prop="patientId">
              <el-input-number v-model="form.patientId" :min="1" style="width: 100%" placeholder="输入患者ID" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="医保患者ID">
              <el-input-number v-model="form.insurancePatientId" :min="1" style="width: 100%" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="items-section">
          <div class="section-header">
            <span>收费项目</span>
            <el-button type="primary" link @click="addItem">+ 添加项目</el-button>
          </div>
          <el-table :data="form.items" border size="small" class="items-table">
            <el-table-column label="编码" width="140">
              <template #default="{ row }">
                <el-input v-model="row.itemCode" placeholder="医保编码" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="项目名称" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.itemName" placeholder="项目名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="80">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :precision="1" size="small" style="width: 70px" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" style="width: 90px" />
              </template>
            </el-table-column>
            <el-table-column label="总价" width="100">
              <template #default="{ row }">
                {{ ((row.quantity || 0) * (row.unitPrice || 0)).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-actions">
          <el-button type="primary" @click="handleCalculate" :loading="calcLoading">
            计算费用
          </el-button>
        </div>
      </el-form>
    </el-card>

    <el-card v-if="previewData" class="preview-card">
      <template #header>
        <div class="card-header">
          <span class="step-title">费用预览</span>
          <el-tag v-if="previewData.eligible" type="success">参保人: {{ previewData.patientName }}</el-tag>
        </div>
      </template>

      <div class="preview-summary">
        <div class="summary-item">
          <span class="label">医保类型</span>
          <span class="value">{{ previewData.insuranceTypeName }}</span>
        </div>
        <div class="summary-item">
          <span class="label">医保卡号</span>
          <span class="value">{{ previewData.insuranceCardNo }}</span>
        </div>
        <div class="summary-item highlight">
          <span class="label">总费用</span>
          <span class="value">¥{{ previewData.totalAmount }}</span>
        </div>
        <div class="summary-item insurance-pay">
          <span class="label">医保支付</span>
          <span class="value">¥{{ previewData.totalInsurancePay }}</span>
        </div>
        <div class="summary-item self-pay">
          <span class="label">个人自付</span>
          <span class="value">¥{{ previewData.totalSelfPay }}</span>
        </div>
      </div>

      <h4 class="section-title">费用明细</h4>
      <el-table :data="previewData.items" border size="small" class="preview-table">
        <el-table-column prop="itemCode" label="编码" width="120" />
        <el-table-column prop="itemName" label="项目名称" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="totalPrice" label="总价" width="90" />
        <el-table-column label="自付比例" width="90">
          <template #default="{ row }">
            {{ (row.selfPayRatio * 100).toFixed(0) }}%
          </template>
        </el-table-column>
        <el-table-column label="医保支付" width="100">
          <template #default="{ row }">
            <span class="insurance-amount">¥{{ row.insurancePay }}</span>
          </template>
        </el-table-column>
        <el-table-column label="个人自付" width="100">
          <template #default="{ row }">
            <span class="self-amount">¥{{ row.selfPay }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目录匹配" width="90">
          <template #default="{ row }">
            <el-tag :type="row.catalogMatched ? 'success' : 'info'" size="small">
              {{ row.catalogMatched ? '已匹配' : '未匹配' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="preview-actions">
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitLoading">
          提交结算
        </el-button>
        <el-button size="large" @click="previewData = null">重新计算</el-button>
      </div>
    </el-card>

    <el-card v-if="submitResult" class="result-card">
      <template #header>
        <span class="step-title">结算结果</span>
      </template>
      <el-result
        :icon="submitResult.settlementStatus === 'SUCCESS' ? 'success' : 'error'"
        :title="submitResult.settlementStatus === 'SUCCESS' ? '结算成功' : '结算失败'"
        :sub-title="submitResult.settlementStatus === 'SUCCESS' ? `流水号: ${submitResult.insuranceClaimNo}` : submitResult.errorMessage || '请重试'"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/insurance/settlement')">返回结算列表</el-button>
          <el-button @click="resetAll">新建结算</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  createSettlementPreview, submitSettlement,
  type InsuranceSettlementRequest, type SettlementPreview, type InsuranceSettlement
} from '@/api/insurance'

const formRef = ref<FormInstance>()
const calcLoading = ref(false)
const submitLoading = ref(false)
const previewData = ref<SettlementPreview | null>(null)
const submitResult = ref<InsuranceSettlement | null>(null)
const settlementId = ref<number>(0)

const form = reactive({
  orderId: undefined as number | undefined,
  patientId: undefined as number | undefined,
  insurancePatientId: undefined as number | undefined,
  items: [] as { catalogItemId?: number; itemCode: string; itemName: string; specification?: string; unit?: string; quantity: number; unitPrice: number; totalPrice: number }[]
})

const addItem = () => {
  form.items.push({
    itemCode: '',
    itemName: '',
    quantity: 1,
    unitPrice: 0,
    totalPrice: 0
  })
}

const removeItem = (index: number) => {
  form.items.splice(index, 1)
}

const handleCalculate = async () => {
  if (!form.orderId || !form.patientId) {
    ElMessage.warning('请填写订单ID和患者ID')
    return
  }
  if (form.items.length === 0) {
    ElMessage.warning('请至少添加一个收费项目')
    return
  }

  calcLoading.value = true
  submitResult.value = null
  try {
    const requestData: InsuranceSettlementRequest = {
      orderId: form.orderId,
      patientId: form.patientId,
      insurancePatientId: form.insurancePatientId,
      items: form.items.map(item => ({
        ...item,
        totalPrice: item.quantity * item.unitPrice
      }))
    }
    const res = await createSettlementPreview(requestData)
    previewData.value = res.data
  } catch (error) {
    ElMessage.error('费用计算失败')
  } finally {
    calcLoading.value = false
  }
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    const res = await submitSettlement(settlementId.value || 1)
    submitResult.value = res.data
  } catch (error) {
    ElMessage.error('结算提交失败')
  } finally {
    submitLoading.value = false
  }
}

const resetAll = () => {
  previewData.value = null
  submitResult.value = null
  settlementId.value = 0
  form.items = []
  form.orderId = undefined
  form.patientId = undefined
  form.insurancePatientId = undefined
}
</script>

<style lang="scss" scoped>
.settlement-create-page {
  max-width: 960px;

  .step-card, .preview-card, .result-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .step-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .settlement-form {
    .items-section {
      margin: 16px 0;
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #606266;
      }
    }

    .form-actions {
      margin-top: 16px;
      text-align: center;
    }
  }

  .preview-summary {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
    margin-bottom: 20px;

    .summary-item {
      background: #f5f7fa;
      border-radius: 8px;
      padding: 16px;
      text-align: center;

      .label {
        display: block;
        font-size: 13px;
        color: #909399;
        margin-bottom: 6px;
      }

      .value {
        display: block;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }

      &.highlight {
        background: #ecf5ff;
        .value { color: #409eff; }
      }

      &.insurance-pay {
        background: #f0f9eb;
        .value { color: #67c23a; }
      }

      &.self-pay {
        background: #fef0f0;
        .value { color: #f56c6c; }
      }
    }
  }

  .section-title {
    margin: 16px 0 8px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .preview-actions {
    margin-top: 20px;
    text-align: center;
    display: flex;
    gap: 12px;
    justify-content: center;
  }

  .insurance-amount { color: #67c23a; font-weight: 500; }
  .self-amount { color: #f56c6c; font-weight: 500; }
}
</style>
