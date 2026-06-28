package com.clinic.service;

import com.clinic.dto.InsuranceSettlementRequest;
import com.clinic.entity.InsuranceSettlement;
import java.util.Map;

public interface InsuranceSettlementService {

    Map<String, Object> list(Long clinicId, String settlementStatus, String keyword, String startDate, String endDate, Integer pageNum, Integer pageSize);

    InsuranceSettlement getById(Long id);

    Map<String, Object> getDetail(Long settlementId);

    Map<String, Object> createPreview(InsuranceSettlementRequest request);

    InsuranceSettlement submit(Long settlementId);

    void cancel(Long settlementId, String reason);

    Map<String, Object> queryFromPlatform(Long settlementId);
}
