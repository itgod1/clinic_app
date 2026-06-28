import { get, post } from '@/utils/request'

export interface Notification {
  id: number
  clinicId: number
  doctorId: number
  title: string
  content: string
  type: number
  isRead: number
  relatedId?: number
  relatedType?: string
  createdAt: string
}

export const getUnreadNotifications = (params: { clinicId: number; doctorId: number }) =>
  get<Notification[]>('/admin/notification/unread', params)

export const getRecentNotifications = (params: { clinicId: number; doctorId: number; limit?: number }) =>
  get<Notification[]>('/admin/notification/recent', params)

export const markNotificationAsRead = (id: number, doctorId: number) =>
  post(`/admin/notification/read/${id}`, { doctorId })

export const getUnreadNotificationCount = (params: { clinicId: number; doctorId: number }) =>
  get<number>('/admin/notification/unreadCount', params)
