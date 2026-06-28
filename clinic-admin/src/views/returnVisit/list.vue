<template>
  <div class="page-container">
    <el-row :gutter="16" class="statistics-row">
      <el-col :span="6">
        <div class="stat-card" @click="handleStatClick('PENDING')">
          <div class="stat-value">{{ statistics.pending }}</div>
          <div class="stat-label">待执行</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-info" @click="handleStatClick('today')">
          <div class="stat-value">{{ statistics.todayTotal }}</div>
          <div class="stat-label">今日待回访</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-warning" @click="handleStatClick('overdue')">
          <div class="stat-value">{{ statistics.overdue }}</div>
          <div class="stat-label">逾期未回访</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-success">
          <div class="stat-value">{{ statistics.executed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </el-col>
    </el-row>

    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @add="handleAdd"
        showAdd
        addText="新建回访"
      />
    </el-card>

    <el-card class="table-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待执行" name="PENDING" />
        <el-tab-pane label="已完成" name="EXECUTED" />
        <el-tab-pane label="已取消" name="CANCELLED" />
        <el-tab-pane label="已逾期" name="OVERDUE" />
      </el-tabs>

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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="患者" prop="patientId">
          <el-select
            v-model="form.patientId"
            filterable
            remote
            :remote-method="searchPatients"
            placeholder="搜索患者"
            @change="handlePatientChange"
          >
            <el-option
              v-for="patient in patientOptions"
              :key="patient.id"
              :label="`${patient.patientName} - ${patient.phone}`"
              :value="patient.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊项目" prop="visitItem">
          <el-input v-model="form.visitItem" placeholder="请输入就诊项目" />
        </el-form-item>
        <el-form-item label="计划日期" prop="planDate">
          <el-date-picker
            v-model="form.planDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="计划时间" prop="planTime">
          <el-select v-model="form.planTime" placeholder="请选择时间">
            <el-option label="上午 09:00" value="09:00" />
            <el-option label="上午 10:00" value="10:00" />
            <el-option label="上午 11:00" value="11:00" />
            <el-option label="下午 14:00" value="14:00" />
            <el-option label="下午 15:00" value="15:00" />
            <el-option label="下午 16:00" value="16:00" />
            <el-option label="下午 17:00" value="17:00" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访方式" prop="visitType">
          <el-select v-model="form.visitType" placeholder="请选择回访方式">
            <el-option label="电话" value="PHONE" />
            <el-option label="短信" value="SMS" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="到店" value="STORE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio :label="0">普通</el-radio>
            <el-radio :label="1">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回访内容">
          <el-input v-model="form.contentTemplate" type="textarea" :rows="3" placeholder="请输入回访内容模板" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordDialogVisible" title="提交回访记录" width="600px" destroy-on-close>
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="100px">
        <el-descriptions :column="2" border v-if="currentPlan">
          <el-descriptions-item label="患者">{{ currentPlan.patientName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentPlan.patientPhone }}</el-descriptions-item>
          <el-descriptions-item label="就诊项目">{{ currentPlan.visitItem }}</el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ currentPlan.planDate }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <el-form-item label="回访方式" prop="visitType">
          <el-select v-model="recordForm.visitType" placeholder="请选择回访方式">
            <el-option label="电话" value="PHONE" />
            <el-option label="短信" value="SMS" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="到店" value="STORE" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访时间" prop="visitDate">
          <el-date-picker
            v-model="recordForm.visitDate"
            type="datetime"
            placeholder="选择回访时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="回访结果" prop="result">
          <el-select v-model="recordForm.result" placeholder="请选择回访结果">
            <el-option label="已联系" value="CONTACTED" />
            <el-option label="未接通" value="NO_ANSWER" />
            <el-option label="拒访" value="REFUSED" />
            <el-option label="无效" value="INVALID" />
          </el-select>
        </el-form-item>
        <el-form-item label="满意度">
          <el-rate v-model="recordForm.satisfaction" :max="5" />
        </el-form-item>
        <el-form-item label="回访内容" prop="content">
          <el-input v-model="recordForm.content" type="textarea" :rows="4" placeholder="请输入回访内容" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="recordForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecordSubmit" :loading="submitLoading">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="回访计划详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentPlan">
        <el-descriptions-item label="患者姓名">{{ currentPlan.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者电话">{{ currentPlan.patientPhone }}</el-descriptions-item>
        <el-descriptions-item label="就诊项目">{{ currentPlan.visitItem }}</el-descriptions-item>
        <el-descriptions-item label="计划日期">{{ currentPlan.planDate }}</el-descriptions-item>
        <el-descriptions-item label="计划时间">{{ currentPlan.planTime }}</el-descriptions-item>
        <el-descriptions-item label="回访方式">{{ currentPlan.visitTypeName }}</el-descriptions-item>
        <el-descriptions-item label="分配人">{{ currentPlan.assigneeName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentPlan.status)">{{ currentPlan.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="currentPlan.priority === 1 ? 'danger' : 'info'">
            {{ currentPlan.priority === 1 ? '紧急' : '普通' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentPlan.createdAtStr }}</el-descriptions-item>
        <el-descriptions-item label="回访内容" :span="2">{{ currentPlan.contentTemplate || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleExecute" v-if="currentPlan?.status === 'PENDING'">
          提交回访
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { TableSearch } from '@/components/TableSearch'
import { Table } from '@/components/Table'
import { useUserStore } from '@/stores'
import { getClinicId } from '@/utils/storage'
import { getPatientList } from '@/api/patient'
import {
  getPlanList,
  getPlanById,
  createPlan,
  updatePlan,
  cancelPlan,
  getTodayPlans,
  getOverduePlans,
  getPlanStatistics,
  getRecordList,
  createRecord,
  type ReturnVisitPlan,
  type ReturnVisitRecord
} from '@/api/returnVisit'

const userStore = useUserStore()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const recordDialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const activeTab = ref('all')
const tableData = ref([])
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})
const statistics = reactive({
  pending: 0,
  executed: 0,
  cancelled: 0,
  todayTotal: 0,
  overdue: 0
})

const currentPlan = ref<ReturnVisitPlan | null>(null)
const patientOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const queryParams = reactive({
  clinicId: getClinicId(),
  planType: 'FOLLOW_UP', // 只显示回访类型
  status: '',
  startDate: '',
  endDate: '',
  keyword: '',
  assigneeId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const form = reactive<Partial<ReturnVisitPlan>>({
  clinicId: getClinicId(),
  patientId: undefined,
  patientName: '',
  patientPhone: '',
  visitItem: '',
  planDate: '',
  planTime: '',
  visitType: 'PHONE',
  priority: 0,
  contentTemplate: ''
})

const recordForm = reactive<Partial<ReturnVisitRecord>>({
  clinicId: getClinicId(),
  patientId: undefined,
  patientName: '',
  visitType: 'PHONE',
  visitDate: '',
  result: '',
  satisfaction: 5,
  content: '',
  remark: ''
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }]
}

const recordRules: FormRules = {
  visitType: [{ required: true, message: '请选择回访方式', trigger: 'change' }],
  visitDate: [{ required: true, message: '请选择回访时间', trigger: 'change' }],
  result: [{ required: true, message: '请选择回访结果', trigger: 'change' }],
  content: [{ required: true, message: '请输入回访内容', trigger: 'blur' }]
}

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '患者姓名/电话' },
  { prop: 'status', label: '状态', type: 'select', options: [
    { label: '全部', value: '' },
    { label: '待执行', value: 'PENDING' },
    { label: '已完成', value: 'EXECUTED' },
    { label: '已取消', value: 'CANCELLED' }
  ]},
  { prop: 'startDate', label: '开始日期', type: 'date', valueFormat: 'YYYY-MM-DD' },
  { prop: 'endDate', label: '结束日期', type: 'date', valueFormat: 'YYYY-MM-DD' }
]

const columns = [
  { prop: 'patientName', label: '患者', width: 100 },
  { prop: 'patientPhone', label: '电话', width: 120 },
  { prop: 'visitItem', label: '就诊项目', minWidth: 150 },
  { prop: 'planDate', label: '计划日期', width: 120 },
  { prop: 'planTime', label: '时间', width: 80 },
  { prop: 'visitTypeName', label: '回访方式', width: 80 },
  { prop: 'assigneeName', label: '分配人', width: 80 },
  { prop: 'priority', label: '优先级', width: 80, formatter: (row: any) =>
    row.priority === 1 ? '<span style="color: #f56c6c">紧急</span>' : '普通'
  },
  { prop: 'statusName', label: '状态', width: 80, formatter: (row: any) => {
    const typeMap: Record<string, string> = {
      PENDING: 'warning',
      EXECUTED: 'success',
      CANCELLED: 'info',
      OVERDUE: 'danger'
    }
    return `<el-tag type="${typeMap[row.status] || 'info'}">${row.statusName}</el-tag>`
  }},
  { prop: 'createdAtStr', label: '创建时间', width: 160 }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: any) => handleView(row) },
  { label: '执行', type: 'success', action: (row: any) => handleExecutePlan(row), condition: (row: any) => row.status === 'PENDING' },
  { label: '取消', type: 'warning', action: (row: any) => handleCancel(row), condition: (row: any) => row.status === 'PENDING' }
]

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (activeTab.value !== 'all') {
      params.status = activeTab.value
    }
    const res: any = await getPlanList(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const clinicId = getClinicId()
    if (!clinicId) return
    const res = await getPlanStatistics(clinicId)
    Object.assign(statistics, res.data || {})
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadData()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.startDate = ''
  queryParams.endDate = ''
  queryParams.assigneeId = undefined
  handleSearch()
}

const handlePageChange = (page: number) => {
  queryParams.pageNum = page
  pagination.current = page
  loadData()
}

const handleTabChange = () => {
  queryParams.pageNum = 1
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新建回访计划'
  resetForm()
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  try {
    const res: any = await getPlanById(row.id)
    currentPlan.value = res.data || res
    detailVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

const handleExecutePlan = (row: any) => {
  currentPlan.value = row
  recordForm.clinicId = getClinicId()
  recordForm.planId = row.id
  recordForm.patientId = row.patientId
  recordForm.patientName = row.patientName
  recordForm.visitType = row.visitType || 'PHONE'
  recordForm.visitDate = new Date().format('yyyy-MM-dd hh:mm:ss')
  recordDialogVisible.value = true
}

const handleExecute = () => {
  if (currentPlan.value) {
    handleExecutePlan(currentPlan.value)
  }
}

const handleCancel = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要取消该回访计划吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelPlan(row.id)
    ElMessage.success('取消成功')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消失败:', error)
    }
  }
}

const handleStatClick = (type: string) => {
  if (type === 'today') {
    queryParams.status = 'PENDING'
    queryParams.startDate = new Date().format('yyyy-MM-dd')
    queryParams.endDate = new Date().format('yyyy-MM-dd')
  } else if (type === 'overdue') {
    queryParams.status = ''
    queryParams.endDate = new Date().format('yyyy-MM-dd')
  } else {
    queryParams.status = type
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
  activeTab.value = type === 'today' || type === 'overdue' ? 'all' : type
  loadData()
}

const searchPatients = async (query: string) => {
  console.log('搜索患者输入:', query)
  if (!query) {
    patientOptions.value = []
    return
  }
  try {
    const clinicId = getClinicId()
    console.log('当前诊所ID:', clinicId)
    if (!clinicId) {
      console.warn('未选择诊所')
      return
    }
    const params = {
      clinicId,
      keyword: query,
      pageNum: 1,
      pageSize: 20
    }
    console.log('请求参数:', params)
    const res: any = await getPatientList(params)
    console.log('API返回结果:', res)
    console.log('患者列表:', res.data?.list)
    patientOptions.value = res.data?.list || []
  } catch (error) {
    console.error('搜索患者失败:', error)
  }
}

const handlePatientChange = (patientId: number) => {
  console.log('选择患者ID:', patientId)
  console.log('患者列表:', patientOptions.value)
  const patient = patientOptions.value.find(p => p.id === patientId || p.id === Number(patientId))
  if (patient) {
    form.patientName = patient.patientName
    form.patientPhone = patient.phone
    console.log('设置患者信息:', form.patientName, form.patientPhone)
  } else {
    console.warn('未找到患者:', patientId)
  }
}

const resetForm = () => {
  Object.assign(form, {
    clinicId: getClinicId(),
    patientId: undefined,
    patientName: '',
    patientPhone: '',
    visitItem: '',
    planDate: '',
    planTime: '',
    visitType: 'PHONE',
    priority: 0,
    contentTemplate: ''
  })
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    if (form.id) {
      await updatePlan(form as ReturnVisitPlan)
      ElMessage.success('更新成功')
    } else {
      await createPlan(form as ReturnVisitPlan)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
    loadStatistics()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleRecordSubmit = async () => {
  submitLoading.value = true
  try {
    await createRecord(recordForm as ReturnVisitRecord)
    ElMessage.success('提交成功')
    recordDialogVisible.value = false
    loadData()
    loadStatistics()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    PENDING: 'warning',
    EXECUTED: 'success',
    CANCELLED: 'info',
    OVERDUE: 'danger'
  }
  return typeMap[status] || 'info'
}

declare global {
  interface Date {
    format(fmt: string): string
  }
}

Date.prototype.format = function(fmt: string) {
  const o: any = {
    'M+': this.getMonth() + 1,
    'd+': this.getDate(),
    'h+': this.getHours(),
    'm+': this.getMinutes(),
    's+': this.getSeconds(),
    'q+': Math.floor((this.getMonth() + 3) / 3),
    'S': this.getMilliseconds()
  }
  if (/(y+)/.test(fmt)) {
    fmt = fmt.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length))
  }
  for (const k in o) {
    if (new RegExp('(' + k + ')').test(fmt)) {
      fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)))
    }
  }
  return fmt
}

onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.statistics-row {
  margin-bottom: 16px;
}

.stat-card {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  border-radius: 8px;
  padding: 20px;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
  }

  .stat-value {
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 8px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  }

  .stat-label {
    font-size: 14px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.95);
  }
}

.stat-card-info {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
  }
}

.stat-card-warning {
  background: linear-gradient(135deg, #f87171 0%, #ef4444 100%);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
  }
}

.stat-card-success {
  background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(34, 197, 94, 0.4);
  }
}
</style>
