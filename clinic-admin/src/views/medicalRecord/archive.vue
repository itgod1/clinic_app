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
      <div v-if="detailToothChartRecords.length > 0" style="margin-top: 16px;">
        <el-divider content-position="left">牙位图</el-divider>
        <ToothChart :model-value="detailToothChartRecords" read-only />
      </div>
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

    <!-- 创建复诊提醒对话框 -->
    <el-dialog v-model="revisitDialogVisible" title="创建复诊提醒" width="600px" destroy-on-close>
      <el-form :model="revisitForm" label-width="100px">
        <el-form-item label="患者姓名">
          <el-input v-model="revisitForm.patientName" disabled />
        </el-form-item>
        <el-form-item label="患者电话">
          <el-input v-model="revisitForm.patientPhone" disabled />
        </el-form-item>
        <el-form-item label="原治疗">
          <el-input v-model="revisitForm.originalTreatment" placeholder="如：拔牙、根管治疗等" />
        </el-form-item>
        <el-form-item label="复诊项目" required>
          <el-input v-model="revisitForm.followUpItem" placeholder="如：种植牙、拆线、复查等" />
        </el-form-item>
        <el-form-item label="复诊说明">
          <el-input v-model="revisitForm.followUpDesc" type="textarea" :rows="3" placeholder="请输入复诊说明" />
        </el-form-item>
        <el-form-item label="恢复期">
          <el-input-number v-model="revisitForm.recoveryDays" :min="1" :max="365" />
          <span style="margin-left: 8px">天</span>
        </el-form-item>
        <el-form-item label="计划日期" required>
          <el-date-picker
            v-model="revisitForm.planDate"
            type="date"
            placeholder="选择计划复诊日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="handlePlanDateChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revisitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRevisitSubmit">创建提醒</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑病历对话框 -->
    <el-dialog
      v-model="medicalDialogVisible"
      :title="medicalDialogTitle"
      width="700px"
      destroy-on-close
    >
      <el-form ref="medicalFormRef" :model="medicalForm" :rules="medicalRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="患者" prop="patientId">
              <el-select
                v-model="medicalForm.patientId"
                placeholder="请选择患者"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in patientOptions"
                  :key="item.id"
                  :label="item.patientName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主治医生" prop="doctorId">
              <el-select v-model="medicalForm.doctorId" placeholder="请选择医生" style="width: 100%">
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
              <el-select v-model="medicalForm.visitType" placeholder="请选择就诊类型" style="width: 100%">
                <el-option label="初诊" :value="1" />
                <el-option label="复诊" :value="2" />
                <el-option label="急诊" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="就诊日期" prop="visitDate">
              <el-date-picker
                v-model="medicalForm.visitDate"
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
            v-model="medicalForm.chiefComplaint"
            type="textarea"
            rows="2"
            placeholder="请输入患者主诉"
          />
        </el-form-item>

        <el-form-item label="诊断结果" prop="diagnosis">
          <el-input
            v-model="medicalForm.diagnosis"
            type="textarea"
            rows="3"
            placeholder="请输入诊断结果"
          />
        </el-form-item>

        <el-form-item label="治疗方案" prop="treatment">
          <el-input
            v-model="medicalForm.treatment"
            type="textarea"
            rows="3"
            placeholder="请输入治疗方案"
          />
        </el-form-item>

        <el-form-item label="医嘱" prop="medicalAdvice">
          <el-input
            v-model="medicalForm.medicalAdvice"
            type="textarea"
            rows="2"
            placeholder="请输入医嘱"
          />
        </el-form-item>

        <el-form-item label="附件" prop="attachUrls">
          <el-input
            v-model="medicalForm.attachUrls"
            placeholder="附件链接（多个用逗号分隔）"
          />
        </el-form-item>

        <el-divider content-position="left">牙位图</el-divider>
        <ToothChart v-model="toothChartRecords" />
      </el-form>
      <template #footer>
        <el-button @click="medicalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMedicalSubmit" :loading="medicalSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import {
  getMedicalRecordList,
  getMedicalRecordDetail,
  createMedicalRecord,
  updateMedicalRecord,
  type MedicalRecord
} from '@/api/medicalRecord'
import { getMedicalRecordPrintData } from '@/api/print'
import { createRevisitFromRecord } from '@/api/returnVisit'
import { getDoctorList, type Doctor } from '@/api/doctor'
import { getPatientList, type Patient } from '@/api/patient'
import type { PrintMedicalRecordData } from '@/types/print'
import { getClinicId } from '@/utils/storage'
import PrintPreview from '@/components/PrintPreview/index.vue'
import ToothChart from '@/components/ToothChart.vue'
import { getToothChart, saveToothChart, type ToothChartRecord } from '@/api/toothChart'

const loading = ref(false)
const detailVisible = ref(false)
const currentRecord = ref<MedicalRecord | null>(null)
const printVisible = ref(false)
const printData = ref<PrintMedicalRecordData>()
const toothChartRecords = ref<ToothChartRecord[]>([])
const detailToothChartRecords = ref<ToothChartRecord[]>([])

// 新增/编辑病历相关
const medicalDialogVisible = ref(false)
const medicalDialogTitle = ref('')
const isMedicalEdit = ref(false)
const medicalFormRef = ref<FormInstance>()
const medicalSubmitLoading = ref(false)
const patientLoading = ref(false)
const patientOptions = ref<Patient[]>([])
const doctorOptions = ref<Doctor[]>([])

const medicalForm = reactive({
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

const medicalRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择就诊类型', trigger: 'change' }],
  chiefComplaint: [{ required: true, message: '请输入主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断结果', trigger: 'blur' }]
}

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
  { prop: 'visitDate', label: '就诊时间', width: 120 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'patientPhone', label: '手机号', width: 120 },
  { prop: 'deptName', label: '科室', width: 100 },
  { prop: 'doctorName', label: '医生', width: 100 },
  { prop: 'visitTypeName', label: '就诊类型', width: 80 },
  { prop: 'diagnosis', label: '诊断', width: 200 }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: MedicalRecord) => handleView(row) },
  { label: '打印', type: 'success', action: (row: MedicalRecord) => handlePrint(row) },
  { label: '复诊提醒', type: 'warning', action: (row: MedicalRecord) => handleCreateRevisit(row) }
]

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    const params: any = {
      clinicId,
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    // 添加搜索条件
    if (queryParams.keyword) {
      params.keyword = queryParams.keyword
    }
    if (queryParams.startDate) {
      params.startDate = queryParams.startDate
    }
    if (queryParams.endDate) {
      params.endDate = queryParams.endDate
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
  queryParams.keyword = ''
  queryParams.startDate = ''
  queryParams.endDate = ''
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
    // 加载牙位图
    try {
      const toothRes = await getToothChart(id)
      detailToothChartRecords.value = toothRes.data || []
    } catch {
      detailToothChartRecords.value = []
    }
    detailVisible.value = true
  } catch (error) {
    console.error('获取病历详情失败:', error)
  }
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

// 创建复诊提醒
const handleCreateRevisit = async (row: MedicalRecord) => {
  try {
    // 获取病历详情以获取完整信息
    const id = row.id || row.recordId
    if (!id) {
      ElMessage.error('病历ID不能为空')
      return
    }

    // 显示创建复诊提醒的对话框
    const { value: formValues } = await ElMessageBox.prompt(
      '',
      '创建复诊提醒',
      {
        confirmButtonText: '创建',
        cancelButtonText: '取消',
        inputValidator: (value) => {
          if (!value) return '请输入复诊项目'
          return true
        },
        inputPlaceholder: '请输入复诊项目，如：种植牙、拆线、复查等',
      }
    )

    if (!formValues) return

    // 设置默认值
    const defaultValues = {
      medicalRecordId: id,
      patientId: row.patientId || row.id,
      patientName: row.patientName,
      patientPhone: row.patientPhone || '',
      originalTreatment: row.treatment || row.diagnosis || '拔牙',
      followUpItem: formValues,
      followUpDesc: '',
      recoveryDays: 90,
      planDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
    }

    // 打开更详细的对话框
    await openRevisitDialog(defaultValues)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建复诊提醒失败:', error)
    }
  }
}

// 打开复诊提醒详细对话框
const revisitDialogVisible = ref(false)
const revisitForm = reactive({
  medicalRecordId: 0,
  patientId: 0,
  patientName: '',
  patientPhone: '',
  originalTreatment: '',
  followUpItem: '',
  followUpDesc: '',
  recoveryDays: 90,
  planDate: ''
})

// 监听恢复期变化，自动计算计划日期
watch(() => revisitForm.recoveryDays, (newVal) => {
  if (newVal) {
    const date = new Date()
    date.setDate(date.getDate() + newVal)
    revisitForm.planDate = date.toISOString().split('T')[0]
  }
})

const openRevisitDialog = async (defaultValues: any) => {
  // 填充表单
  Object.assign(revisitForm, defaultValues)
  revisitDialogVisible.value = true
}

// 根据计划日期计算恢复期
const handlePlanDateChange = (planDate: string) => {
  if (!planDate) return
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const plan = new Date(planDate)
  plan.setHours(0, 0, 0, 0)
  const diffTime = plan.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  if (diffDays > 0) {
    revisitForm.recoveryDays = diffDays
  }
}

const handleRevisitSubmit = async () => {
  try {
    await createRevisitFromRecord(revisitForm)
    ElMessage.success('复诊提醒创建成功')
    revisitDialogVisible.value = false
  } catch (error) {
    console.error('创建复诊提醒失败:', error)
    ElMessage.error('创建失败')
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

const handleAdd = () => {
  medicalDialogTitle.value = '新增病历'
  isMedicalEdit.value = false
  toothChartRecords.value = []
  Object.assign(medicalForm, {
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
  loadPatientOptions()
  medicalDialogVisible.value = true
}

const loadPatientOptions = async () => {
  try {
    const res = await getPatientList({
      clinicId: getClinicId(),
      pageNum: 1,
      pageSize: 100
    })
    patientOptions.value = res.data.list
  } catch (error) {
    console.error('加载患者列表失败:', error)
  }
}

const handleMedicalSubmit = async () => {
  try {
    await medicalFormRef.value?.validate()
    medicalSubmitLoading.value = true

    const submitData = {
      ...medicalForm,
      clinicId: getClinicId()
    }

    let recordId: number | undefined

    if (isMedicalEdit.value) {
      await updateMedicalRecord(submitData)
      recordId = submitData.id
      ElMessage.success('编辑成功')
    } else {
      const res = await createMedicalRecord(submitData)
      recordId = res.data?.id || res.data?.recordId
      ElMessage.success('新增成功')
    }

    // 保存牙位图
    if (recordId && toothChartRecords.value.length >= 0) {
      try {
        await saveToothChart({
          medicalRecordId: recordId,
          clinicId: getClinicId(),
          records: toothChartRecords.value
        })
      } catch (e) {
        console.error('保存牙位图失败:', e)
      }
    }

    medicalDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败，请重试')
  } finally {
    medicalSubmitLoading.value = false
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
