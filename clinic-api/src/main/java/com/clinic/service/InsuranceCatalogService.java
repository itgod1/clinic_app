package com.clinic.service;

import com.clinic.entity.InsuranceCatalog;
import java.util.Map;

public interface InsuranceCatalogService {

    Map<String, Object> list(Long clinicId, String catalogType, String keyword, Integer status, Integer pageNum, Integer pageSize);

    InsuranceCatalog getById(Long id);

    void create(InsuranceCatalog catalog);

    void update(InsuranceCatalog catalog);

    void delete(Long id);
}
