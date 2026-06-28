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
        addText="新增科室"
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
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="科室名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入科室名称" />
        </el-form-item>
        <el-form-item label="科室编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="请输入科室编码" />
        </el-form-item>
        <el-form-item label="科室类型" prop="deptType">
          <el-select v-model="form.deptType" placeholder="请选择科室类型">
            <el-option label="口腔科" :value="1" />
            <el-option label="正畸科" :value="2" />
            <el-option label="种植科" :value="3" />
            <el-option label="儿童牙科" :value="4" />
            <el-option label="修复科" :value="5" />
            <el-option label="牙周科" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import {
  getDepartmentList,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  type Department
} from '@/api/department'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const queryParams = reactive({
  keyword: ''
})

const tableData = ref<Department[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '科室名称/编码' }
]

const form = reactive({
  id: undefined as number | undefined,
  deptName: '',
  deptCode: '',
  deptType: 1,
  sortOrder: 0,
  status: 1
})

const rules = {
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }],
  deptType: [{ required: true, message: '请选择科室类型', trigger: 'change' }]
}

const deptTypeMap: Record<number, string> = {
  1: '口腔科',
  2: '正畸科',
  3: '种植科',
  4: '儿童牙科',
  5: '修复科',
  6: '牙周科'
}

const getDeptTypeTagType = (deptType: number): string => {
  const types: Record<number, string> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'danger',
    5: 'info',
    6: ''
  }
  return types[deptType] || ''
}

const columns = [
  { prop: 'deptCode', label: '科室编码', minWidth: 100 },
  { prop: 'deptName', label: '科室名称', minWidth: 120 },
  {
    prop: 'deptType',
    label: '科室类型',
    minWidth: 100,
    formatter: (row: Department) => {
      return h('el-tag', { type: getDeptTypeTagType(row.deptType), size: 'small' }, () => deptTypeMap[row.deptType] || '-')
    }
  },
  { prop: 'sortOrder', label: '排序', width: 80 },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: Department) => {
      return h('el-tag', { type: row.status === 1 ? 'success' : 'info', size: 'small' }, () => row.status === 1 ? '启用' : '禁用')
    }
  },
  { prop: 'createdAt', label: '创建时间', minWidth: 160 }
]

const operates = [
  { label: '编辑', type: 'warning', action: (row: Department) => handleEdit(row) },
  {
    label: '删除',
    type: 'danger',
    action: (row: Department) => handleDelete(row)
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    if (!clinicId) {
      tableData.value = []
      pagination.total = 0
      return
    }
    const params: any = { clinicId }
    // 添加搜索关键词
    if (queryParams.keyword) {
      params.keyword = queryParams.keyword
    }
    const res = await getDepartmentList(params)
    // 后端返回的是 { total, list } 结构
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
    tableData.value = []
    pagination.total = 0
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
  dialogTitle.value = '新增科室'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    deptName: '',
    deptCode: '',
    deptType: 1,
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: Department) => {
  dialogTitle.value = '编辑科室'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    deptName: row.deptName,
    deptCode: row.deptCode,
    deptType: row.deptType,
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = async (row: Department) => {
  try {
    await ElMessageBox.confirm('确定要删除该科室吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDepartment(row.id)
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
    const clinicId = getClinicId()
    if (!clinicId) {
      ElMessage.error('获取诊所信息失败')
      return
    }
    if (isEdit.value) {
      await updateDepartment({ ...form, clinicId })
      ElMessage.success('编辑成功')
    } else {
      await createDepartment({ ...form, clinicId })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  width: 100%;

  .search-card {
    margin-bottom: 16px;
    width: 100%;
  }

  .table-card {
    background: #fff;
    width: 100%;

    :deep(.el-card__body) {
      width: 100%;
      padding: 0;
    }
  }
}
</style>
