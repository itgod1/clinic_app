<template>
  <view class="doctor-list-page">
    <!-- 搜索栏 -->
    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索医生姓名"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text v-if="searchKeyword" class="clear-icon" @click="clearSearch">✕</text>
      </view>
    </view>

    <!-- 科室筛选 -->
    <scroll-view class="dept-tabs" scroll-x scroll-with-animation>
      <view
        v-for="dept in departmentList"
        :key="dept.id"
        :class="['tab-item', activeDeptId === dept.id ? 'active' : '']"
        @click="selectDept(dept.id)"
      >
        {{ dept.name }}
      </view>
    </scroll-view>

    <!-- 医生列表 -->
    <scroll-view class="doctor-list" scroll-y @scrolltolower="loadMore">
      <doctor-card
        v-for="doctor in doctorList"
        :key="doctor.id"
        :doctor="doctor"
        @click="goToDetail(doctor)"
      />
      <uni-load-more :status="loadStatus" />
      <empty-data
        v-if="doctorList.length === 0 && !loading"
        text="暂无医生信息"
        subtext="换个条件试试"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import DoctorCard from '@/components/doctor-card.vue'
import EmptyData from '@/components/empty-data.vue'
import { getDoctorList as getPublicDoctorList } from '@/api/miniapp/doctor.js'
import { getDepartmentList as getPublicDepartmentList } from '@/api/miniapp/clinic.js'
import { debounce } from '@/utils/index.js'

const loading = ref(false)
const doctorList = ref([])
const departmentList = ref([{ id: 0, name: '全部' }])
const activeDeptId = ref(0)
const searchKeyword = ref('')
const pageNum = ref(1)
const pageSize = 10
const loadStatus = ref('more')

onMounted(() => {
  loadDepartments()
  loadDoctors()
})

onPullDownRefresh(() => {
  pageNum.value = 1
  Promise.all([
    loadDepartments(),
    loadDoctors()
  ]).finally(() => {
    uni.stopPullDownRefresh()
  })
})

const loadDepartments = async () => {
  try {
    const params = { pageSize: 100 }
    // 如果选择了诊所，只显示该诊所的科室
    const clinicId = uni.getStorageSync('clinicId')
    if (clinicId) {
      params.clinicId = clinicId
    }
    const res = await getPublicDepartmentList(params)
    const list = res.data?.list || res.data?.records || []
    // 转换字段名 deptName -> name
    const formattedList = list.map(item => ({
      ...item,
      name: item.deptName || item.name
    }))
    departmentList.value = [{ id: 0, name: '全部' }, ...formattedList]
  } catch (error) {
    console.error('加载科室列表失败:', error)
  }
}

const loadDoctors = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const params = {
      page: pageNum.value,
      pageSize
    }
    // 只有输入了关键词才传递
    if (searchKeyword.value && searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (activeDeptId.value > 0) {
      params.deptId = activeDeptId.value
    }
    // 如果选择了诊所，传递诊所ID
    const clinicId = uni.getStorageSync('clinicId')
    if (clinicId) {
      params.clinicId = clinicId
    }

    const res = await getPublicDoctorList(params)
    const list = res.data?.records || res.data?.list || []

    if (pageNum.value === 1) {
      doctorList.value = list
    } else {
      doctorList.value.push(...list)
    }

    loadStatus.value = list.length < pageSize ? 'noMore' : 'more'
  } catch (error) {
    console.error('加载医生列表失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const selectDept = (id) => {
  activeDeptId.value = id
  pageNum.value = 1
  loadDoctors()
}

const handleSearch = debounce(() => {
  pageNum.value = 1
  loadDoctors()
}, 500)

const clearSearch = () => {
  searchKeyword.value = ''
  pageNum.value = 1
  loadDoctors()
}

const loadMore = () => {
  if (loadStatus.value === 'noMore' || loading.value) return
  pageNum.value++
  loadDoctors()
}

const goToDetail = (doctor) => {
  uni.navigateTo({
    url: `/pages/doctor/detail?id=${doctor.id}`
  })
}
</script>

<style lang="scss" scoped>
.doctor-list-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.search-section {
  background: #fff;
  padding: 20rpx 30rpx;

  .search-box {
    display: flex;
    align-items: center;
    background: #f5f5f5;
    border-radius: 36rpx;
    padding: 16rpx 24rpx;

    .search-icon {
      font-size: 28rpx;
      margin-right: 12rpx;
      color: #999;
    }

    .search-input {
      flex: 1;
      font-size: 28rpx;
      color: #333;
    }

    .clear-icon {
      font-size: 24rpx;
      color: #999;
      padding: 8rpx;
    }
  }
}

.dept-tabs {
  background: #fff;
  padding: 0 20rpx 20rpx;
  white-space: nowrap;

  .tab-item {
    display: inline-block;
    padding: 16rpx 32rpx;
    margin-right: 16rpx;
    font-size: 28rpx;
    color: #666;
    background: #f5f5f5;
    border-radius: 32rpx;
    transition: all 0.3s;

    &.active {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
    }

    &:last-child {
      margin-right: 30rpx;
    }
  }
}

.doctor-list {
  height: calc(100vh - 200rpx);
  padding: 20rpx 30rpx;
}
</style>
