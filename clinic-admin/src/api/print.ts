import request from '@/utils/request'
import type { PrintPrescriptionData, PrintReceiptData, PrintMedicalRecordData } from '@/types/print'

// 获取处方打印数据
export function getPrescriptionPrintData(prescriptionId: number, clinicId: number) {
  return request.get<PrintPrescriptionData>(`/admin/print/prescription/${prescriptionId}`, {
    params: { clinicId }
  })
}

// 获取收费票据打印数据
export function getReceiptPrintData(paymentId: number, clinicId: number) {
  return request.get<PrintReceiptData>(`/admin/print/receipt/${paymentId}`, {
    params: { clinicId }
  })
}

// 获取病历打印数据
export function getMedicalRecordPrintData(recordId: number, clinicId: number) {
  return request.get<PrintMedicalRecordData>(`/admin/print/medical-record/${recordId}`, {
    params: { clinicId }
  })
}
