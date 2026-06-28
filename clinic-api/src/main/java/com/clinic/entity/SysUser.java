package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long deptId;
    private String username;
    private String password;
    private String salt;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Long roleId;
    private String post;
    private Integer sex;
    private Integer status;
    private String openid; // 微信openid

    @TableField(exist = false)
    private String roleName;

    @TableField(exist = false)
    private String clinicName;

    private Integer loginCount;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private LocalDateTime passwordExpireTime;
    private Integer passwordErrorCount;
    private LocalDateTime lockTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}