-- 牙位图记录表
-- 用于存储病历中每颗牙的状况信息，支持FDI双位数牙位标记

CREATE TABLE IF NOT EXISTS tooth_chart_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    medical_record_id BIGINT NOT NULL COMMENT '关联病历ID',
    tooth_number VARCHAR(4) NOT NULL COMMENT '牙位编号(FDI: 成人11-48, 乳牙51-85)',
    condition_type VARCHAR(30) NOT NULL COMMENT '状况类型: CARIES/MISSING/FILLED/CROWN/BRIDGE/IMPLANT/ROOT_CANAL/ABUTMENT/PONTIC/FRACTURE/ABRASION/PERIODONTAL/SEALANT/OTHER',
    surface VARCHAR(50) COMMENT '牙面: O(咬合面)/B(颊面)/L(舌面)/M(近中面)/D(远中面)/P(腭面) 多个逗号分隔',
    note VARCHAR(200) COMMENT '备注描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_medical_record (medical_record_id),
    INDEX idx_clinic_medical (clinic_id, medical_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='牙位图记录表';

-- 口腔影像表
-- 存储患者的口腔影像资料(全景片、口内照、CBCT等)

CREATE TABLE IF NOT EXISTS dental_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    patient_id BIGINT NOT NULL COMMENT '关联患者ID',
    medical_record_id BIGINT COMMENT '关联病历ID(可为空，先拍片后关联)',
    image_type VARCHAR(30) NOT NULL COMMENT '影像类型: PANORAMIC/INTRAORAL/CBCT/CEPHALOMETRIC/PERIAPICAL/BITEWING/OCCLUSAL/PHOTO/OTHER',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    file_name VARCHAR(200) COMMENT '原始文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    mime_type VARCHAR(50) COMMENT 'MIME类型',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    shot_date DATE COMMENT '拍摄日期',
    body_part VARCHAR(30) COMMENT '部位: UPPER/LOWER/FULL/LEFT/RIGHT',
    description VARCHAR(500) COMMENT '影像描述/备注',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_patient (clinic_id, patient_id),
    INDEX idx_medical_record (medical_record_id),
    INDEX idx_image_type (image_type),
    INDEX idx_shot_date (shot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口腔影像表';

-- 影像-牙位关联表
-- 影像与牙位的多对多关联

CREATE TABLE IF NOT EXISTS dental_image_tooth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    image_id BIGINT NOT NULL COMMENT '影像ID',
    tooth_number VARCHAR(4) NOT NULL COMMENT '牙位编号(FDI)',
    INDEX idx_image (image_id),
    INDEX idx_tooth (tooth_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影像-牙位关联表';
