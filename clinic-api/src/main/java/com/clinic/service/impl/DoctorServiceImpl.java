package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.Doctor;
import com.clinic.mapper.DoctorMapper;
import com.clinic.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;

    @Override
    public Page<Doctor> list(Long clinicId, Long deptId, String keyword, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doctor::getClinicId, clinicId);
        queryWrapper.eq(Doctor::getDeleted, 0);

        if (deptId != null) {
            queryWrapper.eq(Doctor::getDeptId, deptId);
        }

        if (status != null) {
            queryWrapper.eq(Doctor::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Doctor::getDoctorName, keyword)
                    .or().like(Doctor::getPhone, keyword));
        }

        queryWrapper.orderByDesc(Doctor::getCreatedAt);

        Page<Doctor> page = new Page<>(pageNum, pageSize);
        Page<Doctor> resultPage = doctorMapper.selectPage(page, queryWrapper);

        // 为每个医生设置科室名称
        resultPage.getRecords().forEach(doctor -> {
            if (doctor.getDeptId() != null) {
                String deptName = doctorMapper.selectDeptNameByDeptId(doctor.getDeptId());
                doctor.setDeptName(deptName);
            }
        });

        return resultPage;
    }

    @Override
    public Doctor getById(Long id) {
        return doctorMapper.selectById(id);
    }

    @Override
    public void create(Doctor doctor) {
        // 生成医生编码
        String doctorCode = generateDoctorCode();
        doctor.setDoctorCode(doctorCode);

        // 设置默认值
        doctor.setCreatedAt(LocalDateTime.now());
        doctor.setUpdatedAt(LocalDateTime.now());
        doctor.setDeleted(0);
        if (doctor.getStatus() == null) {
            doctor.setStatus(1);
        }
        if (doctor.getServiceCount() == null) {
            doctor.setServiceCount(0);
        }
        doctorMapper.insert(doctor);
    }

    @Override
    public void update(Doctor doctor) {
        Doctor doctor1 = new Doctor();
        doctor1.setId(doctor.getId());
        doctor1.setDoctorName(doctor.getDoctorName());
        doctor1.setDeptId(doctor.getDeptId());
        doctor1.setStatus(doctor.getStatus());
        doctor1.setWorkStatus(doctor.getWorkStatus());
        doctor1.setSpecialty(doctor.getSpecialty());
        doctor1.setPhone(doctor.getPhone());
        doctor1.setIntroduction(doctor.getIntroduction());
        doctor1.setTitleName(doctor.getTitleName());
        doctor1.setAvatarUrl(doctor.getAvatarUrl());

        doctorMapper.updateById(doctor1);
    }

    @Override
    public void delete(Long doctorId) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setDeleted(1);
        doctorMapper.updateById(doctor);
    }

    /**
     * 生成医生编码
     */
    public String generateDoctorCode() {
        return "D" + System.currentTimeMillis();
    }
}
