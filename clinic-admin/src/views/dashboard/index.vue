<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">今日挂号</p>
              <p class="stat-value">{{ dashboardData.todayRegCount || 0 }}</p>
              <p class="stat-change" :class="dashboardData.todayRegChange >= 0 ? 'up' : 'down'">
                <el-icon><component :is="dashboardData.todayRegChange >= 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
                {{ Math.abs(dashboardData.todayRegChange || 0) }}% 较昨日
              </p>
            </div>
            <div class="stat-icon" style="background: #409eff;">
              <el-icon :size="32"><Calendar /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">待就诊</p>
              <p class="stat-value">{{ dashboardData.waitingCount || 0 }}</p>
              <p class="stat-change" :class="dashboardData.waitingChange >= 0 ? 'up' : 'down'">
                <el-icon><component :is="dashboardData.waitingChange >= 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
                {{ Math.abs(dashboardData.waitingChange || 0) }}% 较昨日
              </p>
            </div>
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon :size="32"><Timer /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">未缴费</p>
              <p class="stat-value">{{ dashboardData.unpaidCount || 0 }}</p>
              <p class="stat-desc">待处理订单</p>
            </div>
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">今日收入</p>
              <p class="stat-value">¥{{ formatMoney(dashboardData.todayRevenue) }}</p>
              <p class="stat-change" :class="dashboardData.todayRevenueChange >= 0 ? 'up' : 'down'">
                <el-icon><component :is="dashboardData.todayRevenueChange >= 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
                {{ Math.abs(dashboardData.todayRevenueChange || 0) }}% 较昨日
              </p>
            </div>
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon :size="32"><Money /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="content-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近7天趋势</span>
              <el-radio-group v-model="chartType" size="small">
                <el-radio-button label="reg">挂号数</el-radio-button>
                <el-radio-button label="revenue">收入</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" style="width: 100%; height: 320px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <!-- 复诊提醒卡片 -->
        <el-card class="revisit-card" :class="{ 'is-urgent': revisitData.overdue > 0 }">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon size="20"><Calendar /></el-icon>
                <span class="title">复诊提醒</span>
                <el-tag type="warning" size="small">待处理 {{ revisitData.totalCount }} 人</el-tag>
                <el-tag v-if="revisitData.todayDue > 0" type="danger" size="small" style="margin-left: 8px">今日到期 {{ revisitData.todayDue }} 人</el-tag>
                <el-tag v-if="revisitData.overdue > 0" type="danger" size="small" style="margin-left: 8px">已逾期 {{ revisitData.overdue }} 人</el-tag>
              </div>
              <el-button type="primary" link @click="goToRevisitPage">查看全部</el-button>
            </div>
          </template>
          <div class="revisit-list">
            <div v-for="item in revisitData.topList" :key="item._key" class="revisit-item">
              <div class="patient-info">
                <span class="patient-name">{{ item.patientName }}</span>
                <span class="patient-phone">{{ item.patientPhone }}</span>
                <el-tag
                  :type="item._source === 'medical' ? 'primary' : item._source === 'design' ? 'warning' : 'success'"
                  size="small"
                  class="source-tag"
                >{{ item._source === 'medical' ? '病历' : item._source === 'design' ? '设计' : '咨询' }}</el-tag>
              </div>
              <div class="treatment-info">
                <template v-if="item._source === 'medical'">
                  <span class="original">{{ item.originalTreatment || '-' }}</span>
                  <el-icon><ArrowRight /></el-icon>
                  <span class="followup">{{ item.visitItem || '-' }}</span>
                </template>
                <template v-else>
                  <span class="consult-content">{{ item._consultContent || '-' }}</span>
                </template>
              </div>
              <div class="date-info" :class="getRevisitItemClass(item)">
                <span v-if="item.daysUntilDue! <= 0">已到期</span>
                <span v-else-if="item.daysUntilDue === 1">明天</span>
                <span v-else>还有 {{ item.daysUntilDue }} 天</span>
              </div>
              <el-button v-if="item._source === 'medical'" type="primary" size="small" @click="handleRevisitRemind(item)">提醒</el-button>
              <el-button v-else type="primary" size="small" link @click="goToConsultationList">查看</el-button>
            </div>
            <el-empty v-if="!revisitData.topList?.length" description="暂无复诊提醒" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 医生接诊排行 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>医生接诊排行</span>
            </div>
          </template>
          <div class="doctor-ranking">
            <div v-for="(doctor, index) in dashboardData.todayTopDoctors" :key="index" class="doctor-item">
              <span class="rank-num" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="doctor-name">{{ doctor.doctorName }}</span>
              <el-progress :percentage="getDoctorPercent(doctor.count)" :stroke-width="8" :show-text="false" />
              <span class="doctor-count">{{ doctor.count }}人</span>
            </div>
            <el-empty v-if="!dashboardData.todayTopDoctors?.length" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>今日热销项目</span>
            </div>
          </template>
          <div class="item-list">
            <div v-for="(item, index) in dashboardData.todayTopItems" :key="index" class="item-row">
              <span class="item-index">{{ index + 1 }}</span>
              <span class="item-name">{{ item.itemName }}</span>
              <span class="item-quantity">x{{ item.totalQuantity }}</span>
              <span class="item-amount">¥{{ formatMoney(item.totalAmount) }}</span>
            </div>
            <el-empty v-if="!dashboardData.todayTopItems?.length" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>库存预警</span>
              <el-tag v-if="dashboardData.lowStockItems?.length" type="danger" size="small">
                {{ dashboardData.lowStockItems.length }}项
              </el-tag>
            </div>
          </template>
          <div class="stock-list">
            <div v-for="(item, index) in dashboardData.lowStockItems" :key="index" class="stock-row">
              <span class="stock-name">{{ item.itemName }}</span>
              <el-progress
                :percentage="getStockPercent(item.stock, item.lowStockAlert)"
                :status="item.stock <= item.lowStockAlert ? 'exception' : 'success'"
                :stroke-width="8"
              />
              <span class="stock-count" :class="{ danger: item.stock <= item.lowStockAlert }">
                {{ item.stock }}/{{ item.lowStockAlert }}
              </span>
            </div>
            <el-empty v-if="!dashboardData.lowStockItems?.length" description="库存充足" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Timer,
  Warning,
  Money,
  ArrowUp,
  ArrowDown,
  ArrowRight
} from '@element-plus/icons-vue'
import { getDashboardStats, type DashboardStats } from '@/api/report'
import { getRevisitDashboard, markRevisitAsReminded, type RevisitPlan } from '@/api/returnVisit'
import { getUpcomingRevisits, type ConsultationRecord } from '@/api/consultation'
import { useTenantStore } from '@/stores/tenant'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'

const router = useRouter()
const tenantStore = useTenantStore()
const userStore = useUserStore()
const loading = ref(false)
const chartType = ref<'reg' | 'revenue'>('reg')
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

// 统一复诊条目
interface RevisitItem {
  _key: string
  _source: 'medical' | 'consult' | 'design'
  _consultContent?: string
  id?: number
  patientName: string
  patientPhone: string
  daysUntilDue?: number
  originalTreatment?: string
  visitItem?: string
}

// 复诊提醒数据
const revisitData = reactive({
  totalCount: 0,
  todayDue: 0,
  overdue: 0,
  topList: [] as RevisitItem[]
})

// 仪表盘数据
const dashboardData = reactive<DashboardStats>({
  todayDate: '',
  todayRegCount: 0,
  todayRegChange: 0,
  waitingCount: 0,
  waitingChange: 0,
  unpaidCount: 0,
  todayRevenue: 0,
  todayRevenueChange: 0,
  weekRegTrend: [],
  weekRevenueTrend: [],
  todayTopDoctors: [],
  todayTopItems: [],
  lowStockItems: []
})

// 医生接诊最大值（用于计算百分比）
const maxDoctorCount = ref(1)

// 格式化金额
const formatMoney = (amount?: number) => {
  if (!amount) return '0.00'
  return amount.toFixed(2)
}

// 获取医生接诊百分比
const getDoctorPercent = (count: number) => {
  if (!maxDoctorCount.value || maxDoctorCount.value === 0) return 0
  return Math.round((count / maxDoctorCount.value) * 100)
}

// 获取库存百分比
const getStockPercent = (stock: number, alert: number) => {
  const max = Math.max(stock, alert) * 1.5
  return Math.round((stock / max) * 100)
}

// 加载仪表盘数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    // 优先使用当前登录用户的诊所ID
    const clinicId = userStore.currentClinicId || tenantStore.currentTenant?.id
    if (!clinicId) {
      ElMessage.warning('未获取到诊所信息')
      return
    }
    const res = await getDashboardStats(clinicId)
    Object.assign(dashboardData, res.data)

    // 计算医生接诊最大值
    if (res.data.todayTopDoctors?.length) {
      maxDoctorCount.value = Math.max(...res.data.todayTopDoctors.map(d => d.count))
    }

    // 初始化图表
    nextTick(() => {
      initTrendChart()
    })
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 计算 daysUntilDue
const calcDaysUntilDue = (date: string) => {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const due = new Date(date); due.setHours(0, 0, 0, 0)
  return Math.ceil((due.getTime() - today.getTime()) / 86400000)
}

// 加载复诊提醒数据（病历 + 咨询/设计单）
const loadRevisitData = async () => {
  try {
    const clinicId = userStore.currentClinicId || tenantStore.currentTenant?.id
    if (!clinicId) return

    const items: RevisitItem[] = []

    // 1. 病历复诊
    try {
      const res: any = await getRevisitDashboard(clinicId)
      if (res.data) {
        revisitData.todayDue = res.data.statistics?.todayDue || 0
        revisitData.overdue = res.data.statistics?.overdue || 0
        const pendingList: RevisitPlan[] = res.data.pending || res.data.todayDue || []
        pendingList.slice(0, 5).forEach((plan: RevisitPlan) => {
          items.push({
            _key: 'mr-' + plan.id,
            _source: 'medical',
            id: plan.id,
            patientName: plan.patientName || '',
            patientPhone: plan.patientPhone || '',
            daysUntilDue: plan.daysUntilDue,
            originalTreatment: plan.originalTreatment,
            visitItem: plan.visitItem
          })
        })
      }
    } catch (e) {
      console.error('加载病历复诊失败:', e)
    }

    // 2. 咨询/设计单复诊
    try {
      const consultRes = await getUpcomingRevisits({ clinicId })
      const consultList: ConsultationRecord[] = consultRes.data || []
      consultList.forEach((c: ConsultationRecord) => {
        if (!c.nextVisitDate) return
        const source = c.recordType === 2 ? 'design' : 'consult'
        items.push({
          _key: 'cs-' + c.id,
          _source: source as 'consult' | 'design',
          _consultContent: c.consultationContent?.slice(0, 30) || '',
          id: c.id,
          patientName: c.patientName || '',
          patientPhone: c.patientPhone || '',
          daysUntilDue: calcDaysUntilDue(c.nextVisitDate)
        })
      })
    } catch (e) {
      console.error('加载咨询复诊失败:', e)
    }

    // 按到期日排序
    items.sort((a, b) => (a.daysUntilDue ?? 999) - (b.daysUntilDue ?? 999))

    // 重新统计（合并两源）
    const today = new Date(); today.setHours(0, 0, 0, 0)
    revisitData.overdue = items.filter(i => (i.daysUntilDue ?? 0) < 0).length
    revisitData.todayDue = items.filter(i => i.daysUntilDue === 0).length
    revisitData.totalCount = items.length
    revisitData.topList = items.slice(0, 6)
  } catch (error) {
    console.error('加载复诊提醒数据失败:', error)
  }
}

// 跳转到复诊提醒页面
const goToRevisitPage = () => {
  router.push('/returnVisit/revisit')
}

// 跳转到复诊提醒页面（包含咨询/设计单）
const goToConsultationList = () => {
  router.push('/returnVisit/revisit')
}

// 标记复诊提醒
const handleRevisitRemind = async (item: RevisitItem) => {
  try {
    const doctorId = userStore.userInfo?.userId
    if (!doctorId) {
      ElMessage.error('获取医生信息失败')
      return
    }
    await markRevisitAsReminded(item.id!, doctorId)
    ElMessage.success('标记成功')
    loadRevisitData()
  } catch (error) {
    console.error('标记失败:', error)
    ElMessage.error('标记失败')
  }
}

// 获取复诊提醒项样式
const getRevisitItemClass = (item: RevisitItem) => {
  if (item.daysUntilDue === undefined) return ''
  if (item.daysUntilDue <= 0) return 'is-overdue'
  if (item.daysUntilDue <= 3) return 'is-soon'
  return ''
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)

  const isRevenue = chartType.value === 'revenue'
  const data = isRevenue ? dashboardData.weekRevenueTrend : dashboardData.weekRegTrend
  const dates = data?.map(item => {
    const date = new Date(item.date)
    return `${date.getMonth() + 1}/${date.getDate()}`
  }) || []
  const values = data?.map(item => isRevenue ? (item as any).amount : (item as any).count) || []

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const value = params[0].value
        return `${params[0].name}<br/>${isRevenue ? '收入: ¥' + value.toFixed(2) : '挂号: ' + value + '人'}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: {
        color: '#606266',
        formatter: isRevenue ? (value: number) => '¥' + (value / 1000).toFixed(1) + 'k' : '{value}'
      }
    },
    series: [{
      data: values,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: isRevenue ? 'rgba(103, 194, 58, 0.3)' : 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: isRevenue ? 'rgba(103, 194, 58, 0.05)' : 'rgba(64, 158, 255, 0.05)' }
        ])
      },
      lineStyle: { color: isRevenue ? '#67c23a' : '#409eff', width: 2 },
      itemStyle: { color: isRevenue ? '#67c23a' : '#409eff' }
    }]
  }

  trendChart.setOption(option)
}

// 监听图表类型变化
watch(chartType, () => {
  initTrendChart()
})

// 窗口大小变化时重新渲染图表
window.addEventListener('resize', () => {
  trendChart?.resize()
})

onMounted(() => {
  loadDashboardData()
  loadRevisitData()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;

  .stat-cards {
    margin-bottom: 20px;

    .stat-card {
      margin-bottom: 20px;

      .stat-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px;

        .stat-info {
          flex: 1;

          .stat-title {
            color: #909399;
            font-size: 14px;
            margin-bottom: 8px;
          }

          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
            margin-bottom: 8px;
          }

          .stat-change {
            font-size: 12px;
            display: flex;
            align-items: center;
            gap: 4px;

            &.up {
              color: #67c23a;
            }

            &.down {
              color: #f56c6c;
            }
          }

          .stat-desc {
            font-size: 12px;
            color: #909399;
          }
        }

        .stat-icon {
          width: 60px;
          height: 60px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
        }
      }
    }
  }

  .content-row {
    margin-bottom: 20px;

    .chart-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 600;
        font-size: 16px;
      }
    }
  }

  .doctor-ranking {
    .doctor-item {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f2f5;
      gap: 12px;

      &:last-child {
        border-bottom: none;
      }

      .rank-num {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #f0f2f5;
        color: #909399;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        flex-shrink: 0;

        &.top {
          background: #409eff;
          color: #fff;
        }
      }

      .doctor-name {
        width: 80px;
        color: #303133;
        font-size: 14px;
        flex-shrink: 0;
      }

      :deep(.el-progress) {
        flex: 1;
      }

      .doctor-count {
        width: 50px;
        color: #909399;
        font-size: 14px;
        text-align: right;
        flex-shrink: 0;
      }
    }
  }

  .item-list {
    .item-row {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f2f5;
      gap: 12px;

      &:last-child {
        border-bottom: none;
      }

      .item-index {
        width: 24px;
        height: 24px;
        border-radius: 4px;
        background: #f0f2f5;
        color: #606266;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        flex-shrink: 0;
      }

      .item-name {
        flex: 1;
        color: #303133;
        font-size: 14px;
      }

      .item-quantity {
        color: #909399;
        font-size: 13px;
      }

      .item-amount {
        width: 80px;
        color: #f56c6c;
        font-size: 14px;
        font-weight: 500;
        text-align: right;
      }
    }
  }

  .stock-list {
    .stock-row {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f2f5;
      gap: 12px;

      &:last-child {
        border-bottom: none;
      }

      .stock-name {
        width: 120px;
        color: #303133;
        font-size: 14px;
        flex-shrink: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      :deep(.el-progress) {
        flex: 1;
      }

      .stock-count {
        width: 70px;
        color: #67c23a;
        font-size: 13px;
        text-align: right;
        flex-shrink: 0;

        &.danger {
          color: #f56c6c;
        }
      }
    }
  }

  // 复诊提醒卡片样式
  .revisit-card {
    margin-bottom: 20px;

    &.is-urgent {
      border: 1px solid #f56c6c;

      :deep(.el-card__header) {
        background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
      }
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .title {
          font-weight: 600;
          font-size: 16px;
        }
      }
    }

    .revisit-list {
      .revisit-item {
        display: flex;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid #f0f2f5;
        gap: 16px;

        &:last-child {
          border-bottom: none;
        }

        .patient-info {
          width: 200px;
          flex-shrink: 0;

          .source-tag {
            margin-left: 6px;
            vertical-align: middle;
          }

          .patient-name {
            font-weight: 500;
            color: #303133;
            font-size: 14px;
          }

          .patient-phone {
            color: #909399;
            font-size: 12px;
            margin-left: 8px;
          }
        }

        .treatment-info {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 8px;

          .original {
            color: #606266;
            font-size: 13px;
          }

          .followup {
            color: #409eff;
            font-size: 13px;
            font-weight: 500;
          }

          .consult-content {
            color: #606266;
            font-size: 13px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 180px;
          }
        }

        .date-info {
          width: 100px;
          text-align: center;
          font-size: 13px;
          color: #67c23a;

          &.is-soon {
            color: #e6a23c;
            font-weight: 500;
          }

          &.is-overdue {
            color: #f56c6c;
            font-weight: bold;
          }
        }
      }
    }
  }
}
</style>
