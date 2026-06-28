<template>
  <div class="treat-container">
    <el-card class="patient-card">
      <template #header>
        <div class="card-header">
          <span>患者信息</span>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>
      <el-descriptions :column="4">
        <el-descriptions-item label="患者姓名">{{ patientName }}</el-descriptions-item>
        <el-descriptions-item label="挂号编号">{{ regId }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">{{ currentTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="medical-card">
      <template #header>
        <div class="card-header">
          <span>病历信息</span>
        </div>
      </template>
      <el-form :model="medicalForm" label-width="80px">
        <el-form-item label="诊断">
          <el-input
            v-model="medicalForm.diagnosis"
            type="textarea"
            rows="2"
            placeholder="请输入诊断结果"
          />
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-input
            v-model="medicalForm.treatment"
            type="textarea"
            rows="2"
            placeholder="请输入治疗方案"
          />
        </el-form-item>
        <el-form-item label="医嘱">
          <el-input
            v-model="medicalForm.medicalAdvice"
            type="textarea"
            rows="2"
            placeholder="请输入医嘱"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="tooth-chart-card">
      <template #header>
        <div class="card-header">
          <span>牙位图</span>
        </div>
      </template>
      <ToothChart v-model="toothChartRecords" />
    </el-card>

    <el-card class="prescription-card">
      <template #header>
        <div class="card-header">
          <span>处方信息</span>
          <div class="header-right">
            <el-radio-group v-model="prescriptionType" size="small">
              <el-radio-button :label="1">西药处方</el-radio-button>
              <el-radio-button :label="2">中药处方</el-radio-button>
              <el-radio-button :label="3">检查单</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="small" @click="showDrugDialog" style="margin-left: 10px;">
              <el-icon><Plus /></el-icon>添加药品
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="prescriptionItems" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="itemName" label="药品名称" min-width="150" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="数量" width="120">
          <template #default="{ row, $index }">
            <el-input-number v-model="row.quantity" :min="1" size="small" @change="calcSubtotal($index)" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">
            ¥{{ row.unitPrice }}
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100">
          <template #default="{ row }">
            ¥{{ row.subtotal }}
          </template>
        </el-table-column>
        <el-table-column label="用法" width="150">
          <template #default="{ row }">
            <el-input v-model="row.usage" placeholder="如：口服" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="频次" width="120">
          <template #default="{ row }">
            <el-select v-model="row.frequency" placeholder="频次" size="small" clearable>
              <el-option label="每日一次" value="每日一次" />
              <el-option label="每日两次" value="每日两次" />
              <el-option label="每日三次" value="每日三次" />
              <el-option label="每日四次" value="每日四次" />
              <el-option label="睡前服用" value="睡前服用" />
              <el-option label="必要时" value="必要时" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="疗程" width="120">
          <template #default="{ row }">
            <el-input v-model="row.duration" placeholder="如：7天" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" size="small" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="amount-summary">
        <span class="label">处方金额：</span>
        <span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
    </el-card>

    <div class="action-bar">
      <el-button @click="goBack">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitPrescription">
        保存处方
      </el-button>
    </div>

    <!-- 选择药品对话框 -->
    <el-dialog v-model="drugDialogVisible" title="选择药品" width="900px" destroy-on-close>
      <el-form :inline="true" :model="drugQuery" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="drugQuery.keyword" placeholder="名称/编码" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="drugQuery.itemType" placeholder="全部" clearable>
            <el-option label="西药" :value="1" />
            <el-option label="中成药" :value="2" />
            <el-option label="中药饮片" :value="3" />
            <el-option label="耗材" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDrugList">查询</el-button>
          <el-button @click="resetDrugQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="drugList" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="itemCode" label="编码" width="120" />
        <el-table-column prop="itemName" label="名称" min-width="150" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="retailPrice" label="零售价" width="100">
          <template #default="{ row }">
            ¥{{ row.retailPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="drugPagination.current"
          v-model:page-size="drugPagination.size"
          :total="drugPagination.total"
          layout="total, prev, pager, next"
          @change="loadDrugList"
        />
      </div>

      <template #footer>
        <el-button @click="drugDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="selectedDrugs.length === 0" @click="confirmAddDrugs">
          确认添加 ({{ selectedDrugs.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createPrescription, type Prescription, type PrescriptionItem } from '@/api/prescription'
import { getStockList, type StockItem } from '@/api/stock'
import { getClinicId, getUserInfo } from '@/utils/storage'
import ToothChart from '@/components/ToothChart.vue'
import { saveToothChart, type ToothChartRecord } from '@/api/toothChart'
import { createMedicalRecord as createMedicalRecordApi } from '@/api/medicalRecord'

const route = useRoute()
const router = useRouter()

const regId = ref(Number(route.query.regId) || 0)
const patientId = ref(Number(route.query.patientId) || 0)
const patientName = ref(String(route.query.patientName) || '')
const doctorId = ref(Number(route.query.doctorId) || 0)
const currentTime = ref(new Date().toLocaleString())

const prescriptionType = ref(1)
const prescriptionItems = ref<PrescriptionItem[]>([])
const submitLoading = ref(false)

const toothChartRecords = ref<ToothChartRecord[]>([])
const medicalForm = reactive({
  diagnosis: '',
  treatment: '',
  medicalAdvice: ''
})

const totalAmount = computed(() => {
  return prescriptionItems.value.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

// 药品选择相关
const drugDialogVisible = ref(false)
const drugList = ref<StockItem[]>([])
const selectedDrugs = ref<StockItem[]>([])
const drugQuery = reactive({
  clinicId: 0,
  keyword: '',
  itemType: undefined as number | undefined,
  status: 1
})
const drugPagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const showDrugDialog = () => {
  drugDialogVisible.value = true
  loadDrugList()
}

const loadDrugList = async () => {
  try {
    const res = await getStockList({
      clinicId: getClinicId(),
      keyword: drugQuery.keyword,
      itemType: drugQuery.itemType,
      status: 1,
      pageNum: drugPagination.current,
      pageSize: drugPagination.size
    })
    drugList.value = res.data.list
    drugPagination.total = res.data.total
  } catch (error) {
    console.error('加载药品失败:', error)
  }
}

const resetDrugQuery = () => {
  drugQuery.keyword = ''
  drugQuery.itemType = undefined
  drugPagination.current = 1
  loadDrugList()
}

const handleSelectionChange = (selection: StockItem[]) => {
  selectedDrugs.value = selection
}

const confirmAddDrugs = () => {
  selectedDrugs.value.forEach((drug) => {
    const exists = prescriptionItems.value.some(item => item.itemId === drug.id)
    if (!exists) {
      prescriptionItems.value.push({
        itemId: drug.id,
        itemType: drug.itemType,
        itemName: drug.itemName,
        spec: drug.spec,
        unit: drug.unit,
        quantity: 1,
        unitPrice: drug.retailPrice,
        subtotal: drug.retailPrice,
        usage: '',
        frequency: '',
        duration: ''
      })
    }
  })
  drugDialogVisible.value = false
  ElMessage.success(`已添加 ${selectedDrugs.value.length} 个药品`)
}

const calcSubtotal = (index: number) => {
  const item = prescriptionItems.value[index]
  item.subtotal = (item.quantity || 0) * (item.unitPrice || 0)
}

const removeItem = (index: number) => {
  prescriptionItems.value.splice(index, 1)
}

const submitPrescription = async () => {
  if (!medicalForm.diagnosis) {
    ElMessage.warning('请输入诊断结果')
    return
  }

  if (prescriptionItems.value.length === 0) {
    try {
      await ElMessageBox.confirm('当前没有添加药品，是否只保存病历？', '提示', {
        confirmButtonText: '保存病历',
        cancelButtonText: '取消',
        type: 'warning'
      })
      // 只保存病历 + 牙位图
      await saveMedicalRecordWithToothChart()
      ElMessage.success('病历保存成功')
      goBack()
      return
    } catch {
      return
    }
  }

  // 验证用法用量
  for (const item of prescriptionItems.value) {
    if (!item.usage) {
      ElMessage.warning(`请填写【${item.itemName}】的用法`)
      return
    }
  }

  const userInfo = getUserInfo()

  try {
    submitLoading.value = true

    // 先创建病历 + 牙位图
    let recordId: number | undefined
    try {
      const mrRes = await createMedicalRecordApi({
        clinicId: getClinicId()!,
        patientId: patientId.value || undefined,
        patientName: patientName.value,
        doctorId: doctorId.value || userInfo?.doctorId,
        doctorName: userInfo?.realName,
        deptId: userInfo?.deptId,
        registrationId: regId.value,
        visitType: 2,
        diagnosis: medicalForm.diagnosis,
        treatment: medicalForm.treatment || '',
        medicalAdvice: medicalForm.medicalAdvice || ''
      })
      recordId = mrRes.data?.id || mrRes.data?.recordId

      if (recordId && toothChartRecords.value.length > 0) {
        await saveToothChart({
          medicalRecordId: recordId,
          clinicId: getClinicId(),
          records: toothChartRecords.value
        })
      }
    } catch (e) {
      console.error('保存病历失败:', e)
    }

    const data: Prescription = {
      clinicId: getClinicId()!,
      registrationId: regId.value,
      patientId: patientId.value || undefined,
      patientName: patientName.value,
      doctorId: doctorId.value || userInfo?.doctorId,
      doctorName: userInfo?.realName,
      deptId: userInfo?.deptId,
      prescriptionType: prescriptionType.value,
      diagnosis: medicalForm.diagnosis,
      items: prescriptionItems.value,
      totalAmount: totalAmount.value
    }

    const res = await createPrescription(data)
    ElMessage.success(`处方保存成功，单号：${res.data.prescriptionNo}`)
    goBack()
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error?.response?.data?.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

const saveMedicalRecordWithToothChart = async () => {
  const userInfo = getUserInfo()
  const mrRes = await createMedicalRecordApi({
    clinicId: getClinicId()!,
    patientId: patientId.value || undefined,
    patientName: patientName.value,
    doctorId: doctorId.value || userInfo?.doctorId,
    doctorName: userInfo?.realName,
    deptId: userInfo?.deptId,
    registrationId: regId.value,
    visitType: 2,
    diagnosis: medicalForm.diagnosis,
    treatment: medicalForm.treatment || '',
    medicalAdvice: medicalForm.medicalAdvice || ''
  })
  const recordId = mrRes.data?.id || mrRes.data?.recordId
  if (recordId && toothChartRecords.value.length > 0) {
    await saveToothChart({
      medicalRecordId: recordId,
      clinicId: getClinicId(),
      records: toothChartRecords.value
    })
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  drugQuery.clinicId = getClinicId()
})
</script>

<style lang="scss" scoped>
.treat-container {
  padding: 16px;

  .patient-card,
  .medical-card,
  .tooth-chart-card,
  .prescription-card {
    margin-bottom: 16px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-right {
      display: flex;
      align-items: center;
    }
  }

  .amount-summary {
    margin-top: 16px;
    text-align: right;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;

    .label {
      font-size: 14px;
      color: #606266;
    }

    .amount {
      font-size: 20px;
      color: #f56c6c;
      font-weight: bold;
    }
  }

  .action-bar {
    display: flex;
    justify-content: center;
    gap: 16px;
    padding: 20px;
  }

  .search-form {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
