package com.clinic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.common.constant.DictConstants;
import com.clinic.entity.*;
import com.clinic.mapper.*;
import com.clinic.service.MedicalRecordService;
import com.clinic.service.PaymentRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支付流水服务实现
 */
@Service
@RequiredArgsConstructor
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentRecordService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final MedicalRecordService medicalRecordService;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final RegistrationMapper registrationMapper;

    private static final AtomicInteger sequence = new AtomicInteger(0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRecord createPaymentRecord(PaymentRecord record) {
        // 生成订单号
        if (record.getOrderNo() == null) {
            record.setOrderNo(generateOrderNo());
        }
        // 初始状态为未支付
        if (record.getPaymentStatus() == null) {
            record.setPaymentStatus(0);
        }
        paymentRecordMapper.insert(record);
        return record;
    }

    @Override
    public List<PaymentRecord> getByPrescriptionId(Long prescriptionId) {
        return paymentRecordMapper.selectByPrescriptionId(prescriptionId);
    }

    @Override
    public PaymentRecord getByOrderNo(String orderNo) {
        return paymentRecordMapper.selectByOrderNo(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pay(String orderNo, Integer paymentMethod, Long operatorId, String operatorName) {
        PaymentRecord record = getByOrderNo(orderNo);
        if (record == null) {
            throw new RuntimeException("支付记录不存在");
        }
        if (record.getPaymentStatus() != 0) {
            throw new RuntimeException("该订单已支付或已退款");
        }

        // 更新支付记录
        record.setPaymentMethod(paymentMethod);
        record.setPaymentStatus(1);
        record.setPayTime(LocalDateTime.now());
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        paymentRecordMapper.updateById(record);

        // 更新处方支付状态
        Prescription prescription = prescriptionMapper.selectById(record.getPrescriptionId());
        if (prescription != null) {
            prescription.setPaymentStatus(1);
            prescriptionMapper.updateById(prescription);

            // 支付完成后创建就诊记录
            createMedicalRecordAfterPay(prescription);

            // 更新挂号状态为已完成(4)
            if (prescription.getRegistrationId() != null) {
                Registration registration = new Registration();
                registration.setId(prescription.getRegistrationId());
                registration.setStatus(4);
                registration.setUpdatedAt(LocalDateTime.now());
                registrationMapper.updateById(registration);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(String orderNo, BigDecimal refundAmount, String reason, Long operatorId, String operatorName) {
        PaymentRecord record = getByOrderNo(orderNo);
        if (record == null) {
            throw new RuntimeException("支付记录不存在");
        }
        if (record.getPaymentStatus() != 1) {
            throw new RuntimeException("该订单未支付或已退款");
        }
        if (refundAmount.compareTo(record.getActualAmount()) > 0) {
            throw new RuntimeException("退款金额不能超过实付金额");
        }

        // 更新支付记录
        record.setRefundAmount(refundAmount);
        record.setRefundReason(reason);
        record.setRefundTime(LocalDateTime.now());
        record.setPaymentStatus(refundAmount.compareTo(record.getActualAmount()) == 0 ? 2 : 3);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        paymentRecordMapper.updateById(record);

        // 更新处方支付状态
        Prescription prescription = prescriptionMapper.selectById(record.getPrescriptionId());
        if (prescription != null) {
            prescription.setPaymentStatus(refundAmount.compareTo(record.getActualAmount()) == 0 ? 2 : 1);
            prescriptionMapper.updateById(prescription);
        }

        return true;
    }

    @Override
    public String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = sequence.incrementAndGet() % 10000;
        return "PAY" + dateStr + String.format("%04d", seq);
    }

    @Override
    public Map<Integer, BigDecimal> getDailyPaymentMethodStats(Long clinicId) {
        List<Map<String, Object>> list = paymentRecordMapper.selectDailyPaymentMethodStats(clinicId, LocalDate.now());
        Map<Integer, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> item : list) {
            Integer method = (Integer) item.get("paymentMethod");
            BigDecimal amount = (BigDecimal) item.get("amount");
            result.put(method, amount);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getDailyRevenueDetails(Long clinicId) {
        return paymentRecordMapper.selectDailyRevenueDetails(clinicId, LocalDate.now());
    }

    /**
     * 支付完成后创建就诊记录
     */
    private void createMedicalRecordAfterPay(Prescription prescription) {
        try {
            // 检查是否已存在该处方的就诊记录
            MedicalRecord existingRecord = medicalRecordService.getById(prescription.getId());
            if (existingRecord != null) {
                return; // 已存在则不创建
            }

            MedicalRecord medicalRecord = new MedicalRecord();
            medicalRecord.setClinicId(prescription.getClinicId());
            medicalRecord.setPatientId(prescription.getPatientId());
            medicalRecord.setRegistrationId(prescription.getRegistrationId());
            medicalRecord.setDoctorId(prescription.getDoctorId());
            medicalRecord.setDeptId(prescription.getDeptId());
            medicalRecord.setDiagnosis(prescription.getDiagnosis());
            medicalRecord.setVisitType(1); // 1-门诊
            medicalRecord.setCreatedAt(LocalDateTime.now());
            medicalRecord.setUpdatedAt(LocalDateTime.now());

            // 自动填充患者信息
            if (prescription.getPatientId() != null) {
                Patient patient = patientMapper.selectById(prescription.getPatientId());
                if (patient != null) {
                    medicalRecord.setPatientName(patient.getPatientName());
                    medicalRecord.setPatientPhone(patient.getPhone());
                }
            }

            // 自动填充医生信息
            if (prescription.getDoctorId() != null) {
                Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
                if (doctor != null) {
                    medicalRecord.setDoctorName(doctor.getDoctorName());
                }
            }

            // 自动填充科室信息
            if (prescription.getDeptId() != null) {
                Department department = departmentMapper.selectById(prescription.getDeptId());
                if (department != null) {
                    medicalRecord.setDeptName(department.getDeptName());
                }
            }

            medicalRecordService.create(medicalRecord);
        } catch (Exception e) {
            // 就诊记录创建失败不影响支付流程，仅记录日志
            System.err.println("支付完成后创建就诊记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
