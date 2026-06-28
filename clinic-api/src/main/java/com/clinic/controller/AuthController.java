package com.clinic.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.Constants;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.controller.dto.RegisterDTO;
import com.clinic.security.JwtUtil;
import com.clinic.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Api(tags = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @ApiOperation("用户登录")
    @OperationLog(module = "用户认证", type = OperationType.LOGIN, desc = "用户登录系统")
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        Integer loginType = (Integer) params.get("loginType");
        if (loginType == null) {
            loginType = 2;
        }
        return authService.login(username, password, loginType);
    }

    @ApiOperation("小程序登录")
    @PostMapping("/miniapp/login")
    public Result<?> miniappLogin(@RequestBody Map<String, String> params) {
        return authService.miniappLogin(params);
    }

    @ApiOperation("获取用户诊所列表")
    @GetMapping("/clinics")
    public Result<?> getUserClinics() {
        // 从当前登录用户获取userId
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        
        if (authentication != null && authentication.getPrincipal() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) authentication.getPrincipal();
            Object userIdObj = userInfo.get("userId");
            if (userIdObj != null) {
                userId = (userIdObj instanceof Integer) ? ((Integer) userIdObj).longValue() : (Long) userIdObj;
            }
        }
        
        // 如果没有从认证信息获取到，尝试从token解析
        if (userId == null) {
            // 这里简化处理，实际应该从请求头获取token
            return Result.badRequest("无法获取用户信息");
        }
        
        return authService.getUserClinics(userId);
    }

    @ApiOperation("用户登出")
    @OperationLog(module = "用户认证", type = OperationType.LOGOUT, desc = "用户退出系统")
    @PostMapping("/logout")
    public Result<?> logout() {
        return authService.logout();
    }

    @ApiOperation("获取验证码")
    @GetMapping("/captcha")
    public Result<?> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 30);
        String captchaId = IdUtil.simpleUUID();
        String captchaCode = captcha.getCode();
        String captchaImage = captcha.getImageBase64();

        redisTemplate.opsForValue().set(
                Constants.REDIS_CAPTCHA_PREFIX + captchaId,
                captchaCode.toLowerCase(),
                5,
                TimeUnit.MINUTES
        );

        Map<String, String> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("captchaImage", captchaImage);
        return Result.success(data);
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/info")
    public Result<?> getUserInfo(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!jwtUtil.validateToken(token)) {
            return Result.unauthorized("Token无效或已过期");
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", jwtUtil.getUserId(token));
        userInfo.put("username", jwtUtil.getUsername(token));
        userInfo.put("realName", jwtUtil.getRealName(token));
        userInfo.put("clinicId", jwtUtil.getClinicId(token));
        
        return Result.success(userInfo);
    }
}
