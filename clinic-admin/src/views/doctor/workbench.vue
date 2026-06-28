<template>
  <div class="page-container">
    <!-- 通知栏 -->
    <el-card class="notification-card">
      <div class="notification-header">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
        <span class="notification-title">排班通知</span>
        <el-button v-if="unreadCount > 0" type="primary" link @click="markAllAsRead">全部已读</el-button>
      </div>
      <el-timeline v-if="notifications.length > 0" class="notification-list">
        <el-timeline-item
          v-for="item in notifications"
          :key="item.id"
          :type="item.isRead === 0 ? 'primary' : 'info'"
          :hollow="item.isRead === 1"
        >
          <div class="notification-item" :class="{ unread: item.isRead === 0 }">
            <div class="notification-content">{{ item.content }}</div>
            <div class="notification-time">{{ formatTime(item.createdAt) }}</div>
            <el-button
              v-if="item.isRead === 0"
              type="primary"
              link
              size="small"
              @click="markAsRead(item.id)"
            >标记已读</el-button>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无排班通知" :image-size="60" />
    </el-card>

    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @reload="loadData"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElTag } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getRegistrationList, type Registration } from '@/api/registration'
import { getUnreadNotifications, markNotificationAsRead, getUnreadNotificationCount, type Notification } from '@/api/notification'
import { getClinicId, getUserInfo } from '@/utils/storage'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const today = new Date().toISOString().split('T')[0]

const userInfo = getUserInfo()

const queryParams = reactive({
  clinicId: 0,
  regDate: today,
  doctorId: userInfo?.doctorId || undefined as number | undefined,
  status: 1,
  keyword: ''
})

const tableData = ref<Registration[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/手机号/挂号编号' },
  { prop: 'regDate', label: '日期', type: 'date' }
]

const columns = [
  { prop: 'queueNo', label: '排队号', width: 80 },
  { prop: 'regNo', label: '挂号编号', width: 150 },
  { prop: 'regTime', label: '时间', width: 80 },
  { prop: 'patientName', label: '患者姓名', width: 100 },
  { prop: 'deptName', label: '科室', width: 100 },
  { prop: 'visitTypeName', label: '就诊类型', width: 80 },
  { prop: 'chiefComplaint', label: '主诉', width: 200 },
  {
    prop: 'statusName',
    label: '状态',
    width: 100,
    formatter: (row: Registration) => {
      const typeMap: Record<number, string> = {
        1: 'info',
        2: 'warning',
        3: 'primary',
        4: 'success',
        5: 'info',
        6: 'danger',
        7: 'danger'
      }
      return h(ElTag, { type: typeMap[row.status] || 'info', size: 'small' }, () => row.statusName || '未知')
    }
  }
]

const operates = [
  {
    label: '开始诊疗',
    type: 'primary',
    action: (row: Registration) => handleTreat(row)
  }
]

// 通知相关
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)

const loadNotifications = async () => {
  if (!userInfo?.doctorId) return
  try {
    const clinicId = getClinicId()
    if (!clinicId) return
    
    const [notificationsRes, countRes] = await Promise.all([
      getUnreadNotifications({ clinicId, doctorId: userInfo.doctorId }),
      getUnreadNotificationCount({ clinicId, doctorId: userInfo.doctorId })
    ])
    notifications.value = notificationsRes.data
    unreadCount.value = countRes.data
  } catch (error) {
    console.error('加载通知失败:', error)
  }
}

const markAsRead = async (id: number) => {
  if (!userInfo?.doctorId) return
  try {
    await markNotificationAsRead(id, userInfo.doctorId)
    ElMessage.success('已标记为已读')
    loadNotifications()
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

const markAllAsRead = async () => {
  if (!userInfo?.doctorId || notifications.value.length === 0) return
  try {
    const promises = notifications.value
      .filter(n => n.isRead === 0)
      .map(n => markNotificationAsRead(n.id, userInfo.doctorId!))
    await Promise.all(promises)
    ElMessage.success('已全部标记为已读')
    loadNotifications()
  } catch (error) {
    console.error('标记全部已读失败:', error)
  }
}

const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      clinicId: getClinicId(),
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    const res = await getRegistrationList(params)
    tableData.value = res.data.list
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
  queryParams.regDate = today
  queryParams.doctorId = userInfo?.doctorId
  queryParams.status = 1
  queryParams.keyword = ''
  loadData()
}

const handlePageChange = (page: number, size?: number) => {
  pagination.current = page
  if (size) {
    pagination.size = size
  }
  loadData()
}

const handleTreat = (row: Registration) => {
  router.push({
    path: '/doctor/treat',
    query: {
      regId: row.id,
      patientId: row.patientId,
      patientName: row.patientName,
      doctorId: row.doctorId
    }
  })
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadData()
  loadNotifications()
})
</script>

<style lang="scss" scoped>
.page-container {
  .notification-card {
    margin-bottom: 16px;
    background: #f0f9ff;
    border: 1px solid #bae6fd;

    :deep(.el-card__body) {
      padding: 16px;
    }

    .notification-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;
      padding-bottom: 12px;
      border-bottom: 1px solid #e5e7eb;

      .notification-title {
        font-weight: 600;
        color: #0369a1;
        flex: 1;
      }
    }

    .notification-list {
      padding-left: 8px;

      .notification-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 4px 0;

        &.unread {
          .notification-content {
            font-weight: 500;
            color: #0369a1;
          }
        }

        .notification-content {
          flex: 1;
          color: #4b5563;
        }

        .notification-time {
          color: #9ca3af;
          font-size: 12px;
          min-width: 80px;
        }
      }
    }
  }

  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    background: #fff;
  }
}
</style>
