package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.dto.PrintMedicalRecordDTO;
import com.clinic.dto.PrintPrescriptionDTO;
import com.clinic.dto.PrintReceiptDTO;
import com.clinic.service.PrintService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 打印控制器
 */
@Api(tags = "打印管理")
@RestController
@RequestMapping("/admin/print")
@RequiredArgsConstructor
public class PrintController {

    private final PrintService printService;

    @ApiOperation("获取处方打印数据")
    @OperationLog(module = "打印管理", type = OperationType.PRINT, desc = "打印处方")
    @GetMapping("/prescription/{prescriptionId}")
    public Result<PrintPrescriptionDTO> getPrescriptionPrintData(
            @PathVariable Long prescriptionId,
            @RequestParam Long clinicId) {
        PrintPrescriptionDTO data = printService.getPrescriptionPrintData(prescriptionId, clinicId);
        if (data == null) {
            return Result.badRequest("处方不存在");
        }
        return Result.success(data);
    }

    @ApiOperation("获取收费票据打印数据")
    @OperationLog(module = "打印管理", type = OperationType.PRINT, desc = "打印收费票据")
    @GetMapping("/receipt/{paymentId}")
    public Result<PrintReceiptDTO> getReceiptPrintData(
            @PathVariable Long paymentId,
            @RequestParam Long clinicId) {
        PrintReceiptDTO data = printService.getReceiptPrintData(paymentId, clinicId);
        if (data == null) {
            return Result.badRequest("支付记录不存在");
        }
        return Result.success(data);
    }

    @ApiOperation("获取病历打印数据")
    @OperationLog(module = "打印管理", type = OperationType.PRINT, desc = "打印病历")
    @GetMapping("/medical-record/{recordId}")
    public Result<PrintMedicalRecordDTO> getMedicalRecordPrintData(
            @PathVariable Long recordId,
            @RequestParam Long clinicId) {
        PrintMedicalRecordDTO data = printService.getMedicalRecordPrintData(recordId, clinicId);
        if (data == null) {
            return Result.badRequest("病历不存在");
        }
        return Result.success(data);
    }
}
