package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Department;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    public Map<String, Object> list(Long clinicId, Integer deptType, String keyword, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getClinicId, clinicId);
        queryWrapper.eq(status != null, Department::getStatus, status);
        queryWrapper.eq(deptType != null, Department::getDeptType, deptType);
        queryWrapper.eq(Department::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Department::getDeptName, keyword)
                    .or().like(Department::getDeptCode, keyword));
        }

        queryWrapper.orderByAsc(Department::getSortOrder);

        Page<Department> page = new Page<>(pageNum, pageSize);
        Page<Department> resultPage = departmentMapper.selectPage(page, queryWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public void create(Department department) {
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        department.setDeleted(0);
        departmentMapper.insert(department);
    }

    @Override
    public void update(Department department) {
        department.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(department);
    }

    @Override
    public void delete(Long deptId) {
        departmentMapper.deleteById(deptId);
    }

    @Override
    public boolean existsByCode(String deptCode, Long clinicId) {
        LambdaQueryWrapper<Department> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(Department::getDeptCode, deptCode)
                .eq(Department::getClinicId, clinicId);
        return departmentMapper.selectCount(codeWrapper) > 0;
    }

    @Override
    public boolean existsByCode(String deptCode, Long clinicId, Long excludeId) {
        LambdaQueryWrapper<Department> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(Department::getDeptCode, deptCode)
                .eq(Department::getClinicId, clinicId)
                .ne(Department::getId, excludeId);
        return departmentMapper.selectCount(codeWrapper) > 0;
    }

    @Override
    public void validateCodeUnique(String deptCode, Long clinicId) {
        if (existsByCode(deptCode, clinicId)) {
            throw new BusinessException("科室编码已存在");
        }
    }

    @Override
    public void validateCodeUnique(String deptCode, Long clinicId, Long excludeId) {
        if (existsByCode(deptCode, clinicId, excludeId)) {
            throw new BusinessException("科室编码已存在");
        }
    }
}
