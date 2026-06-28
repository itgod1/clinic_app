package com.clinic.controller.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.common.result.Result;
import com.clinic.entity.MedicalRecord;
import com.clinic.entity.Patient;
import com.clinic.mapper.PatientMapper;
import com.clinic.security.UserContext;
import com.clinic.service.MedicalRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小程序端-就诊记录管理
 */
@Api(tags = "小程序-就诊记录")
@RestController("miniappMedicalRecordController")
@RequestMapping("/miniapp/medical")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final PatientMapper patientMapper;

    /**
     * 获取当前用户的就诊记录列表
     */
    @ApiOperation("获取当前用户的就诊记录列表")
    @GetMapping("/record/list")
    public Result<Map<String, Object>> list(
            @ApiParam("诊所ID") @RequestParam Long clinicId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 获取当前用户的所有就诊人ID列表
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getClinicId, clinicId)
                .eq(Patient::getDeleted, 0);
        List<Patient> patients = patientMapper.selectList(queryWrapper);

        if (patients.isEmpty()) {
            return Result.error("未绑定就诊人");
        }

        // 获取所有就诊人ID
        List<Long> patientIds = patients.stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

        // 查询这些就诊人的所有就诊记录
        Map<String, Object> data = medicalRecordService.listByPatientIds(clinicId, patientIds, pageNum, pageSize);
        return Result.success(data);
    }

    /**
     * 获取就诊记录详情
     */
    @ApiOperation("获取就诊记录详情")
    @GetMapping("/record/{id}")
    public Result<MedicalRecord> getById(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        MedicalRecord record = medicalRecordService.getById(id);
        if (record == null) {
            return Result.error("就诊记录不存在");
        }

        // 校验是否属于当前用户的就诊人
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patient::getUserId, userId)
                .eq(Patient::getId, record.getPatientId())
                .eq(Patient::getDeleted, 0);
        Patient patient = patientMapper.selectOne(queryWrapper);
        if (patient == null) {
            return Result.error("无权查看此就诊记录");
        }

        return Result.success(record);
    }
}
