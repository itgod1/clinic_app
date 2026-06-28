<template>
  <div class="page-container insurance-page">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @add="handleAdd"
        showAdd
        addText="新增医保患者"
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
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="关联患者" prop="patientId">
          <el-input-number v-model="form.patientId" :min="1" placeholder="输入患者ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="医保卡号" prop="insuranceCardNo">
          <el-input v-model="form.insuranceCardNo" placeholder="请输入医保卡号" />
        </el-form-item>
        <el-form-item label="险种" prop="insuranceType">
          <el-select v-model="form.insuranceType" placeholder="请选择险种" style="width: 100%">
            <el-option label="职工医保" value="URBAN_EMPLOYEE" />
            <el-option label="居民医保" value="URBAN_RESIDENT" />
            <el-option label="新农合" value="NEW_RURAL" />
            <el-option label="商业保险" value="COMMERCIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="参保城市" prop="insuranceCity">
          <el-input v-model="form.insuranceCity" placeholder="请输入参保城市" />
        </el-form-item>
        <el-form-item label="持卡人姓名" prop="holderName">
          <el-input v-model="form.holderName" placeholder="请输入持卡人姓名" />
        </el-form-item>
        <el-form-item label="持卡人身份证" prop="holderIdCard">
          <el-input v-model="form.holderIdCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-row :gutter="12" style="width: 100%">
            <el-col :span="12">
              <el-date-picker
                v-model="form.validFrom"
                type="date"
                placeholder="开始日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-col>
            <el-col :span="12">
              <el-date-picker
                v-model="form.validTo"
                type="date"
                placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="发卡银行" prop="bankName">
          <el-input v-model="form.bankName" placeholder="请输入发卡银行" />
        </el-form-item>
        <el-form-item label="状态" prop="insuranceStatus">
          <el-radio-group v-model="form.insuranceStatus">
            <el-radio label="ACTIVE">正常</el-radio>
            <el-radio label="SUSPENDED">暂停</el-radio>
            <el-radio label="CANCELLED">注销</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
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
  getInsurancePatientList, deleteInsurancePatient,
  createInsurancePatient, updateInsurancePatient,
  type InsurancePatient
} from '@/api/insurance'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  insuranceType: '',
  insuranceStatus: ''
})

const tableData = ref<InsurancePatient[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '医保卡号/持卡人' },
  {
    prop: 'insuranceType',
    label: '险种',
    type: 'select',
    options: [
      { label: '职工医保', value: 'URBAN_EMPLOYEE' },
      { label: '居民医保', value: 'URBAN_RESIDENT' },
      { label: '新农合', value: 'NEW_RURAL' },
      { label: '商业保险', value: 'COMMERCIAL' }
    ]
  },
  {
    prop: 'insuranceStatus',
    label: '状态',
    type: 'select',
    options: [
      { label: '正常', value: 'ACTIVE' },
      { label: '暂停', value: 'SUSPENDED' },
      { label: '注销', value: 'CANCELLED' }
    ]
  }
]

const form = reactive({
  id: undefined as number | undefined,
  patientId: undefined as number | undefined,
  insuranceCardNo: '',
  insuranceType: 'URBAN_EMPLOYEE',
  insuranceCity: '',
  insuranceStatus: 'ACTIVE',
  holderName: '',
  holderIdCard: '',
  validFrom: '',
  validTo: '',
  bankName: '',
  remark: ''
})

const rules = {
  patientId: [{ required: true, message: '请输入患者ID', trigger: 'blur' }],
  insuranceCardNo: [{ required: true, message: '请输入医保卡号', trigger: 'blur' }],
  insuranceType: [{ required: true, message: '请选择险种', trigger: 'change' }]
}

const columns = [
  { prop: 'patientName', label: '患者姓名', width: 90 },
  { prop: 'insuranceCardNo', label: '医保卡号', width: 160 },
  {
    prop: 'insuranceTypeName',
    label: '险种',
    width: 100,
    formatter: (row: InsurancePatient) => row.insuranceTypeName
  },
  { prop: 'insuranceCity', label: '参保城市', width: 100 },
  { prop: 'holderName', label: '持卡人', width: 90 },
  {
    prop: 'insuranceStatusName',
    label: '状态',
    width: 80,
    formatter: (row: InsurancePatient) => {
      const typeMap: Record<string, string> = { ACTIVE: 'success', SUSPENDED: 'warning', CANCELLED: 'info' }
      return `<el-tag type="${typeMap[row.insuranceStatus] || 'info'}" size="small">${row.insuranceStatusName}</el-tag>`
    }
  },
  { prop: 'validTo', label: '有效期至', width: 110 },
  { prop: 'remark', label: '备注', minWidth: 120 }
]

const operates = [
  { label: '编辑', type: 'warning', action: (row: InsurancePatient) => handleEdit(row) },
  { label: '删除', type: 'danger', action: (row: InsurancePatient) => handleDelete(row) }
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
    const res = await getInsurancePatientList(params)
    tableData.value = res.data.list
    pagination.total = res.data.total
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
  if (size) pagination.size = size
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增医保患者'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    patientId: undefined,
    insuranceCardNo: '',
    insuranceType: 'URBAN_EMPLOYEE',
    insuranceCity: '',
    insuranceStatus: 'ACTIVE',
    holderName: '',
    holderIdCard: '',
    validFrom: '',
    validTo: '',
    bankName: '',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: InsurancePatient) => {
  dialogTitle.value = '编辑医保患者'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    patientId: row.patientId,
    insuranceCardNo: row.insuranceCardNo,
    insuranceType: row.insuranceType,
    insuranceCity: row.insuranceCity,
    insuranceStatus: row.insuranceStatus,
    holderName: row.holderName,
    holderIdCard: row.holderIdCard,
    validFrom: row.validFrom,
    validTo: row.validTo,
    bankName: row.bankName,
    remark: row.remark
  })
  dialogVisible.value = true
}

const handleDelete = async (row: InsurancePatient) => {
  try {
    await ElMessageBox.confirm('确定要删除该医保患者信息吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteInsurancePatient(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
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
      await updateInsurancePatient(submitData)
      ElMessage.success('编辑成功')
    } else {
      await createInsurancePatient(submitData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadData()
})
</script>

<style lang="scss" scoped>
.insurance-page {
  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    background: #fff;
  }
}
</style>
