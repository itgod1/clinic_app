package com.clinic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.clinic.entity.Clinic;

import java.util.List;

/**
 * 诊所服务接口
 */
public interface ClinicService extends IService<Clinic> {

    /**
     * 分页查询诊所列表
     *
     * @param keyword    关键词（名称/编码）
     * @param status     营业状态
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 分页结果
     */
    Page<Clinic> list(String keyword, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 创建诊所
     *
     * @param clinic 诊所信息
     * @return 创建后的诊所
     */
    Clinic create(Clinic clinic);

    /**
     * 更新诊所
     *
     * @param clinic 诊所信息
     * @return 更新后的诊所
     */
    Clinic update(Clinic clinic);

    /**
     * 删除诊所（逻辑删除）
     *
     * @param clinicId 诊所ID
     */
    void delete(Long clinicId);

    /**
     * 根据编码查询诊所
     *
     * @param clinicCode 诊所编码
     * @return 诊所信息
     */
    Clinic getByCode(String clinicCode);

    /**
     * 检查诊所编码是否已存在
     *
     * @param clinicCode 诊所编码
     * @return true-已存在
     */
    boolean checkCodeExists(String clinicCode);

    /**
     * 获取所有营业中的诊所
     *
     * @return 诊所列表
     */
    List<Clinic> getActiveClinics();
}
