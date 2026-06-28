-- 为患者表添加微信openid字段，用于接收AI回访通知
ALTER TABLE patient ADD COLUMN open_id VARCHAR(100) COMMENT '微信openid，用于接收模板消息';

-- 创建索引（如果需要按openid查询）
CREATE INDEX idx_patient_open_id ON patient(open_id);
