package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.MedicalRecord;
import com.clinic.entity.Patient;
import com.clinic.mapper.MedicalRecordMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientMapper patientMapper;
    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    public Map<String, Object> list(Long clinicId, String keyword, Integer memberLevel, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getClinicId, clinicId);
        queryWrapper.eq(Patient::getDeleted, 0);

        if (memberLevel != null) {
            queryWrapper.eq(Patient::getMemberLevel, memberLevel);
        }

        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(Patient::getCreatedAt, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(Patient::getCreatedAt, endDate);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Patient::getPatientName, keyword)
                    .or().like(Patient::getPhone, keyword)
                    .or().like(Patient::getPatientCode, keyword));
        }

        queryWrapper.orderByDesc(Patient::getCreatedAt);

        Page<Patient> page = new Page<>(pageNum, pageSize);
        Page<Patient> resultPage = patientMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(this::enrichPatient);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public Patient getById(Long id) {
        Patient patient = patientMapper.selectById(id);
        if (patient != null) {
            enrichPatient(patient);
        }
        return patient;
    }

    @Override
    public void create(Patient patient) {
        String patientCode = generatePatientCode();
        patient.setPatientCode(patientCode);
        patient.setBalance(BigDecimal.ZERO);
        patient.setPoints(0);
        patient.setVisitCount(0);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setDeleted(0);
        if (patient.getStatus() == null) {
            patient.setStatus(1);
        }
        patientMapper.insert(patient);
    }

    @Override
    public void update(Patient patient) {
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
    }

    @Override
    public void updateStatus(Patient patient) {
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
    }

    @Override
    public void delete(Long patientId) {
        Patient patient = new Patient();
        patient.setId(patientId);
        patientMapper.deleteById(patient);

    }

    @Override
    public boolean existsByPhone(String phone, Long clinicId) {
        LambdaQueryWrapper<Patient> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(Patient::getPhone, phone)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0);
        return patientMapper.selectCount(phoneWrapper) > 0;
    }

    @Override
    public void validatePhoneUnique(String phone, Long clinicId) {
        if (existsByPhone(phone, clinicId)) {
            throw new BusinessException("该手机号已被注册");
        }
    }

    @Override
    public void validatePatient(Patient patient) {
        if (!StringUtils.hasText(patient.getPatientName())) {
            throw new BusinessException("患者姓名不能为空");
        }
        // 手机号改为选填，不再强制验证
        // if (!StringUtils.hasText(patient.getPhone())) {
        //     throw new BusinessException("手机号不能为空");
        // }
    }

    @Override
    public String generatePatientCode() {
        return "P" + System.currentTimeMillis();
    }

    /**
     * 为Patient设置显示名称和最后就诊时间
     */
    private void enrichPatient(Patient patient) {
        patient.setMemberLevelName(DictConstants.getMemberLevelName(patient.getMemberLevel()));

        // 查询该患者最新的病历记录
        LambdaQueryWrapper<MedicalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(MedicalRecord::getPatientId, patient.getId())
                .eq(MedicalRecord::getDeleted, 0)
                .orderByDesc(MedicalRecord::getVisitDate)
                .last("LIMIT 1");
        List<MedicalRecord> records = medicalRecordMapper.selectList(recordWrapper);
        if (!records.isEmpty()) {
            patient.setLastVisitDate(LocalDate.parse(records.get(0).getVisitDate().toString()));
        }
    }

    @Override
    public void recharge(Long patientId, BigDecimal amount, BigDecimal giftAmount, Integer paymentMethod, String remark) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        
        // 更新余额
        BigDecimal totalAmount = amount.add(giftAmount != null ? giftAmount : BigDecimal.ZERO);
        BigDecimal newBalance = patient.getBalance().add(totalAmount);
        patient.setBalance(newBalance);
        patient.setUpdatedAt(LocalDateTime.now());
        
        patientMapper.updateById(patient);
    }
}
