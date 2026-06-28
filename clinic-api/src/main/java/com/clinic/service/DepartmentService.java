package com.clinic.service;

import com.clinic.entity.Department;

import java.util.Map;

public interface DepartmentService {

    /**
     * 获取科室列表
     */
    Map<String, Object> list(Long clinicId, Integer deptType, String keyword, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 创建科室
     */
    void create(Department department);

    /**
     * 更新科室
     */
    void update(Department department);

    /**
     * 删除科室
     */
    void delete(Long deptId);

    /**
     * 检查科室编码是否已存在
     */
    boolean existsByCode(String deptCode, Long clinicId);

    /**
     * 检查科室编码是否已存在（排除指定ID）
     */
    boolean existsByCode(String deptCode, Long clinicId, Long excludeId);

    /**
     * 校验科室编码唯一性
     */
    void validateCodeUnique(String deptCode, Long clinicId);

    /**
     * 校验科室编码唯一性（排除指定ID）
     */
    void validateCodeUnique(String deptCode, Long clinicId, Long excludeId);
}
