package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription_item")
public class PrescriptionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long prescriptionId;

    private Long clinicId;
    private Long itemId;
    private Integer itemType;
    private String itemName;
    private String spec;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    @TableField("`usage`")
    private String usage;
    private String frequency;
    private String duration;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String itemTypeName;
}
