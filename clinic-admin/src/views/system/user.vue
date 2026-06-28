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
        addText="新增用户"
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="不输入则默认为123456" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色">
            <el-option label="超级管理员" :value="1" />
            <el-option label="诊所管理员" :value="2" />
            <el-option label="医生" :value="3" />
            <el-option label="护士" :value="4" />
            <el-option label="收银员" :value="5" />
            <el-option label="库管员" :value="6" />
          </el-select>
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

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="resetPwdDialogVisible"
      title="重置密码"
      width="400px"
      destroy-on-close
    >
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetPwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwdSubmit" :loading="resetPwdLoading">确定</el-button>
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
  getSysUserList,
  getSysUserInfo,
  createSysUser,
  updateSysUser,
  deleteSysUser,
  resetPassword
} from '@/api/system'
import { useTenantStore } from '@/stores/tenant'
import { useUserStore } from '@/stores/user'

interface SysUser {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  roleId: number
  roleName: string
  clinicId: number
  clinicName: string
  status: number
  createdAt: string
}

const tenantStore = useTenantStore()
const userStore = useUserStore()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const queryParams = reactive({
  keyword: '',
  status: undefined as number | undefined,
  roleId: undefined as number | undefined
})

const tableData = ref<SysUser[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/用户名/手机号' },
  {
    prop: 'roleId',
    label: '角色',
    type: 'select',
    options: [
      { label: '超级管理员', value: 1 },
      { label: '诊所管理员', value: 2 },
      { label: '医生', value: 3 },
      { label: '护士', value: 4 },
      { label: '收银员', value: 5 },
      { label: '库管员', value: 6 }
    ]
  },
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
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  roleId: 3,
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const roleMap: Record<number, string> = {
  1: '超级管理员',
  2: '诊所管理员',
  3: '医生',
  4: '护士',
  5: '收银员',
  6: '库管员'
}

const columns = [
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'realName', label: '真实姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 120 },
  { prop: 'email', label: '邮箱', width: 180 },
  {
    prop: 'clinicName',
    label: '所属诊所',
    width: 150,
    formatter: (row: SysUser) => {
      console.log('clinicName formatter:', row.username, row.clinicName)
      return row.clinicName || '-'
    }
  },
  {
    prop: 'roleName',
    label: '角色',
    width: 100,
    formatter: (row: SysUser) => row.roleName || roleMap[row.roleId] || '-'
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: SysUser) => {
      return row.status === 1 ? '<span style="color: #67C23A">启用</span>' : '<span style="color: #F56C6C">禁用</span>'
    }
  },
  { prop: 'createdAt', label: '创建时间', width: 180 }
]

const operates = [
  { label: '编辑', type: 'primary', action: (row: SysUser) => handleEdit(row) },
  { label: '重置密码', type: 'warning', action: (row: SysUser) => handleResetPwd(row) },
  {
    label: '禁用',
    type: 'danger',
    condition: (row: SysUser) => row.status === 1,
    action: (row: SysUser) => handleToggleStatus(row)
  },
  {
    label: '启用',
    type: 'success',
    condition: (row: SysUser) => row.status !== 1,
    action: (row: SysUser) => handleToggleStatus(row)
  },
  { label: '删除', type: 'danger', action: (row: SysUser) => handleDelete(row) }
]

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = userStore.currentClinicId || tenantStore.currentTenant?.id
    if (!clinicId) {
      ElMessage.warning('未获取到诊所信息')
      return
    }
    const res = await getSysUserList({
      clinicId,
      pageNum: pagination.current,
      pageSize: pagination.size,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status,
      roleId: queryParams.roleId
    })
    console.log('用户列表数据:', res.data.list)
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.roleId = undefined
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
  dialogTitle.value = '新增用户'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    phone: '',
    email: '',
    password: '',
    roleId: 3,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: SysUser) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    password: '',
    roleId: row.roleId,
    status: row.status
  })
  dialogVisible.value = true
}

const handleToggleStatus = async (row: SysUser) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await updateSysUser({
      id: row.id,
      status: row.status === 1 ? 0 : 1
    })
    
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}失败:`, error)
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row: SysUser) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteSysUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    
    const clinicId = userStore.currentClinicId || tenantStore.currentTenant?.id
    if (!clinicId) {
      ElMessage.warning('未获取到诊所信息')
      return
    }
    
    const data = {
      ...form,
      clinicId
    }
    
    if (isEdit.value) {
      await updateSysUser(data)
      ElMessage.success('编辑成功')
    } else {
      await createSysUser(data)
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

// 重置密码相关
const resetPwdDialogVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref<FormInstance>()
const currentUserId = ref<number>(0)

const resetPwdForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const resetPwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== resetPwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleResetPwd = (row: SysUser) => {
  currentUserId.value = row.id
  resetPwdForm.newPassword = ''
  resetPwdForm.confirmPassword = ''
  resetPwdDialogVisible.value = true
}

const handleResetPwdSubmit = async () => {
  try {
    await resetPwdFormRef.value?.validate()
    resetPwdLoading.value = true
    
    await resetPassword({
      userId: currentUserId.value,
      newPassword: resetPwdForm.newPassword
    })
    
    ElMessage.success('密码重置成功')
    resetPwdDialogVisible.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
    ElMessage.error('重置密码失败')
  } finally {
    resetPwdLoading.value = false
  }
}

onMounted(() => {
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
