-- AI智能回访相关表

-- AI回访对话记录表
CREATE TABLE IF NOT EXISTS ai_return_visit_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    conversation_id VARCHAR(64) UNIQUE NOT NULL COMMENT '对话唯一标识',
    plan_id BIGINT COMMENT '关联回访计划ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    dify_conversation_id VARCHAR(128) COMMENT 'Dify对话ID',
    channel VARCHAR(20) COMMENT '渠道: AI_PHONE/AI_SMS/AI_WECHAT',
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/COMPLETED/FAILED',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_seconds INT COMMENT '对话时长(秒)',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    summary TEXT COMMENT 'AI总结',
    patient_status VARCHAR(20) COMMENT '患者状态: RECOVERING/GOOD/NEED_FOLLOWUP',
    satisfaction INT COMMENT '满意度 1-5',
    needs_follow_up TINYINT(1) DEFAULT 0 COMMENT '是否需要人工跟进',
    follow_up_reason VARCHAR(500) COMMENT '需跟进原因',
    full_dialogue JSON COMMENT '完整对话记录',
    extracted_info JSON COMMENT '提取的关键信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    INDEX idx_plan_id (plan_id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_dify_conversation_id (dify_conversation_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI回访对话记录表';

-- AI回访任务队列表
CREATE TABLE IF NOT EXISTS ai_return_visit_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '回访计划ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    task_type VARCHAR(20) DEFAULT 'SCHEDULED' COMMENT '任务类型: IMMEDIATE/SCHEDULED',
    execute_time DATETIME COMMENT '计划执行时间',
    priority INT DEFAULT 0 COMMENT '优先级',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    conversation_id BIGINT COMMENT '关联对话记录ID',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    INDEX idx_plan_id (plan_id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_status_time (status, execute_time),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI回访任务队列表';

-- 在return_visit_plan表中添加AI相关字段
ALTER TABLE return_visit_plan
ADD COLUMN IF NOT EXISTS ai_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用AI回访',
ADD COLUMN IF NOT EXISTS ai_conversation_id BIGINT COMMENT 'AI对话记录ID';
