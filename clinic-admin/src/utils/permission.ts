import { useUserStore, RoleEnum, ROLE_PERMISSIONS } from '@/stores'

/**
 * 权限工具函数
 * 用于组件内和路由守卫中的权限判断
 */

/**
 * 检查是否为超级管理员
 */
export const isSuperAdmin = (): boolean => {
  const userStore = useUserStore()
  return userStore.isSuperAdmin
}

/**
 * 检查是否有权限访问指定菜单
 * @param menuKey 菜单标识
 */
export const hasMenuPermission = (menuKey: string): boolean => {
  const userStore = useUserStore()
  return userStore.hasMenuPermission(menuKey)
}

/**
 * 检查是否有指定权限
 * @param permission 权限标识
 */
export const hasPermission = (permission: string): boolean => {
  const userStore = useUserStore()
  return userStore.hasPermission(permission)
}

/**
 * 检查是否可以管理所有租户
 */
export const canManageAllTenants = (): boolean => {
  const userStore = useUserStore()
  return userStore.canManageAllTenants
}

/**
 * 检查是否可以切换租户
 */
export const canSwitchTenant = (): boolean => {
  const userStore = useUserStore()
  return userStore.canSwitchTenant
}

/**
 * 检查是否可以创建租户
 */
export const canCreateTenant = (): boolean => {
  const userStore = useUserStore()
  return userStore.canCreateTenant
}

/**
 * 获取当前角色信息
 */
export const getCurrentRoleInfo = () => {
  const userStore = useUserStore()
  const roleId = userStore.currentRole
  if (!roleId) return null
  
  return {
    roleId,
    ...ROLE_PERMISSIONS[roleId as RoleEnum]
  }
}

/**
 * 检查是否为指定角色
 * @param role 角色枚举或角色ID
 */
export const isRole = (role: RoleEnum | number): boolean => {
  const userStore = useUserStore()
  return userStore.currentRole === role
}

/**
 * 检查是否满足角色级别（大于等于指定角色级别）
 * 级别：SUPER_ADMIN > ADMIN > DOCTOR > NURSE > RECEPTIONIST > PHARMACIST
 * @param minRole 最低要求角色
 */
export const hasRoleLevel = (minRole: RoleEnum): boolean => {
  const userStore = useUserStore()
  const currentRole = userStore.currentRole
  if (!currentRole) return false
  
  return currentRole <= minRole
}

/**
 * 过滤有权限的路由
 * @param routes 路由列表
 */
export const filterPermissionRoutes = (routes: any[]): any[] => {
  const userStore = useUserStore()
  
  return routes.filter(route => {
    // 如果没有配置权限，允许访问
    if (!route.meta?.permission) return true
    
    // 检查权限
    if (typeof route.meta.permission === 'string') {
      return userStore.hasPermission(route.meta.permission)
    }
    
    if (Array.isArray(route.meta.permission)) {
      return route.meta.permission.some((p: string) => userStore.hasPermission(p))
    }
    
    return true
  })
}

/**
 * 指令权限 v-permission
 * 用法: v-permission="'user:create'" 或 v-permission="['user:create', 'user:edit']"
 */
export const permissionDirective = {
  mounted(el: HTMLElement, binding: any) {
    const { value } = binding
    const userStore = useUserStore()
    
    let hasAuth = false
    
    if (typeof value === 'string') {
      hasAuth = userStore.hasPermission(value)
    } else if (Array.isArray(value)) {
      hasAuth = value.some(permission => userStore.hasPermission(permission))
    }
    
    if (!hasAuth) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 角色权限 v-role
 * 用法: v-role="1" 或 v-role="[1, 2]"
 */
export const roleDirective = {
  mounted(el: HTMLElement, binding: any) {
    const { value } = binding
    const userStore = useUserStore()
    
    let hasAuth = false
    
    if (typeof value === 'number') {
      hasAuth = userStore.currentRole === value
    } else if (Array.isArray(value)) {
      hasAuth = value.includes(userStore.currentRole)
    }
    
    if (!hasAuth) {
      el.parentNode?.removeChild(el)
    }
  }
}

export { RoleEnum, ROLE_PERMISSIONS }