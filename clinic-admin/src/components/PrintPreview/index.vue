<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="800px"
    destroy-on-close
    class="print-preview-dialog"
  >
    <div class="print-container" ref="printRef">
      <!-- 处方打印模板 -->
      <template v-if="type === 'prescription' && prescriptionData">
        <div class="print-header">
          <h2 class="clinic-name">{{ prescriptionData.clinic?.name }}</h2>
          <p class="clinic-info">{{ prescriptionData.clinic?.address }} {{ prescriptionData.clinic?.phone }}</p>
          <h3 class="document-title">处方笺</h3>
        </div>
        
        <div class="print-info">
          <div class="info-row">
            <span><strong>处方编号：</strong>{{ prescriptionData.prescription?.prescriptionNo }}</span>
            <span><strong>开方日期：</strong>{{ formatDate(prescriptionData.prescription?.createTime) }}</span>
          </div>
          <div class="info-row">
            <span><strong>患者姓名：</strong>{{ prescriptionData.patient?.name }}</span>
            <span><strong>性别：</strong>{{ prescriptionData.patient?.gender }}</span>
            <span><strong>年龄：</strong>{{ prescriptionData.patient?.age }}岁</span>
            <span><strong>电话：</strong>{{ prescriptionData.patient?.phone }}</span>
          </div>
          <div class="info-row">
            <span><strong>科室：</strong>{{ prescriptionData.prescription?.department }}</span>
            <span><strong>医生：</strong>{{ prescriptionData.doctor?.name }}</span>
          </div>
          <div class="info-row">
            <span><strong>诊断：</strong>{{ prescriptionData.prescription?.diagnosis }}</span>
          </div>
        </div>

        <div class="print-content">
          <table class="print-table">
            <thead>
              <tr>
                <th width="50">序号</th>
                <th width="150">名称</th>
                <th width="100">规格</th>
                <th width="60">数量</th>
                <th width="80">单价</th>
                <th width="80">小计</th>
                <th width="150">用法</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in prescriptionData.items" :key="index">
                <td>{{ index + 1 }}</td>
                <td>{{ item.name }}</td>
                <td>{{ item.spec }}</td>
                <td>{{ item.quantity }}{{ item.unit }}</td>
                <td>¥{{ item.unitPrice }}</td>
                <td>¥{{ item.subtotal }}</td>
                <td>{{ item.usage }} {{ item.frequency }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="print-amount">
          <div class="amount-row">
            <span>原价：<strong>¥{{ prescriptionData.amount?.totalAmount }}</strong></span>
            <span>优惠：<strong>-¥{{ prescriptionData.amount?.discountAmount }}</strong></span>
            <span class="actual">实付：<strong>¥{{ prescriptionData.amount?.actualAmount }}</strong></span>
          </div>
        </div>

        <div class="print-footer">
          <div class="signature">
            <span>医生签名：_______________</span>
            <span>发药人签名：_______________</span>
          </div>
          <p class="tips">温馨提示：请遵医嘱用药，如有不适请及时就诊</p>
        </div>
      </template>

      <!-- 收费票据打印模板 -->
      <template v-if="type === 'receipt' && receiptData">
        <div class="print-header">
          <h2 class="clinic-name">{{ receiptData.clinic?.name }}</h2>
          <p class="clinic-info">{{ receiptData.clinic?.address }} {{ receiptData.clinic?.phone }}</p>
          <h3 class="document-title">收费票据</h3>
        </div>
        
        <div class="print-info">
          <div class="info-row">
            <span><strong>票据编号：</strong>{{ receiptData.receipt?.receiptNo }}</span>
            <span><strong>打印时间：</strong>{{ formatDate(receiptData.receipt?.printTime) }}</span>
          </div>
          <div class="info-row">
            <span><strong>患者姓名：</strong>{{ receiptData.patient?.name }}</span>
            <span><strong>订单号：</strong>{{ receiptData.receipt?.orderNo }}</span>
          </div>
        </div>

        <div class="print-content">
          <table class="print-table">
            <thead>
              <tr>
                <th width="50">序号</th>
                <th width="200">项目名称</th>
                <th width="80">规格</th>
                <th width="60">数量</th>
                <th width="80">单价</th>
                <th width="80">金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in receiptData.items" :key="index">
                <td>{{ index + 1 }}</td>
                <td>{{ item.itemName }}</td>
                <td>{{ item.spec }}</td>
                <td>{{ item.quantity }}</td>
                <td>¥{{ item.unitPrice }}</td>
                <td>¥{{ item.subtotal }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="print-amount">
          <div class="amount-row">
            <span>合计金额：<strong>¥{{ receiptData.payment?.totalAmount }}</strong></span>
            <span>优惠金额：<strong>-¥{{ receiptData.payment?.discountAmount }}</strong></span>
          </div>
          <div class="amount-row">
            <span>实付金额：<strong class="actual">¥{{ receiptData.payment?.actualAmount }}</strong></span>
            <span>支付方式：<strong>{{ receiptData.payment?.paymentMethod }}</strong></span>
          </div>
        </div>

        <div class="print-footer">
          <div class="signature">
            <span>收费员：{{ receiptData.operator?.name }}</span>
            <span>患者签字：_______________</span>
          </div>
          <p class="tips">此票据为收费凭证，请妥善保管</p>
        </div>
      </template>

      <!-- 病历打印模板 -->
      <template v-if="type === 'medicalRecord' && medicalRecordData">
        <div class="print-header">
          <h2 class="clinic-name">{{ medicalRecordData.clinic?.name }}</h2>
          <p class="clinic-info">{{ medicalRecordData.clinic?.address }} {{ medicalRecordData.clinic?.phone }}</p>
          <h3 class="document-title">门诊病历</h3>
        </div>
        
        <div class="print-info">
          <div class="info-row">
            <span><strong>病历号：</strong>{{ medicalRecordData.record?.recordNo }}</span>
            <span><strong>就诊日期：</strong>{{ formatDate(medicalRecordData.record?.visitTime) }}</span>
          </div>
          <div class="info-row">
            <span><strong>患者姓名：</strong>{{ medicalRecordData.patient?.name }}</span>
            <span><strong>性别：</strong>{{ medicalRecordData.patient?.gender }}</span>
            <span><strong>年龄：</strong>{{ medicalRecordData.patient?.age }}岁</span>
          </div>
          <div class="info-row">
            <span><strong>联系电话：</strong>{{ medicalRecordData.patient?.phone }}</span>
            <span><strong>就诊类型：</strong>{{ medicalRecordData.record?.visitType }}</span>
          </div>
          <div class="info-row">
            <span><strong>科室：</strong>{{ medicalRecordData.record?.department }}</span>
            <span><strong>医生：</strong>{{ medicalRecordData.doctor?.name }}</span>
          </div>
        </div>

        <div class="medical-content">
          <div class="content-section">
            <label>主诉：</label>
            <div class="content-text">{{ medicalRecordData.record?.chiefComplaint || '无' }}</div>
          </div>
          <div class="content-section">
            <label>诊断：</label>
            <div class="content-text">{{ medicalRecordData.record?.diagnosis || '无' }}</div>
          </div>
          <div class="content-section">
            <label>治疗方案：</label>
            <div class="content-text">{{ medicalRecordData.record?.treatment || '无' }}</div>
          </div>
          <div class="content-section">
            <label>医嘱：</label>
            <div class="content-text">{{ medicalRecordData.record?.medicalAdvice || '无' }}</div>
          </div>
        </div>

        <div class="print-footer">
          <div class="signature">
            <span>医生签名：_______________</span>
            <span>日期：_______________</span>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="handlePrint">
        <el-icon><Printer /></el-icon>
        打印
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Printer } from '@element-plus/icons-vue'
import type { PrintPrescriptionData, PrintReceiptData, PrintMedicalRecordData } from '@/types/print'

const props = defineProps<{
  modelValue: boolean
  type: 'prescription' | 'receipt' | 'medicalRecord'
  title: string
  prescriptionData?: PrintPrescriptionData
  receiptData?: PrintReceiptData
  medicalRecordData?: PrintMedicalRecordData
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const printRef = ref<HTMLDivElement>()

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const handlePrint = () => {
  const printContent = printRef.value?.innerHTML
  if (!printContent) return

  const printWindow = window.open('', '_blank')
  if (!printWindow) return

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>打印</title>
      <style>
        @media print {
          body { margin: 0; padding: 20px; font-family: 'SimSun', serif; }
          .print-container { width: 210mm; margin: 0 auto; }
          .clinic-name { text-align: center; font-size: 22px; margin-bottom: 5px; }
          .clinic-info { text-align: center; font-size: 12px; color: #666; margin-bottom: 10px; }
          .document-title { text-align: center; font-size: 18px; margin: 15px 0; border-bottom: 2px solid #333; padding-bottom: 10px; }
          .print-info { margin: 15px 0; font-size: 13px; }
          .info-row { display: flex; flex-wrap: wrap; gap: 30px; margin-bottom: 8px; }
          .print-table { width: 100%; border-collapse: collapse; margin: 15px 0; font-size: 13px; }
          .print-table th, .print-table td { border: 1px solid #333; padding: 8px; text-align: left; }
          .print-table th { background: #f5f5f5; }
          .print-amount { margin: 15px 0; text-align: right; font-size: 13px; }
          .amount-row { margin-bottom: 8px; }
          .amount-row span { margin-left: 20px; }
          .actual { color: #f56c6c; font-size: 16px; }
          .print-footer { margin-top: 30px; font-size: 13px; }
          .signature { display: flex; justify-content: space-between; margin: 20px 0; }
          .tips { text-align: center; color: #999; font-size: 12px; margin-top: 20px; }
          .medical-content { margin: 15px 0; }
          .content-section { margin-bottom: 15px; }
          .content-section label { font-weight: bold; display: block; margin-bottom: 5px; }
          .content-text { padding: 10px; border: 1px solid #ddd; min-height: 40px; line-height: 1.6; }
        }
      </style>
    </head>
    <body>
      <div class="print-container">
        ${printContent}
      </div>
    </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 100)
}
</script>

<style lang="scss" scoped>
.print-preview-dialog {
  :deep(.el-dialog__body) {
    max-height: 600px;
    overflow-y: auto;
  }
}

.print-container {
  background: #fff;
  padding: 30px;
  border: 1px solid #dcdfe6;
  min-height: 500px;
}

.print-header {
  text-align: center;
  margin-bottom: 20px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 15px;

  .clinic-name {
    font-size: 24px;
    font-weight: bold;
    margin-bottom: 5px;
    color: #303133;
  }

  .clinic-info {
    font-size: 13px;
    color: #606266;
    margin-bottom: 10px;
  }

  .document-title {
    font-size: 20px;
    font-weight: bold;
    color: #409eff;
  }
}

.print-info {
  margin: 20px 0;
  font-size: 14px;

  .info-row {
    display: flex;
    flex-wrap: wrap;
    gap: 30px;
    margin-bottom: 10px;

    span {
      color: #606266;
    }
  }
}

.print-table {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
  font-size: 13px;

  th,
  td {
    border: 1px solid #dcdfe6;
    padding: 10px;
    text-align: left;
  }

  th {
    background: #f5f7fa;
    font-weight: 600;
    color: #303133;
  }
}

.print-amount {
  margin: 20px 0;
  text-align: right;

  .amount-row {
    margin-bottom: 10px;

    span {
      margin-left: 30px;
      font-size: 14px;
    }

    .actual {
      color: #f56c6c;
      font-size: 18px;
      font-weight: bold;
    }
  }
}

.print-footer {
  margin-top: 40px;
  font-size: 14px;

  .signature {
    display: flex;
    justify-content: space-between;
    margin: 30px 0;
  }

  .tips {
    text-align: center;
    color: #909399;
    font-size: 12px;
    margin-top: 20px;
  }
}

.medical-content {
  margin: 20px 0;

  .content-section {
    margin-bottom: 20px;

    label {
      font-weight: 600;
      display: block;
      margin-bottom: 8px;
      color: #303133;
    }

    .content-text {
      padding: 12px;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      min-height: 50px;
      line-height: 1.6;
      color: #606266;
      background: #fafafa;
    }
  }
}
</style>
