// 挂号状态 - 与后端保持一致
// 后端状态：1已挂号 2已签到 3就诊中 4已完成 5已取消 6已退号 7过号
export const REGISTRATION_STATUS = {
  PENDING: 1,      // 已挂号（待就诊/排队中）
  CHECKED_IN: 2,   // 已签到
  IN_PROGRESS: 3,  // 就诊中
  COMPLETED: 4,    // 已完成
  CANCELLED: 5,    // 已取消
  REFUNDED: 6,     // 已退号
  MISSED: 7        // 过号
}

export const REGISTRATION_STATUS_TEXT = {
  [REGISTRATION_STATUS.PENDING]: '排队中',
  [REGISTRATION_STATUS.CHECKED_IN]: '已签到',
  [REGISTRATION_STATUS.IN_PROGRESS]: '就诊中',
  [REGISTRATION_STATUS.COMPLETED]: '已完成',
  [REGISTRATION_STATUS.CANCELLED]: '已取消',
  [REGISTRATION_STATUS.REFUNDED]: '已退号',
  [REGISTRATION_STATUS.MISSED]: '过号'
}

export const REGISTRATION_STATUS_COLOR = {
  [REGISTRATION_STATUS.PENDING]: '#00b894',
  [REGISTRATION_STATUS.CHECKED_IN]: '#1890ff',
  [REGISTRATION_STATUS.IN_PROGRESS]: '#52c41a',
  [REGISTRATION_STATUS.COMPLETED]: '#999999',
  [REGISTRATION_STATUS.CANCELLED]: '#ff4d4f',
  [REGISTRATION_STATUS.REFUNDED]: '#ff7875',
  [REGISTRATION_STATUS.MISSED]: '#faad14'
}

// 就诊类型
export const VISIT_TYPE = {
  FIRST: 1,   // 初诊
  FOLLOW: 2   // 复诊
}

export const VISIT_TYPE_TEXT = {
  [VISIT_TYPE.FIRST]: '初诊',
  [VISIT_TYPE.FOLLOW]: '复诊'
}

// 医生职称
export const DOCTOR_TITLE = {
  CHIEF: 1,           // 主任医师
  ASSOCIATE_CHIEF: 2, // 副主任医师
  ATTENDING: 3,       // 主治医师
  RESIDENT: 4,        // 住院医师
  PHYSICIAN: 5        // 医师
}

export const DOCTOR_TITLE_TEXT = {
  [DOCTOR_TITLE.CHIEF]: '主任医师',
  [DOCTOR_TITLE.ASSOCIATE_CHIEF]: '副主任医师',
  [DOCTOR_TITLE.ATTENDING]: '主治医师',
  [DOCTOR_TITLE.RESIDENT]: '住院医师',
  [DOCTOR_TITLE.PHYSICIAN]: '医师'
}

// 性别
export const GENDER = {
  UNKNOWN: 0,
  MALE: 1,
  FEMALE: 2
}

export const GENDER_TEXT = {
  [GENDER.UNKNOWN]: '未知',
  [GENDER.MALE]: '男',
  [GENDER.FEMALE]: '女'
}

// 星期
export const WEEK_DAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

// 时段
export const TIME_SLOTS = [
  '08:00-08:30',
  '08:30-09:00',
  '09:00-09:30',
  '09:30-10:00',
  '10:00-10:30',
  '10:30-11:00',
  '11:00-11:30',
  '14:00-14:30',
  '14:30-15:00',
  '15:00-15:30',
  '15:30-16:00',
  '16:00-16:30',
  '16:30-17:00'
]

// 分页默认配置
export const PAGE_CONFIG = {
  pageSize: 10,
  pageNum: 1
}
