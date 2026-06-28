<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @reload="loadData"
        @add="handleOpenClean"
        showAdd
        addText="清理日志"
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.operationTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.realName }} ({{ currentLog.username }})</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="getOperationTypeType(currentLog.operationType)">
            {{ getOperationTypeName(currentLog.operationType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ currentLog.description }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行时长">{{ currentLog.executionTime }}ms</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-content">{{ formatJson(currentLog.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.errorMsg" label="错误信息" :span="2">
          <span class="error-text">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 清理日志弹窗 -->
    <el-dialog v-model="cleanVisible" title="清理日志" width="400px">
      <el-form label-width="120px">
        <el-form-item label="保留天数">
          <el-select v-model="cleanDays" style="width: 200px;">
            <el-option label="保留最近7天" :value="7" />
            <el-option label="保留最近30天" :value="30" />
            <el-option label="保留最近90天" :value="90" />
            <el-option label="保留最近180天" :value="180" />
            <el-option label="保留最近365天" :value="365" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cleanVisible = false">取消</el-button>
        <el-button type="danger" @click="handleClean" :loading="cleanLoading">确认清理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import {
  getOperationLogList,
  deleteOperationLogs,
  cleanOperationLogs,
  getOperationTypeName,
  operationTypeOptions,
  type OperationLog
} from '@/api/operationLog'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const cleanLoading = ref(false)
const detailVisible = ref(false)
const cleanVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)
const cleanDays = ref(30)

const queryParams = reactive({
  clinicId: 0,
  module: '',
  operationType: undefined as number | undefined,
  username: '',
  status: undefined as number | undefined,
  dateRange: [] as string[]
})

const tableData = ref<OperationLog[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'username', label: '操作人', type: 'input', placeholder: '用户名/真实姓名' },
  {
    prop: 'operationType',
    label: '操作类型',
    type: 'select',
    options: operationTypeOptions,
    clearable: true
  },
  { prop: 'module', label: '操作模块', type: 'input', placeholder: '请输入模块名称' },
  {
    prop: 'status',
    label: '操作状态',
    type: 'select',
    options: [
      { label: '成功', value: 1 },
      { label: '失败', value: 0 }
    ],
    clearable: true
  },
  { prop: 'dateRange', label: '操作时间', type: 'datetimerange' }
]

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'operationTime', label: '操作时间', width: 160 },
  { prop: 'username', label: '操作人', width: 120 },
  {
    prop: 'operationType',
    label: '操作类型',
    width: 100,
    formatter: (row: OperationLog) => getOperationTypeName(row.operationType)
  },
  { prop: 'module', label: '操作模块', width: 120 },
  { prop: 'description', label: '操作描述', minWidth: 150 },
  { prop: 'ipAddress', label: 'IP地址', width: 130 },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: OperationLog) => row.status === 1 ? '成功' : '失败'
  },
  {
    prop: 'executionTime',
    label: '执行时长',
    width: 100,
    formatter: (row: OperationLog) => row.executionTime ? `${row.executionTime}ms` : '-'
  }
]

const operates = [
  { label: '查看', type: 'primary', action: (row: OperationLog) => handleView(row) }
]

const getOperationTypeType = (type: number) => {
  const types: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'info',
    5: 'primary',
    6: 'success',
    7: 'info'
  }
  return types[type] || 'info'
}

const formatJson = (json?: string) => {
  if (!json) return '{}'
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    const params: any = {
      clinicId,
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    if (queryParams.module) {
      params.module = queryParams.module
    }
    if (queryParams.operationType !== undefined) {
      params.operationType = queryParams.operationType
    }
    if (queryParams.username) {
      params.username = queryParams.username
    }
    if (queryParams.status !== undefined) {
      params.status = queryParams.status
    }
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      params.startTime = queryParams.dateRange[0]
      params.endTime = queryParams.dateRange[1]
    }
    const res = await getOperationLogList(params)
    tableData.value = res.data.list.map((item: OperationLog) => ({
      ...item,
      operationTypeName: getOperationTypeName(item.operationType)
    }))
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

const handleView = (row: OperationLog) => {
  currentLog.value = row
  detailVisible.value = true
}

const handleOpenClean = () => {
  cleanVisible.value = true
}

const handleClean = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要清理${cleanDays.value}天前的日志吗？此操作不可恢复！`,
      '警告',
      { type: 'warning' }
    )
    cleanLoading.value = true
    await cleanOperationLogs(cleanDays.value)
    ElMessage.success('清理成功')
    cleanVisible.value = false
    loadData()
  } catch (error) {
    console.error('清理失败:', error)
  } finally {
    cleanLoading.value = false
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

  .table-card {
    background: #fff;
  }

  .json-content {
    max-height: 300px;
    overflow: auto;
    background: #f5f7fa;
    padding: 10px;
    border-radius: 4px;
    font-family: monospace;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .error-text {
    color: #f56c6c;
  }
}
</style>
