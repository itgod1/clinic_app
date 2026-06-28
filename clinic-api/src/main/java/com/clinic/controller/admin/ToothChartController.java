package com.clinic.controller.admin;

import com.clinic.common.result.Result;
import com.clinic.entity.ToothChartRecord;
import com.clinic.service.ToothChartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "牙位图管理")
@RestController
@RequestMapping("/admin/tooth-chart")
@RequiredArgsConstructor
public class ToothChartController {

    private final ToothChartService toothChartService;

    @ApiOperation("获取病历的牙位图记录")
    @GetMapping("/{medicalRecordId}")
    public Result<List<ToothChartRecord>> getByMedicalRecordId(@PathVariable Long medicalRecordId) {
        List<ToothChartRecord> records = toothChartService.getByMedicalRecordId(medicalRecordId);
        return Result.success(records);
    }

    @ApiOperation("保存牙位图记录")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody Map<String, Object> params) {
        Long medicalRecordId = Long.valueOf(params.get("medicalRecordId").toString());
        Long clinicId = Long.valueOf(params.get("clinicId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawRecords = (List<Map<String, Object>>) params.get("records");
        List<ToothChartRecord> records = rawRecords.stream().map(r -> {
            ToothChartRecord record = new ToothChartRecord();
            record.setToothNumber((String) r.get("toothNumber"));
            record.setConditionType((String) r.get("conditionType"));
            record.setSurface((String) r.get("surface"));
            record.setNote((String) r.get("note"));
            return record;
        }).toList();
        toothChartService.saveToothChart(medicalRecordId, clinicId, records);
        return Result.success();
    }
}
