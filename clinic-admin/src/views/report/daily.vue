<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="日期">
          <el-date-picker
            v-model="queryForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="6" v-for="stat in stats" :key="stat.label">
        <el-card class="stat-card" :class="stat.class">
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-detail" v-if="stat.detail">{{ stat.detail }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getDailyReport, type DailyReport } from '@/api/report'
import { useTenantStore } from '@/stores/tenant'
import { useUserStore } from '@/stores/user'

const tenantStore = useTenantStore()
const userStore = useUserStore()
const loading = ref(false)

const queryForm = reactive({
  date: new Date().toISOString().split('T')[0]
})

const reportData = reactive<DailyReport>({
  reportDate: '',
  regCount: 0,
  regCancelCount: 0,
  actualVisitCount: 0,
  newPatientCount: 0,
  oldPatientCount: 0,
  prescriptionCount: 0,
  prescriptionAmount: 0,
  discountAmount: 0,
  actualAmount: 0,
  cashAmount: 0,
  wechatAmount: 0,
  alipayAmount: 0,
  cardAmount: 0,
  revenueDetails: []
})

const disabledDate = (time: Date) => time.getTime() > Date.now()
const formatMoney = (amount?: number) => amount ? amount.toFixed(2) : '0.00'

const stats = computed(() => [
  { label: '挂号总数', value: reportData.regCount || 0, detail: `已就诊${reportData.actualVisitCount || 0} / 已取消${reportData.regCancelCount || 0}` },
  { label: '患者统计', value: (reportData.newPatientCount || 0) + (reportData.oldPatientCount || 0), detail: `新患者${reportData.newPatientCount || 0} / 复诊${reportData.oldPatientCount || 0}` },
  { label: '处方数量', value: reportData.prescriptionCount || 0, detail: `金额 ¥${formatMoney(reportData.prescriptionAmount)}` },
  { label: '实收金额', value: `¥${formatMoney(reportData.actualAmount)}`, detail: `优惠 ¥${formatMoney(reportData.discountAmount)}`, class: 'highlight' }
])

const loadReportData = async () => {
  loading.value = true
  try {
    const clinicId = userStore.currentClinicId || tenantStore.currentTenant?.id
    if (!clinicId) {
      ElMessage.warning('未获取到诊所信息')
      return
    }
    const res = await getDailyReport({ clinicId, date: queryForm.date })
    Object.assign(reportData, res.data)
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadReportData()

onMounted(() => loadReportData())
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;

  .search-card {
    margin-bottom: 20px;
  }

  .stat-row {
    .stat-card {
      margin-bottom: 20px;
      text-align: center;

      &.highlight {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;

        .stat-label, .stat-detail {
          color: rgba(255, 255, 255, 0.8);
        }
      }

      .stat-label {
        color: #909399;
        font-size: 14px;
        margin-bottom: 10px;
      }

      .stat-value {
        font-size: 32px;
        font-weight: bold;
        margin-bottom: 10px;
      }

      .stat-detail {
        color: #606266;
        font-size: 12px;
      }
    }
  }
}
</style>
