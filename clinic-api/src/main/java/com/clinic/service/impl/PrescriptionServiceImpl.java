package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.*;
import com.clinic.mapper.*;
import com.clinic.service.MedicalRecordService;
import com.clinic.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final StockItemMapper stockItemMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final RegistrationMapper registrationMapper;
    private final MedicalRecordService medicalRecordService;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientMapper patientMapper;

    @Override
    public Map<String, Object> list(Long clinicId, Long doctorId, Integer prescriptionType, Integer paymentStatus, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getClinicId, clinicId);
        queryWrapper.eq(Prescription::getDeleted, 0);

        if (doctorId != null) {
            queryWrapper.eq(Prescription::getDoctorId, doctorId);
        }

        if (prescriptionType != null) {
            queryWrapper.eq(Prescription::getPrescriptionType, prescriptionType);
        }

        if (paymentStatus != null) {
            queryWrapper.eq(Prescription::getPaymentStatus, paymentStatus);
        }

        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(Prescription::getCreatedAt, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(Prescription::getCreatedAt, endDate);
        }

        queryWrapper.orderByDesc(Prescription::getCreatedAt);

        Page<Prescription> page = new Page<>(pageNum, pageSize);
        Page<Prescription> resultPage = prescriptionMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(this::enrichPrescription);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public Prescription getById(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription != null) {
            enrichPrescription(prescription);
        }
        return prescription;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Prescription prescription) {
        // 设置部门ID和科室名称
        Long deptId = doctorMapper.selectDeptIdByDoctorId(prescription.getDoctorId());
        prescription.setDeptId(deptId);
        
        // 查询并设置科室名称
        if (deptId != null) {
            com.clinic.entity.Department department = departmentMapper.selectById(deptId);
            if (department != null) {
                prescription.setDepartmentName(department.getDeptName());
            }
        }

        // 生成处方单号
        String prescriptionNo = generatePrescriptionNo();
        prescription.setPrescriptionNo(prescriptionNo);
        prescription.setPaymentStatus(0);

        // 计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(prescription.getItems())) {
            for (PrescriptionItem item : prescription.getItems()) {
                if (item.getSubtotal() != null) {
                    totalAmount = totalAmount.add(item.getSubtotal());
                }
            }
        }
        prescription.setTotalAmount(totalAmount);

        if (prescription.getDiscountAmount() == null) {
            prescription.setDiscountAmount(BigDecimal.ZERO);
        }
        if (prescription.getActualAmount() == null) {
            prescription.setActualAmount(prescription.getTotalAmount());
        }
        prescription.setCreatedAt(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescription.setDeleted(0);



        // 保存处方主表
        prescriptionMapper.insert(prescription);

        // 保存处方明细并扣减库存
        if (!CollectionUtils.isEmpty(prescription.getItems())) {
            for (PrescriptionItem item : prescription.getItems()) {
                item.setClinicId(prescription.getClinicId());
                item.setPrescriptionId(prescription.getId());
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                item.setDeleted(0);
                prescriptionItemMapper.insert(item);

                // 扣减药库库存
                if (item.getItemId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    StockItem stockItem = stockItemMapper.selectById(item.getItemId());
                    if (stockItem != null) {
                        if (stockItem.getStock() < item.getQuantity()) {
                            throw new BusinessException("药品【" + item.getItemName() + "】库存不足，当前库存：" + stockItem.getStock());
                        }
                        // 扣减库存
                        LambdaUpdateWrapper<StockItem> updateWrapper = new LambdaUpdateWrapper<>();
                        updateWrapper.eq(StockItem::getId, item.getItemId());
                        updateWrapper.setSql("stock = stock - " + item.getQuantity());
                        stockItemMapper.update(null, updateWrapper);
                    }
                }
            }
        }

        // 如果有关联挂号，更新挂号状态为已就诊(3) - 改为已就诊而不是已完成，等支付后才算完成
        if (prescription.getRegistrationId() != null) {
            Registration registration = new Registration();
            registration.setId(prescription.getRegistrationId());
            registration.setStatus(3);
            registration.setUpdatedAt(LocalDateTime.now());
            registrationMapper.updateById(registration);
        }

        // 就诊记录在支付完成后创建，不在此处创建

        Map<String, Object> result = new HashMap<>();
        result.put("prescriptionId", prescription.getId());
        result.put("prescriptionNo", prescription.getPrescriptionNo());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long prescriptionId, String reason) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (prescription.getPaymentStatus() != 1) {
            throw new BusinessException("当前状态不允许退费");
        }

        // 退回库存
        List<PrescriptionItem> items = prescriptionItemMapper.selectByPrescriptionId(prescriptionId);
        for (PrescriptionItem item : items) {
            if (item.getItemId() != null && item.getQuantity() != null) {
                LambdaUpdateWrapper<StockItem> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(StockItem::getId, item.getItemId());
                updateWrapper.setSql("stock = stock + " + item.getQuantity());
                stockItemMapper.update(null, updateWrapper);
            }
        }

        prescription.setPaymentStatus(2);
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);
    }

    @Override
    public void dispense(Long prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (prescription.getPaymentStatus() != 1) {
            throw new BusinessException("当前状态不允许发药");
        }

        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);
    }

    @Override
    public void validateCanRefund(Long prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (prescription.getPaymentStatus() != 1) {
            throw new BusinessException("当前状态不允许退费");
        }
    }

    @Override
    public void validateCanDispense(Long prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (prescription.getPaymentStatus() != 1) {
            throw new BusinessException("当前状态不允许发药");
        }
    }

    /**
     * 生成处方单号
     */
    private String generatePrescriptionNo() {
        return "RX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 根据处方自动创建就诊记录
     */
    private void createMedicalRecord(Prescription prescription) {
        try {
            MedicalRecord medicalRecord = new MedicalRecord();
            medicalRecord.setClinicId(prescription.getClinicId());
            medicalRecord.setPatientId(prescription.getPatientId());
            medicalRecord.setRegistrationId(prescription.getRegistrationId());
            medicalRecord.setDoctorId(prescription.getDoctorId());
            medicalRecord.setDeptId(prescription.getDeptId());
            medicalRecord.setDiagnosis(prescription.getDiagnosis());
            /*medicalRecord.setTreatment(prescription.getTreatment());
            medicalRecord.setMedicalAdvice(prescription.getMedicalAdvice());*/
            medicalRecord.setVisitType(1); // 1-门诊
            medicalRecord.setCreatedAt(LocalDateTime.now());
            medicalRecord.setUpdatedAt(LocalDateTime.now());

            // 自动填充患者、医生、科室信息
            if (prescription.getPatientId() != null) {
                Patient patient = patientMapper.selectById(prescription.getPatientId());
                if (patient != null) {
                    medicalRecord.setPatientName(patient.getPatientName());
                    medicalRecord.setPatientPhone(patient.getPhone());
                }
            }

            if (prescription.getDoctorId() != null) {
                Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
                if (doctor != null) {
                    medicalRecord.setDoctorName(doctor.getDoctorName());
                }
            }

            if (prescription.getDeptId() != null) {
                Department department = departmentMapper.selectById(prescription.getDeptId());
                if (department != null) {
                    medicalRecord.setDeptName(department.getDeptName());
                }
            }

            medicalRecordService.create(medicalRecord);
        } catch (Exception e) {
            // 就诊记录创建失败不影响处方创建，仅记录日志
            System.err.println("自动创建就诊记录失败: " + e.getMessage());
        }
    }

    /**
     * 为Prescription设置显示名称和明细
     */
    private void enrichPrescription(Prescription prescription) {
        prescription.setPrescriptionTypeName(DictConstants.getPrescriptionTypeName(prescription.getPrescriptionType()));
        prescription.setStatusName(DictConstants.getPaymentStatusName(prescription.getPaymentStatus()));

        // 查询科室名称
        if (prescription.getDeptId() != null && (prescription.getDepartmentName() == null || prescription.getDepartmentName().isEmpty())) {
            com.clinic.entity.Department department = departmentMapper.selectById(prescription.getDeptId());
            if (department != null) {
                prescription.setDepartmentName(department.getDeptName());
            }
        }

        // 查询明细
        List<PrescriptionItem> items = prescriptionItemMapper.selectByPrescriptionId(prescription.getId());
        items.forEach(item -> item.setItemTypeName(DictConstants.getItemTypeName(item.getItemType())));
        prescription.setItems(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long prescriptionId, Integer paymentMethod, BigDecimal payAmount) {
        // 1. 查询处方
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null || prescription.getDeleted() == 1) {
            throw new BusinessException("处方不存在");
        }

        // 2. 校验支付状态
        if (prescription.getPaymentStatus() != 0) {
            throw new BusinessException("该处方已支付或已退款，不能重复支付");
        }

        // 3. 校验支付金额
        BigDecimal actualAmount = prescription.getActualAmount() != null 
                ? prescription.getActualAmount() 
                : prescription.getTotalAmount();
        if (payAmount == null || payAmount.compareTo(actualAmount) != 0) {
            throw new BusinessException("支付金额与实际应付金额不符");
        }

        // 4. 更新处方支付状态
        LambdaUpdateWrapper<Prescription> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Prescription::getId, prescriptionId)
                .set(Prescription::getPaymentStatus, 1)
                .set(Prescription::getPaymentMethod, paymentMethod)
                .set(Prescription::getActualAmount, payAmount)
                .set(Prescription::getUpdatedAt, LocalDateTime.now());

        int rows = prescriptionMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new BusinessException("支付失败，请重试");
        }

        // 5. 支付完成后创建就诊记录
        createMedicalRecordAfterPay(prescription);

        // 6. 更新挂号状态为已完成(4)
        if (prescription.getRegistrationId() != null) {
            Registration registration = new Registration();
            registration.setId(prescription.getRegistrationId());
            registration.setStatus(4);
            registration.setUpdatedAt(LocalDateTime.now());
            registrationMapper.updateById(registration);
        }
    }

    /**
     * 支付完成后创建就诊记录
     */
    private void createMedicalRecordAfterPay(Prescription prescription) {
        System.out.println("[就诊记录创建] 开始创建就诊记录，处方ID: " + prescription.getId());
        try {
            // 检查是否已存在该处方的就诊记录（通过挂号ID检查）
            if (prescription.getRegistrationId() != null) {
                LambdaQueryWrapper<MedicalRecord> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MedicalRecord::getRegistrationId, prescription.getRegistrationId());
                long count = medicalRecordMapper.selectCount(queryWrapper);
                System.out.println("[就诊记录创建] 已存在的就诊记录数: " + count);
                if (count > 0) {
                    System.out.println("[就诊记录创建] 已存在就诊记录，跳过创建");
                    return; // 已存在则不创建
                }
            }

            // 查询挂号记录获取主诉
            String chiefComplaint = null;
            if (prescription.getRegistrationId() != null) {
                Registration registration = registrationMapper.selectById(prescription.getRegistrationId());
                if (registration != null) {
                    chiefComplaint = registration.getChiefComplaint();
                    System.out.println("[就诊记录创建] 从挂号记录获取主诉: " + chiefComplaint);
                }
            }

            MedicalRecord medicalRecord = new MedicalRecord();
            medicalRecord.setClinicId(prescription.getClinicId());
            medicalRecord.setPatientId(prescription.getPatientId());
            medicalRecord.setRegistrationId(prescription.getRegistrationId());
            medicalRecord.setDoctorId(prescription.getDoctorId());
            medicalRecord.setDeptId(prescription.getDeptId());
            medicalRecord.setDiagnosis(prescription.getDiagnosis());
            medicalRecord.setChiefComplaint(chiefComplaint);
            medicalRecord.setVisitType(1); // 1-门诊
            medicalRecord.setCreatedAt(LocalDateTime.now());
            medicalRecord.setUpdatedAt(LocalDateTime.now());

            System.out.println("[就诊记录创建] 准备填充患者信息，patientId: " + prescription.getPatientId());
            // 自动填充患者信息
            if (prescription.getPatientId() != null) {
                Patient patient = patientMapper.selectById(prescription.getPatientId());
                if (patient != null) {
                    medicalRecord.setPatientName(patient.getPatientName());
                    medicalRecord.setPatientPhone(patient.getPhone());
                    System.out.println("[就诊记录创建] 患者信息: " + patient.getPatientName());
                }
            }

            System.out.println("[就诊记录创建] 准备填充医生信息，doctorId: " + prescription.getDoctorId());
            // 自动填充医生信息
            if (prescription.getDoctorId() != null) {
                Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
                if (doctor != null) {
                    medicalRecord.setDoctorName(doctor.getDoctorName());
                    System.out.println("[就诊记录创建] 医生信息: " + doctor.getDoctorName());
                }
            }

            System.out.println("[就诊记录创建] 准备填充科室信息，deptId: " + prescription.getDeptId());
            // 自动填充科室信息
            if (prescription.getDeptId() != null) {
                Department department = departmentMapper.selectById(prescription.getDeptId());
                if (department != null) {
                    medicalRecord.setDeptName(department.getDeptName());
                    System.out.println("[就诊记录创建] 科室信息: " + department.getDeptName());
                }
            }

            System.out.println("[就诊记录创建] 调用 medicalRecordService.create()");
            medicalRecordService.create(medicalRecord);
            System.out.println("[就诊记录创建] 就诊记录创建成功！");
        } catch (Exception e) {
            // 就诊记录创建失败不影响支付流程，但记录详细日志
            System.err.println("[就诊记录创建] 支付完成后创建就诊记录失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("创建就诊记录失败: " + e.getMessage(), e);
        }
    }
}
