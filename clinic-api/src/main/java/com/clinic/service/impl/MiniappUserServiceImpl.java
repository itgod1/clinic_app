package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Clinic;
import com.clinic.entity.MiniappUser;
import com.clinic.entity.UserClinicRelation;
import com.clinic.mapper.ClinicMapper;
import com.clinic.mapper.MiniappUserMapper;
import com.clinic.mapper.UserClinicRelationMapper;
import com.clinic.service.MiniappUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiniappUserServiceImpl extends ServiceImpl<MiniappUserMapper, MiniappUser> implements MiniappUserService {

    private final UserClinicRelationMapper userClinicRelationMapper;
    private final ClinicMapper clinicMapper;

    @Override
    @Transactional
    public MiniappUser findOrCreateByOpenid(Map<String, String> wxData) {
        String openid = wxData.get("openid");
        String sessionKey = wxData.get("sessionKey");
        String unionid = wxData.get("unionid");

        // 1. 查询是否已存在
        MiniappUser user = baseMapper.selectByOpenid(openid);

        if (user != null) {
            log.info("小程序用户已存在: openid={}", openid);
            return user;
        }

        // 2. 创建新用户（不直接分配诊所，通过user_clinic_relation表关联）
        user = new MiniappUser();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        user.setNickname("微信用户" + System.currentTimeMillis() % 10000);
        user.setStatus(1);
        user.setLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        baseMapper.insert(user);
        log.info("创建小程序用户成功: id={}, openid={}", user.getId(), openid);

        // 注意：新用户创建后没有默认诊所，需要通过后台管理或邀请机制关联诊所

        return user;
    }

    @Override
    @Transactional
    public MiniappUser findOrCreateByPhoneOrOpenid(String phone, Map<String, String> wxData) {
        String openid = wxData.get("openid");
        String sessionKey = wxData.get("sessionKey");
        String unionid = wxData.get("unionid");

        // 1. 优先根据手机号查找用户
        if (phone != null && !phone.isEmpty()) {
            MiniappUser userByPhone = baseMapper.selectByPhone(phone);
            if (userByPhone != null) {
                log.info("根据手机号找到已有用户: phone={}, userId={}", phone, userByPhone.getId());
                // 如果openid不同，更新openid
                if (!openid.equals(userByPhone.getOpenid())) {
                    userByPhone.setOpenid(openid);
                    userByPhone.setUnionid(unionid);
                    userByPhone.setUpdatedAt(LocalDateTime.now());
                    baseMapper.updateById(userByPhone);
                    log.info("更新用户openid: userId={}, newOpenid={}", userByPhone.getId(), openid);
                }
                return userByPhone;
            }
        }

        // 2. 根据openid查找用户
        MiniappUser user = baseMapper.selectByOpenid(openid);
        if (user != null) {
            log.info("根据openid找到已有用户: openid={}, userId={}", openid, user.getId());
            // 如果手机号为空，更新手机号
            if (user.getPhone() == null || user.getPhone().isEmpty()) {
                if (phone != null && !phone.isEmpty()) {
                    user.setPhone(phone);
                    user.setUpdatedAt(LocalDateTime.now());
                    baseMapper.updateById(user);
                    log.info("更新用户手机号: userId={}, phone={}", user.getId(), phone);
                }
            }
            return user;
        }

        // 3. 创建新用户
        user = new MiniappUser();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        user.setPhone(phone);
        user.setNickname("微信用户" + System.currentTimeMillis() % 10000);
        user.setStatus(1);
        user.setLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        baseMapper.insert(user);
        log.info("创建小程序用户成功: id={}, openid={}, phone={}", user.getId(), openid, phone);

        return user;
    }

    @Override
    public void updateLoginInfo(Long userId) {
        baseMapper.updateLoginInfo(userId);
    }

    @Override
    public boolean bindPatient(Long miniappUserId, Long patientId) {
        MiniappUser user = new MiniappUser();
        user.setId(miniappUserId);
        user.setPatientId(patientId);
        user.setUpdatedAt(LocalDateTime.now());
        
        return baseMapper.updateById(user) > 0;
    }

    @Override
    public boolean switchClinic(Long miniappUserId, Long clinicId) {
        MiniappUser user = new MiniappUser();
        user.setId(miniappUserId);
        user.setCurrentClinicId(clinicId);
        user.setUpdatedAt(LocalDateTime.now());
        
        return baseMapper.updateById(user) > 0;
    }

    /**
     * 获取用户关联的诊所列表
     */
    @Override
    public List<Map<String, Object>> getUserClinics(Long miniappUserId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 从user_clinic_relation表查询用户关联的诊所
        List<UserClinicRelation> relations = userClinicRelationMapper.selectByUserId(
                miniappUserId, 2); // 2表示小程序用户
        
        for (UserClinicRelation relation : relations) {
            Clinic clinic = clinicMapper.selectById(relation.getClinicId());
            if (clinic != null && clinic.getBusinessStatus() == 1) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", clinic.getId());
                map.put("clinicName", clinic.getClinicName());
                map.put("clinicCode", clinic.getClinicCode());
                map.put("address", clinic.getAddress());
                map.put("contactPhone", clinic.getContactPhone());
                map.put("logoUrl", clinic.getLogoUrl());
                map.put("status", clinic.getBusinessStatus());
                map.put("isDefault", relation.getIsDefault());
                map.put("roleType", relation.getRoleType());
                result.add(map);
            }
        }
        
        return result;
    }

    /**
     * 为用户添加诊所关联（用于后台管理或邀请注册）
     */
    @Override
    @Transactional
    public boolean addUserClinicRelation(Long miniappUserId, Long clinicId, Integer roleType, boolean isDefault) {
        // 检查是否已存在
        List<UserClinicRelation> existing = userClinicRelationMapper.selectByUserId(miniappUserId, 2);
        boolean alreadyExists = existing.stream()
                .anyMatch(r -> r.getClinicId().equals(clinicId));
        
        if (alreadyExists) {
            log.warn("用户已有关联该诊所: userId={}, clinicId={}", miniappUserId, clinicId);
            return false;
        }
        
        UserClinicRelation relation = new UserClinicRelation();
        relation.setUserId(miniappUserId);
        relation.setUserType(2); // 小程序用户
        relation.setClinicId(clinicId);
        relation.setRoleType(roleType);
        relation.setIsDefault(isDefault ? 1 : 0);
        relation.setStatus(1);
        relation.setCreatedAt(LocalDateTime.now());
        relation.setUpdatedAt(LocalDateTime.now());
        
        int result = userClinicRelationMapper.insert(relation);
        
        // 如果是默认诊所，更新用户的current_clinic_id
        if (isDefault && result > 0) {
            switchClinic(miniappUserId, clinicId);
        }
        
        return result > 0;
    }
}
