package com.clinic.service;

import com.clinic.dto.PrintMedicalRecordDTO;
import com.clinic.dto.PrintPrescriptionDTO;
import com.clinic.dto.PrintReceiptDTO;

/**
 * 打印服务
 */
public interface PrintService {

    /**
     * 获取处方打印数据
     *
     * @param prescriptionId 处方ID
     * @param clinicId 诊所ID
     * @return 处方打印数据
     */
    PrintPrescriptionDTO getPrescriptionPrintData(Long prescriptionId, Long clinicId);

    /**
     * 获取收费票据打印数据
     *
     * @param paymentId 支付记录ID
     * @param clinicId 诊所ID
     * @return 收费票据打印数据
     */
    PrintReceiptDTO getReceiptPrintData(Long paymentId, Long clinicId);

    /**
     * 获取病历打印数据
     *
     * @param recordId 病历ID
     * @param clinicId 诊所ID
     * @return 病历打印数据
     */
    PrintMedicalRecordDTO getMedicalRecordPrintData(Long recordId, Long clinicId);
}
