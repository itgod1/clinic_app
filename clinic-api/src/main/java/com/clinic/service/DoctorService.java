package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.Doctor;

public interface DoctorService {

    Page<Doctor> list(Long clinicId, Long deptId, String keyword, Integer status, Integer pageNum, Integer pageSize);

    Doctor getById(Long id);

    void create(Doctor doctor);

    void update( Doctor doctor);

    void delete(Long doctorId);



    //String generateDoctorCode();
}
