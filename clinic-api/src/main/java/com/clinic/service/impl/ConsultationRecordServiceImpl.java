package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.entity.ConsultationRecord;
import com.clinic.entity.Department;
import com.clinic.entity.Doctor;
import com.clinic.entity.Patient;
import com.clinic.mapper.ConsultationRecordMapper;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.mapper.DoctorMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.service.ConsultationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsultationRecordServiceImpl implements ConsultationRecordService {

    private final ConsultationRecordMapper consultationRecordMapper;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public Map<String, Object> list(Long clinicId, Integer recordType, Long doctorId, String startDate, String endDate, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ConsultationRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationRecord::getClinicId, clinicId);
        queryWrapper.eq(ConsultationRecord::getDeleted, 0);

        if (recordType != null) {
            queryWrapper.eq(ConsultationRecord::getRecordType, recordType);
        }
        if (doctorId != null) {
            queryWrapper.eq(ConsultationRecord::getDoctorId, doctorId);
        }
        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(ConsultationRecord::getCreatedAt, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(ConsultationRecord::getCreatedAt, endDate);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(ConsultationRecord::getPatientName, keyword)
                    .or().like(ConsultationRecord::getRecordNo, keyword)
                    .or().like(ConsultationRecord::getPatientPhone, keyword));
        }

        queryWrapper.orderByDesc(ConsultationRecord::getCreatedAt);

        Page<ConsultationRecord> page = new Page<>(pageNum, pageSize);
        Page<ConsultationRecord> resultPage = consultationRecordMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(this::enrichRecord);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public ConsultationRecord getById(Long id) {
        ConsultationRecord record = consultationRecordMapper.selectById(id);
        if (record != null) {
            enrichRecord(record);
        }
        return record;
    }

    @Override
    public void create(ConsultationRecord record) {
        record.setRecordNo(generateRecordNo(record.getRecordType()));
        if (record.getVisitDate() == null) {
            record.setVisitDate(LocalDate.now());
        }
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.setDeleted(0);

        // 自动填充患者姓名电话
        if (record.getPatientId() != null && !StringUtils.hasText(record.getPatientName())) {
            Patient patient = patientMapper.selectById(record.getPatientId());
            if (patient != null) {
                record.setPatientName(patient.getPatientName());
                record.setPatientPhone(patient.getPhone());
            }
        }

        // 自动填充医生姓名和科室信息
        if (record.getDoctorId() != null) {
            Doctor doctor = doctorMapper.selectById(record.getDoctorId());
            if (doctor != null) {
                if (!StringUtils.hasText(record.getDoctorName())) {
                    record.setDoctorName(doctor.getDoctorName());
                }
                record.setDeptId(doctor.getDeptId());
                Department department = departmentMapper.selectById(doctor.getDeptId());
                if (department != null) {
                    record.setDeptName(department.getDeptName());
                }
            }
        }

        consultationRecordMapper.insert(record);
    }

    @Override
    public void update(ConsultationRecord record) {
        record.setUpdatedAt(LocalDateTime.now());
        consultationRecordMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        consultationRecordMapper.deleteById(id);
    }

    @Override
    public java.util.List<ConsultationRecord> getUpcomingRevisits(Long clinicId, Long doctorId) {
        LambdaQueryWrapper<ConsultationRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationRecord::getClinicId, clinicId);
        queryWrapper.eq(ConsultationRecord::getDeleted, 0);
        queryWrapper.isNotNull(ConsultationRecord::getNextVisitDate);
        // 近7天逾期 ~ 未来30天到期
        queryWrapper.ge(ConsultationRecord::getNextVisitDate, LocalDate.now().minusDays(7));
        queryWrapper.le(ConsultationRecord::getNextVisitDate, LocalDate.now().plusDays(30));
        if (doctorId != null) {
            queryWrapper.eq(ConsultationRecord::getDoctorId, doctorId);
        }
        queryWrapper.orderByAsc(ConsultationRecord::getNextVisitDate);

        java.util.List<ConsultationRecord> records = consultationRecordMapper.selectList(queryWrapper);
        records.forEach(this::enrichRecord);
        return records;
    }

    @Override
    public void remind(Long id) {
        ConsultationRecord record = new ConsultationRecord();
        record.setId(id);
        record.setRemindCount(consultationRecordMapper.selectById(id).getRemindCount() == null ? 1 :
                consultationRecordMapper.selectById(id).getRemindCount() + 1);
        record.setRemindedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        consultationRecordMapper.updateById(record);
    }

    @Override
    public void complete(Long id) {
        ConsultationRecord record = new ConsultationRecord();
        record.setId(id);
        record.setStatus(2);
        record.setUpdatedAt(LocalDateTime.now());
        consultationRecordMapper.updateById(record);
    }

    @Override
    public String generateRecordNo(Integer recordType) {
        String prefix = recordType != null && recordType == 2 ? "DS" : "CS";
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private void enrichRecord(ConsultationRecord record) {
        record.setRecordTypeName(DictConstants.getRecordTypeName(record.getRecordType()));
        record.setStatusName(DictConstants.getConsultStatusName(record.getStatus()));
    }
}
