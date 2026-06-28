package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.common.util.SecurityUtils;
import com.clinic.dto.InsuranceEligibilityResult;
import com.clinic.dto.InsuranceSettlementRequest;
import com.clinic.dto.InsuranceSubmitResult;
import com.clinic.entity.InsuranceCatalog;
import com.clinic.entity.InsurancePatient;
import com.clinic.entity.InsuranceSettlement;
import com.clinic.entity.InsuranceSettlementDetail;
import com.clinic.entity.Patient;
import com.clinic.mapper.InsuranceCatalogMapper;
import com.clinic.mapper.InsurancePatientMapper;
import com.clinic.mapper.InsuranceSettlementDetailMapper;
import com.clinic.mapper.InsuranceSettlementMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.service.InsurancePlatformService;
import com.clinic.service.InsuranceSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InsuranceSettlementServiceImpl implements InsuranceSettlementService {

    private final InsuranceSettlementMapper settlementMapper;
    private final InsuranceSettlementDetailMapper detailMapper;
    private final InsurancePatientMapper insurancePatientMapper;
    private final InsuranceCatalogMapper catalogMapper;
    private final PatientMapper patientMapper;
    private final InsurancePlatformService insurancePlatformService;

    @Override
    public Map<String, Object> list(Long clinicId, String settlementStatus, String keyword, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<InsuranceSettlement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InsuranceSettlement::getClinicId, clinicId);
        queryWrapper.eq(InsuranceSettlement::getDeleted, 0);

        if (StringUtils.hasText(settlementStatus)) {
            queryWrapper.eq(InsuranceSettlement::getSettlementStatus, settlementStatus);
        }
        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(InsuranceSettlement::getCreatedAt, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(InsuranceSettlement::getCreatedAt, endDate);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(InsuranceSettlement::getSettlementNo, keyword)
                    .or().like(InsuranceSettlement::getPatientName, keyword));
        }

        queryWrapper.orderByDesc(InsuranceSettlement::getCreatedAt);

        Page<InsuranceSettlement> page = new Page<>(pageNum, pageSize);
        Page<InsuranceSettlement> resultPage = settlementMapper.selectPage(page, queryWrapper);
        resultPage.getRecords().forEach(this::enrich);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public InsuranceSettlement getById(Long id) {
        InsuranceSettlement record = settlementMapper.selectById(id);
        if (record != null) {
            enrich(record);
        }
        return record;
    }

    @Override
    public Map<String, Object> getDetail(Long settlementId) {
        InsuranceSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算记录不存在");
        }
        enrich(settlement);

        LambdaQueryWrapper<InsuranceSettlementDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InsuranceSettlementDetail::getSettlementId, settlementId);
        detailWrapper.eq(InsuranceSettlementDetail::getDeleted, 0);
        List<InsuranceSettlementDetail> details = detailMapper.selectList(detailWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("settlement", settlement);
        data.put("details", details);
        return data;
    }

    @Override
    public Map<String, Object> createPreview(InsuranceSettlementRequest request) {
        // 1. 查询患者信息
        InsurancePatient insurancePatient = getInsurancePatient(request);
        Patient patient = patientMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }

        // 2. 验证医保资格
        InsuranceEligibilityResult eligibility = insurancePlatformService.verifyEligibility(
                request.getPatientId(), insurancePatient.getInsuranceCardNo());
        if (!eligibility.isEligible()) {
            throw new BusinessException("医保资格验证失败: " + eligibility.getMessage());
        }

        // 3. 匹配医保目录计算费用
        List<Map<String, Object>> previewItems = new ArrayList<>();
        BigDecimal totalInsurancePay = BigDecimal.ZERO;
        BigDecimal totalSelfPay = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (request.getItems() != null) {
            for (InsuranceSettlementRequest.SettlementItem item : request.getItems()) {
                BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice()
                        : (item.getUnitPrice() != null && item.getQuantity() != null
                                ? item.getUnitPrice().multiply(item.getQuantity())
                                : BigDecimal.ZERO);
                totalAmount = totalAmount.add(itemTotal);

                // 查找医保目录匹配
                BigDecimal selfPayRatio = new BigDecimal("0.30"); // 默认自付30%
                InsuranceCatalog catalog = findCatalogItem(request.getPatientId(), item.getItemCode(), item.getItemName());
                if (catalog != null) {
                    selfPayRatio = catalog.getSelfPayRatio() != null ? catalog.getSelfPayRatio() : selfPayRatio;
                }

                BigDecimal itemSelfPay = itemTotal.multiply(selfPayRatio).setScale(2, RoundingMode.HALF_UP);
                BigDecimal itemInsurancePay = itemTotal.subtract(itemSelfPay);

                totalInsurancePay = totalInsurancePay.add(itemInsurancePay);
                totalSelfPay = totalSelfPay.add(itemSelfPay);

                Map<String, Object> previewItem = new HashMap<>();
                previewItem.put("itemCode", item.getItemCode());
                previewItem.put("itemName", item.getItemName());
                previewItem.put("quantity", item.getQuantity());
                previewItem.put("unitPrice", item.getUnitPrice());
                previewItem.put("totalPrice", itemTotal);
                previewItem.put("selfPayRatio", selfPayRatio);
                previewItem.put("insurancePay", itemInsurancePay);
                previewItem.put("selfPay", itemSelfPay);
                previewItem.put("catalogMatched", catalog != null);
                previewItems.add(previewItem);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("eligible", true);
        data.put("insuranceTypeName", eligibility.getInsuranceTypeName());
        data.put("insuranceCardNo", insurancePatient.getInsuranceCardNo());
        data.put("patientName", patient.getPatientName());
        data.put("items", previewItems);
        data.put("totalAmount", totalAmount);
        data.put("totalInsurancePay", totalInsurancePay);
        data.put("totalSelfPay", totalSelfPay);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InsuranceSettlement submit(Long settlementId) {
        InsuranceSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算记录不存在");
        }
        if (!"PENDING".equals(settlement.getSettlementStatus())) {
            throw new BusinessException("该结算状态不允许提交");
        }

        // 获取结算明细
        LambdaQueryWrapper<InsuranceSettlementDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InsuranceSettlementDetail::getSettlementId, settlementId);
        detailWrapper.eq(InsuranceSettlementDetail::getDeleted, 0);
        List<InsuranceSettlementDetail> details = null;


        // 提交到医保平台
        InsuranceSubmitResult result = insurancePlatformService.submitClaim(settlement, details);

        settlement.setSubmitTime(LocalDateTime.now());
        if (result.isSuccess()) {
            settlement.setSettlementStatus("SUCCESS");
            settlement.setInsuranceClaimNo(result.getClaimNo());
            settlement.setInsurancePay(result.getInsurancePay());
            settlement.setSelfPay(result.getSelfPay());
            settlement.setAccountPay(result.getAccountPay());
            settlement.setCashPay(result.getCashPay());
            settlement.setCompleteTime(LocalDateTime.now());
        } else {
            settlement.setSettlementStatus("FAILED");
            settlement.setErrorMessage(result.getMessage());
        }
        settlement.setUpdatedAt(LocalDateTime.now());
        settlementMapper.updateById(settlement);

        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long settlementId, String reason) {
        InsuranceSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算记录不存在");
        }
        if ("CANCELLED".equals(settlement.getSettlementStatus())) {
            throw new BusinessException("该结算已取消");
        }

        if ("SUCCESS".equals(settlement.getSettlementStatus())) {
            insurancePlatformService.cancelSettlement(settlement.getSettlementNo(), reason);
        }

        settlement.setSettlementStatus("CANCELLED");
        settlement.setRemark(reason);
        settlement.setUpdatedAt(LocalDateTime.now());
        settlementMapper.updateById(settlement);
    }

    @Override
    public Map<String, Object> queryFromPlatform(Long settlementId) {
        InsuranceSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算记录不存在");
        }
        return insurancePlatformService.querySettlement(settlement.getSettlementNo()) != null
                ? Map.of("result", insurancePlatformService.querySettlement(settlement.getSettlementNo()))
                : Map.of("message", "查询无结果");
    }

    // ============= 辅助方法 =============

    private InsurancePatient getInsurancePatient(InsuranceSettlementRequest request) {
        if (request.getInsurancePatientId() != null) {
            InsurancePatient record = insurancePatientMapper.selectById(request.getInsurancePatientId());
            if (record != null) return record;
        }
        // 根据 patientId 查找
        LambdaQueryWrapper<InsurancePatient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InsurancePatient::getPatientId, request.getPatientId());
        queryWrapper.eq(InsurancePatient::getDeleted, 0);
        queryWrapper.last("LIMIT 1");
        InsurancePatient record = insurancePatientMapper.selectOne(queryWrapper);
        if (record == null) {
            throw new BusinessException("该患者未绑定医保信息");
        }
        return record;
    }

    private InsuranceCatalog findCatalogItem(Long patientId, String itemCode, String itemName) {
        // 先按编码精确匹配
        if (StringUtils.hasText(itemCode)) {
            LambdaQueryWrapper<InsuranceCatalog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsuranceCatalog::getItemCode, itemCode);
            queryWrapper.eq(InsuranceCatalog::getStatus, 1);
            queryWrapper.eq(InsuranceCatalog::getDeleted, 0);
            queryWrapper.last("LIMIT 1");
            InsuranceCatalog catalog = catalogMapper.selectOne(queryWrapper);
            if (catalog != null) return catalog;
        }
        // 按名称模糊匹配
        if (StringUtils.hasText(itemName)) {
            LambdaQueryWrapper<InsuranceCatalog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(InsuranceCatalog::getItemName, itemName);
            queryWrapper.eq(InsuranceCatalog::getStatus, 1);
            queryWrapper.eq(InsuranceCatalog::getDeleted, 0);
            queryWrapper.last("LIMIT 1");
            return catalogMapper.selectOne(queryWrapper);
        }
        return null;
    }

    private void enrich(InsuranceSettlement record) {
        record.setSettlementStatusName(DictConstants.getSettlementStatusName(record.getSettlementStatus()));
    }
}
