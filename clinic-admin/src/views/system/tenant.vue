<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="名称/编码/电话" clearable />
        </el-form-item>
        <el-form-item label="营业状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option label="营业中" :value="1" />
            <el-option label="已停业" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">+ 新增租户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="clinicName" label="诊所名称" width="150" />
        <el-table-column prop="clinicCode" label="诊所编码" width="120" />
        <el-table-column label="地址" min-width="200">
          <template #default="{ row }">
            {{ [row.province, row.city, row.district, row.address].filter(Boolean).join(' ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column label="营业状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.businessStatus === 1 ? 'success' : 'info'">
              {{ row.businessStatus === 1 ? '营业中' : '已停业' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serviceEnd" label="服务到期" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.businessStatus === 1 ? 'danger' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              {{ row.businessStatus === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 租户信息对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="诊所名称" prop="clinicName">
              <el-input v-model="form.clinicName" placeholder="请输入诊所名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="诊所编码" prop="clinicCode">
              <el-input v-model="form.clinicCode" placeholder="请输入诊所编码" :disabled="isEdit" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="诊所地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="所在省份">
              <el-input v-model="form.province" placeholder="省份" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所在城市">
              <el-input v-model="form.city" placeholder="城市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所在区县">
              <el-input v-model="form.district" placeholder="区县" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="营业执照号">
              <el-input v-model="form.licenseNo" placeholder="请输入营业执照号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营业状态" prop="businessStatus">
              <el-radio-group v-model="form.businessStatus">
                <el-radio :label="1">营业中</el-radio>
                <el-radio :label="0">已停业</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="服务开始日期">
              <el-date-picker v-model="form.serviceStart" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务结束日期">
              <el-date-picker v-model="form.serviceEnd" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="诊所简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入诊所简介" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建成功提示对话框 -->
    <el-dialog v-model="successDialogVisible" title="租户创建成功" width="400px">
      <div style="padding: 20px;">
        <p style="margin-bottom: 16px;">租户 <strong>{{ createdClinicName }}</strong> 创建成功！</p>
        <p style="margin-bottom: 8px;">已自动创建诊所管理员账号：</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ adminUsername }}</el-descriptions-item>
          <el-descriptions-item label="密码">123456</el-descriptions-item>
          <el-descriptions-item label="角色">诊所管理员</el-descriptions-item>
        </el-descriptions>
        <p style="margin-top: 16px; color: #f56c6c; font-size: 12px;">
          <el-icon><Warning /></el-icon>
          请妥善保管管理员账号信息，建议首次登录后修改密码。
        </p>
      </div>
      <template #footer>
        <el-button type="primary" @click="successDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import {
  getClinicList,
  createClinic,
  updateClinic,
  deleteClinic,
  checkClinicCode,
  type ClinicInfo
} from '@/api/clinic'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 创建成功提示
const successDialogVisible = ref(false)
const createdClinicName = ref('')
const adminUsername = ref('')

const queryParams = reactive({
  keyword: '',
  status: undefined as number | undefined
})

const tableData = ref<ClinicInfo[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: undefined as number | undefined,
  clinicName: '',
  clinicCode: '',
  address: '',
  province: '',
  city: '',
  district: '',
  contactPerson: '',
  contactPhone: '',
  licenseNo: '',
  businessStatus: 1,
  serviceStart: '',
  serviceEnd: '',
  description: ''
})

const validateClinicCode = async (rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入诊所编码'))
    return
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(value)) {
    callback(new Error('编码只能包含字母、数字、下划线和横线'))
    return
  }
  if (isEdit.value) {
    callback()
    return
  }
  try {
    const res = await checkClinicCode(value)
    if (res.data) {
      callback(new Error('诊所编码已存在'))
    } else {
      callback()
    }
  } catch (error) {
    callback()
  }
}

const rules = {
  clinicName: [{ required: true, message: '请输入诊所名称', trigger: 'blur' }],
  clinicCode: [
    { required: true, message: '请输入诊所编码', trigger: 'blur' },
    { validator: validateClinicCode, trigger: 'blur' }
  ],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入诊所地址', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getClinicList({
      pageNum: pagination.current,
      pageSize: pagination.size,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status
    })
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
  pagination.current = 1
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增租户'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    clinicName: '',
    clinicCode: '',
    address: '',
    province: '',
    city: '',
    district: '',
    contactPerson: '',
    contactPhone: '',
    licenseNo: '',
    businessStatus: 1,
    serviceStart: '',
    serviceEnd: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: ClinicInfo) => {
  dialogTitle.value = '编辑租户'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    clinicName: row.clinicName,
    clinicCode: row.clinicCode,
    address: row.address,
    province: row.province,
    city: row.city,
    district: row.district,
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    licenseNo: '',
    businessStatus: row.businessStatus,
    serviceStart: row.serviceStart,
    serviceEnd: row.serviceEnd,
    description: row.description
  })
  dialogVisible.value = true
}

const handleToggleStatus = async (row: ClinicInfo) => {
  const action = row.businessStatus === 1 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该租户吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await updateClinic({
      id: row.id,
      businessStatus: row.businessStatus === 1 ? 0 : 1
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

const handleDelete = async (row: ClinicInfo) => {
  try {
    await ElMessageBox.confirm('确定要删除该租户吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteClinic(row.id)
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

    const data = { ...form }

    if (isEdit.value) {
      await updateClinic(data)
      ElMessage.success('编辑成功')
      dialogVisible.value = false
      loadData()
    } else {
      await createClinic(data)
      // 显示创建成功提示
      createdClinicName.value = form.clinicName
      adminUsername.value = form.clinicCode + '_admin'
      successDialogVisible.value = true
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
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
  padding: 20px;

  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    background: #fff;
  }
}
</style>
