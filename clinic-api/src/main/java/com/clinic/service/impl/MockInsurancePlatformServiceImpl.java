package com.clinic.service.impl;

import com.clinic.dto.InsuranceEligibilityResult;
import com.clinic.dto.InsuranceQueryResult;
import com.clinic.dto.InsuranceSubmitResult;
import com.clinic.entity.InsuranceSettlement;
import com.clinic.entity.InsuranceSettlementDetail;
import com.clinic.service.InsurancePlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 医保平台 Mock 实现（开发/测试用）
 * 在对接真实省内医保平台前，使用此实现进行功能开发和测试
 */
@Service
@RequiredArgsConstructor
public class MockInsurancePlatformServiceImpl implements InsurancePlatformService {

    @Override
    public InsuranceEligibilityResult verifyEligibility(Long patientId, String insuranceCardNo) {
        InsuranceEligibilityResult result = new InsuranceEligibilityResult();
        result.setEligible(true);
        result.setInsuranceCardNo(insuranceCardNo);
        result.setInsuranceType("URBAN_EMPLOYEE");
        result.setInsuranceTypeName("职工医保");
        result.setHolderName("参保人");
        result.setBalance(new BigDecimal("5000.00"));
        result.setMessage("资格验证通过");
        return result;
    }

    @Override
    public InsuranceSubmitResult submitClaim(InsuranceSettlement settlement, List<InsuranceSettlementDetail> details) {
        BigDecimal totalAmount = settlement.getTotalAmount() != null ? settlement.getTotalAmount() : BigDecimal.ZERO;

        InsuranceSubmitResult result = new InsuranceSubmitResult();
        result.setSuccess(true);
        result.setClaimNo("IC" + System.currentTimeMillis());
        result.setInsurancePay(totalAmount.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP));
        result.setSelfPay(totalAmount.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP));
        result.setAccountPay(BigDecimal.ZERO);
        result.setCashPay(result.getSelfPay());
        result.setMessage("结算提交成功(Mock)");
        return result;
    }

    @Override
    public InsuranceQueryResult querySettlement(String settlementNo) {
        InsuranceQueryResult result = new InsuranceQueryResult();
        result.setSettlementNo(settlementNo);
        result.setStatus("SUCCESS");
        result.setInsurancePay(new BigDecimal("175.00"));
        result.setSelfPay(new BigDecimal("75.00"));
        result.setClaimNo("IC" + System.currentTimeMillis());
        result.setCompleteTime(LocalDateTime.now());
        result.setMessage("查询成功(Mock)");
        return result;
    }

    @Override
    public boolean cancelSettlement(String settlementNo, String reason) {
        return true;
    }
}
