package com.clinic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.DentalImage;
import java.util.List;

public interface DentalImageService {

    Page<DentalImage> list(Long clinicId, Long patientId, String imageType, Integer pageNum, Integer pageSize);

    DentalImage getById(Long id);

    void create(DentalImage image);

    void update(DentalImage image);

    void delete(Long id, Long clinicId);

    List<DentalImage> getByMedicalRecordId(Long medicalRecordId);

    void linkTeeth(Long imageId, List<String> toothNumbers);

    List<String> getLinkedTeeth(Long imageId);
}
