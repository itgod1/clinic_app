package com.clinic.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 系统用户DTO
 */
@Data
public class SysUserDTO {

    private Long id;

    @NotNull(message = "诊所ID不能为空")
    private Long clinicId;

    private Long deptId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String salt;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    private String email;

    private String avatar;

    private Long roleId;

    private String post;

    private Integer sex;

    private Integer status;

    private String remark;

    // 非数据库字段
    private String roleName;
    private String clinicName;
    private String deptName;
    private LocalDateTime createdAt;
}
