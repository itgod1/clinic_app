import { get, post } from '@/utils/request'

export interface DentalImage {
  id?: number
  clinicId: number
  patientId: number
  medicalRecordId?: number
  imageType: string
  imageUrl: string
  thumbnailUrl?: string
  fileName?: string
  fileSize?: number
  mimeType?: string
  width?: number
  height?: number
  shotDate?: string
  bodyPart?: string
  description?: string
  sortOrder?: number
  linkedTeeth?: string[]
}

export const getDentalImageList = (params: {
  clinicId: number
  patientId: number
  imageType?: string
  pageNum?: number
  pageSize?: number
}) => get<{ total: number; list: DentalImage[] }>('/admin/dental-image/list', params)

export const getDentalImageDetail = (id: number) =>
  get<DentalImage>(`/admin/dental-image/${id}`)

export const createDentalImage = (data: Partial<DentalImage>) =>
  post<DentalImage>('/admin/dental-image/create', data)

export const updateDentalImage = (data: Partial<DentalImage>) =>
  post('/admin/dental-image/update', data)

export const deleteDentalImage = (id: number, clinicId: number) =>
  post('/admin/dental-image/delete', { id, clinicId })

export const linkTeethToImage = (data: { imageId: number; toothNumbers: string[] }) =>
  post('/admin/dental-image/link-teeth', data)

export const getImagesByRecord = (medicalRecordId: number) =>
  get<DentalImage[]>(`/admin/dental-image/by-record/${medicalRecordId}`)
