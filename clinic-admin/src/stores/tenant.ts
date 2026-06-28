import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from './user'
import { storage } from '@/utils/storage'
import type { ClinicInfo } from '@/api/clinic'

export interface TenantInfo extends ClinicInfo {
  isActive?: boolean
}

export const useTenantStore = defineStore('tenant', () => {
  // 获取用户 store
  const userStore = useUserStore()

  // State
  const currentTenantId = ref<number | null>(storage.getClinicId())
  const currentTenant = ref<TenantInfo | null>(null)
  const tenantList = ref<TenantInfo[]>([])
  const loading = ref(false)
  const switchLoading = ref(false)

  // Getters
  const isTenantLoaded = computed(() => !!currentTenant.value)
  
  const currentTenantName = computed(() => {
    return currentTenant.value?.clinicName || userStore.currentClinicName || '未选择诊所'
  })

  // 是否可以切换租户（超级管理员可以）
  const canSwitchTenant = computed(() => {
    return userStore.canSwitchTenant && tenantList.value.length > 1
  })

  // 是否为多租户管理员
  const isMultiTenantAdmin = computed(() => {
    return userStore.canManageAllTenants && tenantList.value.length > 0
  })

  // 获取当前租户ID（优先使用 store 中的，其次是用户 store 的）
  const getCurrentTenantId = (): number | null => {
    return currentTenantId.value || userStore.currentClinicId
  }

  // Actions
  
  // 设置当前租户ID
  const setCurrentTenantId = (tenantId: number) => {
    currentTenantId.value = tenantId
    storage.setClinicId(tenantId)
  }

  // 加载当前租户详情
  const loadCurrentTenant = async () => {
    const tenantId = getCurrentTenantId()
    if (!tenantId) {
      console.warn('没有可用的租户ID')
      return null
    }

    // TODO: 从其他途径获取租户信息
    return null
  }

  // 加载租户列表（仅超级管理员需要）
  const loadTenantList = async () => {
    // 如果不是超级管理员，不需要加载列表
    if (!userStore.canManageAllTenants) {
      // 单个租户也放入列表
      if (userStore.currentClinicId && userStore.currentClinicName) {
        tenantList.value = [{
          id: userStore.currentClinicId,
          clinicName: userStore.currentClinicName,
          clinicCode: '',
          address: '',
          province: '',
          city: '',
          district: '',
          contactPerson: '',
          contactPhone: '',
          logoUrl: '',
          businessStatus: 1,
          serviceStart: '',
          serviceEnd: '',
          description: ''
        }]
      }
      return
    }

    try {
      loading.value = true
      // TODO: 调用获取所有租户列表的 API
      // const res = await getTenantList()
      // tenantList.value = res.data.list
      
      // 临时模拟数据
      tenantList.value = [
        {
          id: userStore.currentClinicId || 1,
          clinicName: userStore.currentClinicName || '当前诊所',
          clinicCode: 'main',
          address: '',
          province: '',
          city: '',
          district: '',
          contactPerson: '',
          contactPhone: '',
          logoUrl: '',
          businessStatus: 1,
          serviceStart: '',
          serviceEnd: '',
          description: '',
          isActive: true
        }
      ]
    } catch (error) {
      console.error('加载租户列表失败:', error)
      ElMessage.error('加载诊所列表失败')
    } finally {
      loading.value = false
    }
  }

  // 切换租户
  const switchTenant = async (tenantId: number) => {
    // 检查权限
    if (!userStore.canSwitchTenant) {
      ElMessage.error('没有权限切换诊所')
      return false
    }

    // 检查是否是相同的租户
    if (tenantId === getCurrentTenantId()) {
      return true
    }

    try {
      switchLoading.value = true
      
      // 设置新的租户ID
      setCurrentTenantId(tenantId)
      
      // 重新加载租户信息
      await loadCurrentTenant()
      
      ElMessage.success('诊所切换成功')
      
      // 刷新页面以更新所有数据
      window.location.reload()
      
      return true
    } catch (error) {
      console.error('切换租户失败:', error)
      ElMessage.error('切换诊所失败')
      return false
    } finally {
      switchLoading.value = false
    }
  }

  // 从用户 store 同步租户信息
  const syncFromUserStore = () => {
    if (userStore.currentClinicId && !currentTenantId.value) {
      currentTenantId.value = userStore.currentClinicId
    }
  }

  // 初始化租户信息
  const initTenant = async () => {
    syncFromUserStore()
    
    // 加载当前租户详情
    await loadCurrentTenant()
    
    // 如果是管理员，加载租户列表
    if (userStore.canManageAllTenants) {
      await loadTenantList()
    }
  }

  // 清空租户信息
  const clearTenant = () => {
    currentTenantId.value = null
    currentTenant.value = null
    tenantList.value = []
  }

  return {
    // State
    currentTenantId,
    currentTenant,
    tenantList,
    loading,
    switchLoading,
    // Getters
    isTenantLoaded,
    currentTenantName,
    canSwitchTenant,
    isMultiTenantAdmin,
    // Actions
    getCurrentTenantId,
    setCurrentTenantId,
    loadCurrentTenant,
    loadTenantList,
    switchTenant,
    initTenant,
    clearTenant
  }
})