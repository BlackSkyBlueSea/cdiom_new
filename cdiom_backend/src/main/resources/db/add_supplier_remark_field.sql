-- 为供应商表添加备注字段
USE cdiom_db;

ALTER TABLE supplier 
ADD COLUMN remark TEXT DEFAULT NULL COMMENT '备注/描述' AFTER license_expiry_date;

