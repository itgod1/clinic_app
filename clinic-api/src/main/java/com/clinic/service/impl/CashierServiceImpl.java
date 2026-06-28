package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.common.util.SecurityUtils;
import com.clinic.entity.PaymentRecord;
import com.clinic.entity.Prescription;
import com.clinic.mapper.PrescriptionMapper;
import com.clinic.service.CashierService;
import com.clinic.service.PaymentRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CashierServiceImpl implements CashierService {

    private final PrescriptionMapper prescriptionMapper;
    private final PaymentRecordService paymentRecordService;

    @Override
    public Map<String, Object> unpaidList(Long clinicId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getClinicId, clinicId);
        queryWrapper.eq(Prescription::getPaymentStatus, 0);
        queryWrapper.eq(Prescription::getDeleted, 0);
        queryWrapper.orderByDesc(Prescription::getCreatedAt);

        Page<Prescription> page = new Page<>(pageNum, pageSize);
        Page<Prescription> resultPage = prescriptionMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(p -> {
            p.setStatusName(DictConstants.getPaymentStatusName(p.getPaymentStatus()));
        });

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long pay(String orderId, Integer paymentMethod) {
        // 根据处方单号查询
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getPrescriptionNo, orderId);
        Prescription prescription = prescriptionMapper.selectOne(queryWrapper);
        if (prescription == null) {
            throw new BusinessException("订单不存在");
        }
        if (prescription.getPaymentStatus() != 0) {
            throw new BusinessException("当前状态不允许收费");
        }

        // 获取当前操作人
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();

        // 创建支付记录
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setClinicId(prescription.getClinicId());
        paymentRecord.setPrescriptionId(prescription.getId());
        paymentRecord.setPatientId(prescription.getPatientId());
        paymentRecord.setPatientName(prescription.getPatientName());
        paymentRecord.setAmount(prescription.getTotalAmount());
        paymentRecord.setDiscountAmount(prescription.getDiscountAmount());
        paymentRecord.setActualAmount(prescription.getActualAmount());
        paymentRecord.setPaymentMethod(paymentMethod);
        paymentRecord.setPaymentStatus(0);
        paymentRecordService.createPaymentRecord(paymentRecord);

        // 执行支付
        paymentRecordService.pay(paymentRecord.getOrderNo(), paymentMethod, operatorId, operatorName);

        return paymentRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long orderId) {
        Prescription prescription = prescriptionMapper.selectById(orderId);
        if (prescription == null) {
            throw new BusinessException("订单不存在");
        }
        if (prescription.getPaymentStatus() != 1) {
            throw new BusinessException("当前状态不允许退费");
        }

        // 获取当前操作人
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();

        // 查询支付记录
        PaymentRecord paymentRecord = paymentRecordService.getByPrescriptionId(orderId).stream()
                .filter(p -> p.getPaymentStatus() == 1)
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到已支付记录"));

        // 执行全额退款
        paymentRecordService.refund(paymentRecord.getOrderNo(), prescription.getActualAmount(), "全额退款", operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void discount(Long orderId, BigDecimal discountAmount) {
        Prescription prescription = prescriptionMapper.selectById(orderId);
        if (prescription == null) {
            throw new BusinessException("订单不存在");
        }

        BigDecimal newDiscount = prescription.getDiscountAmount().add(discountAmount);
        BigDecimal newActual = prescription.getTotalAmount().subtract(newDiscount);
        if (newActual.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠金额不能超过原价");
        }

        prescription.setDiscountAmount(newDiscount);
        prescription.setActualAmount(newActual);
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);
    }

    @Override
    public void validateCanPay(Long orderId) {
        Prescription prescription = prescriptionMapper.selectById(orderId);
        if (prescription == null) {
            throw new BusinessException("订单不存在");
        }
        if (prescription.getPaymentStatus() != 0) {
            throw new BusinessException("当前状态不允许收费");
        }
    }

    @Override
    public void validateCanRefund(Long orderId) {
        Prescription prescription = prescriptionMapper.selectById(orderId);
        if (prescription == null) {
            throw new BusinessException("订单不存在");
        }
        if (prescription.getPaymentStatus() != 1) {
            throw new BusinessException("当前状态不允许退费");
        }
    }

    @Override
    public void validateDiscount(Long orderId, BigDecimal discountAmount) {
        Prescription prescription = prescriptionMapper.selectById(orderId);
        if (prescription == null) {
            throw new BusinessException("订单不存在");
        }
        BigDecimal newDiscount = prescription.getDiscountAmount().add(discountAmount);
        BigDecimal newActual = prescription.getTotalAmount().subtract(newDiscount);
        if (newActual.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠金额不能超过原价");
        }
    }
}
