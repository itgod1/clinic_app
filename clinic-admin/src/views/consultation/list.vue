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
        addText="新增记录"
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
        <el-form-item label="记录类型" prop="recordType">
          <el-radio-group v-model="form.recordType">
            <el-radio :value="1">咨询记录</el-radio>
            <el-radio :value="2">设计单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="患者选择">
          <el-radio-group v-model="patientMode" style="margin-bottom: 8px">
            <el-radio value="select">选择已有患者</el-radio>
            <el-radio value="manual">手动输入</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="patientMode === 'select'">
          <el-form-item label="患者">
            <el-select
              v-model="form.patientId"
              filterable
              remote
              :remote-method="searchPatients"
              :loading="patientLoading"
              placeholder="请输入姓名或手机号搜索"
              clearable
              @change="handlePatientChange"
            >
              <el-option
                v-for="p in patientOptions"
                :key="p.id"
                :label="`${p.patientName} - ${p.phone || '无电话'}`"
                :value="p.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="患者姓名" prop="patientName">
            <el-input v-model="form.patientName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="患者电话">
            <el-input v-model="form.patientPhone" placeholder="请输入电话" />
          </el-form-item>
        </template>
        <el-form-item label="选择医生" prop="doctorId">
          <el-select v-model="form.doctorId" placeholder="请选择医生" clearable @change="handleDoctorChange">
            <el-option
              v-for="doctor in doctorOptions"
              :key="doctor.id"
              :label="`${doctor.doctorName} (${doctor.deptName})`"
              :value="doctor.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="咨询日期" prop="visitDate">
          <el-date-picker
            v-model="form.visitDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="咨询内容" prop="consultationContent">
          <el-input v-model="form.consultationContent" type="textarea" rows="3" placeholder="请输入咨询内容或主诉" />
        </el-form-item>
        <el-form-item v-if="form.recordType === 2" label="设计要求">
          <el-input v-model="form.designRequirements" type="textarea" rows="3" placeholder="请输入设计要求（设计单填写）" />
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="form.suggestion" type="textarea" rows="3" placeholder="请输入医生建议或处理意见" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="复诊日期">
          <el-date-picker
            v-model="form.nextVisitDate"
            type="date"
            placeholder="建议复诊日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="记录详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录编号">{{ currentRecord.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="记录类型">
          <el-tag :type="currentRecord.recordType === 2 ? 'warning' : 'primary'" size="small">
            {{ currentRecord.recordTypeName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ currentRecord.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者电话">{{ currentRecord.patientPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ currentRecord.doctorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ currentRecord.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="咨询日期">{{ currentRecord.visitDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRecord.status)" size="small">
            {{ currentRecord.statusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="咨询内容" :span="2">{{ currentRecord.consultationContent || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRecord.recordType === 2" label="设计要求" :span="2">{{ currentRecord.designRequirements || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建议复诊" :span="2">{{ currentRecord.nextVisitDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理意见" :span="2">{{ currentRecord.suggestion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRecord.notes || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getConsultationList, createConsultation, updateConsultation, deleteConsultation, type ConsultationRecord } from '@/api/consultation'
import { getDoctorList, type Doctor } from '@/api/doctor'
import { getPatientList, type Patient } from '@/api/patient'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const currentRecord = ref<ConsultationRecord | null>(null)
const isEdit = ref(false)
const editId = ref<number>()

const patientMode = ref<'select' | 'manual'>('select')
const patientLoading = ref(false)
const patientOptions = ref<Patient[]>([])
const doctorOptions = ref<Doctor[]>([])

const queryParams = reactive({
  clinicId: 0,
  recordType: undefined as number | undefined,
  doctorId: undefined as number | undefined,
  visitDate: [] as string[],
  keyword: ''
})

const tableData = ref<ConsultationRecord[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const doctorSelectOptions = ref<{ label: string; value: number }[]>([])

const searchForms = computed(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/电话/编号' },
  {
    prop: 'recordType',
    label: '类型',
    type: 'select',
    options: [
      { label: '咨询记录', value: 1 },
      { label: '设计单', value: 2 }
    ]
  },
  {
    prop: 'doctorId',
    label: '医生',
    type: 'select',
    options: doctorSelectOptions.value
  },
  { prop: 'visitDate', label: '日期', type: 'daterange' }
])

const form = reactive({
  recordType: 1,
  patientId: undefined as number | undefined,
  patientName: '',
  patientPhone: '',
  doctorId: undefined as number | undefined,
  deptId: undefined as number | undefined,
  visitDate: '',
  consultationContent: '',
  designRequirements: '',
  suggestion: '',
  nextVisitDate: '',
  status: 1,
  notes: ''
})

const rules = {
  consultationContent: [{ required: true, message: '请输入咨询内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const columns = [
  { prop: 'recordNo', label: '编号', width: 160 },
  {
    prop: 'recordTypeName',
    label: '类型',
    width: 100,
    formatter: (row: ConsultationRecord) => {
      const type = row.recordType === 2 ? 'warning' : 'primary'
      return h('el-tag', { type, size: 'small' }, () => row.recordTypeName)
    }
  },
  { prop: 'patientName', label: '患者', width: 100 },
  { prop: 'patientPhone', label: '电话', width: 120 },
  { prop: 'doctorName', label: '医生', width: 100 },
  { prop: 'visitDate', label: '日期', width: 110 },
  {
    prop: 'statusName',
    label: '状态',
    width: 100,
    formatter: (row: ConsultationRecord) => {
      const typeMap: Record<number, string> = { 1: 'info', 2: 'success', 3: 'danger' }
      return h('el-tag', { type: typeMap[row.status] || 'info', size: 'small' }, () => row.statusName)
    }
  },
  { prop: 'consultationContent', label: '内容摘要', minWidth: 200, showOverflowTooltip: true },
  {
    prop: 'nextVisitDate',
    label: '复诊日期',
    width: 130,
    formatter: (row: ConsultationRecord) => {
      if (!row.nextVisitDate) return '-'
      const today = new Date(); today.setHours(0, 0, 0, 0)
      const due = new Date(row.nextVisitDate); due.setHours(0, 0, 0, 0)
      const diff = Math.ceil((due.getTime() - today.getTime()) / 86400000)
      if (diff < 0) return h('span', { style: 'color: #f56c6c; font-weight: bold' }, row.nextVisitDate + ' (已逾期)')
      if (diff <= 3) return h('span', { style: 'color: #e6a23c; font-weight: bold' }, row.nextVisitDate + ` (${diff}天)`)
      return h('span', { style: 'color: #67c23a' }, row.nextVisitDate)
    }
  }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: ConsultationRecord) => handleView(row) },
  { label: '编辑', type: 'warning', action: (row: ConsultationRecord) => handleEdit(row) },
  {
    label: '删除',
    type: 'danger',
    action: (row: ConsultationRecord) => handleDelete(row)
  }
]

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 1: 'info', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const loadDoctors = async () => {
  try {
    const res = await getDoctorList({
      clinicId: getClinicId(),
      status: 1,
      pageSize: 100
    })
    doctorOptions.value = res.data.records || []
    doctorSelectOptions.value = doctorOptions.value.map((d: Doctor) => ({
      label: d.doctorName,
      value: d.id
    }))
  } catch (error) {
    console.error('加载医生列表失败:', error)
  }
}

const searchPatients = async (query: string) => {
  if (!query) {
    patientOptions.value = []
    return
  }
  patientLoading.value = true
  try {
    const res = await getPatientList({
      clinicId: getClinicId(),
      keyword: query,
      pageSize: 20
    })
    patientOptions.value = res.data.list || []
  } catch (error) {
    console.error('搜索患者失败:', error)
  } finally {
    patientLoading.value = false
  }
}

const handlePatientChange = (patientId: number | undefined) => {
  if (patientId) {
    const patient = patientOptions.value.find(p => p.id === patientId)
    if (patient) {
      form.patientName = patient.patientName
      form.patientPhone = patient.phone || ''
    }
  } else {
    form.patientName = ''
    form.patientPhone = ''
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      clinicId: getClinicId(),
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    if (queryParams.keyword) params.keyword = queryParams.keyword
    if (queryParams.recordType) params.recordType = queryParams.recordType
    if (queryParams.doctorId) params.doctorId = queryParams.doctorId
    if (queryParams.visitDate && Array.isArray(queryParams.visitDate) && queryParams.visitDate.length === 2) {
      params.startDate = queryParams.visitDate[0]
      params.endDate = queryParams.visitDate[1]
    }
    const res = await getConsultationList(params)
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
  queryParams.keyword = ''
  queryParams.recordType = undefined
  queryParams.doctorId = undefined
  queryParams.visitDate = []
  loadData()
}

const handlePageChange = (page: number, size?: number) => {
  pagination.current = page
  if (size) pagination.size = size
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = undefined
  dialogTitle.value = '新增记录'
  patientMode.value = 'select'
  Object.assign(form, {
    recordType: 1,
    patientId: undefined,
    patientName: '',
    patientPhone: '',
    doctorId: undefined,
    deptId: undefined,
    visitDate: '',
    consultationContent: '',
    designRequirements: '',
    suggestion: '',
    nextVisitDate: '',
    status: 1,
    notes: ''
  })
  patientOptions.value = []
  loadDoctors()
  dialogVisible.value = true
}

const handleDoctorChange = (doctorId: number | undefined) => {
  if (doctorId) {
    const doctor = doctorOptions.value.find(d => d.id === doctorId)
    if (doctor) {
      form.deptId = doctor.deptId
    }
  } else {
    form.deptId = undefined
  }
}

const handleView = (row: ConsultationRecord) => {
  currentRecord.value = row
  detailVisible.value = true
}

const handleEdit = (row: ConsultationRecord) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑记录'
  patientMode.value = row.patientId ? 'select' : 'manual'
  Object.assign(form, {
    recordType: row.recordType,
    patientId: row.patientId || undefined,
    patientName: row.patientName || '',
    patientPhone: row.patientPhone || '',
    doctorId: row.doctorId || undefined,
    deptId: row.deptId || undefined,
    visitDate: row.visitDate || '',
    consultationContent: row.consultationContent || '',
    designRequirements: row.designRequirements || '',
    suggestion: row.suggestion || '',
    nextVisitDate: row.nextVisitDate || '',
    status: row.status,
    notes: row.notes || ''
  })
  if (row.patientId) {
    patientOptions.value = [{ id: row.patientId, patientName: row.patientName, phone: row.patientPhone } as Patient]
  }
  loadDoctors()
  dialogVisible.value = true
}

const handleDelete = async (row: ConsultationRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除该记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteConsultation(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    const doctor = doctorOptions.value.find(d => d.id === form.doctorId)

    const data: any = {
      clinicId: getClinicId(),
      recordType: form.recordType,
      doctorId: form.doctorId || null,
      doctorName: doctor?.doctorName || null,
      deptId: form.deptId || null,
      deptName: doctor?.deptName || null,
      visitDate: form.visitDate || null,
      consultationContent: form.consultationContent,
      designRequirements: form.recordType === 2 ? form.designRequirements : null,
      suggestion: form.suggestion || null,
      nextVisitDate: form.nextVisitDate || null,
      status: form.status,
      notes: form.notes || null
    }

    if (patientMode.value === 'select' && form.patientId) {
      data.patientId = form.patientId
      data.patientName = form.patientName
      data.patientPhone = form.patientPhone
    } else {
      data.patientId = null
      data.patientName = form.patientName || null
      data.patientPhone = form.patientPhone || null
    }

    if (isEdit.value) {
      data.id = editId.value
      await updateConsultation(data)
      ElMessage.success('更新成功')
    } else {
      await createConsultation(data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    console.error('提交失败:', error)
    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadDoctors()
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
