package com.clinic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.StockItem;

import java.math.BigDecimal;
import java.util.Map;

public interface StockService {

    /**
     * 获取药品列表（带枚举名称转换）
     */
    Map<String, Object> list(Long clinicId, String keyword, Integer itemType, Integer status, Boolean lowStockAlert, Integer pageNum, Integer pageSize);

    /**
     * 获取药品详情（带枚举名称转换）
     */
    StockItem getById(Long id);

    /**
     * 创建药品
     */
    void create(StockItem item);

    /**
     * 更新药品
     */
    void update(StockItem item);

    /**
     * 更新药品价格
     */
    void updatePrice(Long itemId, BigDecimal retailPrice, BigDecimal memberPrice);

    /**
     * 删除药品
     */
    void delete(Long itemId);

    /**
     * 药品入库
     */
    void stockIn(Long itemId, Integer quantity);

    /**
     * 药品出库
     */
    void stockOut(Long itemId, Integer quantity);

    /**
     * 检查药品编码是否已存在
     */
    boolean existsByCode(String itemCode, Long clinicId);

    /**
     * 校验药品编码唯一性
     */
    void validateItemCodeUnique(String itemCode, Long clinicId);

    /**
     * 校验库存是否充足
     */
    void validateStock(Long itemId, Integer quantity);

    /**
     * 获取入库记录列表
     */
    Map<String, Object> getStockInList(Long clinicId, String startDate, String endDate, Integer pageNum, Integer pageSize);

    /**
     * 获取出库记录列表
     */
    Map<String, Object> getStockOutList(Long clinicId, String startDate, String endDate, Integer pageNum, Integer pageSize);
}
