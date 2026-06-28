package com.clinic.common.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DictConstants {

    private DictConstants() {}

    public static final Map<Integer, String> ITEM_TYPE_MAP;
    public static final Map<Integer, String> PRESCRIPTION_TYPE_MAP;
    public static final Map<Integer, String> PAYMENT_STATUS_MAP;
    public static final Map<Integer, String> VISIT_TYPE_MAP;
    public static final Map<Integer, String> REGISTRATION_STATUS_MAP;
    public static final Map<Integer, String> MEMBER_LEVEL_MAP;
    public static final Map<Integer, String> PAYMENT_METHOD_MAP;
    public static final Map<String, String> INSURANCE_TYPE_MAP;
    public static final Map<String, String> INSURANCE_STATUS_MAP;
    public static final Map<String, String> CATALOG_TYPE_MAP;
    public static final Map<String, String> SETTLEMENT_STATUS_MAP;
    public static final Map<String, String> INSURANCE_CATEGORY_MAP;
    public static final Map<Integer, String> RECORD_TYPE_MAP;
    public static final Map<Integer, String> CONSULT_STATUS_MAP;

    static {
        Map<Integer, String> itemTypeMap = new HashMap<>();
        itemTypeMap.put(1, "西药");
        itemTypeMap.put(2, "中成药");
        itemTypeMap.put(3, "中药饮片");
        itemTypeMap.put(4, "耗材");
        ITEM_TYPE_MAP = Collections.unmodifiableMap(itemTypeMap);

        Map<Integer, String> prescriptionTypeMap = new HashMap<>();
        prescriptionTypeMap.put(1, "西药处方");
        prescriptionTypeMap.put(2, "中药处方");
        prescriptionTypeMap.put(3, "检查单");
        PRESCRIPTION_TYPE_MAP = Collections.unmodifiableMap(prescriptionTypeMap);

        Map<Integer, String> paymentStatusMap = new HashMap<>();
        paymentStatusMap.put(0, "待缴费");
        paymentStatusMap.put(1, "已缴费");
        paymentStatusMap.put(2, "已退费");
        PAYMENT_STATUS_MAP = Collections.unmodifiableMap(paymentStatusMap);

        Map<Integer, String> visitTypeMap = new HashMap<>();
        visitTypeMap.put(1, "初诊");
        visitTypeMap.put(2, "复诊");
        visitTypeMap.put(3, "急诊");
        VISIT_TYPE_MAP = Collections.unmodifiableMap(visitTypeMap);

        Map<Integer, String> registrationStatusMap = new HashMap<>();
        registrationStatusMap.put(1, "待就诊");
        registrationStatusMap.put(2, "已就诊");
        registrationStatusMap.put(3, "已取消");
        registrationStatusMap.put(4, "已退号");
        REGISTRATION_STATUS_MAP = Collections.unmodifiableMap(registrationStatusMap);

        Map<Integer, String> memberLevelMap = new HashMap<>();
        memberLevelMap.put(1, "普通会员");
        memberLevelMap.put(2, "银卡会员");
        memberLevelMap.put(3, "金卡会员");
        memberLevelMap.put(4, "钻石会员");
        MEMBER_LEVEL_MAP = Collections.unmodifiableMap(memberLevelMap);

        Map<Integer, String> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(1, "微信");
        paymentMethodMap.put(2, "支付宝");
        paymentMethodMap.put(3, "现金");
        paymentMethodMap.put(4, "银行卡");
        paymentMethodMap.put(5, "会员卡");
        paymentMethodMap.put(6, "医保");
        PAYMENT_METHOD_MAP = Collections.unmodifiableMap(paymentMethodMap);

        Map<String, String> insuranceTypeMap = new HashMap<>();
        insuranceTypeMap.put("URBAN_EMPLOYEE", "职工医保");
        insuranceTypeMap.put("URBAN_RESIDENT", "居民医保");
        insuranceTypeMap.put("NEW_RURAL", "新农合");
        insuranceTypeMap.put("COMMERCIAL", "商业保险");
        INSURANCE_TYPE_MAP = Collections.unmodifiableMap(insuranceTypeMap);

        Map<String, String> insuranceStatusMap = new HashMap<>();
        insuranceStatusMap.put("ACTIVE", "正常");
        insuranceStatusMap.put("SUSPENDED", "暂停");
        insuranceStatusMap.put("CANCELLED", "注销");
        INSURANCE_STATUS_MAP = Collections.unmodifiableMap(insuranceStatusMap);

        Map<String, String> catalogTypeMap = new HashMap<>();
        catalogTypeMap.put("DRUG", "药品");
        catalogTypeMap.put("TREATMENT", "诊疗");
        catalogTypeMap.put("MATERIAL", "耗材");
        CATALOG_TYPE_MAP = Collections.unmodifiableMap(catalogTypeMap);

        Map<String, String> settlementStatusMap = new HashMap<>();
        settlementStatusMap.put("PENDING", "待结算");
        settlementStatusMap.put("SUBMITTED", "已提交");
        settlementStatusMap.put("SUCCESS", "结算成功");
        settlementStatusMap.put("FAILED", "结算失败");
        settlementStatusMap.put("CANCELLED", "已取消");
        SETTLEMENT_STATUS_MAP = Collections.unmodifiableMap(settlementStatusMap);

        Map<String, String> insuranceCategoryMap = new HashMap<>();
        insuranceCategoryMap.put("A", "甲类");
        insuranceCategoryMap.put("B", "乙类");
        insuranceCategoryMap.put("C", "丙类");
        INSURANCE_CATEGORY_MAP = Collections.unmodifiableMap(insuranceCategoryMap);

        Map<Integer, String> recordTypeMap = new HashMap<>();
        recordTypeMap.put(1, "咨询记录");
        recordTypeMap.put(2, "设计单");
        RECORD_TYPE_MAP = Collections.unmodifiableMap(recordTypeMap);

        Map<Integer, String> consultStatusMap = new HashMap<>();
        consultStatusMap.put(1, "进行中");
        consultStatusMap.put(2, "已完成");
        consultStatusMap.put(3, "已取消");
        CONSULT_STATUS_MAP = Collections.unmodifiableMap(consultStatusMap);
    }

    public static String getItemTypeName(Integer type) {
        return ITEM_TYPE_MAP.getOrDefault(type, "-");
    }

    public static String getPrescriptionTypeName(Integer type) {
        return PRESCRIPTION_TYPE_MAP.getOrDefault(type, "-");
    }

    public static String getPaymentStatusName(Integer status) {
        return PAYMENT_STATUS_MAP.getOrDefault(status, "-");
    }

    public static String getVisitTypeName(Integer type) {
        return VISIT_TYPE_MAP.getOrDefault(type, "-");
    }

    public static String getRegistrationStatusName(Integer status) {
        return REGISTRATION_STATUS_MAP.getOrDefault(status, "-");
    }

    public static String getMemberLevelName(Integer level) {
        return MEMBER_LEVEL_MAP.getOrDefault(level, "普通会员");
    }

    public static String getPaymentMethodName(Integer method) {
        return PAYMENT_METHOD_MAP.getOrDefault(method, "-");
    }

    public static String getInsuranceTypeName(String type) {
        return INSURANCE_TYPE_MAP.getOrDefault(type, type);
    }

    public static String getInsuranceStatusName(String status) {
        return INSURANCE_STATUS_MAP.getOrDefault(status, status);
    }

    public static String getCatalogTypeName(String type) {
        return CATALOG_TYPE_MAP.getOrDefault(type, type);
    }

    public static String getSettlementStatusName(String status) {
        return SETTLEMENT_STATUS_MAP.getOrDefault(status, status);
    }

    public static String getInsuranceCategoryName(String category) {
        return INSURANCE_CATEGORY_MAP.getOrDefault(category, category);
    }

    public static String getRecordTypeName(Integer type) {
        return RECORD_TYPE_MAP.getOrDefault(type, "-");
    }

    public static String getConsultStatusName(Integer status) {
        return CONSULT_STATUS_MAP.getOrDefault(status, "-");
    }
}
