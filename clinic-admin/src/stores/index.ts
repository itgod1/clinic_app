import { useUserStore, RoleEnum, ROLE_PERMISSIONS } from './user'
import { useTenantStore } from './tenant'

export { useUserStore, useTenantStore, RoleEnum, ROLE_PERMISSIONS }

// 重新导出类型
export type { UserInfo } from './user'
export type { TenantInfo } from './tenant'