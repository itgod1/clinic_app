package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.common.constant.Constants;
import com.clinic.common.result.Result;
import com.clinic.common.util.WechatUtil;
import com.clinic.controller.dto.RegisterDTO;
import com.clinic.entity.Clinic;
import com.clinic.entity.MiniappUser;
import com.clinic.entity.Patient;
import com.clinic.entity.SysUser;
import com.clinic.mapper.*;
import com.clinic.security.JwtUtil;
import com.clinic.service.AuthService;
import com.clinic.service.MiniappUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final ClinicMapper clinicMapper;
    private final DoctorMapper doctorMapper;
    private final MiniappUserService miniappUserService;
    private final MiniappUserMapper miniappUserMapper;

    private final PatientMapper patientMapper;

    @Override
    public Result<?> login(String username, String password, Integer loginType) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            return Result.badRequest("用户名或密码错误");
        } catch (DisabledException e) {
            return Result.badRequest("账号已被禁用");
        } catch (Exception e) {
            log.error("认证失败: {}", e.getMessage());
            return Result.badRequest("认证失败");
        }

        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getStatus, 1)
        );

        if (user == null) {
            return Result.badRequest("用户不存在或已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getClinicId().intValue(), user.getRealName());

        // 可选：将token存入Redis（仅在Redis可用时）
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(
                        Constants.REDIS_USER_PREFIX + user.getId(),
                        token,
                        Constants.EXPIRE_TIME,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.warn("Redis存储token失败，继续登录流程: {}", e.getMessage());
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("clinicId", user.getClinicId());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("deptId", user.getDeptId());

        if (user.getClinicId() != null) {
            Clinic clinic = clinicMapper.selectById(user.getClinicId());
            if (clinic != null) {
                userInfo.put("clinicName", clinic.getClinicName());
            }
        }

        // 如果角色是医生(3)，查询医生ID
        if (user.getRoleId() != null && user.getRoleId() == 3) {
            Long doctorId = doctorMapper.selectIdByClinicIdAndName(user.getClinicId(), user.getRealName());
            if (doctorId != null) {
                userInfo.put("doctorId", doctorId);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expireTime", System.currentTimeMillis() + Constants.EXPIRE_TIME);
        data.put("userInfo", userInfo);

        return Result.success("登录成功", data);
    }

    @Override
    public Result<?> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) authentication.getPrincipal();
            Object userIdObj = userInfo.get("userId");
            if (userIdObj != null) {
                Long userId = (userIdObj instanceof Integer) ? ((Integer) userIdObj).longValue() : (Long) userIdObj;
                // 可选：从Redis删除token（仅在Redis可用时）
                try {
                    if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                        redisTemplate.delete(Constants.REDIS_USER_PREFIX + userId);
                    }
                } catch (Exception e) {
                    log.warn("Redis删除token失败，继续登出流程: {}", e.getMessage());
                }
            }
        }
        SecurityContextHolder.clearContext();
        return Result.success("登出成功", null);
    }

    @Override
    public Result<?> register(RegisterDTO registerDTO) {
        if (!StringUtils.hasText(registerDTO.getUsername())) {
            return Result.badRequest("用户名不能为空");
        }
        if (!StringUtils.hasText(registerDTO.getPassword())) {
            return Result.badRequest("密码不能为空");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return Result.badRequest("两次输入的密码不一致");
        }

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, registerDTO.getUsername());
        Long count = sysUserMapper.selectCount(queryWrapper);
        if (count > 0) {
            return Result.badRequest("用户名已存在");
        }

        if (StringUtils.hasText(registerDTO.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, registerDTO.getPhone());
            Long phoneCount = sysUserMapper.selectCount(phoneWrapper);
            if (phoneCount > 0) {
                return Result.badRequest("该手机号已被注册");
            }
        }

        if (StringUtils.hasText(registerDTO.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, registerDTO.getEmail());
            Long emailCount = sysUserMapper.selectCount(emailWrapper);
            if (emailCount > 0) {
                return Result.badRequest("该邮箱已被注册");
            }
        }

        SysUser sysUser = new SysUser();
        sysUser.setUsername(registerDTO.getUsername());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        sysUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        sysUser.setSalt(generateSalt());
        sysUser.setRealName(registerDTO.getRealName());
        sysUser.setPhone(registerDTO.getPhone());
        sysUser.setEmail(registerDTO.getEmail());
        sysUser.setClinicId(registerDTO.getClinicId() != null ? registerDTO.getClinicId() : 0L);
        sysUser.setDeptId(registerDTO.getDeptId() != null ? registerDTO.getDeptId() : 0L);
        sysUser.setStatus(1);
        sysUser.setRoleId(1L);
        sysUser.setCreatedAt(LocalDateTime.now());
        sysUser.setUpdatedAt(LocalDateTime.now());

        int insertResult = sysUserMapper.insert(sysUser);
        if (insertResult <= 0) {
            return Result.error("注册失败，请稍后重试");
        }

        log.info("用户注册成功: {}", registerDTO.getUsername());
        return Result.success("注册成功", null);
    }

    @Override
    public Result<?> miniappLogin(Map<String, String> params) {
        String code = params.get("code");
        String encryptedData = params.get("encryptedData");
        String iv = params.get("iv");

        if (!StringUtils.hasText(code)) {
            return Result.badRequest("缺少code参数");
        }

        // 1. 调用微信接口获取openid
        Map<String, String> wxData = WechatUtil.getOpenidByCode(code);
        String openid = wxData.get("openid");
        String sessionKey = wxData.get("sessionKey");

        if (!StringUtils.hasText(openid)) {
            // 模拟微信登录（开发测试用）
            // 使用固定的openid，确保同一用户不会重复创建
            openid = "mock_openid_" + code.substring(0, Math.min(10, code.length()));
            sessionKey = "mock_session_key";
            // 重要：将模拟的openid设置回wxData
            wxData.put("openid", openid);
            wxData.put("sessionKey", sessionKey);
            log.warn("微信登录失败，使用模拟openid: {}", openid);
        }

        // 2. 解密手机号（如果有加密数据）
        String phone = null;
        if (StringUtils.hasText(encryptedData) && StringUtils.hasText(iv)) {
            phone = WechatUtil.decryptData(encryptedData, sessionKey, iv);
            if (phone != null) {
                log.info("解密手机号成功: {}", phone);
            } else {
                log.warn("解密手机号失败，将使用openid查找用户");
            }
        }

        // 3. 优先根据手机号查找用户，找不到再根据openid查找或创建
        MiniappUser miniappUser = miniappUserService.findOrCreateByPhoneOrOpenid(phone, wxData);

        if (miniappUser == null) {
            return Result.error("用户创建失败");
        }

        // 4. 更新登录信息
        miniappUserService.updateLoginInfo(miniappUser.getId());

        // 5. 生成token（使用miniapp_user的id）
        String token = jwtUtil.generateToken(
                miniappUser.getId(),
                "miniapp_" + miniappUser.getId(),
                miniappUser.getCurrentClinicId() != null ? miniappUser.getCurrentClinicId().intValue() : 0,
                miniappUser.getNickname()
        );

        // 6. 获取用户关联的诊所列表（从miniapp_user的current_clinic_id或关联的患者信息）
        List<Map<String, Object>> clinicList = getMiniappUserClinicList(miniappUser);

        // 7. 如果没有绑定patientId，尝试根据openid查找Patient
        Long patientId = miniappUser.getPatientId();
        if (patientId == null) {
            Patient patient = patientMapper.selectByOpenId(miniappUser.getOpenid());
            if (patient != null) {
                patientId = patient.getId();
                // 更新miniappUser的patientId
                miniappUser.setPatientId(patientId);
                miniappUserService.updateById(miniappUser);
            }
        }

        // 8. 组装返回数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", miniappUser.getId());
        userInfo.put("nickname", miniappUser.getNickname());
        userInfo.put("avatarUrl", miniappUser.getAvatarUrl());
        userInfo.put("phone", miniappUser.getPhone());
        userInfo.put("patientId", patientId);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", userInfo);
        data.put("clinicList", clinicList);
        data.put("needSelectClinic", clinicList.size() > 1);

        return Result.success("登录成功", data);
    }

    @Override
    public Result<?> getUserClinics(Long userId) {
        // 这里需要区分是管理端用户还是小程序用户
        // 简单处理：先查miniapp_user，再查sys_user
        MiniappUser miniappUser = miniappUserService.getById(userId);
        if (miniappUser != null) {
            List<Map<String, Object>> clinicList = getMiniappUserClinicList(miniappUser);
            return Result.success(clinicList);
        }

        List<Map<String, Object>> clinicList = getUserClinicList(userId);
        return Result.success(clinicList);
    }

    /**
     * 获取小程序用户的诊所列表（从user_clinic_relation表查询）
     */
    private List<Map<String, Object>> getMiniappUserClinicList(MiniappUser miniappUser) {
        // 从user_clinic_relation表查询用户关联的所有诊所
        List<Map<String, Object>> list = miniappUserService.getUserClinics(miniappUser.getId());
        
        // 如果用户没有关联任何诊所，返回空列表（前端需要处理这种情况）
        if (list.isEmpty()) {
            log.warn("用户未关联任何诊所: miniappUserId={}", miniappUser.getId());
        }
        
        return list;
    }

    /**
     * 获取管理端用户的诊所列表
     */
    private List<Map<String, Object>> getUserClinicList(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getClinicId() == null) {
            return list;
        }

        // 获取用户的主诊所
        Clinic clinic = clinicMapper.selectById(user.getClinicId());
        if (clinic != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", clinic.getId());
            map.put("clinicName", clinic.getClinicName());
            map.put("clinicCode", clinic.getClinicCode());
            map.put("address", clinic.getAddress());
            map.put("contactPhone", clinic.getContactPhone());
            map.put("logoUrl", clinic.getLogoUrl());
            map.put("status", clinic.getBusinessStatus());
            map.put("isDefault", true);
            list.add(map);
        }

        return list;
    }

    private String generateSalt() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
