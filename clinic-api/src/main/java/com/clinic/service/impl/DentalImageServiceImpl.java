package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.DentalImage;
import com.clinic.entity.DentalImageTooth;
import com.clinic.mapper.DentalImageMapper;
import com.clinic.mapper.DentalImageToothMapper;
import com.clinic.service.DentalImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DentalImageServiceImpl implements DentalImageService {

    private final DentalImageMapper dentalImageMapper;
    private final DentalImageToothMapper dentalImageToothMapper;

    @Override
    public Page<DentalImage> list(Long clinicId, Long patientId, String imageType, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<DentalImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DentalImage::getClinicId, clinicId)
               .eq(DentalImage::getPatientId, patientId)
               .eq(DentalImage::getDeleted, 0);
        if (imageType != null && !imageType.isEmpty()) {
            wrapper.eq(DentalImage::getImageType, imageType);
        }
        wrapper.orderByDesc(DentalImage::getCreatedAt);

        Page<DentalImage> page = new Page<>(pageNum, pageSize);
        Page<DentalImage> result = dentalImageMapper.selectPage(page, wrapper);

        // 填充关联牙位
        for (DentalImage img : result.getRecords()) {
            img.setLinkedTeeth(getLinkedTeeth(img.getId()));
        }

        return result;
    }

    @Override
    public DentalImage getById(Long id) {
        DentalImage image = dentalImageMapper.selectById(id);
        if (image != null) {
            image.setLinkedTeeth(getLinkedTeeth(id));
        }
        return image;
    }

    @Override
    public void create(DentalImage image) {
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        image.setDeleted(0);
        dentalImageMapper.insert(image);
    }

    @Override
    public void update(DentalImage image) {
        image.setUpdatedAt(LocalDateTime.now());
        dentalImageMapper.updateById(image);
    }

    @Override
    public void delete(Long id, Long clinicId) {
        LambdaQueryWrapper<DentalImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DentalImage::getId, id)
               .eq(DentalImage::getClinicId, clinicId);
        dentalImageMapper.delete(wrapper);
    }

    @Override
    public List<DentalImage> getByMedicalRecordId(Long medicalRecordId) {
        LambdaQueryWrapper<DentalImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DentalImage::getMedicalRecordId, medicalRecordId)
               .eq(DentalImage::getDeleted, 0)
               .orderByAsc(DentalImage::getSortOrder);
        List<DentalImage> images = dentalImageMapper.selectList(wrapper);
        for (DentalImage img : images) {
            img.setLinkedTeeth(getLinkedTeeth(img.getId()));
        }
        return images;
    }

    @Override
    @Transactional
    public void linkTeeth(Long imageId, List<String> toothNumbers) {
        // 清除旧关联
        LambdaQueryWrapper<DentalImageTooth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DentalImageTooth::getImageId, imageId);
        dentalImageToothMapper.delete(wrapper);

        // 插入新关联
        if (toothNumbers != null && !toothNumbers.isEmpty()) {
            for (String tooth : toothNumbers) {
                DentalImageTooth link = new DentalImageTooth();
                link.setImageId(imageId);
                link.setToothNumber(tooth);
                dentalImageToothMapper.insert(link);
            }
        }
    }

    @Override
    public List<String> getLinkedTeeth(Long imageId) {
        LambdaQueryWrapper<DentalImageTooth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DentalImageTooth::getImageId, imageId);
        return dentalImageToothMapper.selectList(wrapper).stream()
                .map(DentalImageTooth::getToothNumber)
                .toList();
    }
}
