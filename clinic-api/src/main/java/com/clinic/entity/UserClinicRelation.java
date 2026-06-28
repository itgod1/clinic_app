package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-诊所关联表
 * 支持多租户：一个用户可以关联多个诊所
 */
@Data
@TableName("user_clinic_relation")
public class UserClinicRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（可以是sys_user.id或miniapp_user.id）
     */
    private Long userId;

    /**
     * 用户类型：1-管理端用户 2-小程序用户
     */
    private Integer userType;

    /**
     * 诊所ID
     */
    private Long clinicId;

    /**
     * 是否为默认诊所 0-否 1-是
     */
    private Integer isDefault;

    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;

    /**
     * 角色：1-患者 2-医生 3-管理员
     */
    private Integer roleType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
