<template>
  <div class="page-container insurance-page">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        showAdd
        addText="新增结算"
        @add="handleCreate"
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

    <el-dialog
      v-model="detailVisible"
      title="结算详情"
      width="800px"
      destroy-on-close
    >
      <div v-if="currentDetail" class="settlement-detail">
        <el-descriptions :column="3" border class="mb-16">
          <el-descriptions-item label="结算单号">{{ currentDetail.settlement.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ currentDetail.settlement.patientName }}</el-descriptions-item>
          <el-descriptions-item label="结算状态">
            <el-tag :type="statusTagType(currentDetail.settlement.settlementStatus)">
              {{ currentDetail.settlement.settlementStatusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总费用">¥{{ currentDetail.settlement.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="医保支付">¥{{ currentDetail.settlement.insurancePay }}</el-descriptions-item>
          <el-descriptions-item label="个人自付">¥{{ currentDetail.settlement.selfPay }}</el-descriptions-item>
          <el-descriptions-item label="结算流水号" :span="2">{{ currentDetail.settlement.insuranceClaimNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentDetail.settlement.createdAt }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">结算明细</h4>
        <el-table :data="currentDetail.details" border size="small">
          <el-table-column prop="itemCode" label="编码" width="120" />
          <el-table-column prop="itemName" label="项目名称" min-width="150" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column prop="unitPrice" label="单价" width="80" />
          <el-table-column prop="totalPrice" label="总价" width="90" />
          <el-table-column prop="insurancePay" label="医保支付" width="90" />
          <el-table-column prop="selfPay" label="自付" width="80" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import {
  getInsuranceSettlementList, getInsuranceSettlementDetail,
  cancelSettlement, type InsuranceSettlement, type SettlementDetail
} from '@/api/insurance'
import { getClinicId } from '@/utils/storage'

const router = useRouter()
const loading = ref(false)
const detailVisible = ref(false)
const currentDetail = ref<SettlementDetail | null>(null)

const queryParams = reactive({
  clinicId: 0,
  settlementStatus: '',
  keyword: '',
  startDate: '',
  endDate: ''
})

const tableData = ref<InsuranceSettlement[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '结算单号/患者姓名' },
  {
    prop: 'settlementStatus',
    label: '结算状态',
    type: 'select',
    options: [
      { label: '待结算', value: 'PENDING' },
      { label: '已提交', value: 'SUBMITTED' },
      { label: '结算成功', value: 'SUCCESS' },
      { label: '结算失败', value: 'FAILED' },
      { label: '已取消', value: 'CANCELLED' }
    ]
  },
  { prop: 'startDate', label: '日期范围', type: 'daterange' }
]

const columns = [
  { prop: 'settlementNo', label: '结算单号', width: 160 },
  { prop: 'patientName', label: '患者', width: 90 },
  { prop: 'totalAmount', label: '总费用', width: 90 },
  { prop: 'insurancePay', label: '医保支付', width: 90 },
  { prop: 'selfPay', label: '个人自付', width: 90 },
  {
    prop: 'settlementStatusName',
    label: '状态',
    width: 100,
    formatter: (row: InsuranceSettlement) => {
      const typeMap: Record<string, string> = {
        PENDING: 'warning', SUBMITTED: '', SUCCESS: 'success', FAILED: 'danger', CANCELLED: 'info'
      }
      return `<el-tag type="${typeMap[row.settlementStatus] || 'info'}" size="small">${row.settlementStatusName}</el-tag>`
    }
  },
  { prop: 'insuranceClaimNo', label: '流水号', width: 150 },
  { prop: 'createdAt', label: '创建时间', width: 160 }
]

const operates = [
  { label: '详情', type: 'primary', action: (row: InsuranceSettlement) => handleViewDetail(row) },
  {
    label: '取消',
    type: 'danger',
    condition: (row: InsuranceSettlement) => row.settlementStatus === 'PENDING' || row.settlementStatus === 'SUBMITTED',
    action: (row: InsuranceSettlement) => handleCancel(row)
  }
]

const statusTagType = (status: string) => {
  const map: Record<string, string> = { PENDING: 'warning', SUBMITTED: '', SUCCESS: 'success', FAILED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      clinicId: getClinicId(),
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    const res = await getInsuranceSettlementList(params)
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => { pagination.current = 1; loadData() }

const handlePageChange = (page: number, size?: number) => {
  pagination.current = page
  if (size) pagination.size = size
  loadData()
}

const handleCreate = () => {
  router.push('/insurance/settlement-create')
}

const handleViewDetail = async (row: InsuranceSettlement) => {
  try {
    const res = await getInsuranceSettlementDetail(row.id)
    currentDetail.value = res.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleCancel = async (row: InsuranceSettlement) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消结算', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '取消原因'
    })
    if (reason) {
      await cancelSettlement(row.id, reason)
      ElMessage.success('已取消')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadData()
})
</script>

<style lang="scss" scoped>
.insurance-page {
  .search-card { margin-bottom: 16px; }
  .table-card { background: #fff; }
}

.settlement-detail {
  .mb-16 { margin-bottom: 16px; }
  .section-title { margin: 16px 0 8px; font-size: 14px; font-weight: 600; color: #303133; }
}
</style>
