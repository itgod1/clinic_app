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
        addText="新增患者"
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
      >
        <template #toolbar>
          <el-button type="success" @click="handleExport">导出Excel</el-button>
        </template>
      </Table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="患者姓名" prop="patientName">
          <el-input v-model="form.patientName" placeholder="请输入患者姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号（选填）" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="150" />
        </el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="form.birthday"
            type="date"
            placeholder="选择生日"
            value-format="YYYY-MM-DD"
            @change="handleBirthdayChange"
          />
        </el-form-item>
        <el-form-item label="会员等级" prop="memberLevel">
          <el-select v-model="form.memberLevel" placeholder="请选择会员等级">
            <el-option label="普通会员" :value="1" />
            <el-option label="银卡会员" :value="2" />
            <el-option label="金卡会员" :value="3" />
            <el-option label="钻石会员" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input v-model="form.allergyHistory" type="textarea" rows="2" placeholder="请输入过敏史" />
        </el-form-item>
        <el-form-item label="既往病史" prop="medicalHistory">
          <el-input v-model="form.medicalHistory" type="textarea" rows="2" placeholder="请输入既往病史" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="患者详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentPatient">
        <el-descriptions-item label="患者姓名">{{ currentPatient.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者编号">{{ currentPatient.patientCode }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentPatient.phone }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentPatient.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentPatient.age }}岁</el-descriptions-item>
        <el-descriptions-item label="生日">{{ currentPatient.birthday }}</el-descriptions-item>
        <el-descriptions-item label="会员等级">{{ currentPatient.memberLevelName }}</el-descriptions-item>
        <el-descriptions-item label="余额">¥{{ currentPatient.balance }}</el-descriptions-item>
        <el-descriptions-item label="积分">{{ currentPatient.points }}</el-descriptions-item>
        <el-descriptions-item label="就诊次数">{{ currentPatient.visitCount }}</el-descriptions-item>
        <el-descriptions-item label="最后就诊">{{ currentPatient.lastVisitDate || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentPatient.status === 1 ? 'success' : 'danger'">
            {{ currentPatient.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="过敏史" :span="2">{{ currentPatient.allergyHistory || '无' }}</el-descriptions-item>
        <el-descriptions-item label="既往病史" :span="2">{{ currentPatient.medicalHistory || '无' }}</el-descriptions-item>
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
import { getPatientList, deletePatient, createPatient, updatePatient, type Patient } from '@/api/patient'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentPatient = ref<Patient | null>(null)

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  memberLevel: undefined,
  startDate: '',
  endDate: ''
})

const tableData = ref<Patient[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/手机号/编号' },
  {
    prop: 'memberLevel',
    label: '会员等级',
    type: 'select',
    options: [
      { label: '普通会员', value: 1 },
      { label: '银卡会员', value: 2 },
      { label: '金卡会员', value: 3 },
      { label: '钻石会员', value: 4 }
    ]
  },
  { prop: 'startDate', label: '注册日期', type: 'daterange' }
]

const form = reactive({
  id: undefined as number | undefined,
  patientName: '',
  phone: '',
  gender: 1,
  age: 0,
  birthday: '',
  memberLevel: 1,
  allergyHistory: '',
  medicalHistory: ''
})

const rules = {
  patientName: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }]
}

const columns = [
  { prop: 'patientCode', label: '患者编号', width: 120 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 120 },
  {
    prop: 'gender',
    label: '性别',
    width: 60,
    formatter: (row: Patient) => row.gender === 1 ? '男' : '女'
  },
  { prop: 'age', label: '年龄', width: 60 },
  { prop: 'lastVisitDate', label: '最后就诊', width: 120 },
  {
    prop: 'allergyHistory',
    label: '过敏史',
    minWidth: 150,
    formatter: (row: Patient) => row.allergyHistory || '-'
  },
  {
    prop: 'medicalHistory',
    label: '既往病史',
    minWidth: 150,
    formatter: (row: Patient) => row.medicalHistory || '-'
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: Patient) => row.status === 1 ? '正常' : '禁用'
  }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: Patient) => handleView(row) },
  { label: '编辑', type: 'warning', action: (row: Patient) => handleEdit(row) },
  { label: '删除', type: 'danger', action: (row: Patient) => handleDelete(row) }
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
    const res = await getPatientList(params)
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

const handleAdd = () => {
  dialogTitle.value = '新增患者'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    patientName: '',
    phone: '',
    gender: 1,
    age: 0,
    birthday: '',
    memberLevel: 1,
    allergyHistory: '',
    medicalHistory: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: Patient) => {
  dialogTitle.value = '编辑患者'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    patientName: row.patientName,
    phone: row.phone,
    gender: row.gender,
    age: row.age,
    birthday: row.birthday,
    memberLevel: row.memberLevel,
    allergyHistory: row.allergyHistory,
    medicalHistory: row.medicalHistory
  })
  dialogVisible.value = true
}

const handleView = async (row: Patient) => {
  try {
    currentPatient.value = row
    detailVisible.value = true
  } catch (error) {
    console.error('获取患者详情失败:', error)
  }
}

const handleDelete = async (row: Patient) => {
  try {
    await ElMessageBox.confirm('确定要删除该患者吗？删除后不可恢复', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePatient(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请重试')
    }
  }
}

// 根据生日计算年龄
const handleBirthdayChange = (birthday: string) => {
  if (!birthday) return
  const birthDate = new Date(birthday)
  const today = new Date()
  let age = today.getFullYear() - birthDate.getFullYear()
  const monthDiff = today.getMonth() - birthDate.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--
  }
  form.age = age >= 0 ? age : 0
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
      await updatePatient(submitData)
      ElMessage.success('编辑成功')
    } else {
      await createPatient(submitData)
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

const handleExport = () => {
  ElMessage.info('导出功能开发中')
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