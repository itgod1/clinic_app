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
        addText="新建挂号"
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
        <el-form-item label="患者姓名" prop="patientName">
          <el-input v-model="form.patientName" placeholder="请输入患者姓名" />
        </el-form-item>
        <el-form-item label="患者电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入患者电话（可选）" />
        </el-form-item>
        <el-form-item label="选择医生" prop="doctorId">
          <el-select v-model="form.doctorId" placeholder="请选择医生" @change="handleDoctorChange">
            <el-option
              v-for="doctor in doctorOptions"
              :key="doctor.id"
              :label="`${doctor.doctorName} (${doctor.deptName})`"
              :value="doctor.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊日期" prop="regDate">
          <el-date-picker
            v-model="form.regDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item label="就诊时间" prop="regTime">
          <el-select v-model="form.regTime" placeholder="请选择时间">
            <el-option-group label="上午">
              <el-option label="08:00-08:30" value="08:00-08:30" />
              <el-option label="08:30-09:00" value="08:30-09:00" />
              <el-option label="09:00-09:30" value="09:00-09:30" />
              <el-option label="09:30-10:00" value="09:30-10:00" />
              <el-option label="10:00-10:30" value="10:00-10:30" />
              <el-option label="10:30-11:00" value="10:30-11:00" />
              <el-option label="11:00-11:30" value="11:00-11:30" />
            </el-option-group>
            <el-option-group label="下午">
              <el-option label="14:00-14:30" value="14:00-14:30" />
              <el-option label="14:30-15:00" value="14:30-15:00" />
              <el-option label="15:00-15:30" value="15:00-15:30" />
              <el-option label="15:30-16:00" value="15:30-16:00" />
              <el-option label="16:00-16:30" value="16:00-16:30" />
              <el-option label="16:30-17:00" value="16:30-17:00" />
              <el-option label="17:00-17:30" value="17:00-17:30" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="就诊类型" prop="visitType">
          <el-select v-model="form.visitType" placeholder="请选择就诊类型">
            <el-option label="初诊" :value="1" />
            <el-option label="复诊" :value="2" />
            <el-option label="急诊" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="主诉" prop="chiefComplaint">
          <el-input v-model="form.chiefComplaint" type="textarea" rows="3" placeholder="请输入主要症状" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="挂号详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentRegistration">
        <el-descriptions-item label="挂号编号">{{ currentRegistration.regNo }}</el-descriptions-item>
        <el-descriptions-item label="挂号日期">{{ currentRegistration.regDate }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">{{ currentRegistration.regTime }}</el-descriptions-item>
        <el-descriptions-item label="排队号">{{ currentRegistration.queueNo }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ currentRegistration.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者电话">{{ currentRegistration.phone }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ currentRegistration.deptName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ currentRegistration.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="就诊类型">{{ currentRegistration.visitTypeName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRegistration.status)">
            {{ currentRegistration.statusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="主诉" :span="2">{{ currentRegistration.chiefComplaint || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentRegistration?.status === 1"
          type="danger"
          @click="handleCancel(currentRegistration)"
        >
          取消挂号
        </el-button>
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
import { getRegistrationList, cancelRegistration, createRegistration, type Registration } from '@/api/registration'
import { getDoctorList, type Doctor } from '@/api/doctor'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const currentRegistration = ref<Registration | null>(null)

const doctorOptions = ref<Doctor[]>([])

const queryParams = reactive({
  clinicId: 0,
  regDate: [] as string[],
  doctorId: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: ''
})

const tableData = ref<Registration[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const doctorSelectOptions = ref<{ label: string; value: number }[]>([])

const searchForms = computed(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/手机号/挂号编号' },
  { prop: 'regDate', label: '挂号日期', type: 'daterange' },
  {
    prop: 'doctorId',
    label: '医生',
    type: 'select',
    options: doctorSelectOptions.value
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '已挂号', value: 1 },
      { label: '已签到', value: 2 },
      { label: '就诊中', value: 3 },
      { label: '已完成', value: 4 },
      { label: '已取消', value: 5 },
      { label: '已退号', value: 6 },
      { label: '过号', value: 7 }
    ]
  }
])

const form = reactive({
  patientId: undefined as number | undefined,
  patientName: '',
  phone: '',
  doctorId: undefined as number | undefined,
  deptId: undefined as number | undefined,
  regDate: '',
  regTime: '',
  visitType: 1,
  chiefComplaint: ''
})

const rules = {
  patientName: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  regDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }],
  regTime: [{ required: true, message: '请选择就诊时间', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择就诊类型', trigger: 'change' }]
}

const columns = [
  { prop: 'regNo', label: '挂号编号', width: 150 },
  { prop: 'regDate', label: '挂号日期', width: 120 },
  { prop: 'regTime', label: '时间', width: 80 },
  { prop: 'queueNo', label: '排队号', width: 80 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'deptName', label: '科室', width: 100 },
  { prop: 'doctorName', label: '医生', width: 100 },
  { prop: 'visitTypeName', label: '就诊类型', width: 80 },
  {
    prop: 'statusName',
    label: '状态',
    width: 100,
    formatter: (row: Registration) => {
      const typeMap: Record<number, string> = {
        1: 'warning',
        2: 'success',
        3: 'info',
        4: 'danger'
      }
      return h('el-tag', { type: typeMap[row.status] || 'info', size: 'small' }, () => row.statusName)
    }
  }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: Registration) => handleView(row) },
  {
    label: '取消',
    type: 'danger',
    condition: (row: Registration) => row.status === 1,
    action: (row: Registration) => handleCancel(row)
  }
]

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'danger'
  }
  return types[status] || 'info'
}

const disabledDate = (date: Date) => {
  return date.getTime() < Date.now() - 86400000
}

const loadDoctors = async () => {
  try {
    const res = await getDoctorList({
      clinicId: getClinicId(),
      status: 1,
      pageSize: 100
    })
    doctorOptions.value = res.data.records || []
    // 更新搜索表单中的医生选项
    doctorSelectOptions.value = doctorOptions.value.map((d: Doctor) => ({
      label: d.doctorName,
      value: d.id
    }))
  } catch (error) {
    console.error('加载医生列表失败:', error)
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
    // 添加搜索条件
    if (queryParams.keyword) {
      params.keyword = queryParams.keyword
    }
    if (queryParams.doctorId) {
      params.doctorId = queryParams.doctorId
    }
    if (queryParams.status) {
      params.status = queryParams.status
    }
    // 处理日期范围
    if (queryParams.regDate && Array.isArray(queryParams.regDate) && queryParams.regDate.length === 2) {
      params.startDate = queryParams.regDate[0]
      params.endDate = queryParams.regDate[1]
    }
    const res = await getRegistrationList(params)
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
  queryParams.regDate = []
  queryParams.doctorId = undefined
  queryParams.status = undefined
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
  dialogTitle.value = '新建挂号'
  Object.assign(form, {
    patientId: undefined,
    patientName: '',
    phone: '',
    doctorId: undefined,
    deptId: undefined,
    regDate: '',
    regTime: '',
    visitType: 1,
    chiefComplaint: ''
  })
  loadDoctors()
  dialogVisible.value = true
}

const handleDoctorChange = (doctorId: number) => {
  const doctor = doctorOptions.value.find(d => d.id === doctorId)
  if (doctor) {
    form.deptId = doctor.deptId
  }
}

const handleView = (row: Registration) => {
  currentRegistration.value = row
  detailVisible.value = true
}

const handleCancel = async (row: Registration) => {
  try {
    await ElMessageBox.confirm('确定要取消该挂号吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelRegistration({ id: row.id })
    ElMessage.success('取消成功')
    detailVisible.value = false
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消失败:', error)
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    const doctor = doctorOptions.value.find(d => d.id === form.doctorId)

    const data = {
      clinicId: getClinicId(),
      patientId: form.patientId || null,
      patientName: form.patientName,
      phone: form.phone || null,
      doctorId: form.doctorId,
      doctorName: doctor?.doctorName || '',
      deptId: form.deptId,
      deptName: doctor?.deptName || '',
      regDate: form.regDate,
      regTime: form.regTime,
      visitType: form.visitType,
      chiefComplaint: form.chiefComplaint
    }

    await createRegistration(data)
    ElMessage.success('挂号成功')
    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    console.error('提交失败:', error)
    ElMessage.error(error?.response?.data?.message || '挂号失败')
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