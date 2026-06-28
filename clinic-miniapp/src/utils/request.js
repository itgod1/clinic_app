// 基础 URL 配置
// 开发环境使用 localhost，真机调试需要修改为电脑局域网 IP
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://10.13.34.148:8080/api'

// 不需要clinicId的白名单
const NO_CLINIC_ID_URLS = [
  '/auth/',
  '/public/',
  '/clinic/list',
  '/miniapp/clinic/',
  '/miniapp/clinics',
  '/miniapp/auth/',
  '/miniapp/schedule/',
  '/miniapp/doctor/',
  '/ai/return-visit/',
  '/ai/dify/'
]

// 完全公开接口（不需要token，401不跳转）
const PUBLIC_URLS = [
  '/auth/',
  '/public/',
  '/miniapp/'
]

// AI回访接口（需要X-API-Key）
const AI_RETURN_VISIT_URLS = [
  '/ai/return-visit/'
]

// 检查是否是AI回访接口
const isAIReturnVisitUrl = (url) => {
  return AI_RETURN_VISIT_URLS.some(u => url.includes(u))
}

// 检查是否是公开接口
const isPublicUrl = (url) => {
  return PUBLIC_URLS.some(u => url.includes(u))
}

// 检查是否需要clinicId
const needClinicId = (url) => {
  return !NO_CLINIC_ID_URLS.some(u => url.includes(u))
}

const request = (options) => {
  return new Promise((resolve, reject) => {
    const url = options.url
    const method = (options.method || 'GET').toUpperCase()

    // 处理数据，自动添加clinicId
    let data = options.data || {}
    let params = options.params || {}

    // 获取token、clinicId和userInfo
    const token = uni.getStorageSync('token')
    const clinicId = uni.getStorageSync('clinicId')
    const userInfoStr = uni.getStorageSync('userInfo')
    let userId = null
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        userId = userInfo.userId || userInfo.id
        console.log('获取到userId:', userId)
      } catch (e) {
        console.error('解析userInfo失败:', e)
      }
    }

    // 自动添加clinicId
    if (needClinicId(url)) {
      if (!clinicId) {
        uni.showToast({ title: '请先选择诊所', icon: 'none' })
        reject(new Error('请先选择诊所'))
        return
      }
      if (method === 'GET') {
        params = { ...params, clinicId }
      } else {
        data = { ...data, clinicId }
      }
    }

    // 小程序就诊人和缴费接口自动添加userId
    console.log('检查就诊人接口:', url, 'userId:', userId, 'method:', method)
    const needUserIdUrls = ['/miniapp/patient/', '/miniapp/payments/']
    const needUserId = needUserIdUrls.some(u => url.includes(u))
    if (needUserId && userId) {
      if (method === 'GET') {
        params = { ...params, userId }
        console.log('GET请求添加userId到params:', params)
      } else {
        data = { ...data, userId }
        console.log('POST请求添加userId到data:', data)
      }
    }

    // 构建完整URL
    let fullUrl = BASE_URL + url

    // GET请求参数拼接到URL
    if (method === 'GET' && Object.keys(params).length > 0) {
      const queryString = Object.keys(params)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
        .join('&')
      fullUrl += (fullUrl.includes('?') ? '&' : '?') + queryString
    }

    // 构建请求配置
    const requestConfig = {
      url: fullUrl,
      method: method,
      data: method !== 'GET' ? data : undefined,
      header: {
        'Content-Type': 'application/json'
      }
    }

    // 调试：打印POST请求的数据
    if (method !== 'GET' && data) {
      console.log('POST请求data:', JSON.stringify(data))
    }

    // 手动添加Authorization头（确保格式正确）
    if (token) {
      const authValue = 'Bearer ' + token
      requestConfig.header['Authorization'] = authValue
      console.log('设置Authorization:', authValue.substring(0, 30) + '...')
    }

    if (clinicId) {
      requestConfig.header['X-Tenant-Id'] = clinicId
    }

    // AI回访接口添加X-API-Key
    if (isAIReturnVisitUrl(url)) {
      requestConfig.header['X-API-Key'] = 'sk-clinic-ai-2025-a1b2c3d4e5f6'
    }

    // 调试信息
    console.log('====== 请求调试 ======')
    console.log('请求URL:', fullUrl)
    console.log('请求header:', JSON.stringify(requestConfig.header))

    uni.request({
      ...requestConfig,
      success: (res) => {
        console.log('响应状态:', res.statusCode)
        if (res.statusCode === 200) {
          const result = res.data
          if (result.code === 200) {
            resolve(result)
          } else if (result.code === 401) {
            // Token过期
            // 公开接口返回401时不自动跳转（可能是接口设计问题）
            if (isPublicUrl(url)) {
              reject(result)
              return
            }
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.showToast({ title: '登录已过期', icon: 'none' })
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/login/index' })
            }, 1500)
            reject(result)
          } else if (result.code === 403) {
            // 无权限访问该诊所
            uni.showModal({
              title: '提示',
              content: '您没有权限访问该诊所',
              showCancel: false,
              success: () => {
                uni.reLaunch({ url: '/pages/clinic/select' })
              }
            })
            reject(result)
          } else {
            uni.showToast({
              title: result.message || '请求失败',
              icon: 'none'
            })
            reject(result)
          }
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        console.log('请求失败:', err)
        uni.showToast({ title: '网络请求失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 快捷方法
export const get = (url, params, config = {}) => {
  return request({ url, method: 'GET', params, ...config })
}

export const post = (url, data, config = {}) => {
  return request({ url, method: 'POST', data, ...config })
}

export const put = (url, data, config = {}) => {
  return request({ url, method: 'PUT', data, ...config })
}

export const del = (url, data, config = {}) => {
  return request({ url, method: 'DELETE', data, ...config })
}

export default request
