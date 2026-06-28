<template>
  <div class="page-container">
    <h2>404 - 页面不存在</h2>
    <p class="desc">您访问的页面不存在或没有权限访问</p>
    <div class="actions">
      <el-button type="primary" @click="goHome">返回首页</el-button>
      <el-button @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { storage } from '@/utils/storage'

const router = useRouter()
const userStore = useUserStore()

const goHome = () => {
  router.push('/')
}

const logout = async () => {
  try {
    await userStore.logout()
  } catch (error) {
    // 如果 logout 接口失败，强制清除本地状态
    storage.clear()
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;

  h2 {
    font-size: 48px;
    color: #303133;
    margin-bottom: 16px;
  }

  .desc {
    font-size: 16px;
    color: #909399;
    margin-bottom: 32px;
  }

  .actions {
    display: flex;
    gap: 16px;
  }
}
</style>