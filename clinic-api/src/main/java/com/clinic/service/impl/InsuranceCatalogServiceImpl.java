package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.entity.InsuranceCatalog;
import com.clinic.mapper.InsuranceCatalogMapper;
import com.clinic.service.InsuranceCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InsuranceCatalogServiceImpl implements InsuranceCatalogService {

    private final InsuranceCatalogMapper insuranceCatalogMapper;

    @Override
    public Map<String, Object> list(Long clinicId, String catalogType, String keyword, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<InsuranceCatalog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InsuranceCatalog::getClinicId, clinicId);
        queryWrapper.eq(InsuranceCatalog::getDeleted, 0);

        if (StringUtils.hasText(catalogType)) {
            queryWrapper.eq(InsuranceCatalog::getCatalogType, catalogType);
        }
        if (status != null) {
            queryWrapper.eq(InsuranceCatalog::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(InsuranceCatalog::getItemName, keyword)
                    .or().like(InsuranceCatalog::getItemCode, keyword));
        }

        queryWrapper.orderByAsc(InsuranceCatalog::getCatalogType)
                .orderByAsc(InsuranceCatalog::getItemCode);

        Page<InsuranceCatalog> page = new Page<>(pageNum, pageSize);
        Page<InsuranceCatalog> resultPage = insuranceCatalogMapper.selectPage(page, queryWrapper);
        resultPage.getRecords().forEach(this::enrich);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public InsuranceCatalog getById(Long id) {
        InsuranceCatalog record = insuranceCatalogMapper.selectById(id);
        if (record != null) {
            enrich(record);
        }
        return record;
    }

    @Override
    public void create(InsuranceCatalog catalog) {
        catalog.setCreatedAt(LocalDateTime.now());
        catalog.setUpdatedAt(LocalDateTime.now());
        catalog.setDeleted(0);
        if (catalog.getStatus() == null) {
            catalog.setStatus(1);
        }
        insuranceCatalogMapper.insert(catalog);
    }

    @Override
    public void update(InsuranceCatalog catalog) {
        catalog.setUpdatedAt(LocalDateTime.now());
        insuranceCatalogMapper.updateById(catalog);
    }

    @Override
    public void delete(Long id) {
        insuranceCatalogMapper.deleteById(id);
    }

    private void enrich(InsuranceCatalog record) {
        record.setCatalogTypeName(DictConstants.getCatalogTypeName(record.getCatalogType()));
        record.setInsuranceCategoryName(DictConstants.getInsuranceCategoryName(record.getInsuranceCategory()));
    }
}
