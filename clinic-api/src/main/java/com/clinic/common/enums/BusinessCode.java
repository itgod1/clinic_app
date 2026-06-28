package com.clinic.common.enums;

import lombok.Getter;

@Getter
public enum BusinessCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    USERNAME_PASSWORD_ERROR(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账户已被禁用"),
    CAPTCHA_ERROR(1003, "验证码错误"),

    REG_FULL(2001, "挂号已满"),
    NOT_IN_BOOKING_TIME(2002, "不在可预约时间内"),
    REG_EXPIRED(2003, "预约已过期"),
    REG_CONFLICT(2004, "存在冲突预约"),

    BALANCE_NOT_ENOUGH(3001, "余额不足"),
    PAYMENT_FAILED(3002, "支付失败"),
    ORDER_ALREADY_PAID(3003, "订单已支付"),
    REFUND_EXCEED(3004, "退款金额超出实际支付"),

    STOCK_NOT_ENOUGH(4001, "库存不足"),
    ITEM_DISABLED(4002, "项目已停用"),

    DATA_NOT_EXIST(5001, "数据不存在"),
    DATA_ALREADY_EXIST(5002, "数据已存在"),
    DATA_HAS_RELATION(5003, "数据已关联，无法删除");

    private final Integer code;
    private final String message;

    BusinessCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}