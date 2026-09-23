-- 挂号幂等键：agent/小程序防重复提交（同键重复调用返回同一单）
ALTER TABLE registration ADD COLUMN client_request_id VARCHAR(64) NULL COMMENT '幂等键（agent:或客户端生成）';
ALTER TABLE registration ADD UNIQUE KEY uk_reg_client_request_id (client_request_id);
