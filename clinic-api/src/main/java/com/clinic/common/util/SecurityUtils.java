package com.clinic.common.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.clinic.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public class SecurityUtils {

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateToken() {
        return IdUtil.simpleUUID();
    }

    public static String md5(String str) {
        return SecureUtil.md5(str);
    }

    public static String sha256(String str) {
        return SecureUtil.sha256(str);
    }

    public static String generateNo(String prefix) {
        return prefix + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    public static String generateRegNo() {
        return generateNo("GH");
    }

    public static String generateRecordNo() {
        return generateNo("MR");
    }

    public static String generatePrescriptionNo() {
        return generateNo("CF");
    }

    public static String generateOrderNo() {
        return generateNo("ORDER");
    }

    public static String generateCardNo() {
        return "CK" + System.currentTimeMillis();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUserId();
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return "system";
    }

    /**
     * 获取当前登录用户真实姓名
     */
    public static String getCurrentRealName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getRealName();
        }
        return "system";
    }

    /**
     * 获取当前登录用户认证信息
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户的诊所ID
     */
    public static Long getCurrentClinicId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getClinicId();
        }
        return null;
    }
}