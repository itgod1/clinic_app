package com.clinic.common.constant;

/**
 * 操作类型常量
 */
public class OperationType {

    /**
     * 新增
     */
    public static final int CREATE = 1;

    /**
     * 修改
     */
    public static final int UPDATE = 2;

    /**
     * 删除
     */
    public static final int DELETE = 3;

    /**
     * 查询
     */
    public static final int QUERY = 4;

    /**
     * 导出
     */
    public static final int EXPORT = 5;

    /**
     * 登录
     */
    public static final int LOGIN = 6;

    /**
     * 登出
     */
    public static final int LOGOUT = 7;

    /**
     * 其他
     */
    public static final int OTHER = 8;

    /**
     * 审核
     */
    public static final int AUDIT = 9;

    /**
     * 导入
     */
    public static final int IMPORT = 10;

    /**
     * 打印
     */
    public static final int PRINT = 11;

    /**
     * 支付
     */
    public static final int PAY = 12;

    /**
     * 退费
     */
    public static final int REFUND = 13;

    /**
     * 医保结算
     */
    public static final int INSURANCE_SETTLE = 14;

    /**
     * 咨询记录-创建
     */
    public static final int CONSULTATION_CREATE = 15;

    /**
     * 咨询记录-更新
     */
    public static final int CONSULTATION_UPDATE = 16;

    /**
     * 获取操作类型名称
     */
    public static String getName(int type) {
        switch (type) {
            case CREATE:
                return "新增";
            case UPDATE:
                return "修改";
            case DELETE:
                return "删除";
            case QUERY:
                return "查询";
            case EXPORT:
                return "导出";
            case LOGIN:
                return "登录";
            case LOGOUT:
                return "登出";
            case AUDIT:
                return "审核";
            case IMPORT:
                return "导入";
            case PRINT:
                return "打印";
            case PAY:
                return "支付";
            case REFUND:
                return "退费";
            case INSURANCE_SETTLE:
                return "医保结算";
            case CONSULTATION_CREATE:
                return "咨询记录创建";
            case CONSULTATION_UPDATE:
                return "咨询记录更新";
            case OTHER:
            default:
                return "其他";
        }
    }
}
