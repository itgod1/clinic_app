<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        showAdd
        addText="新建收费"
      />
    </el-card>

    <el-card class="table-card">
      <Table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :operates="operates"
        @page-change="handlePageChange"
        @reload="loadData"
      />
    </el-card>

    <el-dialog v-model="payDialogVisible" title="收银" width="600px" destroy-on-close>
      <el-form ref="payFormRef" :model="payForm" :rules="payRules" label-width="100px">
        <el-descriptions :column="2" border v-if="currentPrescription" class="order-info">
          <el-descriptions-item label="处方单号">{{ currentPrescription.prescriptionNo }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ currentPrescription.patientName }}</el-descriptions-item>
          <el-descriptions-item label="医生">{{ currentPrescription.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="诊断">{{ currentPrescription.diagnosis }}</el-descriptions-item>
          <el-descriptions-item label="原价">¥{{ currentPrescription.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="优惠">-¥{{ currentPrescription.discountAmount }}</el-descriptions-item>
          <el-descriptions-item label="应付" :span="2">
            <span class="actual-amount">¥{{ currentPrescription.actualAmount }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-form-item label="支付方式" prop="paymentMethod">
          <el-radio-group v-model="payForm.paymentMethod">
            <el-radio :label="1">现金</el-radio>
            <el-radio :label="2">微信</el-radio>
            <el-radio :label="3">支付宝</el-radio>
            <el-radio :label="4">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实收金额">
          <el-input-number v-model="payForm.receivedAmount" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="找零">
          <span>{{ (payForm.receivedAmount - currentPrescription?.actualAmount).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="payForm.remark" type="textarea" rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePay" :loading="payLoading">确认收款</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="discountDialogVisible" title="优惠" width="500px" destroy-on-close>
      <el-form ref="discountFormRef" :model="discountForm" label-width="100px">
        <el-form-item label="处方单号">
          <span>{{ currentPrescription?.prescriptionNo }}</span>
        </el-form-item>
        <el-form-item label="原价">
          <span>¥{{ currentPrescription?.totalAmount }}</span>
        </el-form-item>
        <el-form-item label="已优惠">
          <span>-¥{{ currentPrescription?.discountAmount }}</span>
        </el-form-item>
        <el-form-item label="应付">
          <span class="actual-amount">¥{{ currentPrescription?.actualAmount }}</span>
        </el-form-item>
        <el-form-item label="本次优惠" prop="discountAmount">
          <el-input-number v-model="discountForm.discountAmount" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="优惠原因" prop="discountReason">
          <el-input v-model="discountForm.discountReason" type="textarea" rows="2" placeholder="请输入优惠原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="discountDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDiscount" :loading="discountLoading">确认优惠</el-button>
      </template>
    </el-dialog>

    <!-- 打印预览 -->
    <PrintPreview
      v-model="printVisible"
      type="receipt"
      title="收费票据打印预览"
      :receipt-data="printData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getUnpaidList, cashierPay, cashierDiscount, type UnpaidPrescription } from '@/api/cashier'
import { getReceiptPrintData, type PrintReceiptData } from '@/api/print'
import { getClinicId, getUserInfo } from '@/utils/storage'
import PrintPreview from '@/components/PrintPreview/index.vue'

const router = useRouter()
const loading = ref(false)
const payLoading = ref(false)
const discountLoading = ref(false)
const dialogVisible = ref(false)
const payDialogVisible = ref(false)
const discountDialogVisible = ref(false)
const printVisible = ref(false)
const formRef = ref<FormInstance>()
const payFormRef = ref<FormInstance>()
const discountFormRef = ref<FormInstance>()
const currentPrescription = ref<UnpaidPrescription | null>(null)
const printData = ref<PrintReceiptData>()
const currentPaymentId = ref<number>(0)

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  startDate: '',
  endDate: ''
})

const tableData = ref<UnpaidPrescription[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '处方单号/患者姓名' },
  { prop: 'startDate', label: '日期', type: 'daterange' }
]

const payForm = reactive({
  patientId: 0,
  orderId: 0,
  paymentMethod: 1,
  operatorId: 0,
  receivedAmount: 0,
  remark: ''
})

const payRules = {
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const discountForm = reactive({
  orderId: 0,
  discountAmount: 0,
  discountReason: '',
  operatorId: 0
})

const columns = [
  { prop: 'prescriptionNo', label: '处方单号', width: 150 },
  { prop: 'createTime', label: '开方时间', width: 160 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'doctorName', label: '医生', width: 100 },
  { prop: 'diagnosis', label: '诊断', width: 150 },
  { prop: 'totalAmount', label: '原价', width: 100, formatter: (row: UnpaidPrescription) => `¥${row.totalAmount}` },
  { prop: 'discountAmount', label: '优惠', width: 100, formatter: (row: UnpaidPrescription) => `-¥${row.discountAmount}` },
  {
    prop: 'actualAmount',
    label: '应付',
    width: 100,
    formatter: (row: UnpaidPrescription) => `¥${row.actualAmount}`
  },
  {
    prop: 'paymentStatus',
    label: '状态',
    width: 100,
    formatter: (row: UnpaidPrescription) => row.statusName
  }
]

const operates = [
  {
    label: '收费',
    type: 'primary',
    condition: (row: UnpaidPrescription) => row.paymentStatus === 0,
    action: (row: UnpaidPrescription) => handleOpenPay(row)
  },
  {
    label: '优惠',
    type: 'warning',
    condition: (row: UnpaidPrescription) => row.paymentStatus === 0,
    action: (row: UnpaidPrescription) => handleOpenDiscount(row)
  },
  {
    label: '医保结算',
    type: 'success',
    condition: (row: UnpaidPrescription) => row.paymentStatus === 0,
    action: (row: UnpaidPrescription) => handleInsuranceSettle(row)
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    const params = {
      clinicId,
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    const res = await getUnpaidList(params)
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  pagination.current = 1
  loadData()
}

const handlePageChange = (page: number, size?: number) => {
  pagination.current = page
  if (size) {
    pagination.size = size
  }
  loadData()
}

const handleOpenPay = (row: UnpaidPrescription) => {
  currentPrescription.value = row
  payForm.patientId = row.patientId || 0
  payForm.orderId = row.prescriptionNo || row.prescriptionId || 0
  payForm.receivedAmount = row.actualAmount || 0
  payForm.paymentMethod = 1
  payForm.remark = ''
  payDialogVisible.value = true
}

const handlePay = async () => {
  try {
    await payFormRef.value?.validate()
    payLoading.value = true
    const userInfo = getUserInfo()
    payForm.operatorId = userInfo?.userId || 0
    const res: any = await cashierPay(payForm)
    ElMessage.success('收费成功')
    payDialogVisible.value = false
    // 如果有支付记录ID，可以打印票据
    if (res.data?.paymentId) {
      currentPaymentId.value = res.data.paymentId
      // 自动打开打印预览
      await openReceiptPrint(res.data.paymentId)
    }
    loadData()
  } catch (error) {
    console.error('收费失败:', error)
  } finally {
    payLoading.value = false
  }
}

const openReceiptPrint = async (paymentId: number) => {
  try {
    const clinicId = getClinicId()
    const res = await getReceiptPrintData(paymentId, clinicId)
    printData.value = res.data
    printVisible.value = true
  } catch (error) {
    console.error('获取打印数据失败:', error)
  }
}

const handleOpenDiscount = (row: UnpaidPrescription) => {
  currentPrescription.value = row
  discountForm.orderId = row.prescriptionId
  discountForm.discountAmount = 0
  discountForm.discountReason = ''
  discountDialogVisible.value = true
}

const handleDiscount = async () => {
  if (!discountForm.discountAmount || discountForm.discountAmount <= 0) {
    ElMessage.warning('请输入正确的优惠金额')
    return
  }
  if (!discountForm.discountReason) {
    ElMessage.warning('请输入优惠原因')
    return
  }
  try {
    discountLoading.value = true
    const userInfo = getUserInfo()
    discountForm.operatorId = userInfo?.userId || 0
    await cashierDiscount(discountForm)
    ElMessage.success('优惠成功')
    discountDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('优惠失败:', error)
  } finally {
    discountLoading.value = false
  }
}

const handleInsuranceSettle = (row: UnpaidPrescription) => {
  router.push({
    path: '/insurance/settlement-create',
    query: {
      orderId: row.prescriptionId,
      patientId: row.patientId
    }
  })
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    background: #fff;
  }

  .actual-amount {
    font-size: 20px;
    font-weight: bold;
    color: #f56c6c;
  }

  .order-info {
    margin-bottom: 20px;
  }
}
</style>