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
        addText="新增医生"
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
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="医生头像">
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleAvatarChange"
                accept="image/*"
              >
                <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="医生姓名" prop="doctorName">
              <el-input v-model="form.doctorName" placeholder="请输入医生姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属科室" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择科室">
                <el-option
                  v-for="dept in deptOptions"
                  :key="dept.id"
                  :label="dept.deptName"
                  :value="dept.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职称" prop="titleName">
              <el-select v-model="form.titleName" placeholder="请选择职称">
                <el-option label="主任医师" value="主任医师" />
                <el-option label="副主任医师" value="副主任医师" />
                <el-option label="主治医师" value="主治医师" />
                <el-option label="住院医师" value="住院医师" />
                <el-option label="实习医师" value="实习医师" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="专业特长" prop="specialty">
          <el-input v-model="form.specialty" placeholder="请输入专业特长" />
        </el-form-item>
        <el-form-item label="个人简介" prop="introduction">
          <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="请输入个人简介" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工作状态" prop="workStatus">
              <el-radio-group v-model="form.workStatus">
                <el-radio :label="1">在岗</el-radio>
                <el-radio :label="2">离岗</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="医生详情" width="700px" destroy-on-close>
      <div v-if="currentDoctor" class="doctor-detail">
        <div class="doctor-avatar-section">
          <el-avatar 
            :size="100" 
            :src="currentDoctor.avatarUrl" 
            :icon="!currentDoctor.avatarUrl ? 'UserFilled' : undefined"
          />
          <div class="doctor-name">{{ currentDoctor.doctorName }}</div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="医生编号">{{ currentDoctor.doctorCode }}</el-descriptions-item>
          <el-descriptions-item label="所属科室">{{ currentDoctor.deptName }}</el-descriptions-item>
          <el-descriptions-item label="职称">{{ currentDoctor.titleName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentDoctor.phone }}</el-descriptions-item>
          <el-descriptions-item label="专业特长">{{ currentDoctor.specialty || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接诊数量">{{ currentDoctor.serviceCount }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ currentDoctor.rating || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="工作状态">
            <el-tag :type="currentDoctor.workStatus === 1 ? 'success' : 'info'">
              {{ currentDoctor.workStatus === 1 ? '在岗' : '离岗' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentDoctor.status === 1 ? 'success' : 'danger'">
              {{ currentDoctor.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">{{ currentDoctor.introduction || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 排班弹窗 -->
    <el-dialog
      v-model="scheduleVisible"
      :title="`医生排班 - ${currentScheduleDoctor?.doctorName || ''}`"
      width="900px"
      destroy-on-close
    >
      <div v-if="currentScheduleDoctor" class="schedule-container">
        <el-row :gutter="20" class="schedule-header">
          <el-col :span="12">
            <el-form-item label="选择周">
              <el-date-picker
                v-model="scheduleWeek"
                type="week"
                placeholder="选择周"
                format="yyyy年第WW周"
                @change="handleWeekChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="text-right">
            <el-button type="primary" @click="saveSchedule">保存排班</el-button>
          </el-col>
        </el-row>

        <el-table :data="scheduleData" border stripe>
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="weekDay" label="星期" width="80" />
          <el-table-column label="上午" min-width="200">
            <template #default="{ row }">
              <el-checkbox v-model="row.am.isWork">出诊</el-checkbox>
              <el-time-picker
                v-if="row.am.isWork"
                v-model="row.am.startTime"
                placeholder="开始时间"
                format="HH:mm"
                style="width: 100px; margin-left: 10px;"
              />
              <el-time-picker
                v-if="row.am.isWork"
                v-model="row.am.endTime"
                placeholder="结束时间"
                format="HH:mm"
                style="width: 100px; margin-left: 5px;"
              />
            </template>
          </el-table-column>
          <el-table-column label="下午" min-width="200">
            <template #default="{ row }">
              <el-checkbox v-model="row.pm.isWork">出诊</el-checkbox>
              <el-time-picker
                v-if="row.pm.isWork"
                v-model="row.pm.startTime"
                placeholder="开始时间"
                format="HH:mm"
                style="width: 100px; margin-left: 10px;"
              />
              <el-time-picker
                v-if="row.pm.isWork"
                v-model="row.pm.endTime"
                placeholder="结束时间"
                format="HH:mm"
                style="width: 100px; margin-left: 5px;"
              />
            </template>
          </el-table-column>
          <el-table-column label="号源数" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.limit" :min="0" :max="50" size="small" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 查看排班表弹窗 -->
    <el-dialog
      v-model="viewScheduleVisible"
      :title="`查看排班表 - ${currentViewDoctor?.doctorName || ''}`"
      width="900px"
      destroy-on-close
    >
      <div v-if="currentViewDoctor" class="schedule-container">
        <el-row :gutter="20" class="schedule-header">
          <el-col :span="12">
            <el-form-item label="选择日期范围">
              <el-date-picker
                v-model="viewScheduleDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="loadDoctorSchedule"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-table :data="doctorScheduleList" border stripe v-loading="scheduleLoading">
          <el-table-column prop="scheduleDate" label="日期" width="120" />
          <el-table-column prop="weekDay" label="星期" width="80">
            <template #default="{ row }">
              {{ getWeekDay(row.scheduleDate) }}
            </template>
          </el-table-column>
          <el-table-column label="上午" min-width="200">
            <template #default="{ row }">
              <el-tag v-if="row.amIsWork" type="success">出诊 {{ row.amStartTime }}-{{ row.amEndTime }}</el-tag>
              <el-tag v-else type="info">休息</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下午" min-width="200">
            <template #default="{ row }">
              <el-tag v-if="row.pmIsWork" type="success">出诊 {{ row.pmStartTime }}-{{ row.pmEndTime }}</el-tag>
              <el-tag v-else type="info">休息</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="limitNum" label="号源数" width="100" />
        </el-table>

        <el-empty v-if="doctorScheduleList.length === 0 && !scheduleLoading" description="暂无排班数据" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getDoctorList, createDoctor, updateDoctor, deleteDoctor, type Doctor } from '@/api/doctor'
import { getScheduleList, batchSetSchedule, type Schedule } from '@/api/schedule'
import { getDepartmentList, type Department } from '@/api/department'
import { getClinicId } from '@/utils/storage'
import { uploadImage } from '@/api/upload'
import { Plus } from '@element-plus/icons-vue'

interface ScheduleRow {
  date: string
  weekDay: string
  am: {
    isWork: boolean
    startTime: string
    endTime: string
  }
  pm: {
    isWork: boolean
    startTime: string
    endTime: string
  }
  limit: number
}

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const scheduleVisible = ref(false)
const viewScheduleVisible = ref(false)
const scheduleLoading = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentDoctor = ref<Doctor | null>(null)
const currentScheduleDoctor = ref<Doctor | null>(null)
const currentViewDoctor = ref<Doctor | null>(null)
const scheduleWeek = ref<Date | null>(null)
const scheduleData = ref<ScheduleRow[]>([])
const doctorScheduleList = ref<Schedule[]>([])
const viewScheduleDateRange = ref<string[]>([])

const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const deptOptions = ref<{ id: number; deptName: string }[]>([])

const queryParams = reactive({
  clinicId: 0,
  deptId: undefined as number | undefined,
  keyword: '',
  status: undefined as number | undefined
})

const tableData = ref<Doctor[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = computed(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/编号' },
  {
    prop: 'deptId',
    label: '科室',
    type: 'select',
    options: deptOptions.value.map(d => ({ label: d.deptName, value: d.id }))
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
])

const form = reactive({
  id: undefined as number | undefined,
  doctorName: '',
  deptId: undefined as number | undefined,
  titleName: '',
  specialty: '',
  phone: '',
  avatarUrl: '',
  introduction: '',
  workStatus: 1,
  status: 1
})

const rules = {
  doctorName: [{ required: true, message: '请输入医生姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const columns = [
  { prop: 'doctorCode', label: '医生编号', minWidth: 100 },
  {
    prop: 'avatarUrl',
    label: '头像',
    width: 70,
    align: 'center',
    formatter: (row: Doctor) => {
      if (row.avatarUrl) {
        return `<img src="${row.avatarUrl}" style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover;" />`
      }
      return '<div style="width: 40px; height: 40px; border-radius: 50%; background: #e4e7ed; display: inline-block; line-height: 40px; color: #909399; font-size: 12px;">无</div>'
    }
  },
  { prop: 'doctorName', label: '医生姓名', minWidth: 90 },
  { prop: 'deptName', label: '科室', minWidth: 100 },
  { prop: 'titleName', label: '职称', minWidth: 90 },
  { prop: 'phone', label: '手机号', minWidth: 110 },
  { prop: 'serviceCount', label: '接诊数', width: 80 },
  {
    prop: 'workStatus',
    label: '在岗状态',
    minWidth: 90,
    formatter: (row: Doctor) => row.workStatus === 1 ? '在岗' : '离岗'
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: Doctor) => row.status === 1 ? '正常' : '禁用'
  }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: Doctor) => handleView(row) },
  { label: '编辑', type: 'warning', action: (row: Doctor) => handleEdit(row) },
  { label: '设置排班', type: 'success', action: (row: Doctor) => handleSchedule(row) },
  { label: '查看排班', type: 'info', action: (row: Doctor) => handleViewSchedule(row) },
  {
    label: '删除',
    type: 'danger',
    action: (row: Doctor) => handleDelete(row)
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      clinicId: getClinicId(),
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    const res = await getDoctorList(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
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

const handleAdd = () => {
  dialogTitle.value = '新增医生'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    doctorName: '',
    deptId: undefined,
    titleName: '',
    specialty: '',
    phone: '',
    avatarUrl: '',
    introduction: '',
    workStatus: 1,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: Doctor) => {
  dialogTitle.value = '编辑医生'
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    doctorName: row.doctorName,
    deptId: row.deptId,
    titleName: row.titleName || '',
    specialty: row.specialty,
    phone: row.phone,
    avatarUrl: row.avatarUrl || '',
    introduction: row.introduction,
    workStatus: row.workStatus,
    status: row.status
  })
  dialogVisible.value = true
}

const handleView = (row: Doctor) => {
  currentDoctor.value = row
  detailVisible.value = true
}

const handleSchedule = (row: Doctor) => {
  currentScheduleDoctor.value = row
  scheduleWeek.value = new Date()
  initScheduleData()
  scheduleVisible.value = true
}

const initScheduleData = () => {
  const data: ScheduleRow[] = []
  const weekStart = scheduleWeek.value || new Date()

  // 获取本周一的日期
  const day = weekStart.getDay()
  const diff = weekStart.getDate() - day + (day === 0 ? -6 : 1)
  const monday = new Date(weekStart)
  monday.setDate(diff)

  for (let i = 0; i < 7; i++) {
    const date = new Date(monday)
    date.setDate(monday.getDate() + i)
    data.push({
      date: date.toISOString().split('T')[0],
      weekDay: weekDays[date.getDay()],
      am: {
        isWork: false,
        startTime: '08:00',
        endTime: '12:00'
      },
      pm: {
        isWork: false,
        startTime: '14:00',
        endTime: '18:00'
      },
      limit: 20
    })
  }
  scheduleData.value = data
}

const handleWeekChange = () => {
  initScheduleData()
}

const saveSchedule = async () => {
  try {
    const schedules: Schedule[] = scheduleData.value.map(row => ({
      clinicId: getClinicId()!,
      doctorId: currentScheduleDoctor.value?.id!,
      scheduleDate: row.date,
      amIsWork: row.am.isWork,
      amStartTime: row.am.isWork ? row.am.startTime : undefined,
      amEndTime: row.am.isWork ? row.am.endTime : undefined,
      pmIsWork: row.pm.isWork,
      pmStartTime: row.pm.isWork ? row.pm.startTime : undefined,
      pmEndTime: row.pm.isWork ? row.pm.endTime : undefined,
      limitNum: row.limit
    }))

    await batchSetSchedule({
      clinicId: getClinicId()!,
      doctorId: currentScheduleDoctor.value?.id!,
      schedules
    })

    ElMessage.success('排班保存成功')
    scheduleVisible.value = false
  } catch (error) {
    console.error('保存排班失败:', error)
    ElMessage.error('保存排班失败')
  }
}

const handleViewSchedule = (row: Doctor) => {
  currentViewDoctor.value = row
  // 默认显示未来7天的排班
  const today = new Date()
  const nextWeek = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000)
  viewScheduleDateRange.value = [
    today.toISOString().split('T')[0],
    nextWeek.toISOString().split('T')[0]
  ]
  viewScheduleVisible.value = true
  loadDoctorSchedule()
}

const loadDoctorSchedule = async () => {
  if (!currentViewDoctor.value || viewScheduleDateRange.value.length !== 2) return
  
  scheduleLoading.value = true
  try {
    const res = await getScheduleList({
      clinicId: getClinicId()!,
      doctorId: currentViewDoctor.value.id,
      startDate: viewScheduleDateRange.value[0],
      endDate: viewScheduleDateRange.value[1]
    })
    doctorScheduleList.value = res.data
  } catch (error) {
    console.error('加载排班表失败:', error)
    ElMessage.error('加载排班表失败')
  } finally {
    scheduleLoading.value = false
  }
}

const getWeekDay = (dateStr: string) => {
  const date = new Date(dateStr)
  return weekDays[date.getDay()]
}

const handleDelete = async (row: Doctor) => {
  try {
    await ElMessageBox.confirm('确定要删除该医生吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDoctor(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (isEdit.value) {
      await updateDoctor(form)
      ElMessage.success('编辑成功')
    } else {
      await createDoctor({ ...form, clinicId: getClinicId() })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleAvatarChange = async (file: any) => {
  const rawFile = file.raw
  if (!rawFile) return

  // 检查文件类型
  if (!rawFile.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }

  // 检查文件大小（限制2MB）
  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  try {
    const res = await uploadImage(rawFile)
    form.avatarUrl = res.data.url
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('上传头像失败:', error)
    ElMessage.error('上传头像失败')
  }
}

const loadDeptOptions = async () => {
  const clinicId = getClinicId()
  if (!clinicId) return
  try {
    const res = await getDepartmentList({
      clinicId: clinicId,
      status: 1,
      pageNum: 1,
      pageSize: 100
    })
    deptOptions.value = res.data.list.map((dept: Department) => ({
      id: dept.id,
      deptName: dept.deptName
    }))
  } catch (error) {
    console.error('加载科室列表失败:', error)
  }
}

onMounted(() => {
  const clinicId = getClinicId()
  if (clinicId) {
    queryParams.clinicId = clinicId
    loadDeptOptions()
    loadData()
  }
})
</script>

<style lang="scss" scoped>
.page-container {
  width: 100%;

  .search-card {
    margin-bottom: 16px;
    width: 100%;
  }

  .table-card {
    background: #fff;
    width: 100%;

    :deep(.el-card__body) {
      width: 100%;
      padding: 0;
    }
  }
}

.schedule-container {
  .schedule-header {
    margin-bottom: 20px;
    align-items: center;
  }

  .text-right {
    text-align: right;
  }
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);

    &:hover {
      border-color: var(--el-color-primary);
    }
  }

  .avatar {
    width: 100px;
    height: 100px;
    object-fit: cover;
    display: block;
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    text-align: center;
    line-height: 100px;
  }
}

.doctor-detail {
  .doctor-avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 20px;

    .doctor-name {
      margin-top: 10px;
      font-size: 18px;
      font-weight: 500;
      color: var(--el-text-color-primary);
    }
  }
}
</style>