package com.clinic.service;

import com.clinic.common.result.Result;
import com.clinic.controller.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {
    Result<?> login(String username, String password, Integer loginType);
    Result<?> logout();
    Result<?> register(RegisterDTO registerDTO);
    
    /**
     * 小程序登录
     * @param params 包含code, encryptedData, iv
     * @return 登录结果
     */
    Result<?> miniappLogin(Map<String, String> params);
    
    /**
     * 获取用户关联的诊所列表
     * @param userId 用户ID
     * @return 诊所列表
     */
    Result<?> getUserClinics(Long userId);
}
