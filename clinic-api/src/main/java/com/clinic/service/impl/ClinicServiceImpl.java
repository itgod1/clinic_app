package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Clinic;
import com.clinic.entity.SysUser;
import com.clinic.entity.SysUserRole;
import com.clinic.mapper.ClinicMapper;
import com.clinic.mapper.SysUserRoleMapper;
import com.clinic.service.ClinicService;
import com.clinic.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j

/**
 * 诊所服务实现
 */
@Service
@RequiredArgsConstructor
public class ClinicServiceImpl extends ServiceImpl<ClinicMapper, Clinic> implements ClinicService {

    private final ClinicMapper clinicMapper;
    private final SysUserService sysUserService;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public Page<Clinic> list(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        Page<Clinic> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getDeleted, 0);

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Clinic::getClinicName, keyword)
                    .or()
                    .like(Clinic::getClinicCode, keyword)
                    .or()
                    .like(Clinic::getContactPhone, keyword)
            );
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq(Clinic::getBusinessStatus, status);
        }

        queryWrapper.orderByDesc(Clinic::getCreatedAt);
        return clinicMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Clinic create(Clinic clinic) {
        // 检查编码是否已存在
        if (checkCodeExists(clinic.getClinicCode())) {
            throw new RuntimeException("诊所编码已存在");
        }

        // 设置默认值
        if (clinic.getBusinessStatus() == null) {
            clinic.setBusinessStatus(1); // 默认营业中
        }

        clinic.setCreatedAt(LocalDateTime.now());
        clinic.setUpdatedAt(LocalDateTime.now());
        clinic.setDeleted(0);

        clinicMapper.insert(clinic);

        // 自动创建诊所管理员账号
        createDefaultAdmin(clinic);

        return clinic;
    }

    /**
     * 为新诊所创建默认管理员账号
     */
    private void createDefaultAdmin(Clinic clinic) {
        String baseUsername = clinic.getClinicCode() + "_admin";
        String adminUsername = baseUsername;
        int retry = 0;

        // 如果用户名已存在，尝试添加序号
        while (sysUserService.checkUsernameExists(adminUsername) && retry < 10) {
            retry++;
            adminUsername = baseUsername + retry;
            log.warn("用户名 [{}] 已存在，尝试使用: {}", baseUsername, adminUsername);
        }

        log.info("开始为诊所 [{}] 创建管理员账号: {}", clinic.getClinicName(), adminUsername);

        try {
            SysUser admin = new SysUser();
            admin.setUsername(adminUsername);
            admin.setRealName(clinic.getContactPerson() != null ? clinic.getContactPerson() : "管理员");
            admin.setPhone(clinic.getContactPhone());
            admin.setEmail(clinic.getClinicCode() + "@clinic.com");
            admin.setClinicId(clinic.getId());
            admin.setRoleId(2L); // 诊所管理员角色
            admin.setStatus(1);
            admin.setPassword("123456");

            sysUserService.create(admin);
            log.info("管理员账号 [{}] 创建成功，ID: {}", adminUsername, admin.getId());

            // 插入用户角色关联表
            if (admin.getId() != null) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(admin.getId());
                userRole.setRoleId(2L); // 诊所管理员角色
                sysUserRoleMapper.insert(userRole);
                log.info("用户角色关联已创建: userId={}, roleId=2", admin.getId());
            }
        } catch (Exception e) {
            log.error("创建管理员账号 [{}] 失败: {}", adminUsername, e.getMessage(), e);
            // 不抛出异常，让诊所创建成功，但记录错误日志
            // throw new RuntimeException("创建管理员账号失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Clinic update(Clinic clinic) {
        Clinic existingClinic = clinicMapper.selectById(clinic.getId());
        if (existingClinic == null) {
            throw new RuntimeException("诊所不存在");
        }

        // 如果修改了编码，检查是否与其他诊所冲突
        if (!existingClinic.getClinicCode().equals(clinic.getClinicCode())) {
            if (checkCodeExists(clinic.getClinicCode())) {
                throw new RuntimeException("诊所编码已存在");
            }
        }

        clinic.setUpdatedAt(LocalDateTime.now());
        clinicMapper.updateById(clinic);

        return clinicMapper.selectById(clinic.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long clinicId) {
        Clinic clinic = clinicMapper.selectById(clinicId);
        if (clinic == null) {
            throw new RuntimeException("诊所不存在");
        }

        // MyBatis-Plus @TableLogic 会自动处理为逻辑删除
        clinicMapper.deleteById(clinicId);
    }

    @Override
    public Clinic getByCode(String clinicCode) {
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getClinicCode, clinicCode)
                .eq(Clinic::getDeleted, 0);
        return clinicMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean checkCodeExists(String clinicCode) {
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getClinicCode, clinicCode)
                .eq(Clinic::getDeleted, 0);
        return clinicMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public List<Clinic> getActiveClinics() {
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getBusinessStatus, 1)
                .eq(Clinic::getDeleted, 0)
                .orderByDesc(Clinic::getCreatedAt);
        return clinicMapper.selectList(queryWrapper);
    }
}
