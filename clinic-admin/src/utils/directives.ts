import type { App } from 'vue'
import { permissionDirective, roleDirective } from './permission'

/**
 * 注册全局指令
 */
export const setupDirectives = (app: App) => {
  // 权限指令 v-permission
  app.directive('permission', permissionDirective)
  
  // 角色指令 v-role
  app.directive('role', roleDirective)
}

export default setupDirectives