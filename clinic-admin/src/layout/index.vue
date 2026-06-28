<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '72px' : '260px'" class="aside">
      <div class="logo" :class="{ collapse: isCollapse }">
        <el-icon v-if="!isCollapse" :size="32" color="#409eff"><House /></el-icon>
        <span v-if="!isCollapse">诊所管理系统</span>
        <el-icon v-else :size="24"><House /></el-icon>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        class="aside-menu"
      >
        <el-menu-item v-if="hasPermission('dashboard')" index="/dashboard" @click="goTo('/dashboard')">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <el-sub-menu v-if="hasPermission('clinic')" index="clinic">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>诊所管理</span>
          </template>
          <el-menu-item index="/clinic/department" @click="goTo('/clinic/department')">科室管理</el-menu-item>
          <el-menu-item index="/doctor/list" @click="goTo('/doctor/list')">医生管理</el-menu-item>
          <el-menu-item index="/doctor/schedule" @click="goTo('/doctor/schedule')">排班管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPermission('workbench')" index="/doctor/workbench" @click="goTo('/doctor/workbench')">
          <el-icon><FirstAidKit /></el-icon>
          <span>医生工作台</span>
        </el-menu-item>

        <el-sub-menu v-if="hasPermission('patient')" index="patient">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>患者管理</span>
          </template>
          <el-menu-item index="/patient/list" @click="goTo('/patient/list')">患者档案</el-menu-item>
          <el-menu-item index="/medical-record/list" @click="goTo('/medical-record/list')">病历档案</el-menu-item>
          <el-menu-item index="/consultation/list" @click="goTo('/consultation/list')">咨询记录</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasPermission('registration')" index="registration">
          <template #title>
            <el-icon><Tickets /></el-icon>
            <span>诊疗管理</span>
          </template>
          <el-menu-item index="/registration/list" @click="goTo('/registration/list')">挂号管理</el-menu-item>
          <el-menu-item index="/registration/prescription" @click="goTo('/registration/prescription')">处方管理</el-menu-item>
          <el-menu-item index="/returnVisit/list" @click="goTo('/returnVisit/list')">回访中心</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPermission('cashier')" index="/cashier/list" @click="goTo('/cashier/list')">
          <el-icon><Money /></el-icon>
          <span>收费管理</span>
        </el-menu-item>

        <el-sub-menu v-if="hasPermission('insurance')" index="insurance">
          <template #title>
            <el-icon><Umbrella /></el-icon>
            <span>医保管理</span>
          </template>
          <el-menu-item index="/insurance/patient" @click="goTo('/insurance/patient')">医保患者</el-menu-item>
          <el-menu-item index="/insurance/catalog" @click="goTo('/insurance/catalog')">医保目录</el-menu-item>
          <el-menu-item index="/insurance/settlement" @click="goTo('/insurance/settlement')">医保结算</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPermission('stock')" index="/stock/list" @click="goTo('/stock/list')">
          <el-icon><Box /></el-icon>
          <span>药库管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('member')" index="/member/list" @click="goTo('/member/list')">
          <el-icon><Medal /></el-icon>
          <span>会员管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('imaging')" index="/imaging/gallery" @click="goTo('/imaging/gallery')">
          <el-icon><Picture /></el-icon>
          <span>影像管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('report')" index="/report/statistics" @click="goTo('/report/statistics')">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计报表</span>
        </el-menu-item>

        <el-sub-menu v-if="hasPermission('system')" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user" @click="goTo('/system/user')">用户管理</el-menu-item>
          <el-menu-item index="/system/operation-log" @click="goTo('/system/operation-log')">操作日志</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPermission('tenant')" index="/system/tenant" @click="goTo('/system/tenant')">
          <el-icon><OfficeBuilding /></el-icon>
          <span>租户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="router.push('/dashboard')">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumb">{{ breadcrumb }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 租户选择器 -->
          <TenantSelector class="tenant-selector-wrapper" />
          
          <el-divider direction="vertical" />
          
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo?.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              <div class="user-meta">
                <span class="username">{{ userInfo?.realName || '管理员' }}</span>
                <span class="role-name">{{ userInfo?.roleName || '' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="info">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="changePasswordLoading">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage, type FormInstance } from 'element-plus'
import { Fold, Expand, House, ArrowDown, Odometer, User, UserFilled, Tickets, Money, Box, Medal, DataAnalysis, Setting, FirstAidKit, OfficeBuilding, Document, Picture, Umbrella } from '@element-plus/icons-vue'
import TenantSelector from '@/components/TenantSelector.vue'
import { useUserStore, useTenantStore } from '@/stores'
import { getClinicId, storage } from '@/utils/storage'
import { changePassword } from '@/api/system'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tenantStore = useTenantStore()

const isCollapse = ref(false)

// 从 store 获取用户信息
const userInfo = computed(() => userStore.userInfo)

// 权限检查
const hasPermission = (menuKey: string) => {
  const result = userStore.hasMenuPermission(menuKey)
  // 调试日志（生产环境可删除）
  if (menuKey === 'tenant') {
    console.log('租户菜单权限检查:', {
      menuKey,
      result,
      roleId: userStore.currentRole,
      isSuperAdmin: userStore.isSuperAdmin
    })
  }
  return result
}

const activeMenu = computed(() => route.path)

const breadcrumb = computed(() => {
  const path = route.path
  if (path === '/dashboard') return ''
  if (path === '/clinic/department') return '科室管理'
  if (path === '/doctor/list') return '医生管理'
  if (path === '/doctor/schedule') return '排班管理'
  if (path === '/doctor/workbench') return '医生工作台'
  if (path === '/doctor/treat') return '患者诊疗'
  if (path === '/patient/list') return '患者档案'
  if (path === '/medical-record/list') return '病历档案'
  if (path === '/consultation/list') return '咨询记录'
  if (path === '/registration/list') return '挂号管理'
  if (path === '/registration/prescription') return '处方管理'
  if (path === '/cashier/list') return '待缴费'
  if (path === '/stock/list') return '药品管理'
  if (path === '/member/list') return '会员列表'
  if (path === '/report/statistics') return '统计概览'
  if (path === '/imaging/gallery') return '影像管理'
  if (path === '/imaging/compare') return '影像对比'
  if (path === '/system/user') return '用户管理'
  if (path === '/system/role') return '角色管理'
  if (path === '/system/menu') return '菜单管理'
  if (path === '/system/operation-log') return '操作日志'
  if (path === '/insurance/patient') return '医保患者'
  if (path === '/insurance/catalog') return '医保目录'
  if (path === '/insurance/settlement') return '医保结算'
  if (path === '/insurance/settlement-create') return '医保结算'
  return ''
})

// 修改密码相关
const passwordDialogVisible = ref(false)
const changePasswordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 使用 store 的登出方法
    await userStore.logout()
  } else if (command === 'info') {
    // TODO: 打开个人信息页面
    router.push('/system/user')
  } else if (command === 'password') {
    // 打开修改密码对话框
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
  }
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    changePasswordLoading.value = true

    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    // 修改成功后退出登录
    setTimeout(() => {
      userStore.logout()
    }, 1500)
  } catch (error: any) {
    console.error('修改密码失败:', error)
    ElMessage.error(error?.response?.data?.message || '修改密码失败')
  } finally {
    changePasswordLoading.value = false
  }
}

const goTo = (path: string) => {
  router.push(path)
}

onMounted(async () => {
  // 初始化用户信息（如果还没有加载）
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      console.error('获取用户信息失败')
      // 获取失败，跳转到登录页
      router.push('/login')
      return
    }
  }

  // 初始化租户信息
  try {
    await tenantStore.initTenant()
  } catch (e) {
    console.error('初始化租户信息失败')
  }
})
</script>

<style lang="scss" scoped>
// 样式已移至 styles/layout.scss
// 这里保留一些组件特有的样式
</style>