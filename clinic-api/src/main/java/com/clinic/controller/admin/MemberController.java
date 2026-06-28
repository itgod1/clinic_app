package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.controller.dto.RechargeRequest;
import com.clinic.service.PatientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "会员管理")
@RestController
@RequestMapping("/admin/member")
@RequiredArgsConstructor
public class MemberController {

    private final PatientService patientService;

    @ApiOperation("会员充值")
    @OperationLog(module = "会员管理", type = OperationType.CREATE, desc = "会员充值")
    @PostMapping("/recharge")
    public Result<String> recharge(@RequestBody RechargeRequest request) {
        patientService.recharge(request.getPatientId(), request.getAmount(),
                request.getGiftAmount(), request.getPaymentMethod(), request.getRemark());
        return Result.success("充值成功");
    }
}
