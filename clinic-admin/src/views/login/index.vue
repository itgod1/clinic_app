<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="login-title">
          <h1>口腔诊所管理系统</h1>
          <p>Clinic Management System</p>
        </div>
        <div class="login-desc">
          <p>高效管理 · 智能诊疗 · 优质服务</p>
        </div>
      </div>
      <div class="login-right">
        <h2>用户登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
          <el-form-item>
            <div class="register-link">
              <el-button type="primary" link @click="showRegisterDialog = true">没有账号？立即注册</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-dialog v-model="showRegisterDialog" title="用户注册" width="500px" destroy-on-close>
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegisterDialog = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'
import { useUserStore, useTenantStore } from '@/stores'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const tenantStore = useTenantStore()

const formRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registerLoading = ref(false)
const showRegisterDialog = ref(false)

const form = reactive({
  username: '',
  password: '',
  loginType: 2
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    const res = await login(form)
    
    console.log('登录成功，返回数据:', res.data)
    console.log('userInfo:', res.data.userInfo)
    console.log('roleId:', res.data.userInfo?.roleId)
    
    // 使用 store 存储登录信息
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    
    // 初始化租户信息
    if (res.data.userInfo.clinicId) {
      tenantStore.setCurrentTenantId(res.data.userInfo.clinicId)
    }
    
    ElMessage.success('登录成功')
    
    // 延迟跳转，确保数据保存完成
    setTimeout(() => {
      console.log('准备跳转，当前 roleId:', userStore.currentRole)
      router.push('/')
    }, 100)
  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  try {
    await registerFormRef.value?.validate()
    registerLoading.value = true
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    showRegisterDialog.value = false
    form.username = registerForm.username
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.realName = ''
    registerForm.phone = ''
    registerForm.email = ''
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    registerLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-left {
  width: 450px;
  background: linear-gradient(135deg, #409eff 0%, #667eea 100%);
  padding: 60px 40px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .login-title {
    h1 {
      font-size: 32px;
      margin-bottom: 10px;
    }
    p {
      font-size: 16px;
      opacity: 0.8;
    }
  }

  .login-desc {
    margin-top: 60px;
    font-size: 18px;
    opacity: 0.9;
  }
}

.login-right {
  flex: 1;
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;

  h2 {
    font-size: 28px;
    color: #303133;
    margin-bottom: 40px;
    text-align: center;
  }

  .login-form {
    .login-btn {
      width: 100%;
    }

    .register-link {
      width: 100%;
      text-align: center;
    }
  }
}
</style>