-- 医保对接功能数据表
-- 包含患者医保信息、医保目录、医保结算及结算明细

-- 1. 患者医保信息表
CREATE TABLE IF NOT EXISTS insurance_patient (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    patient_id BIGINT NOT NULL COMMENT '关联患者ID',
    insurance_card_no VARCHAR(50) NOT NULL COMMENT '医保卡号',
    insurance_type VARCHAR(30) DEFAULT 'URBAN_EMPLOYEE' COMMENT '险种: URBAN_EMPLOYEE职工医保/URBAN_RESIDENT居民医保/NEW_RURAL新农合/COMMERCIAL商业保险',
    insurance_city VARCHAR(50) COMMENT '参保城市',
    insurance_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE正常/SUSPENDED暂停/CANCELLED注销',
    holder_name VARCHAR(50) COMMENT '持卡人姓名',
    holder_id_card VARCHAR(18) COMMENT '持卡人身份证',
    valid_from DATE COMMENT '有效期起',
    valid_to DATE COMMENT '有效期止',
    bank_name VARCHAR(100) COMMENT '发卡银行',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_patient (clinic_id, patient_id),
    INDEX idx_card_no (insurance_card_no),
    INDEX idx_insurance_type (insurance_type),
    INDEX idx_insurance_status (insurance_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者医保信息表';

-- 2. 医保目录表
CREATE TABLE IF NOT EXISTS insurance_catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    catalog_type VARCHAR(20) NOT NULL COMMENT '目录类型: DRUG药品/TREATMENT诊疗/MATERIAL耗材',
    item_code VARCHAR(50) NOT NULL COMMENT '医保项目编码',
    item_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    specification VARCHAR(100) COMMENT '规格',
    manufacturer VARCHAR(100) COMMENT '生产厂家',
    unit VARCHAR(20) COMMENT '单位',
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    self_pay_ratio DECIMAL(5,4) DEFAULT 0.0000 COMMENT '自付比例(0-1)',
    insurance_category VARCHAR(10) DEFAULT 'A' COMMENT '甲乙丙类: A甲类/B乙类/C丙类',
    dosage_form VARCHAR(50) COMMENT '剂型(药品)',
    icd_code VARCHAR(20) COMMENT '限制使用ICD编码',
    hospital_level VARCHAR(10) COMMENT '限制医院等级: 1一级/2二级/3三级',
    daily_limit DECIMAL(10,2) COMMENT '日限额',
    status TINYINT DEFAULT 1 COMMENT '启用状态: 0禁用 1启用',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_clinic_type (clinic_id, catalog_type),
    INDEX idx_item_code (item_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保目录表';

-- 3. 医保结算表
CREATE TABLE IF NOT EXISTS insurance_settlement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    order_id BIGINT NOT NULL COMMENT '处方/订单ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    insurance_patient_id BIGINT COMMENT '关联insurance_patient.id',
    settlement_no VARCHAR(50) NOT NULL COMMENT '结算单号',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总费用',
    insurance_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '医保支付金额',
    self_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '个人自付金额',
    account_pay DECIMAL(10,2) DEFAULT 0.00 COMMENT '医保账户支付',
    cash_pay DECIMAL(10,2) DEFAULT 0.00 COMMENT '现金支付(患者实际付现部分)',
    settlement_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING待结算/SUBMITTED已提交/SUCCESS结算成功/FAILED结算失败/CANCELLED已取消',
    insurance_claim_no VARCHAR(50) COMMENT '医保平台返回的结算流水号',
    submit_time DATETIME COMMENT '提交医保平台时间',
    complete_time DATETIME COMMENT '结算完成时间',
    error_message VARCHAR(500) COMMENT '失败原因',
    operator_id BIGINT COMMENT '操作员ID',
    operator_name VARCHAR(50) COMMENT '操作员姓名',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    UNIQUE INDEX idx_settlement_no (settlement_no),
    INDEX idx_order (order_id),
    INDEX idx_patient (clinic_id, patient_id),
    INDEX idx_status (settlement_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保结算表';

-- 4. 医保结算明细表
CREATE TABLE IF NOT EXISTS insurance_settlement_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    settlement_id BIGINT NOT NULL COMMENT '关联insurance_settlement.id',
    catalog_item_id BIGINT COMMENT '关联insurance_catalog.id',
    item_code VARCHAR(50) COMMENT '医保项目编码',
    item_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) COMMENT '单位',
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00 COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    total_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总价',
    self_pay_ratio DECIMAL(5,4) DEFAULT 0.0000 COMMENT '自付比例',
    insurance_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '该项目医保支付',
    self_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '该项目个人自付',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_settlement (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保结算明细表';
