-- V100__extend_verification_code_type.sql
-- 扩展 verification_codes.type 列长度，支持 UPDATE_EMAIL_OLD_CONFIRM / RESET_PASSWORD 等长类型
ALTER TABLE verification_codes MODIFY COLUMN type VARCHAR(50) NOT NULL;
