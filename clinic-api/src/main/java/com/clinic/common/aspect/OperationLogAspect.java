package com.clinic.common.aspect;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.util.IpUtils;
import com.clinic.common.util.SecurityUtils;
import com.clinic.mapper.OperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 定义切点 - 带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.clinic.common.annotation.OperationLog)")
    public void operationLogPointCut() {
    }

    /**
     * 方法成功返回后记录日志
     */
    @AfterReturning(pointcut = "operationLogPointCut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            handleLog(joinPoint, result, null);
        } catch (Exception e) {
            log.error("记录操作日志异常(成功):", e);
        }
    }

    /**
     * 方法抛出异常后记录日志
     */
    @AfterThrowing(pointcut = "operationLogPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        try {
            handleLog(joinPoint, null, e);
        } catch (Exception ex) {
            log.error("记录操作日志异常(失败):", ex);
        }
    }

    /**
     * 处理日志记录
     */
    private void handleLog(JoinPoint joinPoint, Object result, Exception exception) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        if (annotation == null) {
            return;
        }

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        // 构建日志实体
        com.clinic.entity.OperationLog operationLog = new com.clinic.entity.OperationLog();

        // 设置诊所ID
        Long clinicId = getClinicIdFromRequest(request);
        operationLog.setClinicId(clinicId);

        // 设置用户信息
        Long userId = null;
        String username = null;
        String realName = null;

        // 对于登录操作，从请求参数获取用户名
        if (annotation.type() == OperationType.LOGIN) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0 && args[0] instanceof Map) {
                    Map<String, Object> params = (Map<String, Object>) args[0];
                    username = (String) params.get("username");
                }
            } catch (Exception e) {
                // ignore
            }
        } else {
            // 其他操作从当前认证用户获取
            try {
                userId = SecurityUtils.getCurrentUserId();
                username = SecurityUtils.getCurrentUsername();
                realName = SecurityUtils.getCurrentRealName();
            } catch (Exception e) {
                // ignore
            }
        }

        operationLog.setUserId(userId);
        operationLog.setUsername(username != null ? username : "system");
        operationLog.setRealName(realName != null ? realName : (username != null ? username : "system"));

        // 设置操作信息
        operationLog.setModule(annotation.module());
        operationLog.setOperationType(annotation.type());
        operationLog.setDescription(annotation.desc());

        // 设置请求信息
        operationLog.setRequestMethod(request.getMethod());
        operationLog.setRequestUrl(request.getRequestURI());
        operationLog.setIpAddress(IpUtils.getIpAddress(request));
        operationLog.setUserAgent(request.getHeader("User-Agent"));

        // 设置请求参数
        if (annotation.saveRequest()) {
            String params = getRequestParams(joinPoint, annotation.excludeFields());
            operationLog.setRequestParams(params);
        }

        // 设置响应结果
        if (annotation.saveResponse() && result != null) {
            try {
                String responseStr = objectMapper.writeValueAsString(result);
                // 限制长度
                if (responseStr.length() > 2000) {
                    responseStr = responseStr.substring(0, 2000) + "...";
                }
                operationLog.setResponseData(responseStr);
            } catch (Exception e) {
                log.warn("序列化响应结果失败", e);
            }
        }

        // 设置操作状态
        if (exception != null) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(exception.getMessage());
        } else {
            operationLog.setStatus(1);
        }

        // 设置操作时间
        operationLog.setOperationTime(LocalDateTime.now());

        // 异步保存日志
        new Thread(() -> {
            try {
                operationLogMapper.insert(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败:", e);
            }
        }).start();
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(JoinPoint joinPoint, String[] excludeFields) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "{}";
            }

            // 过滤掉HttpServletRequest和HttpServletResponse
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                String className = arg.getClass().getName();
                if (className.contains("HttpServletRequest") ||
                    className.contains("HttpServletResponse") ||
                    className.contains("MultipartFile")) {
                    continue;
                }
                params.put("arg" + i, arg);
            }

            String json = objectMapper.writeValueAsString(params);

            // 敏感字段脱敏
            for (String field : excludeFields) {
                json = json.replaceAll("\"" + field + "\":\"[^\"]*\"", "\"" + field + "\":\"***\"");
            }

            // 限制长度
            if (json.length() > 2000) {
                json = json.substring(0, 2000) + "...";
            }

            return json;
        } catch (Exception e) {
            log.warn("获取请求参数失败", e);
            return "{}";
        }
    }

    /**
     * 从请求中获取诊所ID
     */
    private Long getClinicIdFromRequest(HttpServletRequest request) {
        // 优先从请求参数获取
        String clinicId = request.getParameter("clinicId");
        if (clinicId != null && !clinicId.isEmpty()) {
            try {
                return Long.valueOf(clinicId);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // 从请求头获取
        clinicId = request.getHeader("X-Clinic-Id");
        if (clinicId != null && !clinicId.isEmpty()) {
            try {
                return Long.valueOf(clinicId);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // 从当前用户获取
        try {
            Long userClinicId = SecurityUtils.getCurrentClinicId();
            if (userClinicId != null) {
                return userClinicId;
            }
        } catch (Exception e) {
            // ignore
        }

        // 默认返回0
        return 0L;
    }
}
