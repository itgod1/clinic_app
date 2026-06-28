package com.clinic.controller.miniapp;

import com.clinic.common.result.Result;
import com.clinic.entity.Patient;
import com.clinic.service.miniapp.MiniappPatientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序 - 就诊人管理
 */
@Api(tags = "小程序-就诊人管理")
@RestController("miniappPatientController")
@RequestMapping("/miniapp/patient")
@RequiredArgsConstructor
public class PatientController {

    private final MiniappPatientService miniappPatientService;

    @ApiOperation("获取当前用户的就诊人列表")
    @GetMapping("/list")
    public Result<List<Patient>> list(
            @ApiParam("用户ID") @RequestParam Long userId,
            @ApiParam("诊所ID") @RequestParam Long clinicId) {
        List<Patient> list = miniappPatientService.getMyPatients(userId, clinicId);
        return Result.success(list);
    }

    @ApiOperation("获取就诊人详情")
    @GetMapping("/{id}")
    public Result<Patient> getById(
            @ApiParam("就诊人ID") @PathVariable Long id,
            @ApiParam("用户ID") @RequestParam Long userId) {
        Patient patient = miniappPatientService.getMyPatientById(id, userId);
        return Result.success(patient);
    }

    @ApiOperation("添加就诊人")
    @PostMapping("/create")
    public Result<String> create(
            @ApiParam("用户ID") @RequestParam(required = false) Long userId,
            @RequestBody Patient patient) {
        // 从RequestParam或patient中获取userId
        Long finalUserId = userId != null ? userId : patient.getUserId();
        if (finalUserId == null) {
            return Result.badRequest("缺少必要参数: userId");
        }
        miniappPatientService.createMyPatient(finalUserId, patient);
        return Result.success("添加成功");
    }

    @ApiOperation("更新就诊人")
    @PostMapping("/update")
    public Result<String> update(
            @ApiParam("用户ID") @RequestParam(required = false) Long userId,
            @RequestBody Patient patient) {
        // 从RequestParam或patient中获取userId
        Long finalUserId = userId != null ? userId : patient.getUserId();
        if (finalUserId == null) {
            return Result.badRequest("缺少必要参数: userId");
        }
        miniappPatientService.updateMyPatient(finalUserId, patient);
        return Result.success("更新成功");
    }

    @ApiOperation("删除就诊人")
    @PostMapping("/delete")
    public Result<String> delete(
            @ApiParam("就诊人ID") @RequestParam Long patientId,
            @ApiParam("用户ID") @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bodyUserId) {
        // 支持从URL参数或body中获取userId
        Long finalUserId = userId != null ? userId : bodyUserId;
        if (finalUserId == null) {
            return Result.badRequest("缺少必要参数: userId");
        }
        miniappPatientService.deleteMyPatient(patientId, finalUserId);
        return Result.success("删除成功");
    }
}
