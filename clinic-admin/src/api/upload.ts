import { post } from '@/utils/request'

export interface UploadResult {
  url: string
  filename: string
}

export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return post<UploadResult>('/upload/file', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return post<UploadResult>('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
