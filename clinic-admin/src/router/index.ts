import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/storage'
import { useUserStore, RoleEnum } from '@/stores'

// 角色映射（路由角色名 -> 角色ID）
const ROLE_MAP: Record<string, number> = {
  'super_admin': RoleEnum.SUPER_ADMIN,
  'admin': RoleEnum.CLINIC_ADMIN,
  'doctor': RoleEnum.DOCTOR,
  'nurse': RoleEnum.NURSE,
  'cashier': RoleEnum.CASHIER,
  'storekeeper': RoleEnum.STOREKEEPER
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { 
          title: '工作台', 
          icon: 'Odometer',
          menuKey: 'dashboard',
          roles: ['super_admin', 'admin', 'doctor', 'nurse', 'cashier', 'storekeeper'] 
        }
      },
      {
        path: 'clinic/department',
        name: 'Department',
        component: () => import('@/views/clinic/department.vue'),
        meta: {
          title: '科室管理',
          icon: 'House',
          menuKey: 'clinic',
          roles: ['super_admin', 'admin']
        }
      },
      {
        path: 'doctor/list',
        name: 'DoctorList',
        component: () => import('@/views/doctor/list.vue'),
        meta: {
          title: '医生管理',
          icon: 'User',
          menuKey: 'clinic',
          roles: ['super_admin', 'admin']
        }
      },
      {
        path: 'doctor/schedule',
        name: 'DoctorSchedule',
        component: () => import('@/views/doctor/schedule.vue'),
        meta: {
          title: '排班管理',
          menuKey: 'clinic',
          roles: ['super_admin', 'admin']
        }
      },
      {
        path: 'doctor/workbench',
        name: 'DoctorWorkbench',
        component: () => import('@/views/doctor/workbench.vue'),
        meta: {
          title: '医生工作台',
          icon: 'FirstAidKit',
          menuKey: 'workbench',
          roles: ['super_admin', 'admin', 'doctor']
        }
      },
      {
        path: 'doctor/treat',
        name: 'DoctorTreat',
        component: () => import('@/views/doctor/treat.vue'),
        meta: {
          title: '患者诊疗',
          menuKey: 'workbench',
          roles: ['super_admin', 'admin', 'doctor'],
          hidden: true
        }
      },
      {
        path: 'patient/list',
        name: 'PatientList',
        component: () => import('@/views/patient/list.vue'),
        meta: {
          title: '患者档案',
          icon: 'UserFilled',
          menuKey: 'patient',
          roles: ['super_admin', 'admin', 'doctor', 'nurse', 'cashier']
        }
      },
      {
        path: 'medical-record/list',
        name: 'MedicalRecordArchive',
        component: () => import('@/views/medicalRecord/archive.vue'),
        meta: {
          title: '病历档案',
          icon: 'Document',
          menuKey: 'medicalRecord',
          roles: ['super_admin', 'admin', 'doctor', 'nurse']
        }
      },
      {
        path: 'consultation/list',
        name: 'ConsultationList',
        component: () => import('@/views/consultation/list.vue'),
        meta: {
          title: '咨询记录',
          menuKey: 'consultation',
          roles: ['super_admin', 'admin', 'doctor', 'nurse']
        }
      },
      {
        path: 'registration/list',
        name: 'RegistrationList',
        component: () => import('@/views/registration/list.vue'),
        meta: { 
          title: '挂号管理', 
          icon: 'Tickets',
          menuKey: 'registration',
          roles: ['super_admin', 'admin', 'doctor', 'nurse', 'cashier'] 
        }
      },
      {
        path: 'registration/prescription',
        name: 'PrescriptionList',
        component: () => import('@/views/prescription/list.vue'),
        meta: { 
          title: '处方管理',
          menuKey: 'registration',
          roles: ['super_admin', 'admin', 'doctor'] 
        }
      },
      {
        path: 'returnVisit/list',
        name: 'ReturnVisitList',
        component: () => import('@/views/returnVisit/list.vue'),
        meta: { 
          title: '回访中心', 
          icon: 'Phone',
          menuKey: 'returnVisit',
          roles: ['super_admin', 'admin', 'doctor', 'nurse'] 
        }
      },
      {
        path: 'returnVisit/revisit',
        name: 'RevisitReminder',
        component: () => import('@/views/returnVisit/revisit.vue'),
        meta: { 
          title: '复诊提醒', 
          icon: 'Calendar',
          menuKey: 'returnVisit',
          roles: ['super_admin', 'admin', 'doctor', 'nurse'] 
        }
      },
      {
        path: 'cashier/list',
        name: 'CashierList',
        component: () => import('@/views/cashier/list.vue'),
        meta: { 
          title: '待缴费', 
          icon: 'Money',
          menuKey: 'cashier',
          roles: ['super_admin', 'admin', 'cashier'] 
        }
      },
      {
        path: 'stock/list',
        name: 'StockList',
        component: () => import('@/views/pharmacy/list.vue'),
        meta: { 
          title: '药品管理', 
          icon: 'Box',
          menuKey: 'stock',
          roles: ['super_admin', 'admin', 'storekeeper'] 
        }
      },
      {
        path: 'member/list',
        name: 'MemberList',
        component: () => import('@/views/member/list.vue'),
        meta: { 
          title: '会员列表', 
          icon: 'Medal',
          menuKey: 'member',
          roles: ['super_admin', 'admin', 'cashier'] 
        }
      },
      {
        path: 'report/statistics',
        name: 'ReportStatistics',
        component: () => import('@/views/report/statistics.vue'),
        meta: {
          title: '统计概览',
          icon: 'DataAnalysis',
          menuKey: 'report',
          roles: ['super_admin', 'admin']
        }
      },
      {
        path: 'imaging/gallery',
        name: 'ImagingGallery',
        component: () => import('@/views/imaging/gallery.vue'),
        meta: {
          title: '影像管理',
          icon: 'Picture',
          menuKey: 'imaging',
          roles: ['super_admin', 'admin', 'doctor']
        }
      },
      {
        path: 'imaging/compare',
        name: 'ImagingCompare',
        component: () => import('@/views/imaging/compare.vue'),
        meta: {
          title: '影像对比',
          menuKey: 'imaging',
          roles: ['super_admin', 'admin', 'doctor'],
          hidden: true
        }
      },
      {
        path: 'system/user',
        name: 'SysUser',
        component: () => import('@/views/system/user.vue'),
        meta: { 
          title: '用户管理', 
          icon: 'Setting',
          menuKey: 'system',
          roles: ['super_admin', 'admin'] 
        }
      },
      {
        path: 'system/role',
        name: 'SysRole',
        component: () => import('@/views/system/role/list.vue'),
        meta: {
          title: '角色管理',
          icon: 'Setting',
          menuKey: 'system',
          roles: ['super_admin'],
          hidden: true
        }
      },
      {
        path: 'system/menu',
        name: 'SysMenu',
        component: () => import('@/views/system/menu/list.vue'),
        meta: {
          title: '菜单管理',
          icon: 'Setting',
          menuKey: 'system',
          roles: ['super_admin'],
          hidden: true
        }
      },
      {
        path: 'system/operation-log',
        name: 'OperationLog',
        component: () => import('@/views/system/operationLog/list.vue'),
        meta: { 
          title: '操作日志', 
          icon: 'Setting',
          menuKey: 'system',
          roles: ['super_admin', 'admin'] 
        }
      },
      {
        path: 'system/tenant',
        name: 'Tenant',
        component: () => import('@/views/system/tenant.vue'),
        meta: {
          title: '租户管理',
          icon: 'OfficeBuilding',
          menuKey: 'tenant',
          roles: ['super_admin'] // 仅超级管理员可见
        }
      },
      // ============= 医保管理 =============
      {
        path: 'insurance/patient',
        name: 'InsurancePatient',
        component: () => import('@/views/insurance/patient.vue'),
        meta: {
          title: '医保患者',
          menuKey: 'insurance',
          roles: ['super_admin', 'admin', 'cashier']
        }
      },
      {
        path: 'insurance/catalog',
        name: 'InsuranceCatalog',
        component: () => import('@/views/insurance/catalog.vue'),
        meta: {
          title: '医保目录',
          menuKey: 'insurance',
          roles: ['super_admin', 'admin']
        }
      },
      {
        path: 'insurance/settlement',
        name: 'InsuranceSettlement',
        component: () => import('@/views/insurance/settlement.vue'),
        meta: {
          title: '医保结算',
          menuKey: 'insurance',
          roles: ['super_admin', 'admin', 'cashier']
        }
      },
      {
        path: 'insurance/settlement-create',
        name: 'InsuranceSettlementCreate',
        component: () => import('@/views/insurance/settlement-create.vue'),
        meta: {
          title: '医保结算',
          menuKey: 'insurance',
          roles: ['super_admin', 'admin', 'cashier'],
          hidden: true
        }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/404/index.vue'),
    meta: { title: '404', public: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 检查是否有路由权限
 */
const hasRoutePermission = (to: any): boolean => {
  // 公共路由允许访问
  if (to.meta?.public) return true
  
  const userStore = useUserStore()
  const userRole = userStore.currentRole
  
  // 如果没有用户信息（如接口 404），允许访问 dashboard
  // 这样用户至少可以退出登录
  if (!userRole) {
    return to.path === '/dashboard' || to.path === '/'
  }
  
  // 超级管理员有所有权限
  if (userStore.isSuperAdmin) return true
  
  // 检查路由角色限制
  if (to.meta?.roles && Array.isArray(to.meta.roles)) {
    const allowedRoleIds = to.meta.roles
      .map((roleName: string) => ROLE_MAP[roleName])
      .filter(Boolean)
    
    if (allowedRoleIds.length > 0 && !allowedRoleIds.includes(userRole)) {
      return false
    }
  }
  
  // 检查菜单权限
  if (to.meta?.menuKey) {
    return userStore.hasMenuPermission(to.meta.menuKey)
  }
  
  return true
}

router.beforeEach(async (to, from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - 口腔诊所管理系统`
  }

  const token = getToken()
  
  console.log('路由守卫:', { to: to.path, from: from.path, hasToken: !!token })
  
  // 未登录且访问非公开页面
  if (to.path !== '/login' && !token) {
    console.log('未登录，跳转到登录页')
    next('/login')
    return
  }
  
  // 已登录访问登录页
  if (to.path === '/login' && token) {
    console.log('已登录访问登录页，跳转到首页')
    next('/')
    return
  }
  
  // 已登录访问其他页面，检查权限
  if (token && !to.meta?.public) {
    const userStore = useUserStore()
    
    console.log('检查权限:', { userInfo: !!userStore.userInfo, currentRole: userStore.currentRole })
    
    // 首次加载，尝试获取用户信息
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
        console.log('获取用户信息成功:', userStore.userInfo)
      } catch (error: any) {
        // 如果是 401，说明 token 过期，清除登录状态
        if (error?.response?.status === 401) {
          userStore.clearUserInfo()
          ElMessage.error('登录已过期，请重新登录')
          next('/login')
          return
        }
        
        // 如果是 404，说明接口不存在，尝试从 storage 恢复用户信息
        // 不从 localStorage 恢复，只是继续访问，让页面自己处理
        console.warn('获取用户信息失败:', error?.message || error)
      }
    }
    
    // 检查路由权限（如果获取用户信息失败，允许继续访问）
    const hasPermission = hasRoutePermission(to)
    console.log('权限检查结果:', { path: to.path, hasPermission })
    
    if (!hasPermission) {
      ElMessage.error('没有权限访问该页面')
      next('/404')
      return
    }
  }
  
  next()
})

export default router