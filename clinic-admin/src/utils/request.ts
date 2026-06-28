import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { storage } from './storage'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

interface RequestConfig extends InternalAxiosRequestConfig {
  loading?: boolean
  skipTenantId?: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 不需要租户ID的公共接口列表
const PUBLIC_URLS = [
  '/auth/login',
  '/auth/register',
  '/auth/captcha',
  '/auth/refresh',
  '/public/',
  '/upload/'
]

// 检查是否为公共接口
const isPublicUrl = (url?: string): boolean => {
  if (!url) return false
  return PUBLIC_URLS.some(publicUrl => url.includes(publicUrl))
}

service.interceptors.request.use(
  (config) => {
    // 注入 Token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 自动注入租户ID（多租户支持）
    // 条件：1. 不是公共接口 2. 没有明确跳过 3. 有 clinicId
    if (!isPublicUrl(config.url) && !config.skipTenantId) {
      const clinicId = storage.getClinicId()
      if (clinicId) {
        const method = config.method?.toLowerCase()
        
        // GET 请求：添加到 params
        if (method === 'get') {
          config.params = {
            ...config.params,
            _tenantId: clinicId  // 使用 _tenantId 避免与业务参数冲突
          }
        }
        // POST/PUT/DELETE 请求：添加到 data
        else if (['post', 'put', 'delete', 'patch'].includes(method || '')) {
          // 如果 data 是 FormData，追加字段
          if (config.data instanceof FormData) {
            config.data.append('_tenantId', clinicId.toString())
          }
          // 如果 data 是普通对象，合并字段
          else if (config.data && typeof config.data === 'object') {
            config.data = {
              ...config.data,
              _tenantId: clinicId
            }
          }
          // 如果没有 data，创建新对象
          else {
            config.data = { _tenantId: clinicId }
          }
        }
        
        // 同时添加到 Header，方便后端统一获取
        config.headers['X-Tenant-Id'] = clinicId.toString()
      }
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      return Promise.reject(new Error(res.message || '未授权'))
    } else if (res.code === 403) {
      ElMessage.error('没有权限访问该资源')
      return Promise.reject(new Error(res.message || '禁止访问'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 400:
          ElMessage.error('参数错误')
          break
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error('请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export const request = async <T = any>(
  config: RequestConfig
): Promise<ApiResponse<T>> => {
  return service(config) as Promise<ApiResponse<T>>
}

export const get = async <T = any>(
  url: string,
  params?: any,
  config?: RequestConfig
): Promise<ApiResponse<T>> => {
  return request<T>({
    url,
    method: 'GET',
    params,
    ...config
  })
}

export const post = async <T = any>(
  url: string,
  data?: any,
  config?: RequestConfig
): Promise<ApiResponse<T>> => {
  return request<T>({
    url,
    method: 'POST',
    data,
    ...config
  })
}

export const put = async <T = any>(
  url: string,
  data?: any,
  config?: RequestConfig
): Promise<ApiResponse<T>> => {
  return request<T>({
    url,
    method: 'PUT',
    data,
    ...config
  })
}

export const del = async <T = any>(
  url: string,
  params?: any,
  config?: RequestConfig
): Promise<ApiResponse<T>> => {
  return request<T>({
    url,
    method: 'DELETE',
    params,
    ...config
  })
}

export default service