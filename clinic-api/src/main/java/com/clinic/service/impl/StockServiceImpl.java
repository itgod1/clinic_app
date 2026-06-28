package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.StockItem;
import com.clinic.mapper.StockItemMapper;
import com.clinic.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockItemMapper stockItemMapper;

    @Override
    public Map<String, Object> list(Long clinicId, String keyword, Integer itemType, Integer status, Boolean lowStockAlert, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<StockItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StockItem::getClinicId, clinicId);
        queryWrapper.eq(StockItem::getDeleted, 0);

        if (itemType != null) {
            queryWrapper.eq(StockItem::getItemType, itemType);
        }

        if (status != null) {
            queryWrapper.eq(StockItem::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(StockItem::getItemName, keyword)
                    .or().like(StockItem::getItemCode, keyword)
                    .or().like(StockItem::getBarcode, keyword));
        }

        if (lowStockAlert != null && lowStockAlert) {
            queryWrapper.apply("stock <= low_stock_alert");
        }

        queryWrapper.orderByDesc(StockItem::getCreatedAt);

        Page<StockItem> page = new Page<>(pageNum, pageSize);
        Page<StockItem> resultPage = stockItemMapper.selectPage(page, queryWrapper);

        // 设置显示名称和低库存标记
        resultPage.getRecords().forEach(this::enrichStockItem);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public StockItem getById(Long id) {
        StockItem item = stockItemMapper.selectById(id);
        if (item != null) {
            enrichStockItem(item);
        }
        return item;
    }

    @Override
    public void create(StockItem item) {
        // 默认值设置
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setDeleted(0);
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getStock() == null) {
            item.setStock(0);
        }
        if (item.getLowStockAlert() == null) {
            item.setLowStockAlert(10);
        }
        stockItemMapper.insert(item);
    }

    @Override
    public void update(StockItem item) {
        item.setUpdatedAt(LocalDateTime.now());
        stockItemMapper.updateById(item);
    }

    @Override
    public void updatePrice(Long itemId, BigDecimal retailPrice, BigDecimal memberPrice) {
        StockItem item = stockItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("药品不存在");
        }
        item.setRetailPrice(retailPrice);
        item.setMemberPrice(memberPrice);
        item.setUpdatedAt(LocalDateTime.now());
        stockItemMapper.updateById(item);
    }

    @Override
    public void delete(Long itemId) {
        StockItem item = new StockItem();
        item.setId(itemId);
        item.setDeleted(1);
        item.setUpdatedAt(LocalDateTime.now());
        stockItemMapper.updateById(item);
    }

    @Override
    public void stockIn(Long itemId, Integer quantity) {
        StockItem item = stockItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("药品不存在");
        }
        item.setStock(item.getStock() + quantity);
        item.setUpdatedAt(LocalDateTime.now());
        stockItemMapper.updateById(item);
    }

    @Override
    public void stockOut(Long itemId, Integer quantity) {
        StockItem item = stockItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("药品不存在");
        }
        if (item.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
        item.setStock(item.getStock() - quantity);
        item.setUpdatedAt(LocalDateTime.now());
        stockItemMapper.updateById(item);
    }

    @Override
    public boolean existsByCode(String itemCode, Long clinicId) {
        LambdaQueryWrapper<StockItem> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(StockItem::getItemCode, itemCode)
                .eq(StockItem::getClinicId, clinicId);
        return stockItemMapper.selectCount(codeWrapper) > 0;
    }

    @Override
    public void validateItemCodeUnique(String itemCode, Long clinicId) {
        if (existsByCode(itemCode, clinicId)) {
            throw new BusinessException("药品编码已存在");
        }
    }

    @Override
    public void validateStock(Long itemId, Integer quantity) {
        StockItem item = stockItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("药品不存在");
        }
        if (item.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
    }

    @Override
    public Map<String, Object> getStockInList(Long clinicId, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        // TODO: 实现入库记录查询，需要创建 stock_record 表
        Map<String, Object> data = new HashMap<>();
        data.put("list", java.util.Collections.emptyList());
        data.put("total", 0);
        return data;
    }

    @Override
    public Map<String, Object> getStockOutList(Long clinicId, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        // TODO: 实现出库记录查询，需要创建 stock_record 表
        Map<String, Object> data = new HashMap<>();
        data.put("list", java.util.Collections.emptyList());
        data.put("total", 0);
        return data;
    }

    /**
     * 为StockItem设置显示名称和计算字段
     */
    private void enrichStockItem(StockItem item) {
        item.setItemTypeName(DictConstants.getItemTypeName(item.getItemType()));
        item.setIsLowStock(item.getStock() <= item.getLowStockAlert());
    }
}
