<template>
  <div class="page-container">
    <el-row :gutter="16" class="statistics-row">
      <el-col :span="5">
        <div class="stat-card" @click="handleStatClick('PENDING')">
          <div class="stat-value">{{ statistics.pending }}</div>
          <div class="stat-label">待提醒</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="stat-card stat-card-info" @click="handleStatClick('todayDue')">
          <div class="stat-value">{{ statistics.todayDue }}</div>
          <div class="stat-label">今日到期</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="stat-card stat-card-warning" @click="handleStatClick('overdue')">
          <div class="stat-value">{{ statistics.overdue }}</div>
          <div class="stat-label">已逾期</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="stat-card stat-card-primary" @click="handleStatClick('REMINDED')">
          <div class="stat-value">{{ statistics.reminded }}</div>
          <div class="stat-label">已提醒</div>
        </div>
      </el-col>
      <el-col :span="4">
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
      />
    </el-card>

    <el-card class="table-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待提醒" name="PENDING" />
        <el-tab-pane label="已提醒" name="REMINDED" />
        <el-tab-pane label="已完成" name="EXECUTED" />
        <el-tab-pane label="已取消" name="CANCELLED" />
        <el-tab-pane label="咨询/设计" name="CONSULT" />
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

    <el-dialog v-model="detailVisible" :title="currentPlan?._source === 'medical' ? '复诊提醒详情' : '咨询/设计详情'" width="600px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentPlan">
        <el-descriptions-item label="患者姓名">{{ currentPlan.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者电话">{{ currentPlan.patientPhone }}</el-descriptions-item>
        <el-descriptions-item label="来源">
          <el-tag v-if="currentPlan._source === 'consult'" type="success" size="small">咨询</el-tag>
          <el-tag v-else-if="currentPlan._source === 'design'" type="warning" size="small">设计</el-tag>
          <el-tag v-else type="primary" size="small">病历</el-tag>
        </el-descriptions-item>
        <template v-if="currentPlan._source === 'medical'">
          <el-descriptions-item label="原治疗">{{ currentPlan.originalTreatment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="复诊项目">{{ currentPlan.visitItem }}</el-descriptions-item>
          <el-descriptions-item label="恢复期">{{ currentPlan.recoveryDays ? currentPlan.recoveryDays + '天' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ currentPlan.planDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentPlan.status)">{{ currentPlan.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="距离到期">
            <span :class="getDaysClass(currentPlan.daysUntilDue)">
              {{ currentPlan.daysUntilDue !== undefined ? (currentPlan.daysUntilDue <= 0 ? '已到期' : currentPlan.daysUntilDue + '天后') : '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="提醒次数">{{ currentPlan.remindCount || 0 }}次</el-descriptions-item>
          <el-descriptions-item label="上次提醒">{{ currentPlan.remindedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="复诊说明" :span="2">{{ currentPlan.contentTemplate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentPlan.createdAtStr }}</el-descriptions-item>
        </template>
        <template v-else>
          <el-descriptions-item label="咨询日期">{{ currentPlan._rawConsult?.visitDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="医生">{{ currentPlan._rawConsult?.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划复诊">{{ currentPlan.planDate }}</el-descriptions-item>
          <el-descriptions-item label="距离到期">
            <span :class="getDaysClass(currentPlan.daysUntilDue)">
              {{ currentPlan.daysUntilDue !== undefined ? (currentPlan.daysUntilDue <= 0 ? '已到期' : currentPlan.daysUntilDue + '天后') : '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentPlan.status === 'EXECUTED' ? 'success' : 'warning'" size="small">{{ currentPlan.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提醒次数">{{ currentPlan.remindCount || 0 }}次</el-descriptions-item>
          <el-descriptions-item label="咨询内容" :span="2">{{ currentPlan._rawConsult?.consultationContent || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentPlan._source === 'design'" label="设计要求" :span="2">{{ currentPlan._rawConsult?.designRequirements || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理意见" :span="2">{{ currentPlan._rawConsult?.suggestion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentPlan._rawConsult?.notes || '-' }}</el-descriptions-item>
        </template>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleRemind" v-if="currentPlan?.status === 'PENDING'">
          标记已提醒
        </el-button>
        <el-button type="success" @click="handleComplete" v-if="currentPlan?.status === 'PENDING' || currentPlan?.status === 'REMINDED'">
          标记完成
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
import {
  getRevisitList,
  getRevisitStatistics,
  markRevisitAsReminded,
  markRevisitAsCompleted,
  type RevisitPlan,
  type RevisitStatistics
} from '@/api/returnVisit'
import { getUpcomingRevisits, remindConsultation, completeConsultation, type ConsultationRecord } from '@/api/consultation'

const userStore = useUserStore()
// 统一行类型
interface MergedRow {
  _key: string
  _source: 'medical' | 'consult' | 'design'
  _consultContent?: string
  _planDate?: string
  _displayInfo?: string
  _createdAt?: string
  _rawConsult?: ConsultationRecord
  _rawPlan?: RevisitPlan
  id?: number
  patientName: string
  patientPhone: string
  originalTreatment?: string
  visitItem?: string
  recoveryDays?: number
  planDate?: string
  daysUntilDue?: number
  status?: string
  statusName?: string
  remindCount?: number
  createdAtStr?: string
  remindedAt?: string
  contentTemplate?: string
}

const loading = ref(false)
const detailVisible = ref(false)
const activeTab = ref('all')
const allRows = ref<MergedRow[]>([])
const tableData = ref<MergedRow[]>([])
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})
const statistics = reactive<RevisitStatistics>({
  pending: 0,
  reminded: 0,
  appointed: 0,
  executed: 0,
  cancelled: 0,
  todayDue: 0,
  overdue: 0
})

const currentPlan = ref<MergedRow | null>(null)

const queryParams = reactive({
  clinicId: getClinicId(),
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '患者姓名/电话/复诊项目' },
  { prop: 'status', label: '状态', type: 'select', options: [
    { label: '全部', value: '' },
    { label: '待提醒', value: 'PENDING' },
    { label: '已提醒', value: 'REMINDED' },
    { label: '已完成', value: 'EXECUTED' },
    { label: '已取消', value: 'CANCELLED' }
  ]}
]

const columns = [
  {
    prop: '_source', label: '来源', width: 70, formatter: (row: any) => {
      if (row._source === 'consult') return '<el-tag type="success" size="small">咨询</el-tag>'
      if (row._source === 'design') return '<el-tag type="warning" size="small">设计</el-tag>'
      return '<el-tag type="primary" size="small">病历</el-tag>'
    }
  },
  { prop: 'patientName', label: '患者', width: 100 },
  { prop: 'patientPhone', label: '电话', width: 120 },
  { prop: '_displayInfo', label: '内容', minWidth: 160, formatter: (row: any) => {
    if (row._source === 'medical') {
      const t = (row.originalTreatment || '-') + ' → ' + (row.visitItem || '-')
      return `<span style="font-size:13px;color:#606266">${t}</span>`
    }
    return `<span style="font-size:13px;color:#606266">${row._consultContent || '-'}</span>`
  }},
  { prop: 'recoveryDays', label: '恢复期', width: 80, formatter: (row: any) =>
    row._source === 'medical' ? (row.recoveryDays ? row.recoveryDays + '天' : '-') : '-'
  },
  { prop: 'planDate', label: '计划日期', width: 120, formatter: (row: any) =>
    row._planDate || row.planDate || '-'
  },
  { prop: 'daysUntilDue', label: '距离到期', width: 90, formatter: (row: any) => {
    if (row.daysUntilDue === undefined) return '-'
    if (row.daysUntilDue <= 0) return '<span style="color: #f56c6c; font-weight: bold">已到期</span>'
    if (row.daysUntilDue <= 3) return `<span style="color: #e6a23c; font-weight: bold">${row.daysUntilDue}天</span>`
    return `<span style="color: #67c23a">${row.daysUntilDue}天</span>`
  }},
  { prop: 'statusName', label: '状态', width: 80, formatter: (row: any) => {
    if (row._source !== 'medical') return '<el-tag type="warning" size="small">待跟进</el-tag>'
    const typeMap: Record<string, string> = {
      PENDING: 'warning', REMINDED: 'info', EXECUTED: 'success', CANCELLED: 'info', OVERDUE: 'danger'
    }
    return `<el-tag type="${typeMap[row.status] || 'info'}" size="small">${row.statusName}</el-tag>`
  }},
  { prop: 'remindCount', label: '提醒次数', width: 80, formatter: (row: any) =>
    row._source === 'medical' ? (row.remindCount || 0) + '次' : '-'
  },
  { prop: '_createdAt', label: '创建时间', width: 160 }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: any) => handleView(row) },
  { label: '提醒', type: 'success', action: (row: any) => handleRemindRow(row), condition: (row: any) => row.status === 'PENDING' || row._source !== 'medical' },
  { label: '完成', type: 'warning', action: (row: any) => handleCompleteRow(row), condition: (row: any) => row.status === 'PENDING' || row.status === 'REMINDED' || row._source !== 'medical' }
]

const calcDays = (date: string) => {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const due = new Date(date); due.setHours(0, 0, 0, 0)
  return Math.ceil((due.getTime() - today.getTime()) / 86400000)
}

const loadData = async () => {
  loading.value = true
  try {
    const rows: MergedRow[] = []

    // 1. 病历复诊
    const params = { ...queryParams }
    if (activeTab.value !== 'all' && activeTab.value !== 'CONSULT') {
      params.status = activeTab.value
    }
    if (activeTab.value !== 'CONSULT') {
      try {
        const res: any = await getRevisitList(params)
        const medicalList: RevisitPlan[] = res.data?.records || []
        pagination.total = res.data?.total || 0
        medicalList.forEach((plan: RevisitPlan) => {
          rows.push({
            _key: 'mr-' + plan.id,
            _source: 'medical',
            _rawPlan: plan,
            _planDate: plan.planDate,
            _createdAt: plan.createdAtStr || '',
            id: plan.id,
            patientName: plan.patientName || '',
            patientPhone: plan.patientPhone || '',
            originalTreatment: plan.originalTreatment,
            visitItem: plan.visitItem,
            recoveryDays: plan.recoveryDays,
            planDate: plan.planDate,
            daysUntilDue: plan.daysUntilDue,
            status: plan.status,
            statusName: plan.statusName,
            remindCount: plan.remindCount,
            createdAtStr: plan.createdAtStr,
            remindedAt: plan.remindedAt,
            contentTemplate: plan.contentTemplate
          })
        })
      } catch (e) {
        console.error('加载病历复诊失败:', e)
      }
    }

    // 2. 咨询/设计单复诊
    if (activeTab.value === 'all' || activeTab.value === 'PENDING' || activeTab.value === 'CONSULT') {
      try {
        const clinicId = queryParams.clinicId
        const consultRes = await getUpcomingRevisits({ clinicId })
        const consultList: ConsultationRecord[] = consultRes.data || []
        // 关键词过滤
        let filtered = consultList
        if (queryParams.keyword) {
          const kw = queryParams.keyword.toLowerCase()
          filtered = consultList.filter(c =>
            (c.patientName && c.patientName.includes(kw)) ||
            (c.patientPhone && c.patientPhone.includes(kw)) ||
            (c.recordNo && c.recordNo.toLowerCase().includes(kw))
          )
        }
        // PENDING tab 排除已完成的
        if (activeTab.value === 'PENDING') {
          filtered = filtered.filter(c => c.status !== 2)
        }
        filtered.forEach((c: ConsultationRecord) => {
          if (!c.nextVisitDate) return
          const source = c.recordType === 2 ? 'design' : 'consult' as const
          const isCompleted = c.status === 2
          rows.push({
            _key: 'cs-' + c.id,
            _source: source,
            _rawConsult: c,
            _planDate: c.nextVisitDate,
            _createdAt: c.createdAt || '',
            _consultContent: c.consultationContent || '',
            id: c.id,
            patientName: c.patientName || '',
            patientPhone: c.patientPhone || '',
            planDate: c.nextVisitDate,
            daysUntilDue: calcDays(c.nextVisitDate),
            status: isCompleted ? 'EXECUTED' : 'PENDING',
            statusName: isCompleted ? '已完成' : '待跟进',
            remindCount: c.remindCount || 0,
            remindedAt: c.remindedAt || '',
            contentTemplate: c.suggestion || ''
          })
        })
      } catch (e) {
        console.error('加载咨询复诊失败:', e)
      }
    }

    // 排序
    rows.sort((a, b) => (a.daysUntilDue ?? 999) - (b.daysUntilDue ?? 999))

    allRows.value = rows
    // 前端分页
    pagination.total = rows.length
    const start = (pagination.current - 1) * pagination.size
    tableData.value = rows.slice(start, start + pagination.size)
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
    const res = await getRevisitStatistics(clinicId)
    Object.assign(statistics, res.data || {})

    // 合并咨询/设计单统计
    try {
      const consultRes = await getUpcomingRevisits({ clinicId })
      const consultList: ConsultationRecord[] = consultRes.data || []
      let consultOverdue = 0, consultToday = 0, consultPending = 0
      consultList.forEach(c => {
        if (!c.nextVisitDate) return
        const diff = calcDays(c.nextVisitDate)
        if (diff < 0) consultOverdue++
        else if (diff === 0) consultToday++
        else consultPending++
      })
      statistics.pending += consultPending + consultToday + consultOverdue
      statistics.todayDue += consultToday
      statistics.overdue += consultOverdue
    } catch (e) {
      console.error('加载咨询统计失败:', e)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  pagination.current = 1
  queryParams.keyword = ''
  queryParams.status = ''
  loadData()
}

const handlePageChange = (page: number) => {
  pagination.current = page
  // 前端分页：从 allRows 切片
  const start = (page - 1) * pagination.size
  tableData.value = allRows.value.slice(start, start + pagination.size)
}

const handleTabChange = () => {
  queryParams.pageNum = 1
  pagination.current = 1
  loadData()
}

const handleView = (row: any) => {
  currentPlan.value = row
  detailVisible.value = true
}

const handleRemindRow = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定已联系患者 ${row.patientName} 并提醒复诊吗？`, '确认提醒', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    if (row._source === 'medical') {
      const doctorId = userStore.userInfo?.userId
      if (!doctorId) {
        ElMessage.error('获取医生信息失败')
        return
      }
      await markRevisitAsReminded(row.id, doctorId)
    } else {
      await remindConsultation(row.id)
    }
    ElMessage.success('标记成功')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('标记失败:', error)
    }
  }
}

const handleRemind = async () => {
  if (!currentPlan.value) return
  await handleRemindRow(currentPlan.value)
  detailVisible.value = false
}

const handleCompleteRow = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定患者 ${row.patientName} 已完成复诊吗？`, '确认完成', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    if (row._source === 'medical') {
      const doctorId = userStore.userInfo?.userId
      if (!doctorId) {
        ElMessage.error('获取医生信息失败')
        return
      }
      await markRevisitAsCompleted(row.id, doctorId)
    } else {
      await completeConsultation(row.id)
    }
    ElMessage.success('标记成功')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('标记失败:', error)
    }
  }
}

const handleComplete = async () => {
  if (!currentPlan.value) return
  await handleCompleteRow(currentPlan.value)
  detailVisible.value = false
}

const handleStatClick = (type: string) => {
  if (type === 'todayDue') {
    queryParams.status = 'PENDING'
  } else if (type === 'overdue') {
    queryParams.status = 'PENDING'
  } else {
    queryParams.status = type
  }
  activeTab.value = type === 'todayDue' || type === 'overdue' ? 'PENDING' : type
  loadData()
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    PENDING: 'warning',
    REMINDED: 'info',
    EXECUTED: 'success',
    CANCELLED: 'info',
    OVERDUE: 'danger'
  }
  return typeMap[status] || 'info'
}

const getDaysClass = (days?: number) => {
  if (days === undefined) return ''
  if (days <= 0) return 'text-danger font-bold'
  if (days <= 3) return 'text-warning font-bold'
  return 'text-success'
}

onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: 16px;
}

.statistics-row {
  margin-bottom: 16px;
}

.stat-card {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-radius: 8px;
  padding: 16px;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
  }

  .stat-value {
    font-size: 28px;
    font-weight: bold;
    margin-bottom: 4px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  }

  .stat-label {
    font-size: 13px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.95);
  }
}

.stat-card-info {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
  }
}

.stat-card-warning {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
  }
}

.stat-card-primary {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(14, 165, 233, 0.4);
  }
}

.stat-card-purple {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
  }
}

.stat-card-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);

  &:hover {
    box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
  }
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.text-danger {
  color: #f56c6c;
}

.text-warning {
  color: #e6a23c;
}

.text-success {
  color: #67c23a;
}

.font-bold {
  font-weight: bold;
}
</style>
