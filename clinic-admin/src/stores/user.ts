import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { logout as logoutApi } from '@/api/auth'
import { storage } from '@/utils/storage'
import router from '@/router'

// 角色枚举（与数据库 sys_role 表对应）
export enum RoleEnum {
  SUPER_ADMIN = 1,    // 超级管理员 - 可管理所有租户
  CLINIC_ADMIN = 2,   // 诊所管理员 - 可管理单个租户
  DOCTOR = 3,         // 医生
  NURSE = 4,          // 护士
  CASHIER = 5,        // 收银员
  STOREKEEPER = 6     // 库管员
}

// 角色权限配置（与数据库 sys_role 表对应）
export const ROLE_PERMISSIONS = {
  [RoleEnum.SUPER_ADMIN]: {
    name: '超级管理员',
    canManageAllTenants: true,
    canSwitchTenant: true,
    canCreateTenant: true,
    canDeleteTenant: true,
    systemMenus: ['*'] // 所有菜单
  },
  [RoleEnum.CLINIC_ADMIN]: {
    name: '诊所管理员',
    canManageAllTenants: false,
    canSwitchTenant: false,
    canCreateTenant: false,
    canDeleteTenant: false,
    systemMenus: ['dashboard', 'clinic', 'workbench', 'patient', 'medicalRecord', 'consultation', 'registration', 'returnVisit', 'cashier', 'insurance', 'stock', 'member', 'imaging', 'report', 'system']
  },
  [RoleEnum.DOCTOR]: {
    name: '医生',
    canManageAllTenants: false,
    canSwitchTenant: false,
    canCreateTenant: false,
    canDeleteTenant: false,
    systemMenus: ['dashboard', 'workbench', 'patient', 'medicalRecord', 'consultation', 'registration', 'returnVisit', 'imaging']
  },
  [RoleEnum.NURSE]: {
    name: '护士',
    canManageAllTenants: false,
    canSwitchTenant: false,
    canCreateTenant: false,
    canDeleteTenant: false,
    systemMenus: ['dashboard', 'patient', 'medicalRecord', 'consultation', 'registration', 'returnVisit']
  },
  [RoleEnum.CASHIER]: {
    name: '收银员',
    canManageAllTenants: false,
    canSwitchTenant: false,
    canCreateTenant: false,
    canDeleteTenant: false,
    systemMenus: ['dashboard', 'patient', 'registration', 'cashier', 'insurance', 'member']
  },
  [RoleEnum.STOREKEEPER]: {
    name: '库管员',
    canManageAllTenants: false,
    canSwitchTenant: false,
    canCreateTenant: false,
    canDeleteTenant: false,
    systemMenus: ['dashboard', 'stock']
  }
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  clinicId: number
  clinicName: string
  roleId: number
  roleName: string
  avatar?: string
  permissions?: string[]
  doctorId?: number
  deptId?: number
}

export const useUserStore = defineStore('user', () => {
  // State
  const userInfo = ref<UserInfo | null>(null)
  const token = ref<string | null>(storage.getToken())

  // Getters
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  
  const currentRole = computed(() => userInfo.value?.roleId || null)
  
  const rolePermission = computed(() => {
    if (!currentRole.value) return null
    return ROLE_PERMISSIONS[currentRole.value as RoleEnum] || null
  })

  // 是否为超级管理员
  const isSuperAdmin = computed(() => currentRole.value === RoleEnum.SUPER_ADMIN)
  
  // 是否可以管理多个租户
  const canManageAllTenants = computed(() => {
    return rolePermission.value?.canManageAllTenants || false
  })
  
  // 是否可以切换租户
  const canSwitchTenant = computed(() => {
    return rolePermission.value?.canSwitchTenant || false
  })
  
  // 是否可以创建租户
  const canCreateTenant = computed(() => {
    return rolePermission.value?.canCreateTenant || false
  })

  // 当前诊所ID
  const currentClinicId = computed(() => userInfo.value?.clinicId || null)
  
  // 当前诊所名称
  const currentClinicName = computed(() => userInfo.value?.clinicName || '')

  // 检查是否有权限访问某个菜单
  const hasMenuPermission = (menuKey: string): boolean => {
    if (!rolePermission.value) return false
    if (isSuperAdmin.value) return true
    const menus = rolePermission.value.systemMenus
    return menus.includes('*') || menus.includes(menuKey)
  }

  // 检查是否有某个权限
  const hasPermission = (permission: string): boolean => {
    if (!userInfo.value?.permissions) return false
    if (isSuperAdmin.value) return true
    return userInfo.value.permissions.includes(permission) || 
           userInfo.value.permissions.includes('*')
  }

  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    storage.setToken(newToken)
  }

  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
    storage.setUserInfo(info)
    if (info.clinicId) {
      storage.setClinicId(info.clinicId)
    }
  }

  const fetchUserInfo = async () => {
    // 从本地存储获取用户信息（登录时已保存）
    const storedUserInfo = storage.getUserInfo<UserInfo>()
    if (storedUserInfo) {
      userInfo.value = storedUserInfo
      return storedUserInfo
    }
    throw new Error('未找到用户信息，请重新登录')
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略错误
    }
    token.value = null
    userInfo.value = null
    storage.clear()
    router.push('/login')
  }

  const clearUserInfo = () => {
    token.value = null
    userInfo.value = null
    storage.clear()
  }

  // 初始化从 storage 恢复
  const initFromStorage = () => {
    const storedToken = storage.getToken()
    const storedUserInfo = storage.getUserInfo<UserInfo>()
    if (storedToken) {
      token.value = storedToken
    }
    if (storedUserInfo) {
      userInfo.value = storedUserInfo
    }
  }

  return {
    // State
    userInfo,
    token,
    // Getters
    isLoggedIn,
    currentRole,
    rolePermission,
    isSuperAdmin,
    canManageAllTenants,
    canSwitchTenant,
    canCreateTenant,
    currentClinicId,
    currentClinicName,
    // Actions
    setToken,
    setUserInfo,
    fetchUserInfo,
    logout,
    clearUserInfo,
    initFromStorage,
    hasMenuPermission,
    hasPermission
  }
})