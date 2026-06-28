package com.clinic.controller.miniapp;

import com.clinic.common.result.Result;
import com.clinic.common.util.SecurityUtils;
import com.clinic.service.RegistrationService;
import com.clinic.vo.QueueInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小程序 - 挂号相关接口
 */
@Api(tags = "小程序-挂号管理")
@RestController("miniappRegistrationController")
@RequestMapping("/miniapp")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * 获取当前用户的挂号列表
     */
    @ApiOperation("获取当前用户的挂号列表")
    @GetMapping("/registration/my-list")
    public Result<Map<String, Object>> myList(
            @ApiParam("诊所ID") @RequestParam(required = false) Long clinicId,
            @ApiParam("状态") @RequestParam(required = false) Integer status,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.badRequest("用户未登录");
        }
        
        Map<String, Object> data = registrationService.listByUserId(userId, clinicId, status, pageNum, pageSize);
        return Result.success(data);
    }

    /**
     * 获取挂号排队信息
     */
    @ApiOperation("获取挂号排队信息")
    @GetMapping("/registration/{id}/queue")
    public Result<QueueInfoVO> getQueueInfo(
            @ApiParam("挂号ID") @PathVariable Long id) {
        QueueInfoVO queueInfo = registrationService.getQueueInfo(id);
        return Result.success(queueInfo);
    }
}
