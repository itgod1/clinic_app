const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'
const CLINIC_ID_KEY = 'clinicId'
const CLINIC_NAME_KEY = 'clinicName'
const CLINIC_LIST_KEY = 'clinicList'

export const storage = {
  // Token
  getToken() {
    return uni.getStorageSync(TOKEN_KEY)
  },
  setToken(token) {
    uni.setStorageSync(TOKEN_KEY, token)
  },
  removeToken() {
    uni.removeStorageSync(TOKEN_KEY)
  },
  
  // 用户信息
  getUserInfo() {
    try {
      const data = uni.getStorageSync(USER_INFO_KEY)
      return data ? JSON.parse(data) : null
    } catch {
      return null
    }
  },
  setUserInfo(userInfo) {
    uni.setStorageSync(USER_INFO_KEY, JSON.stringify(userInfo))
  },
  removeUserInfo() {
    uni.removeStorageSync(USER_INFO_KEY)
  },
  
  // 诊所ID
  getClinicId() {
    return uni.getStorageSync(CLINIC_ID_KEY)
  },
  setClinicId(clinicId) {
    uni.setStorageSync(CLINIC_ID_KEY, clinicId)
  },
  removeClinicId() {
    uni.removeStorageSync(CLINIC_ID_KEY)
  },
  
  // 诊所名称
  getClinicName() {
    return uni.getStorageSync(CLINIC_NAME_KEY)
  },
  setClinicName(name) {
    uni.setStorageSync(CLINIC_NAME_KEY, name)
  },
  removeClinicName() {
    uni.removeStorageSync(CLINIC_NAME_KEY)
  },
  
  // 诊所列表
  getClinicList() {
    try {
      const data = uni.getStorageSync(CLINIC_LIST_KEY)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  },
  setClinicList(list) {
    uni.setStorageSync(CLINIC_LIST_KEY, JSON.stringify(list))
  },
  removeClinicList() {
    uni.removeStorageSync(CLINIC_LIST_KEY)
  },
  
  // 清空所有
  clear() {
    this.removeToken()
    this.removeUserInfo()
    this.removeClinicId()
    this.removeClinicName()
    this.removeClinicList()
  }
}

export default storage
