package com.clinic.service;

import com.clinic.dto.InsuranceEligibilityResult;
import com.clinic.dto.InsuranceSubmitResult;
import com.clinic.dto.InsuranceQueryResult;
import com.clinic.entity.InsuranceSettlement;
import com.clinic.entity.InsuranceSettlementDetail;

import java.util.List;

/**
 * 医保平台对接接口
 * 抽象省内医保平台的各种API调用，不同省份可实现各自的接口
 */
public interface InsurancePlatformService {

    /**
     * 验证患者医保资格
     */
    InsuranceEligibilityResult verifyEligibility(Long patientId, String insuranceCardNo);

    /**
     * 提交结算申请到医保平台
     */
    InsuranceSubmitResult submitClaim(InsuranceSettlement settlement, List<InsuranceSettlementDetail> details);

    /**
     * 查询结算状态
     */
    InsuranceQueryResult querySettlement(String settlementNo);

    /**
     * 取消/撤销结算
     */
    boolean cancelSettlement(String settlementNo, String reason);
}
