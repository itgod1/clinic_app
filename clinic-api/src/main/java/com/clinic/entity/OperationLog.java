package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志审计表
 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 诊所ID
     */
    private Long clinicId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作类型：1新增 2修改 3删除 4查询 5导出 6登录 7登出 8其他
     */
    private Integer operationType;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String requestParams;

    /**
     * 响应结果
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String responseData;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 浏览器UA
     */
    private String userAgent;

    /**
     * 操作状态：0失败 1成功
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行时长（毫秒）
     */
    private Long executionTime;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 创建时间
     */
    //@TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
