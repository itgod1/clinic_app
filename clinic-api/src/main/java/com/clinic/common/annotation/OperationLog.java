package com.clinic.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     * 1-新增, 2-修改, 3-删除, 4-查询, 5-导出, 6-登录, 7-登出, 8-其他
     */
    int type() default 8;

    /**
     * 操作描述
     */
    String desc() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveRequest() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResponse() default false;

    /**
     * 排除敏感字段（不记录到日志中）
     */
    String[] excludeFields() default {"password", "passwd", "pwd", "secret", "token"};
}
