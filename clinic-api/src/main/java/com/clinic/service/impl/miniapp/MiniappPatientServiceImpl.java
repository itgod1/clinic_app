package com.clinic.service.impl.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Patient;
import com.clinic.mapper.PatientMapper;
import com.clinic.service.miniapp.MiniappPatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MiniappPatientServiceImpl implements MiniappPatientService {

    private final PatientMapper patientMapper;

    @Override
    public List<Patient> getMyPatients(Long userId, Long clinicId) {
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0)
                .orderByDesc(Patient::getCreatedAt);
        List<Patient> patients = patientMapper.selectList(queryWrapper);

        // 第一个就诊人标记为默认
        if (!patients.isEmpty()) {
            patients.get(0).setIsDefault(true);
            for (int i = 1; i < patients.size(); i++) {
                patients.get(i).setIsDefault(false);
            }
        }

        return patients;
    }

    @Override
    public Patient getMyPatientById(Long patientId, Long userId) {
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getId, patientId)
                .eq(Patient::getUserId, userId)
                .eq(Patient::getDeleted, 0);
        Patient patient = patientMapper.selectOne(queryWrapper);
        if (patient == null) {
            throw new BusinessException("就诊人不存在或无权访问");
        }
        return patient;
    }

    @Override
    public void createMyPatient(Long userId, Patient patient) {
        // 参数校验
        if (!StringUtils.hasText(patient.getPatientName())) {
            throw new BusinessException("就诊人姓名不能为空");
        }
        if (!StringUtils.hasText(patient.getPhone())) {
            throw new BusinessException("手机号不能为空");
        }

        // 检查该用户下是否已有相同手机号的就诊人
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, patient.getClinicId())
                .eq(Patient::getPhone, patient.getPhone())
                .eq(Patient::getDeleted, 0);
        if (patientMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("该手机号已添加为就诊人");
        }

        // 设置用户ID
        patient.setUserId(userId);
        patient.setPatientCode(generatePatientCode());
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setDeleted(0);
        if (patient.getStatus() == null) {
            patient.setStatus(1);
        }

        patientMapper.insert(patient);
    }

    @Override
    public void updateMyPatient(Long userId, Patient patient) {
        // 验证就诊人属于当前用户
        Patient existPatient = getMyPatientById(patient.getId(), userId);
        if (existPatient == null) {
            throw new BusinessException("就诊人不存在或无权修改");
        }

        // 如果修改了手机号，检查是否与其他就诊人重复
        if (StringUtils.hasText(patient.getPhone()) && !patient.getPhone().equals(existPatient.getPhone())) {
            LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Patient::getUserId, userId)
                    .eq(Patient::getClinicId, existPatient.getClinicId())
                    .eq(Patient::getPhone, patient.getPhone())
                    .eq(Patient::getDeleted, 0)
                    .ne(Patient::getId, patient.getId());
            if (patientMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("该手机号已添加为其他就诊人");
            }
        }

        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
    }

    @Override
    public void deleteMyPatient(Long patientId, Long userId) {
        // 验证就诊人属于当前用户
        Patient patient = getMyPatientById(patientId, userId);
        if (patient == null) {
            throw new BusinessException("就诊人不存在或无权删除");
        }

        // 逻辑删除
        patient.setDeleted(1);
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
    }

    @Override
    public void setDefaultPatient(Long userId, Long patientId, Long clinicId) {
        // 先取消该用户所有默认就诊人
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0);
        List<Patient> patients = patientMapper.selectList(queryWrapper);

        // 设置新的默认就诊人
        Patient defaultPatient = getMyPatientById(patientId, userId);
        defaultPatient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(defaultPatient);
    }

    @Override
    public Patient getDefaultPatient(Long userId, Long clinicId) {
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0)
                .orderByAsc(Patient::getCreatedAt)
                .last("LIMIT 1");
        return patientMapper.selectOne(queryWrapper);
    }

    private String generatePatientCode() {
        return "P" + System.currentTimeMillis();
    }
}
