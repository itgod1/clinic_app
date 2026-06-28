-- 改造回访中心，支持复诊提醒功能
-- 在return_visit_plan表中添加区分回访类型和复诊提醒的字段

-- 添加计划类型字段
ALTER TABLE return_visit_plan 
ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) DEFAULT 'FOLLOW_UP' COMMENT '计划类型: FOLLOW_UP回访/REVISIT复诊提醒';

-- 添加复诊提醒相关字段
ALTER TABLE return_visit_plan 
ADD COLUMN IF NOT EXISTS recovery_days INT DEFAULT 0 COMMENT '恢复期天数，如：90天（3个月）',
ADD COLUMN IF NOT EXISTS original_treatment VARCHAR(200) COMMENT '原治疗项目，如：拔牙、根管治疗等',
ADD COLUMN IF NOT EXISTS reminded_at DATETIME COMMENT '提醒时间',
ADD COLUMN IF NOT EXISTS reminded_to BIGINT COMMENT '提醒给谁（医生ID）',
ADD COLUMN IF NOT EXISTS remind_count INT DEFAULT 0 COMMENT '提醒次数',
ADD COLUMN IF NOT EXISTS appointed_at DATETIME COMMENT '预约时间',
ADD COLUMN IF NOT EXISTS appointed_by BIGINT COMMENT '预约操作人ID',
ADD COLUMN IF NOT EXISTS appointed_by_name VARCHAR(50) COMMENT '预约操作人姓名',
ADD COLUMN IF NOT EXISTS appointment_note TEXT COMMENT '预约备注',
ADD COLUMN IF NOT EXISTS actual_date DATE COMMENT '实际复诊日期';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_plan_type ON return_visit_plan(plan_type);
CREATE INDEX IF NOT EXISTS idx_status_type ON return_visit_plan(status, plan_type);

-- 更新现有数据为回访类型
UPDATE return_visit_plan SET plan_type = 'FOLLOW_UP' WHERE plan_type IS NULL;
