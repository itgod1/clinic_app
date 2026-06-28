package com.clinic.controller.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.result.Result;
import com.clinic.dto.PayPrescriptionDTO;
import com.clinic.entity.Department;
import com.clinic.entity.Patient;
import com.clinic.entity.Prescription;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.PrescriptionMapper;
import com.clinic.service.PrescriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小程序 - 缴费相关接口
 */
@Api(tags = "小程序-门诊缴费")
@RestController("miniappPaymentController")
@RequestMapping("/miniapp")
@RequiredArgsConstructor
public class PaymentController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionMapper prescriptionMapper;
    private final PatientMapper patientMapper;
    private final DepartmentMapper departmentMapper;

    /**
     * 获取待缴费列表
     */
    @ApiOperation("获取待缴费列表")
    @GetMapping("/payments/unpaid")
    public Result<Map<String, Object>> getUnpaidList(
            @ApiParam("诊所ID") @RequestParam Long clinicId,
            @ApiParam("用户ID") @RequestParam Long userId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        // 1. 根据 userId 查询关联的所有患者
        LambdaQueryWrapper<Patient> patientWrapper = new LambdaQueryWrapper<>();
        patientWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0);
        List<Patient> patients = patientMapper.selectList(patientWrapper);

        if (patients.isEmpty()) {
            // 没有关联的患者，返回空列表
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("list", new java.util.ArrayList<>());
            emptyData.put("total", 0);
            emptyData.put("pages", 0);
            emptyData.put("current", pageNum);
            emptyData.put("size", pageSize);
            return Result.success(emptyData);
        }

        // 2. 获取所有患者ID
        List<Long> patientIds = patients.stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

        // 3. 查询这些患者的待缴费处方
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getClinicId, clinicId)
                .in(Prescription::getPatientId, patientIds)
                .eq(Prescription::getPaymentStatus, 0)
                .eq(Prescription::getDeleted, 0)
                .orderByDesc(Prescription::getCreatedAt);

        Page<Prescription> page = new Page<>(pageNum, pageSize);
        Page<Prescription> resultPage = prescriptionMapper.selectPage(page, queryWrapper);

        // 补充状态名称和科室名称
        for (Prescription prescription : resultPage.getRecords()) {
            prescription.setStatusName("待缴费");
            // 如果科室名称为空，查询补充
            if (prescription.getDepartmentName() == null || prescription.getDepartmentName().isEmpty()) {
                if (prescription.getDeptId() != null) {
                    Department department = departmentMapper.selectById(prescription.getDeptId());
                    if (department != null) {
                        prescription.setDepartmentName(department.getDeptName());
                    }
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        data.put("pages", resultPage.getPages());
        data.put("current", resultPage.getCurrent());
        data.put("size", resultPage.getSize());

        return Result.success(data);
    }

    /**
     * 获取缴费记录列表
     */
    @ApiOperation("获取缴费记录列表")
    @GetMapping("/payments/history")
    public Result<Map<String, Object>> getPaymentHistory(
            @ApiParam("诊所ID") @RequestParam Long clinicId,
            @ApiParam("用户ID") @RequestParam Long userId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        // 1. 根据 userId 查询关联的所有患者
        LambdaQueryWrapper<Patient> patientWrapper = new LambdaQueryWrapper<>();
        patientWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0);
        List<Patient> patients = patientMapper.selectList(patientWrapper);

        if (patients.isEmpty()) {
            // 没有关联的患者，返回空列表
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("list", new java.util.ArrayList<>());
            emptyData.put("total", 0);
            emptyData.put("pages", 0);
            emptyData.put("current", pageNum);
            emptyData.put("size", pageSize);
            return Result.success(emptyData);
        }

        // 2. 获取所有患者ID
        List<Long> patientIds = patients.stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

        // 3. 查询这些患者的已缴费处方
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getClinicId, clinicId)
                .in(Prescription::getPatientId, patientIds)
                .ne(Prescription::getPaymentStatus, 0)
                .eq(Prescription::getDeleted, 0)
                .orderByDesc(Prescription::getCreatedAt);

        Page<Prescription> page = new Page<>(pageNum, pageSize);
        Page<Prescription> resultPage = prescriptionMapper.selectPage(page, queryWrapper);

        // 补充状态名称和科室名称
        for (Prescription prescription : resultPage.getRecords()) {
            String statusName;
            switch (prescription.getPaymentStatus()) {
                case 1:
                    statusName = "已支付";
                    break;
                case 2:
                    statusName = "已退款";
                    break;
                default:
                    statusName = "未知";
            }
            prescription.setStatusName(statusName);
            
            // 如果科室名称为空，查询补充
            if (prescription.getDepartmentName() == null || prescription.getDepartmentName().isEmpty()) {
                if (prescription.getDeptId() != null) {
                    Department department = departmentMapper.selectById(prescription.getDeptId());
                    if (department != null) {
                        prescription.setDepartmentName(department.getDeptName());
                    }
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        data.put("pages", resultPage.getPages());
        data.put("current", resultPage.getCurrent());
        data.put("size", resultPage.getSize());

        return Result.success(data);
    }

    /**
     * 获取处方详情
     */
    @ApiOperation("获取处方详情")
    @GetMapping("/payments/{id}")
    public Result<Prescription> getPaymentDetail(@PathVariable Long id) {
        Prescription prescription = prescriptionService.getById(id);
        if (prescription == null || prescription.getDeleted() == 1) {
            return Result.error("处方不存在");
        }
        return Result.success(prescription);
    }

    /**
     * 支付处方
     */
    @ApiOperation("支付处方")
    @PostMapping("/payments/pay")
    public Result<?> payPrescription(@RequestBody PayPrescriptionDTO dto) {

        // 校验参数
        if (dto.getPrescriptionId() == null) {
            return Result.error("缺少必要参数: prescriptionId");
        }
        if (dto.getClinicId() == null) {
            return Result.error("缺少必要参数: clinicId");
        }
        if (dto.getPaymentMethod() == null) {
            return Result.error("缺少必要参数: paymentMethod");
        }

        Long prescriptionId = dto.getPrescriptionId();
        Long clinicId = dto.getClinicId();
        Integer paymentMethod = dto.getPaymentMethod();

        // 校验处方是否属于该诊所
        Prescription prescription = prescriptionService.getById(prescriptionId);
        if (prescription == null || prescription.getDeleted() == 1) {
            return Result.error("处方不存在");
        }
        if (!prescription.getClinicId().equals(clinicId)) {
            return Result.error("处方不属于该诊所");
        }

        // 执行支付
        try {
            java.math.BigDecimal payAmount = prescription.getActualAmount() != null
                    ? prescription.getActualAmount()
                    : prescription.getTotalAmount();
            prescriptionService.pay(prescriptionId, paymentMethod, payAmount);
            return Result.success("支付成功");
        } catch (com.clinic.common.exception.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("支付失败：" + e.getMessage());
        }
    }
}
