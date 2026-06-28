package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("dental_image")
public class DentalImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long patientId;
    private Long medicalRecordId;
    private String imageType;
    private String imageUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private LocalDate shotDate;
    private String bodyPart;
    private String description;
    private Integer sortOrder;

    @TableField(exist = false)
    private List<String> linkedTeeth;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
