package com.clinic.controller.admin;


import com.clinic.common.result.Result;
import com.clinic.entity.DentalImage;
import com.clinic.service.DentalImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "口腔影像管理")
@RestController
@RequestMapping("/admin/dental-image")
@RequiredArgsConstructor
public class DentalImageController {

    private final DentalImageService dentalImageService;

    @ApiOperation("获取影像列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam Long patientId,
            @RequestParam(required = false) String imageType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        var page = dentalImageService.list(clinicId, patientId, imageType, pageNum, pageSize);
        return Result.success(Map.of("list", page.getRecords(), "total", page.getTotal()));
    }

    @ApiOperation("获取影像详情")
    @GetMapping("/{id}")
    public Result<DentalImage> getById(@PathVariable Long id) {
        DentalImage image = dentalImageService.getById(id);
        if (image == null) {
            return Result.badRequest("影像不存在");
        }
        return Result.success(image);
    }

    @ApiOperation("创建影像记录")
    @PostMapping("/create")
    public Result<DentalImage> create(@RequestBody DentalImage image) {
        dentalImageService.create(image);
        return Result.success(image);
    }

    @ApiOperation("更新影像信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody DentalImage image) {
        dentalImageService.update(image);
        return Result.success("更新成功");
    }

    @ApiOperation("删除影像")
    @PostMapping("/delete")
    public Result<String> delete(@RequestBody Map<String, Long> params) {
        dentalImageService.delete(params.get("id"), params.get("clinicId"));
        return Result.success("删除成功");
    }

    @ApiOperation("关联牙位")
    @PostMapping("/link-teeth")
    public Result<String> linkTeeth(@RequestBody Map<String, Object> params) {
        Long imageId = Long.valueOf(params.get("imageId").toString());
        @SuppressWarnings("unchecked")
        List<String> toothNumbers = (List<String>) params.get("toothNumbers");
        dentalImageService.linkTeeth(imageId, toothNumbers);
        return Result.success("关联成功");
    }

    @ApiOperation("获取病历关联的影像")
    @GetMapping("/by-record/{medicalRecordId}")
    public Result<List<DentalImage>> getByMedicalRecordId(@PathVariable Long medicalRecordId) {
        List<DentalImage> images = dentalImageService.getByMedicalRecordId(medicalRecordId);
        return Result.success(images);
    }
}
