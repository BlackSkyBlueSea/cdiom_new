-- ============================================
-- 添加供应商表 remark 字段
-- ============================================

USE cdiom_db;

-- 方法1：直接添加（如果字段已存在会报错，可以忽略）
-- ALTER TABLE supplier 
-- ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '备注/描述' 
-- AFTER license_expiry_date;

-- 方法2：安全的添加方式（检查字段是否存在）
SET @dbname = DATABASE();
SET @tablename = 'supplier';
SET @columnname = 'remark';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT ''Field already exists'' AS result',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(500) DEFAULT NULL COMMENT ''备注/描述'' AFTER license_expiry_date')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
