// 诊所信息
export interface ClinicInfo {
  name: string
  address: string
  phone: string
  licenseNo?: string
}

// 处方打印数据
export interface PrintPrescriptionData {
  clinic: ClinicInfo
  prescription: {
    prescriptionNo: string
    prescriptionType: string
    department: string
    createTime: string
    diagnosis: string
  }
  patient: {
    name: string
    phone: string
    gender: string
    age: number
  }
  doctor: {
    name: string
    title: string
  }
  items: PrescriptionItemInfo[]
  amount: {
    totalAmount: number
    discountAmount: number
    actualAmount: number
  }
}

export interface PrescriptionItemInfo {
  itemType: string
  name: string
  spec: string
  unit: string
  quantity: number
  unitPrice: number
  subtotal: number
  usage: string
  frequency: string
  duration: string
}

// 收费票据打印数据
export interface PrintReceiptData {
  clinic: ClinicInfo
  receipt: {
    receiptNo: string
    orderNo: string
    printTime: string
  }
  patient: {
    name: string
    phone: string
  }
  items: ChargeItemInfo[]
  payment: {
    totalAmount: number
    discountAmount: number
    actualAmount: number
    paymentMethod: string
    receivedAmount: number
    changeAmount: number
  }
  operator: {
    name: string
  }
}

export interface ChargeItemInfo {
  itemName: string
  spec: string
  quantity: number
  unitPrice: number
  subtotal: number
}

// 病历打印数据
export interface PrintMedicalRecordData {
  clinic: ClinicInfo
  record: {
    recordNo: string
    visitTime: string
    visitType: string
    department: string
    chiefComplaint: string
    diagnosis: string
    treatment: string
    medicalAdvice: string
  }
  patient: {
    name: string
    gender: string
    age: number
    phone: string
    address: string
    idCard?: string
  }
  doctor: {
    name: string
    title: string
  }
}
