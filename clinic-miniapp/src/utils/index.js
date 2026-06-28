import dayjs from 'dayjs'

// 格式化日期
export const formatDate = (date, format = 'YYYY-MM-DD') => {
  if (!date) return ''
  return dayjs(date).format(format)
}

// 格式化日期时间
export const formatDateTime = (date, format = 'YYYY-MM-DD HH:mm') => {
  if (!date) return ''
  return dayjs(date).format(format)
}

// 格式化手机号
export const formatPhone = (phone) => {
  if (!phone || phone.length !== 11) return phone
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// 防抖
export const debounce = (fn, delay = 300) => {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

// 节流
export const throttle = (fn, delay = 300) => {
  let timer = null
  return function(...args) {
    if (timer) return
    timer = setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}

// 生成最近7天日期
export const generateDateList = (days = 7) => {
  const list = []
  const today = new Date()
  
  for (let i = 0; i < days; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + i)
    list.push({
      date: dayjs(date).format('YYYY-MM-DD'),
      day: i === 0 ? '今天' : i === 1 ? '明天' : ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()],
      weekDay: date.getDay()
    })
  }
  
  return list
}

// 检查是否今天
export const isToday = (date) => {
  return dayjs(date).isSame(dayjs(), 'day')
}

// 计算年龄
export const calculateAge = (birthDate) => {
  if (!birthDate) return '-'
  const birth = dayjs(birthDate)
  const now = dayjs()
  const years = now.diff(birth, 'year')
  if (years < 1) {
    const months = now.diff(birth, 'month')
    return months < 1 ? '1个月' : `${months}个月`
  }
  return `${years}岁`
}

// 深拷贝
export const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj.getTime())
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  if (obj instanceof Object) {
    const copy = {}
    Object.keys(obj).forEach(key => {
      copy[key] = deepClone(obj[key])
    })
    return copy
  }
  return obj
}

// 显示加载中
export const showLoading = (title = '加载中...') => {
  uni.showLoading({ title, mask: true })
}

// 隐藏加载
export const hideLoading = () => {
  uni.hideLoading()
}

// 显示成功提示
export const showSuccess = (title = '操作成功', duration = 1500) => {
  uni.showToast({ title, icon: 'success', duration })
}

// 显示错误提示
export const showError = (title = '操作失败', duration = 1500) => {
  uni.showToast({ title, icon: 'none', duration })
}

// 确认对话框
export const showConfirm = (content, title = '提示') => {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm)
      }
    })
  })
}
