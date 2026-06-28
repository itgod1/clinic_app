package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.InsurancePatient;
import com.clinic.entity.Patient;
import com.clinic.mapper.InsurancePatientMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.service.InsurancePatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InsurancePatientServiceImpl implements InsurancePatientService {

    private final InsurancePatientMapper insurancePatientMapper;
    private final PatientMapper patientMapper;

    @Override
    public Map<String, Object> list(Long clinicId, String keyword, String insuranceType, String insuranceStatus, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<InsurancePatient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InsurancePatient::getClinicId, clinicId);
        queryWrapper.eq(InsurancePatient::getDeleted, 0);

        if (StringUtils.hasText(insuranceType)) {
            queryWrapper.eq(InsurancePatient::getInsuranceType, insuranceType);
        }
        if (StringUtils.hasText(insuranceStatus)) {
            queryWrapper.eq(InsurancePatient::getInsuranceStatus, insuranceStatus);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(InsurancePatient::getInsuranceCardNo, keyword)
                    .or().like(InsurancePatient::getHolderName, keyword));
        }

        queryWrapper.orderByDesc(InsurancePatient::getCreatedAt);

        Page<InsurancePatient> page = new Page<>(pageNum, pageSize);
        Page<InsurancePatient> resultPage = insurancePatientMapper.selectPage(page, queryWrapper);
        resultPage.getRecords().forEach(this::enrich);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public InsurancePatient getById(Long id) {
        InsurancePatient record = insurancePatientMapper.selectById(id);
        if (record != null) {
            enrich(record);
        }
        return record;
    }

    @Override
    public InsurancePatient getByPatientId(Long patientId) {
        LambdaQueryWrapper<InsurancePatient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InsurancePatient::getPatientId, patientId);
        queryWrapper.eq(InsurancePatient::getDeleted, 0);
        queryWrapper.last("LIMIT 1");
        InsurancePatient record = insurancePatientMapper.selectOne(queryWrapper);
        if (record != null) {
            enrich(record);
        }
        return record;
    }

    @Override
    public void create(InsurancePatient record) {
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.setDeleted(0);
        if (!StringUtils.hasText(record.getInsuranceStatus())) {
            record.setInsuranceStatus("ACTIVE");
        }
        insurancePatientMapper.insert(record);
    }

    @Override
    public void update(InsurancePatient record) {
        record.setUpdatedAt(LocalDateTime.now());
        insurancePatientMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        insurancePatientMapper.deleteById(id);
    }

    @Override
    public void validateUnique(InsurancePatient record) {
        if (!StringUtils.hasText(record.getInsuranceCardNo())) {
            throw new BusinessException("医保卡号不能为空");
        }
        LambdaQueryWrapper<InsurancePatient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InsurancePatient::getInsuranceCardNo, record.getInsuranceCardNo());
        queryWrapper.eq(InsurancePatient::getClinicId, record.getClinicId());
        queryWrapper.eq(InsurancePatient::getDeleted, 0);
        if (record.getId() != null) {
            queryWrapper.ne(InsurancePatient::getId, record.getId());
        }
        if (insurancePatientMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("该医保卡号已存在");
        }
    }

    private void enrich(InsurancePatient record) {
        record.setInsuranceTypeName(DictConstants.getInsuranceTypeName(record.getInsuranceType()));
        record.setInsuranceStatusName(DictConstants.getInsuranceStatusName(record.getInsuranceStatus()));

        if (record.getPatientId() != null) {
            Patient patient = patientMapper.selectById(record.getPatientId());
            if (patient != null) {
                record.setPatientName(patient.getPatientName());
            }
        }
    }
}
