<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-row :gutter="20" align="middle">
        <el-col :span="6">
          <el-form-item label="选择日期">
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              @change="loadScheduleData"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="科室">
            <el-select v-model="selectedDept" placeholder="全部科室" clearable @change="filterDoctors">
              <el-option
                v-for="dept in deptOptions"
                :key="dept.id"
                :label="dept.deptName"
                :value="dept.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="loadScheduleData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <div v-if="scheduleData.length > 0" class="schedule-table-wrapper">
        <table class="schedule-table">
          <thead>
            <tr>
              <th class="doctor-header">医生</th>
              <th class="dept-header">科室</th>
              <th class="time-header">上午</th>
              <th class="time-header">下午</th>
              <th class="limit-header">号源数</th>
              <th class="action-header">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredScheduleData" :key="item.doctorId">
              <td class="doctor-cell">
                <div class="doctor-info">
                  <el-avatar :size="40" :icon="'User'" />
                  <span class="doctor-name">{{ item.doctorName }}</span>
                </div>
              </td>
              <td class="dept-cell">{{ item.deptName }}</td>
              <td class="time-cell">
                <el-tag v-if="item.amIsWork" type="success" size="small">
                  出诊 {{ item.amStartTime }}-{{ item.amEndTime }}
                </el-tag>
                <el-tag v-else type="info" size="small">休息</el-tag>
              </td>
              <td class="time-cell">
                <el-tag v-if="item.pmIsWork" type="success" size="small">
                  出诊 {{ item.pmStartTime }}-{{ item.pmEndTime }}
                </el-tag>
                <el-tag v-else type="info" size="small">休息</el-tag>
              </td>
              <td class="limit-cell">{{ item.totalQuota }}</td>
              <td class="action-cell">
                <el-button type="primary" link @click="handleEditSchedule(item)">编辑排班</el-button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <el-empty v-else description="暂无排班数据" />
    </el-card>

    <!-- 编辑排班弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑排班" width="500px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="医生">
          <span>{{ editForm.doctorName }}</span>
        </el-form-item>
        <el-form-item label="日期">
          <span>{{ editForm.scheduleDate }}</span>
        </el-form-item>
        <el-form-item label="上午">
          <el-checkbox v-model="editForm.amIsWork">出诊</el-checkbox>
          <template v-if="editForm.amIsWork">
            <el-time-picker
              v-model="editForm.amStartTime"
              placeholder="开始时间"
              format="HH:mm"
              style="width: 120px; margin-left: 10px;"
            />
            <el-time-picker
              v-model="editForm.amEndTime"
              placeholder="结束时间"
              format="HH:mm"
              style="width: 120px; margin-left: 10px;"
            />
          </template>
        </el-form-item>
        <el-form-item label="下午">
          <el-checkbox v-model="editForm.pmIsWork">出诊</el-checkbox>
          <template v-if="editForm.pmIsWork">
            <el-time-picker
              v-model="editForm.pmStartTime"
              placeholder="开始时间"
              format="HH:mm"
              style="width: 120px; margin-left: 10px;"
            />
            <el-time-picker
              v-model="editForm.pmEndTime"
              placeholder="结束时间"
              format="HH:mm"
              style="width: 120px; margin-left: 10px;"
            />
          </template>
        </el-form-item>
        <el-form-item label="号源数">
          <el-input-number v-model="editForm.totalQuota" :min="0" :max="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getScheduleList, batchSetSchedule, type Schedule } from '@/api/schedule'
import { getDoctorList, type Doctor } from '@/api/doctor'
import { getDepartmentList, type Department } from '@/api/department'
import { getClinicId } from '@/utils/storage'

interface ScheduleItem {
  id?: number
  clinicId: number
  doctorId: number
  scheduleDate: string
  amIsWork: boolean
  amStartTime: string
  amEndTime: string
  pmIsWork: boolean
  pmStartTime: string
  pmEndTime: string
  totalQuota: number
  doctorName: string
  deptName: string
}

const selectedDate = ref<string>('')
const selectedDept = ref<number | null>(null)
const loading = ref(false)
const saveLoading = ref(false)
const dialogVisible = ref(false)

const doctorList = ref<Doctor[]>([])
const deptOptions = ref<Department[]>([])
const scheduleData = ref<ScheduleItem[]>([])

const editForm = reactive({
  doctorId: 0,
  doctorName: '',
  scheduleDate: '',
  amIsWork: false,
  amStartTime: '08:00',
  amEndTime: '12:00',
  pmIsWork: false,
  pmStartTime: '14:00',
  pmEndTime: '18:00',
  totalQuota: 20
})

// 过滤后的排班数据
const filteredScheduleData = computed(() => {
  if (!selectedDept.value) return scheduleData.value
  return scheduleData.value.filter(item => {
    const doctor = doctorList.value.find(d => d.id === item.doctorId)
    return doctor?.deptId === selectedDept.value
  })
})

const initDate = () => {
  const today = new Date()
  selectedDate.value = today.toISOString().split('T')[0]
}

const loadDoctors = async () => {
  try {
    const res = await getDoctorList({
      clinicId: getClinicId()!,
      pageNum: 1,
      pageSize: 100
    })
    doctorList.value = res.data.records
  } catch (error) {
    console.error('加载医生列表失败:', error)
  }
}

const loadDepartments = async () => {
  try {
    const res = await getDepartmentList({
      clinicId: getClinicId()!,
      pageNum: 1,
      pageSize: 100
    })
    deptOptions.value = res.data.list
  } catch (error) {
    console.error('加载科室列表失败:', error)
  }
}

const loadScheduleData = async () => {
  if (!selectedDate.value) {
    ElMessage.warning('请选择日期')
    return
  }

  loading.value = true
  try {
    const res = await getScheduleList({
      clinicId: getClinicId()!,
      startDate: selectedDate.value,
      endDate: selectedDate.value
    })

    // 按医生ID分组，合并上午和下午排班
    const doctorScheduleMap = new Map<number, ScheduleItem>()

    res.data.forEach((schedule: Schedule) => {
      const doctor = doctorList.value.find(d => d.id === schedule.doctorId)
      const doctorName = doctor?.doctorName || '未知医生'
      const deptName = doctor?.deptName || '未知科室'

      let item = doctorScheduleMap.get(schedule.doctorId)
      if (!item) {
        item = {
          id: schedule.id,
          clinicId: schedule.clinicId,
          doctorId: schedule.doctorId,
          scheduleDate: schedule.scheduleDate,
          amIsWork: false,
          amStartTime: '08:00',
          amEndTime: '12:00',
          pmIsWork: false,
          pmStartTime: '14:00',
          pmEndTime: '18:00',
          totalQuota: 0,
          doctorName,
          deptName
        }
        doctorScheduleMap.set(schedule.doctorId, item)
      }

      // 判断是上午还是下午（根据时段开始时间）
      const hour = parseInt(schedule.timeSlotStart?.substring(0, 2) || '0')
      if (hour < 12) {
        // 上午
        item.amIsWork = true
        item.amStartTime = schedule.timeSlotStart?.substring(0, 5) || '08:00'
        item.amEndTime = schedule.timeSlotEnd?.substring(0, 5) || '12:00'
        item.totalQuota += schedule.totalQuota || 0
      } else {
        // 下午
        item.pmIsWork = true
        item.pmStartTime = schedule.timeSlotStart?.substring(0, 5) || '14:00'
        item.pmEndTime = schedule.timeSlotEnd?.substring(0, 5) || '18:00'
        item.totalQuota += schedule.totalQuota || 0
      }
    })

    // 如果没有排班数据，显示所有医生（默认休息状态）
    if (doctorScheduleMap.size === 0) {
      scheduleData.value = doctorList.value.map(doctor => ({
        id: 0,
        clinicId: getClinicId()!,
        doctorId: doctor.id,
        scheduleDate: selectedDate.value!,
        amIsWork: false,
        amStartTime: '08:00',
        amEndTime: '12:00',
        pmIsWork: false,
        pmStartTime: '14:00',
        pmEndTime: '18:00',
        totalQuota: 20,
        doctorName: doctor.doctorName,
        deptName: doctor.deptName
      }))
    } else {
      scheduleData.value = Array.from(doctorScheduleMap.values())
    }
  } catch (error) {
    console.error('加载排班数据失败:', error)
    ElMessage.error('加载排班数据失败')
  } finally {
    loading.value = false
  }
}

const filterDoctors = () => {
  // 科室筛选通过 computed 属性自动处理
}

const handleReset = () => {
  selectedDept.value = null
  initDate()
  loadScheduleData()
}

const handleEditSchedule = (item: ScheduleItem) => {
  editForm.doctorId = item.doctorId
  editForm.doctorName = item.doctorName
  editForm.scheduleDate = item.scheduleDate
  editForm.amIsWork = item.amIsWork
  editForm.amStartTime = item.amStartTime || '08:00'
  editForm.amEndTime = item.amEndTime || '12:00'
  editForm.pmIsWork = item.pmIsWork
  editForm.pmStartTime = item.pmStartTime || '14:00'
  editForm.pmEndTime = item.pmEndTime || '18:00'
  editForm.totalQuota = item.totalQuota
  dialogVisible.value = true
}

const saveSchedule = async () => {
  saveLoading.value = true
  try {
    await batchSetSchedule({
      clinicId: getClinicId()!,
      doctorId: editForm.doctorId,
      schedules: [{
        scheduleDate: editForm.scheduleDate,
        amIsWork: editForm.amIsWork,
        amStartTime: editForm.amStartTime,
        amEndTime: editForm.amEndTime,
        pmIsWork: editForm.pmIsWork,
        pmStartTime: editForm.pmStartTime,
        pmEndTime: editForm.pmEndTime,
        limitNum: editForm.totalQuota
      }]
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadScheduleData()
  } catch (error) {
    console.error('保存排班失败:', error)
    ElMessage.error('保存排班失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(async () => {
  initDate()
  // 先加载医生和科室列表，再加载排班数据
  await Promise.all([loadDoctors(), loadDepartments()])
  await loadScheduleData()
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

  .schedule-table-wrapper {
    overflow-x: auto;
  }

  .schedule-table {
    width: 100%;
    border-collapse: collapse;

    th, td {
      padding: 12px;
      text-align: left;
      border-bottom: 1px solid #ebeef5;
    }

    th {
      background-color: #f5f7fa;
      font-weight: 600;
      color: #606266;
    }

    .doctor-header {
      width: 150px;
    }

    .dept-header {
      width: 120px;
    }

    .time-header {
      width: 200px;
    }

    .limit-header {
      width: 100px;
    }

    .action-header {
      width: 120px;
    }

    .doctor-cell {
      .doctor-info {
        display: flex;
        align-items: center;
        gap: 10px;

        .doctor-name {
          font-weight: 500;
        }
      }
    }

    .time-cell {
      .el-tag {
        min-width: 140px;
        text-align: center;
      }
    }

    tbody tr:hover {
      background-color: #f5f7fa;
    }
  }
}
</style>
