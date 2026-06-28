package com.clinic.service.impl;

import com.clinic.dto.PrintMedicalRecordDTO;
import com.clinic.dto.PrintPrescriptionDTO;
import com.clinic.dto.PrintReceiptDTO;
import com.clinic.entity.*;
import com.clinic.mapper.*;
import com.clinic.service.PrintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final ClinicMapper clinicMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    private final DepartmentMapper departmentMapper;

    @Override
    public PrintPrescriptionDTO getPrescriptionPrintData(Long prescriptionId, Long clinicId) {
        // 获取处方信息
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            return null;
        }

        // 获取诊所信息
        Clinic clinic = clinicMapper.selectById(clinicId);

        // 获取患者信息
        Patient patient = patientMapper.selectById(prescription.getPatientId());

        // 获取医生信息
        SysUser doctor = sysUserMapper.selectById(prescription.getDoctorId());

        // 获取处方项目
        List<PrescriptionItem> items = prescriptionItemMapper.selectByPrescriptionId(prescriptionId);

        // 组装打印数据
        PrintPrescriptionDTO dto = new PrintPrescriptionDTO();

        // 诊所信息
        PrintPrescriptionDTO.ClinicInfo clinicInfo = new PrintPrescriptionDTO.ClinicInfo();
        clinicInfo.setName(clinic != null ? clinic.getClinicName() : "");
        clinicInfo.setAddress(clinic != null ? clinic.getAddress() : "");
        clinicInfo.setPhone(clinic != null ? clinic.getContactPhone() : "");
        clinicInfo.setLicenseNo(clinic != null ? clinic.getLicenseNo() : "");
        dto.setClinic(clinicInfo);

        // 处方信息
        PrintPrescriptionDTO.PrescriptionInfo prescriptionInfo = new PrintPrescriptionDTO.PrescriptionInfo();
        prescriptionInfo.setPrescriptionNo(prescription.getPrescriptionNo());
        prescriptionInfo.setPrescriptionType(getPrescriptionTypeName(prescription.getPrescriptionType()));
        
        // 获取科室名称，优先使用 departmentName，为空则根据 deptId 查询
        String departmentName = prescription.getDepartmentName();
        if ((departmentName == null || departmentName.isEmpty()) && prescription.getDeptId() != null) {
            Department department = departmentMapper.selectById(prescription.getDeptId());
            if (department != null) {
                departmentName = department.getDeptName();
            }
        }
        prescriptionInfo.setDepartment(departmentName != null ? departmentName : "");
        
        prescriptionInfo.setCreateTime(prescription.getCreatedAt());
        prescriptionInfo.setDiagnosis(prescription.getDiagnosis());
        dto.setPrescription(prescriptionInfo);

        // 患者信息
        PrintPrescriptionDTO.PatientInfo patientInfo = new PrintPrescriptionDTO.PatientInfo();
        patientInfo.setName(prescription.getPatientName());
        patientInfo.setPhone(patient != null ? patient.getPhone() : "");
        patientInfo.setGender(patient != null ? getGenderText(patient.getGender()) : "");
        patientInfo.setAge(patient != null ? patient.getAge() : null);
        dto.setPatient(patientInfo);

        // 医生信息
        PrintPrescriptionDTO.DoctorInfo doctorInfo = new PrintPrescriptionDTO.DoctorInfo();
        doctorInfo.setName(prescription.getDoctorName());
        //doctorInfo.setTitle(doctor != null ? doctor.getTitle() : "");
        dto.setDoctor(doctorInfo);

        // 处方项目
        List<PrintPrescriptionDTO.PrescriptionItemInfo> itemInfos = new ArrayList<>();
        for (PrescriptionItem item : items) {
            PrintPrescriptionDTO.PrescriptionItemInfo itemInfo = new PrintPrescriptionDTO.PrescriptionItemInfo();
            itemInfo.setItemType(getItemTypeName(item.getItemType()));
            itemInfo.setName(item.getItemName());
            itemInfo.setSpec(item.getSpec());
            itemInfo.setUnit(item.getUnit());
            itemInfo.setQuantity(item.getQuantity());
            itemInfo.setUnitPrice(item.getUnitPrice());
            itemInfo.setSubtotal(item.getSubtotal());
            itemInfo.setUsage(item.getUsage());
            itemInfo.setFrequency(item.getFrequency());
            itemInfo.setDuration(item.getDuration());
            itemInfos.add(itemInfo);
        }
        dto.setItems(itemInfos);

        // 金额信息
        PrintPrescriptionDTO.AmountInfo amountInfo = new PrintPrescriptionDTO.AmountInfo();
        amountInfo.setTotalAmount(prescription.getTotalAmount());
        amountInfo.setDiscountAmount(prescription.getDiscountAmount());
        amountInfo.setActualAmount(prescription.getActualAmount());
        dto.setAmount(amountInfo);

        return dto;
    }

    @Override
    public PrintReceiptDTO getReceiptPrintData(Long paymentId, Long clinicId) {
        // 获取支付记录
        PaymentRecord payment = paymentRecordMapper.selectById(paymentId);
        if (payment == null) {
            return null;
        }

        // 获取诊所信息
        Clinic clinic = clinicMapper.selectById(clinicId);

        // 获取处方信息
        Prescription prescription = prescriptionMapper.selectById(payment.getPrescriptionId());

        // 获取处方项目
        List<PrescriptionItem> items = new ArrayList<>();
        if (prescription != null) {
            items = prescriptionItemMapper.selectByPrescriptionId(prescription.getId());
        }

        // 组装打印数据
        PrintReceiptDTO dto = new PrintReceiptDTO();

        // 诊所信息
        PrintReceiptDTO.ClinicInfo clinicInfo = new PrintReceiptDTO.ClinicInfo();
        clinicInfo.setName(clinic != null ? clinic.getClinicName() : "");
        clinicInfo.setAddress(clinic != null ? clinic.getAddress() : "");
        clinicInfo.setPhone(clinic != null ? clinic.getContactPhone() : "");
        dto.setClinic(clinicInfo);

        // 票据信息
        PrintReceiptDTO.ReceiptInfo receiptInfo = new PrintReceiptDTO.ReceiptInfo();
        receiptInfo.setReceiptNo(generateReceiptNo());
        receiptInfo.setOrderNo(payment.getOrderNo());
        receiptInfo.setPrintTime(LocalDateTime.now());
        dto.setReceipt(receiptInfo);

        // 患者信息
        PrintReceiptDTO.PatientInfo patientInfo = new PrintReceiptDTO.PatientInfo();
        patientInfo.setName(payment.getPatientName());
        dto.setPatient(patientInfo);

        // 收费项目
        List<PrintReceiptDTO.ChargeItemInfo> chargeItems = new ArrayList<>();
        for (PrescriptionItem item : items) {
            PrintReceiptDTO.ChargeItemInfo chargeItem = new PrintReceiptDTO.ChargeItemInfo();
            chargeItem.setItemName(item.getItemName());
            chargeItem.setSpec(item.getSpec());
            chargeItem.setQuantity(item.getQuantity());
            chargeItem.setUnitPrice(item.getUnitPrice());
            chargeItem.setSubtotal(item.getSubtotal());
            chargeItems.add(chargeItem);
        }
        dto.setItems(chargeItems);

        // 支付信息
        PrintReceiptDTO.PaymentInfo paymentInfo = new PrintReceiptDTO.PaymentInfo();
        paymentInfo.setTotalAmount(payment.getAmount());
        paymentInfo.setDiscountAmount(payment.getDiscountAmount());
        paymentInfo.setActualAmount(payment.getActualAmount());
        paymentInfo.setPaymentMethod(getPaymentMethodName(payment.getPaymentMethod()));
        paymentInfo.setReceivedAmount(payment.getActualAmount());
        paymentInfo.setChangeAmount(BigDecimal.ZERO);
        dto.setPayment(paymentInfo);

        // 操作员信息
        PrintReceiptDTO.OperatorInfo operatorInfo = new PrintReceiptDTO.OperatorInfo();
        operatorInfo.setName(payment.getOperatorName());
        dto.setOperator(operatorInfo);

        return dto;
    }

    @Override
    public PrintMedicalRecordDTO getMedicalRecordPrintData(Long recordId, Long clinicId) {
        // 获取病历信息
        MedicalRecord record = medicalRecordMapper.selectById(recordId);
        if (record == null) {
            return null;
        }

        // 获取诊所信息
        Clinic clinic = clinicMapper.selectById(clinicId);

        // 获取患者信息
        Patient patient = patientMapper.selectById(record.getPatientId());

        // 获取医生信息
        SysUser doctor = sysUserMapper.selectById(record.getDoctorId());

        // 组装打印数据
        PrintMedicalRecordDTO dto = new PrintMedicalRecordDTO();

        // 诊所信息
        PrintMedicalRecordDTO.ClinicInfo clinicInfo = new PrintMedicalRecordDTO.ClinicInfo();
        clinicInfo.setName(clinic != null ? clinic.getClinicName() : "");
        clinicInfo.setAddress(clinic != null ? clinic.getAddress() : "");
        clinicInfo.setPhone(clinic != null ? clinic.getContactPhone() : "");
        dto.setClinic(clinicInfo);

        // 病历信息
        PrintMedicalRecordDTO.RecordInfo recordInfo = new PrintMedicalRecordDTO.RecordInfo();
        recordInfo.setRecordNo(record.getRecordNo());
        recordInfo.setVisitTime(record.getCreatedAt());
        recordInfo.setVisitType(getVisitTypeName(record.getVisitType()));
        recordInfo.setDepartment(record.getDeptName());
        recordInfo.setChiefComplaint(record.getChiefComplaint());
        recordInfo.setDiagnosis(record.getDiagnosis());
        recordInfo.setTreatment(record.getTreatment());
        recordInfo.setMedicalAdvice(record.getMedicalAdvice());
        dto.setRecord(recordInfo);

        // 患者信息
        PrintMedicalRecordDTO.PatientInfo patientInfo = new PrintMedicalRecordDTO.PatientInfo();
        patientInfo.setName(record.getPatientName());
        patientInfo.setGender(patient != null ? getGenderText(patient.getGender()) : "");
        patientInfo.setAge(patient != null ? patient.getAge() : null);
        patientInfo.setPhone(patient != null ? patient.getPhone() : "");
        patientInfo.setAddress("");
        dto.setPatient(patientInfo);

        // 医生信息
        PrintMedicalRecordDTO.DoctorInfo doctorInfo = new PrintMedicalRecordDTO.DoctorInfo();
        doctorInfo.setName(record.getDoctorName());
        //doctorInfo.setTitle(doctor != null ? doctor.getTitle() : "");
        dto.setDoctor(doctorInfo);

        return dto;
    }

    private String getPrescriptionTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "西药处方";
            case 2: return "中药处方";
            case 3: return "检查单";
            default: return "其他";
        }
    }

    private String getItemTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "药品";
            case 2: return "检查";
            case 3: return "治疗";
            default: return "其他";
        }
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null) return "";
        switch (method) {
            case 1: return "现金";
            case 2: return "微信";
            case 3: return "支付宝";
            case 4: return "银行卡";
            case 5: return "会员卡";
            default: return "其他";
        }
    }

    private String getVisitTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "初诊";
            case 2: return "复诊";
            case 3: return "急诊";
            default: return "其他";
        }
    }

    private String getGenderText(Integer gender) {
        if (gender == null) return "";
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    private String generateReceiptNo() {
        return "R" + System.currentTimeMillis();
    }
}
