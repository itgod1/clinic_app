<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @reload="loadData"
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

    <el-dialog v-model="detailVisible" title="处方详情" width="900px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentPrescription" class="prescription-header">
        <el-descriptions-item label="处方单号">{{ currentPrescription.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="处方类型">
          <el-tag :type="currentPrescription.prescriptionType === 1 ? 'success' : 'warning'">
            {{ currentPrescription.prescriptionTypeName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="科室">{{ currentPrescription.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ currentPrescription.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ currentPrescription.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentPrescription.createdAt?.replace('T', ' ') }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentPrescription.paymentStatus)">
            {{ currentPrescription.statusName }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">处方项目</el-divider>

      <el-table :data="currentPrescription?.items" border stripe>
        <el-table-column prop="itemTypeName" label="类型" width="100" />
        <el-table-column prop="itemName" label="名称" min-width="150" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">
            ¥{{ row.unitPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="subtotal" label="小计" width="100">
          <template #default="{ row }">
            ¥{{ row.subtotal }}
          </template>
        </el-table-column>
        <el-table-column prop="usage" label="用法" width="150" />
      </el-table>

      <div class="amount-info">
        <el-row :gutter="20">
          <el-col :span="6">
            <span class="label">原价：</span>
            <span class="value">¥{{ currentPrescription?.totalAmount }}</span>
          </el-col>
          <el-col :span="6">
            <span class="label">优惠：</span>
            <span class="value discount">-¥{{ currentPrescription?.discountAmount }}</span>
          </el-col>
          <el-col :span="6">
            <span class="label">应付：</span>
            <span class="value actual">¥{{ currentPrescription?.actualAmount }}</span>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handlePrintCurrent">
          <el-icon><Printer /></el-icon>
          打印处方
        </el-button>
        <el-button
          v-if="currentPrescription?.paymentStatus === 0"
          type="primary"
          @click="handleDispense"
        >
          确认发药
        </el-button>
      </template>
    </el-dialog>

    <!-- 打印预览 -->
    <PrintPreview
      v-model="printVisible"
      type="prescription"
      title="处方打印预览"
      :prescription-data="printData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getPrescriptionList, getPrescriptionDetail, dispensePrescription, type Prescription } from '@/api/prescription'
import { getPrescriptionPrintData, type PrintPrescriptionData } from '@/api/print'
import { getClinicId, getUserInfo } from '@/utils/storage'
import PrintPreview from '@/components/PrintPreview/index.vue'

const loading = ref(false)
const detailVisible = ref(false)
const currentPrescription = ref<Prescription | null>(null)
const printVisible = ref(false)
const printData = ref<PrintPrescriptionData>()

const queryParams = reactive({
  clinicId: 0,
  prescriptionType: undefined as number | undefined,
  paymentStatus: undefined as number | undefined,
  dateRange: [] as string[]
})

const tableData = ref<Prescription[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  {
    prop: 'prescriptionType',
    label: '处方类型',
    type: 'select',
    options: [
      { label: '西药处方', value: 1 },
      { label: '中药处方', value: 2 },
      { label: '检查单', value: 3 }
    ]
  },
  {
    prop: 'paymentStatus',
    label: '缴费状态',
    type: 'select',
    options: [
      { label: '待缴费', value: 0 },
      { label: '已缴费', value: 1 },
      { label: '已退费', value: 2 }
    ]
  },
  { prop: 'dateRange', label: '日期', type: 'daterange' }
]

const columns = [
  { prop: 'prescriptionNo', label: '处方单号', width: 150 },
  {
    prop: 'createdAt',
    label: '开方时间',
    width: 160,
    formatter: (row: Prescription) => {
      if (!row.createdAt) return '-'
      return row.createdAt.replace('T', ' ')
    }
  },
  { prop: 'doctorName', label: '医生', width: 100 },
  { prop: 'departmentName', label: '科室', width: 100 },
  { prop: 'diagnosis', label: '诊断', width: 150 },
  {
    prop: 'prescriptionType',
    label: '类型',
    width: 100,
    formatter: (row: Prescription) => row.prescriptionTypeName
  },
  { prop: 'totalAmount', label: '原价', width: 100, formatter: (row: Prescription) => `¥${row.totalAmount}` },
  { prop: 'actualAmount', label: '应付', width: 100, formatter: (row: Prescription) => `¥${row.actualAmount}` },
  {
    prop: 'statusName',
    label: '状态',
    width: 100,
    formatter: (row: Prescription) => row.statusName
  }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: Prescription) => handleView(row) },
  { label: '打印', type: 'success', action: (row: Prescription) => handlePrint(row) }
]

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return types[status] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    const params: any = {
      clinicId,
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    // 添加查询条件
    if (queryParams.prescriptionType !== undefined) {
      params.prescriptionType = queryParams.prescriptionType
    }
    if (queryParams.paymentStatus !== undefined) {
      params.paymentStatus = queryParams.paymentStatus
    }
    // 日期范围
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      params.startDate = queryParams.dateRange[0]
      params.endDate = queryParams.dateRange[1]
    }
    const res = await getPrescriptionList(params)
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

const handleView = async (row: Prescription) => {
  try {
    const id = row.id || row.prescriptionId
    if (!id) {
      ElMessage.error('处方ID不能为空')
      return
    }
    const res = await getPrescriptionDetail(id)
    currentPrescription.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('获取处方详情失败:', error)
  }
}

const handleDispense = async () => {
  if (!currentPrescription.value) return
  try {
    const userInfo = getUserInfo()
    const prescriptionId = currentPrescription.value.id || currentPrescription.value.prescriptionId
    if (!prescriptionId) {
      ElMessage.error('处方ID不能为空')
      return
    }
    await dispensePrescription({
      prescriptionId,
      operatorId: userInfo?.userId || 0
    })
    ElMessage.success('确认发药成功')
    detailVisible.value = false
    loadData()
  } catch (error) {
    console.error('确认发药失败:', error)
  }
}

const handlePrint = async (row: Prescription) => {
  try {
    const id = row.id || row.prescriptionId
    if (!id) {
      ElMessage.error('处方ID不能为空')
      return
    }
    const clinicId = getClinicId()
    const res = await getPrescriptionPrintData(id, clinicId)
    printData.value = res.data
    printVisible.value = true
  } catch (error) {
    console.error('获取打印数据失败:', error)
    ElMessage.error('获取打印数据失败')
  }
}

const handlePrintCurrent = () => {
  if (!currentPrescription.value) {
    ElMessage.error('当前没有处方数据')
    return
  }
  handlePrint(currentPrescription.value)
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

  .prescription-header {
    margin-bottom: 20px;
  }

  .amount-info {
    margin-top: 20px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;

    .label {
      color: #909399;
    }

    .value {
      font-size: 16px;
      font-weight: 500;
    }

    .discount {
      color: #f56c6c;
    }

    .actual {
      color: #f56c6c;
      font-size: 20px;
      font-weight: bold;
    }
  }
}
</style>