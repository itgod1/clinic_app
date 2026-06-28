package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Department;
import com.clinic.entity.Doctor;
import com.clinic.entity.MedicalRecord;
import com.clinic.entity.Patient;
import com.clinic.entity.ToothChartRecord;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.mapper.DoctorMapper;
import com.clinic.mapper.MedicalRecordMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.ToothChartRecordMapper;
import com.clinic.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final ToothChartRecordMapper toothChartRecordMapper;

    @Override
    public Map<String, Object> list(Long clinicId, Long patientId, Long doctorId, String startDate, String endDate, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MedicalRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MedicalRecord::getClinicId, clinicId);
        queryWrapper.eq(MedicalRecord::getDeleted, 0);

        if (patientId != null) {
            queryWrapper.eq(MedicalRecord::getPatientId, patientId);
        }

        if (doctorId != null) {
            queryWrapper.eq(MedicalRecord::getDoctorId, doctorId);
        }

        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(MedicalRecord::getCreatedAt, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(MedicalRecord::getCreatedAt, endDate);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(MedicalRecord::getPatientName, keyword)
                    .or().like(MedicalRecord::getRecordNo, keyword));
        }

        queryWrapper.orderByDesc(MedicalRecord::getCreatedAt);

        Page<MedicalRecord> page = new Page<>(pageNum, pageSize);
        Page<MedicalRecord> resultPage = medicalRecordMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(this::enrichMedicalRecord);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public MedicalRecord getById(Long id) {
        MedicalRecord record = medicalRecordMapper.selectById(id);
        if (record != null) {
            enrichMedicalRecord(record);
        }
        return record;
    }

    @Override
    public void create(MedicalRecord medicalRecord) {
        String recordNo = generateRecordNo();
        medicalRecord.setRecordNo(recordNo);
        if (medicalRecord.getVisitDate() == null) {
            medicalRecord.setVisitDate(LocalDate.now());
        }
        // 如果没有关联挂号，设置默认值0
        if (medicalRecord.getRegistrationId() == null) {
            medicalRecord.setRegistrationId(0L);
        }
        medicalRecord.setCreatedAt(LocalDateTime.now());
        medicalRecord.setUpdatedAt(LocalDateTime.now());
        medicalRecord.setDeleted(0);

        // 自动填充患者姓名
        if (medicalRecord.getPatientId() != null && !StringUtils.hasText(medicalRecord.getPatientName())) {
            Patient patient = patientMapper.selectById(medicalRecord.getPatientId());
            if (patient != null) {
                medicalRecord.setPatientName(patient.getPatientName());
                medicalRecord.setPatientPhone(patient.getPhone());
            }
        }

        // 自动填充医生姓名和科室信息
        if (medicalRecord.getDoctorId() != null) {
            Doctor doctor = doctorMapper.selectById(medicalRecord.getDoctorId());
            if (doctor != null) {
                if (!StringUtils.hasText(medicalRecord.getDoctorName())) {
                    medicalRecord.setDoctorName(doctor.getDoctorName());
                }
                // 根据医生自动填充科室信息
                medicalRecord.setDeptId(doctor.getDeptId());
                Department department = departmentMapper.selectById(doctor.getDeptId());
                if (department != null) {
                    medicalRecord.setDeptName(department.getDeptName());
                }
            }
        }

        medicalRecordMapper.insert(medicalRecord);
    }

    @Override
    public void update(MedicalRecord medicalRecord) {
        medicalRecord.setUpdatedAt(LocalDateTime.now());
        medicalRecord.setUpdatedAt(LocalDateTime.now());
        medicalRecordMapper.updateById(medicalRecord);
    }

    @Override
    public void delete(Long recordId) {
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setDeleted(1);
        record.setUpdatedAt(LocalDateTime.now());
        medicalRecordMapper.updateById(record);
    }

    @Override
    public String generateRecordNo() {
        return "MR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    @Override
    public void validateMedicalRecord(MedicalRecord medicalRecord) {
        if (medicalRecord.getPatientId() == null) {
            throw new BusinessException("患者不能为空");
        }
        if (!StringUtils.hasText(medicalRecord.getChiefComplaint())) {
            throw new BusinessException("主诉不能为空");
        }
    }

    /**
     * 为MedicalRecord设置显示名称及牙位图数据
     */
    private void enrichMedicalRecord(MedicalRecord record) {
        record.setVisitTypeName(DictConstants.getVisitTypeName(record.getVisitType()));
        // 加载牙位图记录
        LambdaQueryWrapper<ToothChartRecord> toothWrapper = new LambdaQueryWrapper<>();
        toothWrapper.eq(ToothChartRecord::getMedicalRecordId, record.getId())
                    .eq(ToothChartRecord::getDeleted, 0);
        List<ToothChartRecord> toothRecords = toothChartRecordMapper.selectList(toothWrapper);
        record.setToothChartRecords(toothRecords);
        // 生成牙位摘要
        if (toothRecords != null && !toothRecords.isEmpty()) {
            List<String> sortedTeeth = toothRecords.stream()
                .map(ToothChartRecord::getToothNumber)
                .distinct()
                .sorted()
                .toList();
            record.setToothChartSummary(sortedTeeth.size() + "颗牙有记录: " + String.join(", ", sortedTeeth));
        }
    }

    @Override
    public Map<String, Object> listByPatientIds(Long clinicId, List<Long> patientIds, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MedicalRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MedicalRecord::getClinicId, clinicId);
        queryWrapper.eq(MedicalRecord::getDeleted, 0);
        queryWrapper.in(MedicalRecord::getPatientId, patientIds);
        queryWrapper.orderByDesc(MedicalRecord::getCreatedAt);

        Page<MedicalRecord> page = new Page<>(pageNum, pageSize);
        Page<MedicalRecord> resultPage = medicalRecordMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(this::enrichMedicalRecord);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }
}
