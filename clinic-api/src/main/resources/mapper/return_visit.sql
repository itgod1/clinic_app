-- 回访中心模块数据库表
-- 创建时间: 2026-05-02

-- 回访计划表
CREATE TABLE IF NOT EXISTS return_visit_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    patient_name VARCHAR(50) COMMENT '患者姓名',
    patient_phone VARCHAR(20) COMMENT '患者电话',
    registration_id BIGINT COMMENT '挂号ID',
    medical_record_id BIGINT COMMENT '病历ID',
    visit_item VARCHAR(200) COMMENT '就诊项目',
    plan_date DATE COMMENT '计划回访日期',
    plan_time VARCHAR(20) COMMENT '计划回访时间',
    visit_type VARCHAR(20) COMMENT '回访类型: PHONE电话/SMS短信/WECHAT微信/STORE到店',
    content_template VARCHAR(1000) COMMENT '回访内容模板',
    assignee_id BIGINT COMMENT '分配人ID',
    assignee_name VARCHAR(50) COMMENT '分配人姓名',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING待执行/EXECUTED已执行/CANCELLED已取消/OVERDUE已逾期',
    priority INT DEFAULT 0 COMMENT '优先级 0普通 1紧急',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_clinic_id (clinic_id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_plan_date (plan_date),
    INDEX idx_status (status),
    INDEX idx_assignee_id (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回访计划表';

-- 回访记录表
CREATE TABLE IF NOT EXISTS return_visit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    plan_id BIGINT COMMENT '关联回访计划ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    patient_name VARCHAR(50) COMMENT '患者姓名',
    patient_phone VARCHAR(20) COMMENT '患者电话',
    registration_id BIGINT COMMENT '挂号ID',
    medical_record_id BIGINT COMMENT '病历ID',
    doctor_id BIGINT COMMENT '回访医生ID',
    doctor_name VARCHAR(50) COMMENT '回访医生姓名',
    visit_date DATETIME COMMENT '实际回访时间',
    visit_type VARCHAR(20) COMMENT '回访方式: PHONE电话/SMS短信/WECHAT微信/STORE到店',
    content TEXT COMMENT '回访内容',
    result VARCHAR(20) COMMENT '回访结果: CONTACTED已联系/NO_ANSWER未接通/REFUSED拒访/INVALID无效',
    satisfaction INT COMMENT '满意度 1-5',
    next_plan_id BIGINT COMMENT '关联下次回访计划ID',
    record_url VARCHAR(500) COMMENT '录音地址',
    remark TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_clinic_id (clinic_id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_visit_date (visit_date),
    INDEX idx_doctor_id (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回访记录表';

-- 回访模板表
CREATE TABLE IF NOT EXISTS return_visit_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT COMMENT '诊所ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    visit_type VARCHAR(20) COMMENT '适用回访类型: PHONE/SMS/WECHAT/STORE',
    content_template VARCHAR(2000) COMMENT '内容模板',
    result_options VARCHAR(500) COMMENT '结果选项，逗号分隔如：已联系,未接通,拒访',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态: ENABLED启用/DISABLED禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_clinic_id (clinic_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回访模板表';

-- 回访自动分配规则表
CREATE TABLE IF NOT EXISTS return_visit_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    condition_type VARCHAR(50) COMMENT '触发条件类型: VISIT_ITEM就诊项目/DOCTOR医生/DEPT科室/ALL全部',
    condition_value VARCHAR(200) COMMENT '条件值，如就诊项目名称',
    assignee_id BIGINT COMMENT '分配给谁',
    assignee_name VARCHAR(50) COMMENT '分配人姓名',
    assignee_role VARCHAR(50) COMMENT '分配角色: DOCTOR医生/NURSE护士/CONSULTANT咨询师',
    priority INT DEFAULT 0 COMMENT '优先级 0最低 10最高',
    auto_create INT DEFAULT 1 COMMENT '是否自动创建回访计划 0否 1是',
    status VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态: ENABLED启用/DISABLED禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_clinic_id (clinic_id),
    INDEX idx_status (status),
    INDEX idx_condition_type (condition_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回访自动分配规则表';

-- 初始化回访模板数据
INSERT INTO return_visit_template (clinic_id, name, visit_type, content_template, result_options, sort_order, status) VALUES
(NULL, '根管治疗回访', 'PHONE', '您好，您在{date}进行的根管治疗目前感觉如何？是否有疼痛或不适？', '已联系,未接通,拒访', 1, 'ENABLED'),
(NULL, '拔牙术后回访', 'PHONE', '您好，您在{date}拔牙后恢复情况怎么样？是否有出血不止或剧烈疼痛？', '已联系,未接通,拒访', 2, 'ENABLED'),
(NULL, '种植手术后回访', 'PHONE', '您好，您在{date}进行的种植手术目前感觉如何？是否有红肿或不适？', '已联系,未接通,拒访', 3, 'ENABLED'),
(NULL, '正畸复诊提醒', 'SMS', '您好，您的正畸复诊时间即将到来，请按时到店。', '已预约,需改期,已取消', 4, 'ENABLED'),
(NULL, '洁牙服务回访', 'WECHAT', '您好，您在{date}进行的洁牙服务满意吗？有什么建议吗？', '满意,一般,不满意', 5, 'ENABLED');
