<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @add="handleAdd"
        showAdd
        addText="新增病历"
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
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="患者" prop="patientId">
              <el-select
                v-model="form.patientId"
                placeholder="请选择患者"
                filterable
                remote
                :remote-method="searchPatients"
                :loading="patientLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="item in patientOptions"
                  :key="item.id"
                  :label="`${item.patientName} (${item.phone})`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主治医生" prop="doctorId">
              <el-select v-model="form.doctorId" placeholder="请选择医生" style="width: 100%">
                <el-option
                  v-for="item in doctorOptions"
                  :key="item.id"
                  :label="item.doctorName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="就诊类型" prop="visitType">
              <el-select v-model="form.visitType" placeholder="请选择就诊类型" style="width: 100%">
                <el-option label="初诊" :value="1" />
                <el-option label="复诊" :value="2" />
                <el-option label="急诊" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="就诊日期" prop="visitDate">
              <el-date-picker
                v-model="form.visitDate"
                type="date"
                placeholder="选择就诊日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="主诉" prop="chiefComplaint">
          <el-input
            v-model="form.chiefComplaint"
            type="textarea"
            rows="2"
            placeholder="请输入患者主诉"
          />
        </el-form-item>

        <el-form-item label="诊断结果" prop="diagnosis">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            rows="3"
            placeholder="请输入诊断结果"
          />
        </el-form-item>

        <el-form-item label="治疗方案" prop="treatment">
          <el-input
            v-model="form.treatment"
            type="textarea"
            rows="3"
            placeholder="请输入治疗方案"
          />
        </el-form-item>

        <el-form-item label="医嘱" prop="medicalAdvice">
          <el-input
            v-model="form.medicalAdvice"
            type="textarea"
            rows="2"
            placeholder="请输入医嘱"
          />
        </el-form-item>

        <el-form-item label="附件" prop="attachUrls">
          <el-input
            v-model="form.attachUrls"
            placeholder="附件链接（多个用逗号分隔）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="病历详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="病历编号">{{ currentRecord.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ currentRecord.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者电话">{{ currentRecord.patientPhone }}</el-descriptions-item>
        <el-descriptions-item label="主治医生">{{ currentRecord.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ currentRecord.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊类型">{{ currentRecord.visitTypeName }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ currentRecord.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRecord.createTime }}</el-descriptions-item>
        <el-descriptions-item label="主诉" :span="2">{{ currentRecord.chiefComplaint }}</el-descriptions-item>
        <el-descriptions-item label="诊断结果" :span="2">{{ currentRecord.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ currentRecord.treatment }}</el-descriptions-item>
        <el-descriptions-item label="医嘱" :span="2">{{ currentRecord.medicalAdvice || '无' }}</el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">{{ currentRecord.attachUrls || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import {
  getMedicalRecordList,
  createMedicalRecord,
  updateMedicalRecord,
  deleteMedicalRecord,
  type MedicalRecord
} from '@/api/medicalRecord'
import { getDoctorList, type Doctor } from '@/api/doctor'
import { getPatientList, type Patient } from '@/api/patient'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentRecord = ref<MedicalRecord | null>(null)

const patientLoading = ref(false)
const patientOptions = ref<Patient[]>([])
const doctorOptions = ref<Doctor[]>([])

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  patientId: undefined as number | undefined,
  doctorId: undefined as number | undefined,
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

const form = reactive({
  id: undefined as number | undefined,
  patientId: undefined as number | undefined,
  doctorId: undefined as number | undefined,
  deptId: undefined as number | undefined,
  visitType: 1,
  visitDate: '',
  chiefComplaint: '',
  diagnosis: '',
  treatment: '',
  medicalAdvice: '',
  attachUrls: ''
})

const rules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择就诊类型', trigger: 'change' }],
  chiefComplaint: [{ required: true, message: '请输入主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断结果', trigger: 'blur' }]
}

const columns = [
  { prop: 'recordNo', label: '病历编号', width: 140 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'patientPhone', label: '手机号', width: 120 },
  { prop: 'doctorName', label: '主治医生', width: 100 },
  { prop: 'deptName', label: '科室', width: 100 },
  {
    prop: 'visitType',
    label: '就诊类型',
    width: 80,
    formatter: (row: MedicalRecord) => {
      const map: Record<number, string> = { 1: '初诊', 2: '复诊', 3: '急诊' }
      return map[row.visitType] || '-'
    }
  },
  { prop: 'visitDate', label: '就诊日期', width: 100 },
  {
    prop: 'chiefComplaint',
    label: '主诉',
    minWidth: 150,
    formatter: (row: MedicalRecord) => row.chiefComplaint?.substring(0, 20) + (row.chiefComplaint?.length > 20 ? '...' : '')
  },
  { prop: 'createTime', label: '创建时间', width: 150 }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: MedicalRecord) => handleView(row) },
  { label: '编辑', type: 'warning', action: (row: MedicalRecord) => handleEdit(row) },
  { label: '删除', type: 'danger', action: (row: MedicalRecord) => handleDelete(row) }
]

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      clinicId: getClinicId(),
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

const loadDoctors = async () => {
  try {
    const res = await getDoctorList({
      clinicId: getClinicId(),
      pageNum: 1,
      pageSize: 100
    })
    doctorOptions.value = res.data.records
  } catch (error) {
    console.error('加载医生列表失败:', error)
  }
}

const searchPatients = async (keyword: string) => {
  if (!keyword) return
  patientLoading.value = true
  try {
    const res = await getPatientList({
      clinicId: getClinicId(),
      keyword,
      pageNum: 1,
      pageSize: 20
    })
    patientOptions.value = res.data.list
  } catch (error) {
    console.error('搜索患者失败:', error)
  } finally {
    patientLoading.value = false
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

const handleAdd = () => {
  dialogTitle.value = '新增病历'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    patientId: undefined,
    doctorId: undefined,
    deptId: undefined,
    visitType: 1,
    visitDate: new Date().toISOString().split('T')[0],
    chiefComplaint: '',
    diagnosis: '',
    treatment: '',
    medicalAdvice: '',
    attachUrls: ''
  })
  patientOptions.value = []
  dialogVisible.value = true
}

const handleEdit = (row: MedicalRecord) => {
  dialogTitle.value = '编辑病历'
  isEdit.value = true
  Object.assign(form, {
    id: row.id || row.recordId,
    patientId: row.patientId,
    doctorId: row.doctorId,
    deptId: row.deptId,
    visitType: row.visitType,
    visitDate: row.visitDate,
    chiefComplaint: row.chiefComplaint,
    diagnosis: row.diagnosis,
    treatment: row.treatment,
    medicalAdvice: row.medicalAdvice,
    attachUrls: row.attachUrls
  })
  patientOptions.value = [{
    id: row.patientId,
    patientName: row.patientName,
    phone: row.patientPhone
  } as Patient]
  dialogVisible.value = true
}

const handleView = (row: MedicalRecord) => {
  currentRecord.value = row
  detailVisible.value = true
}

const handleDelete = async (row: MedicalRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除该病历吗？删除后不可恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const id = row.id || row.recordId
    if (id) {
      await deleteMedicalRecord(id)
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请重试')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    const submitData = {
      ...form,
      clinicId: getClinicId()
    }

    if (isEdit.value) {
      await updateMedicalRecord(submitData)
      ElMessage.success('编辑成功')
    } else {
      await createMedicalRecord(submitData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadData()
  loadDoctors()
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
