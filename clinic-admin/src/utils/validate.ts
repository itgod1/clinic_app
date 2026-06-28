export const validatePhone = (phone: string): boolean => {
  return /^1[3-9]\d{9}$/.test(phone)
}

export const validateEmail = (email: string): boolean => {
  return /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(email)
}

export const validateIdCard = (idCard: string): boolean => {
  return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(idCard)
}

export const validatePassword = (password: string): boolean => {
  return password.length >= 6 && password.length <= 20
}

export const validateCode = (code: string, length: number = 6): boolean => {
  return new RegExp(`^\\d{${length}}$`).test(code)
}