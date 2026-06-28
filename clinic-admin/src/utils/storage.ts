const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'
const CLINIC_ID_KEY = 'clinicId'

export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY)

export const setToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token)

export const removeToken = (): void => localStorage.removeItem(TOKEN_KEY)

export const getUserInfo = <T = any>(): T | null => {
  const info = localStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

export const setUserInfo = (userInfo: any): void => {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export const removeUserInfo = (): void => localStorage.removeItem(USER_INFO_KEY)

export const getClinicId = (): number | null => {
  const id = localStorage.getItem(CLINIC_ID_KEY)
  return id ? parseInt(id) : null
}

export const getClinicIdOrDefault = (defaultValue: number = 0): number => {
  const id = localStorage.getItem(CLINIC_ID_KEY)
  return id ? parseInt(id) : defaultValue
}

export const setClinicId = (clinicId: number): void => {
  localStorage.setItem(CLINIC_ID_KEY, clinicId.toString())
}

export const storage = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  },

  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY)
  },

  getUserInfo<T = any>(): T | null {
    const info = localStorage.getItem(USER_INFO_KEY)
    return info ? JSON.parse(info) : null
  },

  setUserInfo(userInfo: any): void {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  },

  removeUserInfo(): void {
    localStorage.removeItem(USER_INFO_KEY)
  },

  getClinicId(): number | null {
    const id = localStorage.getItem(CLINIC_ID_KEY)
    return id ? parseInt(id) : null
  },

  getClinicIdOrDefault(defaultValue: number = 0): number {
    const id = localStorage.getItem(CLINIC_ID_KEY)
    return id ? parseInt(id) : defaultValue
  },

  setClinicId(clinicId: number): void {
    localStorage.setItem(CLINIC_ID_KEY, clinicId.toString())
  },

  clear(): void {
    localStorage.clear()
  }
}

export default storage