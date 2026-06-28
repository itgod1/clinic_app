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
        addText="新增目录项"
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
      width="650px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目录类型" prop="catalogType">
              <el-select v-model="form.catalogType" placeholder="请选择类型" style="width: 100%">
                <el-option label="药品" value="DRUG" />
                <el-option label="诊疗" value="TREATMENT" />
                <el-option label="耗材" value="MATERIAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="甲乙丙类" prop="insuranceCategory">
              <el-select v-model="form.insuranceCategory" placeholder="请选择类别" style="width: 100%">
                <el-option label="甲类" value="A" />
                <el-option label="乙类" value="B" />
                <el-option label="丙类" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="医保项目编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="请输入医保项目编码" />
        </el-form-item>
        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" placeholder="规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="单位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自付比例" prop="selfPayRatio">
              <el-input-number v-model="form.selfPayRatio" :min="0" :max="1" :precision="4" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="生产厂家" prop="manufacturer">
          <el-input v-model="form.manufacturer" placeholder="生产厂家" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="生效日期" prop="effectiveDate">
              <el-date-picker v-model="form.effectiveDate" type="date" placeholder="生效日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效日期" prop="expireDate">
              <el-date-picker v-model="form.expireDate" type="date" placeholder="失效日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="启用状态" prop="status">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="禁用" />
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
  getInsuranceCatalogList, deleteInsuranceCatalog,
  createInsuranceCatalog, updateInsuranceCatalog,
  type InsuranceCatalog
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
  catalogType: '',
  keyword: '',
  status: undefined as number | undefined
})

const tableData = ref<InsuranceCatalog[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  {
    prop: 'catalogType',
    label: '目录类型',
    type: 'select',
    options: [
      { label: '药品', value: 'DRUG' },
      { label: '诊疗', value: 'TREATMENT' },
      { label: '耗材', value: 'MATERIAL' }
    ]
  },
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '名称/编码' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const form = reactive({
  id: undefined as number | undefined,
  catalogType: 'DRUG',
  itemCode: '',
  itemName: '',
  specification: '',
  manufacturer: '',
  unit: '',
  unitPrice: 0,
  selfPayRatio: 0,
  insuranceCategory: 'A',
  dosageForm: '',
  effectiveDate: '',
  expireDate: '',
  statusBool: true
})

const rules = {
  catalogType: [{ required: true, message: '请选择目录类型', trigger: 'change' }],
  itemCode: [{ required: true, message: '请输入医保项目编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  insuranceCategory: [{ required: true, message: '请选择甲乙丙类', trigger: 'change' }]
}

const columns = [
  { prop: 'catalogTypeName', label: '类型', width: 70 },
  { prop: 'itemCode', label: '医保编码', width: 130 },
  { prop: 'itemName', label: '项目名称', minWidth: 150 },
  { prop: 'specification', label: '规格', width: 100 },
  { prop: 'unit', label: '单位', width: 60 },
  { prop: 'unitPrice', label: '单价', width: 80 },
  {
    prop: 'selfPayRatio',
    label: '自付比例',
    width: 90,
    formatter: (row: InsuranceCatalog) => `${(row.selfPayRatio * 100).toFixed(0)}%`
  },
  {
    prop: 'insuranceCategoryName',
    label: '类别',
    width: 70,
    formatter: (row: InsuranceCatalog) => row.insuranceCategoryName
  },
  {
    prop: 'status',
    label: '状态',
    width: 70,
    formatter: (row: InsuranceCatalog) => `<el-tag type="${row.status === 1 ? 'success' : 'info'}" size="small">${row.status === 1 ? '启用' : '禁用'}</el-tag>`
  }
]

const operates = [
  { label: '编辑', type: 'warning', action: (row: InsuranceCatalog) => handleEdit(row) },
  { label: '删除', type: 'danger', action: (row: InsuranceCatalog) => handleDelete(row) }
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
    if (!params.status && params.status !== 0) delete params.status
    const res = await getInsuranceCatalogList(params)
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
  dialogTitle.value = '新增目录项'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    catalogType: 'DRUG',
    itemCode: '',
    itemName: '',
    specification: '',
    manufacturer: '',
    unit: '',
    unitPrice: 0,
    selfPayRatio: 0,
    insuranceCategory: 'A',
    dosageForm: '',
    effectiveDate: '',
    expireDate: '',
    statusBool: true
  })
  dialogVisible.value = true
}

const handleEdit = (row: InsuranceCatalog) => {
  dialogTitle.value = '编辑目录项'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    catalogType: row.catalogType,
    itemCode: row.itemCode,
    itemName: row.itemName,
    specification: row.specification,
    manufacturer: row.manufacturer,
    unit: row.unit,
    unitPrice: row.unitPrice,
    selfPayRatio: row.selfPayRatio,
    insuranceCategory: row.insuranceCategory,
    dosageForm: row.dosageForm,
    effectiveDate: row.effectiveDate,
    expireDate: row.expireDate,
    statusBool: row.status === 1
  })
  dialogVisible.value = true
}

const handleDelete = async (row: InsuranceCatalog) => {
  try {
    await ElMessageBox.confirm('确定要删除该目录项吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteInsuranceCatalog(row.id)
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
      clinicId: getClinicId(),
      status: form.statusBool ? 1 : 0
    }
    delete (submitData as any).statusBool

    if (isEdit.value) {
      await updateInsuranceCatalog(submitData)
      ElMessage.success('编辑成功')
    } else {
      await createInsuranceCatalog(submitData)
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
