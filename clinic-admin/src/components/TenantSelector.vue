<template>
  <div class="tenant-selector">
    <!-- 单租户显示（普通用户） -->
    <div v-if="!canSwitchTenant" class="tenant-display">
      <el-icon class="tenant-icon"><OfficeBuilding /></el-icon>
      <span class="tenant-name" :title="currentTenantName">{{ currentTenantName }}</span>
    </div>

    <!-- 多租户切换（管理员） -->
    <el-dropdown
      v-else
      trigger="click"
      :disabled="switchLoading"
      @command="handleSwitchTenant"
    >
      <div class="tenant-dropdown-trigger" :class="{ 'is-loading': switchLoading }">
        <el-icon class="tenant-icon"><OfficeBuilding /></el-icon>
        <span class="tenant-name" :title="currentTenantName">{{ currentTenantName }}</span>
        <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
        <el-icon v-if="switchLoading" class="loading-icon"><Loading /></el-icon>
      </div>
      
      <template #dropdown>
        <el-dropdown-menu class="tenant-dropdown-menu">
          <!-- 当前租户信息 -->
          <div class="current-tenant-info" v-if="currentTenant">
            <div class="info-header">
              <el-tag size="small" type="success">当前</el-tag>
              <span class="clinic-code">{{ currentTenant.clinicCode }}</span>
            </div>
            <div class="info-body">
              <p class="clinic-name">{{ currentTenant.clinicName }}</p>
              <p class="clinic-address" v-if="currentTenant.address">
                <el-icon><Location /></el-icon>
                {{ currentTenant.address }}
              </p>
            </div>
          </div>

          <el-divider v-if="tenantList.length > 1" />

          <!-- 租户列表 -->
          <div v-if="tenantList.length > 1" class="tenant-list-header">
            <span>切换诊所</span>
            <el-tag size="small" type="info">{{ tenantList.length }}个</el-tag>
          </div>
          
          <el-dropdown-item
            v-for="tenant in otherTenants"
            :key="tenant.id"
            :command="tenant.id"
            :disabled="tenant.id === currentTenantId"
          >
            <div class="tenant-item">
              <el-icon><OfficeBuilding /></el-icon>
              <div class="tenant-item-info">
                <span class="name">{{ tenant.clinicName }}</span>
                <span class="code">{{ tenant.clinicCode }}</span>
              </div>
              <el-icon v-if="tenant.id === currentTenantId" class="check-icon"><Check /></el-icon>
            </div>
          </el-dropdown-item>

          <!-- 管理入口（超级管理员） -->
          <template v-if="canManageAllTenants">
            <el-divider />
            <el-dropdown-item command="manage" divided>
              <div class="tenant-item manage-item">
                <el-icon><Setting /></el-icon>
                <span>诊所管理</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="create" v-if="canCreateTenant">
              <div class="tenant-item manage-item">
                <el-icon><Plus /></el-icon>
                <span>新增诊所</span>
              </div>
            </el-dropdown-item>
          </template>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 新增诊所对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="新增诊所"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="诊所名称" prop="clinicName">
          <el-input v-model="createForm.clinicName" placeholder="请输入诊所名称" />
        </el-form-item>
        <el-form-item label="诊所编码" prop="clinicCode">
          <el-input v-model="createForm.clinicCode" placeholder="请输入唯一编码" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="createForm.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="createForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="createForm.address" placeholder="请输入地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateTenant">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { 
  OfficeBuilding, 
  ArrowDown, 
  Loading, 
  Location, 
  Check, 
  Setting, 
  Plus 
} from '@element-plus/icons-vue'
import { useTenantStore, useUserStore } from '@/stores'

const router = useRouter()
const tenantStore = useTenantStore()
const userStore = useUserStore()

// 响应式数据
const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = ref({
  clinicName: '',
  clinicCode: '',
  contactPerson: '',
  contactPhone: '',
  address: ''
})

const createRules: FormRules = {
  clinicName: [
    { required: true, message: '请输入诊所名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  clinicCode: [
    { required: true, message: '请输入诊所编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '只能包含字母、数字、下划线和横线', trigger: 'blur' }
  ],
  contactPerson: [
    { required: true, message: '请输入联系人', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// Computed
const currentTenantName = computed(() => tenantStore.currentTenantName)
const currentTenantId = computed(() => tenantStore.currentTenantId)
const currentTenant = computed(() => tenantStore.currentTenant)
const tenantList = computed(() => tenantStore.tenantList)
const canSwitchTenant = computed(() => tenantStore.canSwitchTenant)
const canManageAllTenants = computed(() => userStore.canManageAllTenants)
const canCreateTenant = computed(() => userStore.canCreateTenant)
const switchLoading = computed(() => tenantStore.switchLoading)

// 过滤出其他租户（不包括当前）
const otherTenants = computed(() => {
  return tenantList.value.filter(t => t.id !== currentTenantId.value)
})

// 切换租户
const handleSwitchTenant = async (command: string | number) => {
  if (typeof command === 'number') {
    // 确认切换
    try {
      await ElMessageBox.confirm(
        '切换诊所后页面将刷新，是否继续？',
        '切换诊所',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      const success = await tenantStore.switchTenant(command)
      if (success) {
        ElMessage.success('诊所切换成功')
      }
    } catch (error) {
      // 用户取消
    }
  } else if (command === 'manage') {
    // 跳转到诊所管理页面
    router.push('/system/tenant')
  } else if (command === 'create') {
    // 打开新增诊所对话框
    createDialogVisible.value = true
  }
}

// 创建租户
const handleCreateTenant = async () => {
  if (!createFormRef.value) return
  
  try {
    await createFormRef.value.validate()
    createLoading.value = true
    
    // TODO: 调用创建租户 API
    // await createTenant(createForm.value)
    
    ElMessage.success('诊所创建成功')
    createDialogVisible.value = false
    
    // 刷新租户列表
    await tenantStore.loadTenantList()
  } catch (error) {
    console.error('创建诊所失败:', error)
    ElMessage.error('创建诊所失败')
  } finally {
    createLoading.value = false
  }
}

// 初始化
onMounted(() => {
  tenantStore.initTenant()
})
</script>

<style lang="scss" scoped>
.tenant-selector {
  display: inline-flex;
  align-items: center;
}

.tenant-display {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 4px;
  color: #409eff;
  font-size: 14px;

  .tenant-icon {
    font-size: 16px;
  }

  .tenant-name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.tenant-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 4px;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(64, 158, 255, 0.2);
  }

  &.is-loading {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .tenant-icon {
    font-size: 16px;
  }

  .tenant-name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .dropdown-arrow {
    font-size: 12px;
    transition: transform 0.3s;
  }

  &:hover .dropdown-arrow {
    transform: rotate(180deg);
  }

  .loading-icon {
    animation: rotating 2s linear infinite;
  }
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

<style lang="scss">
// 下拉菜单样式
.tenant-dropdown-menu {
  min-width: 280px;
  padding: 8px 0;

  .current-tenant-info {
    padding: 12px 16px;
    background: #f5f7fa;
    margin: 0 8px 8px;
    border-radius: 4px;

    .info-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .clinic-code {
        font-size: 12px;
        color: #909399;
      }
    }

    .info-body {
      .clinic-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin: 0 0 4px;
      }

      .clinic-address {
        font-size: 12px;
        color: #909399;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 4px;

        .el-icon {
          font-size: 12px;
        }
      }
    }
  }

  .el-divider {
    margin: 8px 0;
  }

  .tenant-list-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 16px;
    font-size: 12px;
    color: #909399;
  }

  .tenant-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;

    .el-icon {
      font-size: 16px;
      color: #909399;
    }

    .tenant-item-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .name {
        font-size: 14px;
        color: #303133;
      }

      .code {
        font-size: 12px;
        color: #909399;
      }
    }

    .check-icon {
      color: #67c23a;
      font-size: 14px;
    }

    &.manage-item {
      color: #409eff;

      .el-icon {
        color: #409eff;
      }
    }
  }

  .el-dropdown-menu__item {
    padding: 8px 16px;
    line-height: normal;

    &:hover {
      background: #f5f7fa;
    }

    &.is-disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}
</style>