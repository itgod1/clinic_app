<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        showAdd
        addText="新建病历"
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

    <el-dialog v-model="detailVisible" title="病历详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="病历号">{{ currentRecord.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ currentRecord.patientName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRecord.patientPhone }}</el-descriptions-item>
        <el-descriptions-item label="就诊类型">{{ currentRecord.visitTypeName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ currentRecord.deptName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ currentRecord.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="主诉" :span="2">{{ currentRecord.chiefComplaint }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ currentRecord.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ currentRecord.treatment }}</el-descriptions-item>
        <el-descriptions-item label="医嘱" :span="2">{{ currentRecord.medicalAdvice }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRecord.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentRecord.updateTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handlePrintCurrent">
          <el-icon><Printer /></el-icon>
          打印病历
        </el-button>
      </template>
    </el-dialog>

    <!-- 打印预览 -->
    <PrintPreview
      v-model="printVisible"
      type="medicalRecord"
      title="病历打印预览"
      :medical-record-data="printData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getMedicalRecordList, getMedicalRecordDetail, type MedicalRecord } from '@/api/medicalRecord'
import { getMedicalRecordPrintData, type PrintMedicalRecordData } from '@/api/print'
import { getClinicId } from '@/utils/storage'
import PrintPreview from '@/components/PrintPreview/index.vue'

const loading = ref(false)
const detailVisible = ref(false)
const currentRecord = ref<MedicalRecord | null>(null)
const printVisible = ref(false)
const printData = ref<PrintMedicalRecordData>()

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  startDate: '',
  endDate: ''
})

const tableData = ref<MedicalRecord[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '病历号/患者姓名' },
  { prop: 'startDate', label: '就诊日期', type: 'daterange' }
]

const columns = [
  { prop: 'recordNo', label: '病历号', width: 150 },
  { prop: 'createTime', label: '就诊时间', width: 160 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'patientPhone', label: '手机号', width: 120 },
  { prop: 'deptName', label: '科室', width: 100 },
  { prop: 'doctorName', label: '医生', width: 100 },
  { prop: 'visitTypeName', label: '就诊类型', width: 80 },
  { prop: 'diagnosis', label: '诊断', width: 200 }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: MedicalRecord) => handleView(row) },
  { label: '编辑', type: 'warning', action: (row: MedicalRecord) => handleEdit(row) },
  { label: '打印', type: 'success', action: (row: MedicalRecord) => handlePrint(row) }
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
    const res = await getMedicalRecordList(params)
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

const handleView = async (row: MedicalRecord) => {
  try {
    const id = row.id || row.recordId
    if (!id) {
      ElMessage.error('病历ID不能为空')
      return
    }
    const res = await getMedicalRecordDetail(id)
    currentRecord.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('获取病历详情失败:', error)
  }
}

const handleEdit = (row: MedicalRecord) => {
  ElMessage.info('编辑功能开发中')
}

const handlePrint = async (row: MedicalRecord) => {
  try {
    const id = row.id || row.recordId
    if (!id) {
      ElMessage.error('病历ID不能为空')
      return
    }
    const clinicId = getClinicId()
    const res = await getMedicalRecordPrintData(id, clinicId)
    printData.value = res.data
    printVisible.value = true
  } catch (error) {
    console.error('获取打印数据失败:', error)
    ElMessage.error('获取打印数据失败')
  }
}

const handlePrintCurrent = () => {
  if (!currentRecord.value) {
    ElMessage.error('当前没有病历数据')
    return
  }
  handlePrint(currentRecord.value)
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
}
</style>