package com.clinic.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.result.Result;
import com.clinic.entity.OperationLog;
import com.clinic.service.OperationLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 */
@Api(tags = "系统管理-操作日志")
@RestController
@RequestMapping("/admin/operation-log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @ApiOperation("分页查询操作日志")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer operationType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        var result = operationLogService.pageQuery(page, clinicId, module, operationType, username, startTime, endTime, status);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @ApiOperation("获取日志详情")
    @GetMapping("/{id}")
    public Result<OperationLog> getById(@PathVariable Long id) {
        // 这里可以从数据库查询详细内容
        return Result.success(null);
    }

    @ApiOperation("批量删除日志")
    @PostMapping("/delete")
    public Result<String> delete(@RequestBody List<Long> ids) {
        operationLogService.deleteByIds(ids);
        return Result.success("删除成功");
    }

    @ApiOperation("清理日志")
    @PostMapping("/clean")
    public Result<String> clean(@RequestParam int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        operationLogService.cleanLogsBefore(before);
        return Result.success("清理成功");
    }
}
