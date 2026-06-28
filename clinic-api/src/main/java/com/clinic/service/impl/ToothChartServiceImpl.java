package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.entity.ToothChartRecord;
import com.clinic.mapper.ToothChartRecordMapper;
import com.clinic.service.ToothChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToothChartServiceImpl implements ToothChartService {

    private final ToothChartRecordMapper toothChartRecordMapper;

    @Override
    public List<ToothChartRecord> getByMedicalRecordId(Long medicalRecordId) {
        LambdaQueryWrapper<ToothChartRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToothChartRecord::getMedicalRecordId, medicalRecordId)
               .eq(ToothChartRecord::getDeleted, 0);
        return toothChartRecordMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void saveToothChart(Long medicalRecordId, Long clinicId, List<ToothChartRecord> records) {
        // 先删除该病历的旧牙位记录（逻辑删除）
        LambdaQueryWrapper<ToothChartRecord> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ToothChartRecord::getMedicalRecordId, medicalRecordId);
        List<ToothChartRecord> oldRecords = toothChartRecordMapper.selectList(deleteWrapper);
        for (ToothChartRecord old : oldRecords) {
            old.setDeleted(1);
            old.setUpdatedAt(LocalDateTime.now());
            toothChartRecordMapper.updateById(old);
        }

        // 插入新记录
        if (records != null && !records.isEmpty()) {
            for (ToothChartRecord record : records) {
                record.setId(null);
                record.setMedicalRecordId(medicalRecordId);
                record.setClinicId(clinicId);
                record.setCreatedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());
                record.setDeleted(0);
                toothChartRecordMapper.insert(record);
            }
        }
    }
}
