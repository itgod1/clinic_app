-- 复诊提醒计划表
-- 用于管理患者拔牙后种牙、治疗后续复诊等需要延期复诊的场景

CREATE TABLE IF NOT EXISTS follow_up_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    clinic_id BIGINT NOT NULL COMMENT '诊所ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    patient_name VARCHAR(50) COMMENT '患者姓名',
    patient_phone VARCHAR(20) COMMENT '患者电话',
    registration_id BIGINT COMMENT '挂号ID',
    medical_record_id BIGINT COMMENT '病历ID',
    doctor_id BIGINT COMMENT '创建医生ID',
    doctor_name VARCHAR(50) COMMENT '创建医生姓名',
    
    -- 复诊信息
    follow_up_item VARCHAR(200) COMMENT '复诊项目，如：种植牙、拆线、复查等',
    follow_up_desc TEXT COMMENT '复诊说明',
    original_treatment VARCHAR(200) COMMENT '原治疗项目，如：拔牙、根管治疗等',
    
    -- 时间安排
    recovery_days INT DEFAULT 0 COMMENT '恢复期天数，如：90天（3个月）',
    plan_date DATE NOT NULL COMMENT '计划复诊日期（到期提醒日期）',
    remind_days_before INT DEFAULT 1 COMMENT '提前几天提醒，默认提前1天',
    actual_date DATE COMMENT '实际复诊日期',
    
    -- 状态管理
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: 
        PENDING-待提醒,
        REMINDED-已提醒,
        APPOINTED-已预约,
        COMPLETED-已完成,
        CANCELLED-已取消',
    priority INT DEFAULT 0 COMMENT '优先级 0普通 1紧急',
    
    -- 提醒记录
    reminded_at DATETIME COMMENT '提醒时间',
    reminded_to BIGINT COMMENT '提醒给谁（医生ID）',
    remind_count INT DEFAULT 0 COMMENT '提醒次数',
    
    -- 预约记录
    appointed_at DATETIME COMMENT '预约时间',
    appointed_by BIGINT COMMENT '预约操作人ID',
    appointed_by_name VARCHAR(50) COMMENT '预约操作人姓名',
    appointment_note TEXT COMMENT '预约备注',
    
    -- 完成记录
    completed_at DATETIME COMMENT '完成时间',
    completed_by BIGINT COMMENT '完成操作人ID',
    completion_note TEXT COMMENT '完成备注',
    
    -- 关联后续挂号
    follow_up_registration_id BIGINT COMMENT '复诊挂号ID',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    
    INDEX idx_clinic_id (clinic_id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_plan_date (plan_date),
    INDEX idx_status (status),
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_status_date (status, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复诊提醒计划表';
