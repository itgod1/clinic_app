// 诊所检查mixin
export const clinicCheck = {
  onShow() {
    this.checkClinic()
  },
  
  methods: {
    checkClinic() {
      const clinicId = uni.getStorageSync('clinicId')
      const token = uni.getStorageSync('token')
      
      // 未登录
      if (!token) {
        uni.reLaunch({ url: '/pages/login/index' })
        return false
      }
      
      // 未选择诊所
      if (!clinicId) {
        const clinicList = uni.getStorageSync('clinicList')
        if (clinicList && clinicList.length > 0) {
          uni.reLaunch({ url: '/pages/clinic/select' })
        } else {
          uni.reLaunch({ url: '/pages/login/index' })
        }
        return false
      }
      
      return true
    }
  }
}
