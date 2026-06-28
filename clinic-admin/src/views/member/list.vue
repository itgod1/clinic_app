<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        showAdd
        addText="会员充值"
      />
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.totalMembers }}</p>
              <p class="stat-label">会员总数</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon :size="24"><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">¥{{ stats.totalBalance }}</p>
              <p class="stat-label">会员储值</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon :size="24"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.totalPoints }}</p>
              <p class="stat-label">会员积分</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.newThisMonth }}</p>
              <p class="stat-label">本月新增</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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

    <el-dialog v-model="rechargeDialogVisible" title="会员充值" width="500px" destroy-on-close>
      <el-form ref="rechargeFormRef" :model="rechargeForm" label-width="100px">
        <el-form-item label="会员姓名">
          <span>{{ currentPatient?.patientName }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span>¥{{ currentPatient?.balance }}</span>
        </el-form-item>
        <el-form-item label="充值金额" prop="amount">
          <el-input-number v-model="rechargeForm.amount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="赠送金额" prop="giftAmount">
          <el-input-number v-model="rechargeForm.giftAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="rechargeForm.paymentMethod">
            <el-option label="现金" :value="1" />
            <el-option label="微信" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="银行卡" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge" :loading="rechargeLoading">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Coin, Tickets, TrendCharts } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getPatientList, type Patient } from '@/api/patient'
import { rechargeMember, type RechargeParams } from '@/api/recharge'
import { getClinicId, getUserInfo } from '@/utils/storage'

const loading = ref(false)
const rechargeLoading = ref(false)
const rechargeDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const rechargeFormRef = ref<FormInstance>()
const currentPatient = ref<Patient | null>(null)

const stats = reactive({
  totalMembers: 0,
  totalBalance: 0,
  totalPoints: 0,
  newThisMonth: 0
})

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  memberLevel: undefined as number | undefined
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
  }
]

const rechargeForm = reactive({
  amount: 0,
  giftAmount: 0,
  paymentMethod: 1,
  remark: ''
})

const memberLevelMap: Record<number, string> = {
  1: '普通会员',
  2: '银卡会员',
  3: '金卡会员',
  4: '钻石会员'
}

const memberLevelTypeMap: Record<number, string> = {
  1: 'info',
  2: 'success',
  3: 'warning',
  4: 'danger'
}

const columns = [
  { prop: 'patientCode', label: '会员编号', width: 140 },
  { prop: 'patientName', label: '姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 120 },
  {
    prop: 'memberLevel',
    label: '等级',
    width: 100,
    formatter: (row: Patient) => {
      const levelName = memberLevelMap[row.memberLevel] || '普通会员'
      const type = memberLevelTypeMap[row.memberLevel] || 'info'
      return h('el-tag', { type, size: 'small' }, () => levelName)
    }
  },
  { prop: 'balance', label: '余额', width: 100, formatter: (row: Patient) => `¥${row.balance}` },
  { prop: 'points', label: '积分', width: 100 },
  { prop: 'visitCount', label: '消费次数', width: 100 },
  { prop: 'lastVisitDate', label: '最后消费', width: 120 },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: Patient) => row.status === 1 ? '正常' : '禁用'
  }
]

const operates = [
  { label: '充值', type: 'primary', action: (row: Patient) => handleOpenRecharge(row) }
]

import { h } from 'vue'

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
    if (queryParams.memberLevel) {
      params.memberLevel = queryParams.memberLevel
    }
    const res = await getPatientList(params)
    tableData.value = res.data.list
    pagination.total = res.data.total

    stats.totalMembers = res.data.total || 0
    stats.totalBalance = res.data.list.reduce((sum: number, p: Patient) => sum + (p.balance || 0), 0)
    stats.totalPoints = res.data.list.reduce((sum: number, p: Patient) => sum + (p.points || 0), 0)
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

const handleOpenRecharge = (row: Patient) => {
  currentPatient.value = row
  rechargeForm.amount = 0
  rechargeForm.giftAmount = 0
  rechargeForm.paymentMethod = 1
  rechargeForm.remark = ''
  rechargeDialogVisible.value = true
}

const handleRecharge = async () => {
  if (rechargeForm.amount <= 0) {
    ElMessage.warning('请输入正确的充值金额')
    return
  }
  if (!currentPatient.value) {
    ElMessage.warning('请选择要充值的会员')
    return
  }
  try {
    rechargeLoading.value = true
    const userInfo = getUserInfo()
    const params: RechargeParams = {
      patientId: currentPatient.value.id,
      amount: rechargeForm.amount,
      giftAmount: rechargeForm.giftAmount || 0,
      paymentMethod: rechargeForm.paymentMethod,
      remark: rechargeForm.remark
    }
    await rechargeMember(params)
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
    loadData()
  } catch (error: any) {
    console.error('充值失败:', error)
    ElMessage.error(error?.response?.data?.message || '充值失败')
  } finally {
    rechargeLoading.value = false
  }
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

  .stat-row {
    margin-bottom: 16px;
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 50px;
      height: 50px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .stat-info {
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #303133;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .table-card {
    background: #fff;
  }
}
</style>