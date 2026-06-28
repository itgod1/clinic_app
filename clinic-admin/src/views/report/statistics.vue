<template>
  <div class="page-container">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.todayRegCount }}</p>
              <p class="stat-label">今日挂号</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon :size="24"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.waitingCount }}</p>
              <p class="stat-label">待就诊</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon :size="24"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">¥{{ stats.todayRevenue }}</p>
              <p class="stat-label">今日收入</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon :size="24"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.unpaidCount }}</p>
              <p class="stat-label">待缴费</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="14">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>本周就诊趋势</span>
            </div>
          </template>
          <div ref="weekChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>医生接诊排行</span>
            </div>
          </template>
          <div class="doctor-ranking">
            <div v-for="(doctor, index) in topDoctors" :key="index" class="doctor-item">
              <span class="rank-num" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="doctor-name">{{ doctor.doctorName }}</span>
              <span class="doctor-count">{{ doctor.count }} 人</span>
            </div>
            <el-empty v-if="topDoctors.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>本周收入趋势</span>
            </div>
          </template>
          <div ref="revenueChartRef" style="width: 100%; height: 280px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>低库存预警</span>
            </div>
          </template>
          <div class="low-stock-list">
            <div v-for="(item, index) in lowStockItems" :key="index" class="stock-item">
              <span class="item-name">{{ item.itemName }}</span>
              <el-tag type="danger" size="small">库存: {{ item.stock }}</el-tag>
            </div>
            <el-empty v-if="lowStockItems.length === 0" description="暂无预警" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>日常报表</span>
          <el-date-picker
            v-model="reportDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            @change="loadDailyReport"
            style="width: 200px"
          />
        </div>
      </template>
      <el-descriptions :column="3" border v-if="dailyReport">
        <el-descriptions-item label="挂号数">{{ dailyReport.regCount }}</el-descriptions-item>
        <el-descriptions-item label="取消挂号">{{ dailyReport.regCancelCount }}</el-descriptions-item>
        <el-descriptions-item label="实际就诊">{{ dailyReport.actualVisitCount }}</el-descriptions-item>
        <el-descriptions-item label="初诊">{{ dailyReport.newPatientCount }}</el-descriptions-item>
        <el-descriptions-item label="复诊">{{ dailyReport.oldPatientCount }}</el-descriptions-item>
        <el-descriptions-item label="处方数">{{ dailyReport.prescriptionCount }}</el-descriptions-item>
        <el-descriptions-item label="处方金额">¥{{ dailyReport.prescriptionAmount }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">-¥{{ dailyReport.discountAmount }}</el-descriptions-item>
        <el-descriptions-item label="实收金额">
          <span class="actual-amount">¥{{ dailyReport.actualAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="现金">¥{{ dailyReport.cashAmount }}</el-descriptions-item>
        <el-descriptions-item label="微信">¥{{ dailyReport.wechatAmount }}</el-descriptions-item>
        <el-descriptions-item label="支付宝">¥{{ dailyReport.alipayAmount }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="请选择日期查看日报表" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TrendCharts, Clock, Money, Warning } from '@element-plus/icons-vue'
import { getDashboardStats, getDailyReport, type DashboardStats, type DailyReport } from '@/api/report'
import { getClinicId } from '@/utils/storage'
import * as echarts from 'echarts'

const stats = reactive({
  todayRegCount: 0,
  waitingCount: 0,
  unpaidCount: 0,
  todayRevenue: 0
})

const topDoctors = ref<{ doctorName: string; count: number }[]>([])
const lowStockItems = ref<{ itemName: string; stock: number }[]>([])
const weekChartRef = ref<HTMLElement>()
const revenueChartRef = ref<HTMLElement>()

const reportDate = ref('')
const dailyReport = ref<DailyReport | null>(null)

const initWeekChart = (data: { date: string; count: number }[]) => {
  if (!weekChartRef.value) return
  const chart = echarts.init(weekChartRef.value)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [{
      data: data.map(d => d.count),
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      },
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' }
    }]
  }
  chart.setOption(option)
}

const initRevenueChart = (data: { date: string; amount: number }[]) => {
  if (!revenueChartRef.value) return
  const chart = echarts.init(revenueChartRef.value)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [{
      data: data.map(d => d.amount),
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#67c23a' },
          { offset: 1, color: '#95d475' }
        ])
      }
    }]
  }
  chart.setOption(option)
}

const loadData = async () => {
  const clinicId = getClinicId()
  if (!clinicId) return

  try {
    const res = await getDashboardStats(clinicId)
    const data = res.data

    stats.todayRegCount = data.todayRegCount
    stats.waitingCount = data.waitingCount
    stats.unpaidCount = data.unpaidCount
    stats.todayRevenue = data.todayRevenue

    topDoctors.value = data.todayTopDoctors || []
    lowStockItems.value = data.lowStockItems || []

    if (data.weekRegTrend && data.weekRegTrend.length > 0) {
      initWeekChart(data.weekRegTrend)
    }

    if (data.weekRevenueTrend && data.weekRevenueTrend.length > 0) {
      initRevenueChart(data.weekRevenueTrend)
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

const loadDailyReport = async () => {
  if (!reportDate.value) return

  const clinicId = getClinicId()
  if (!clinicId) return

  try {
    const res = await getDailyReport({
      clinicId,
      date: reportDate.value
    })
    dailyReport.value = res.data
  } catch (error) {
    console.error('加载日报表失败:', error)
  }
}

onMounted(() => {
  const today = new Date()
  reportDate.value = today.toISOString().split('T')[0]
  loadData()
  loadDailyReport()
})
</script>

<style lang="scss" scoped>
.page-container {
  .stat-row {
    margin-bottom: 20px;
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

  .content-row {
    margin-bottom: 20px;
  }

  .chart-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .doctor-ranking {
    .doctor-item {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f2f5;

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
        margin-right: 12px;

        &.top {
          background: #409eff;
          color: #fff;
        }
      }

      .doctor-name {
        flex: 1;
        color: #303133;
      }

      .doctor-count {
        color: #909399;
        font-size: 14px;
      }
    }
  }

  .low-stock-list {
    .stock-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f2f5;

      &:last-child {
        border-bottom: none;
      }

      .item-name {
        color: #303133;
      }
    }
  }

  .table-card {
    .actual-amount {
      color: #f56c6c;
      font-weight: bold;
      font-size: 16px;
    }
  }
}
</style>