package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_item")
public class StockItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private String itemCode;
    private String itemName;
    private Integer itemType;
    private Long categoryId;
    private String unit;
    private String spec;
    private String manufacturer;
    private String barcode;
    private BigDecimal purchasePrice;
    private BigDecimal retailPrice;
    private BigDecimal memberPrice;
    private Integer stock;
    private Integer lowStockAlert;
    private Integer status;

    @TableField(exist = false)
    private Boolean isLowStock;

    @TableField(exist = false)
    private String itemTypeName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}