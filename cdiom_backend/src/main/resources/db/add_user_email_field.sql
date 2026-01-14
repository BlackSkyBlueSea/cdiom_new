-- ============================================
-- 为用户表添加邮箱字段
-- ============================================
-- 说明：为sys_user表添加email字段，用于存储用户邮箱地址
-- 邮箱字段用于超级管理员启用/禁用操作的验证
-- 注意：
-- 1. 邮箱字段可以为NULL（普通用户可以不填写邮箱）
-- 2. 超级管理员账户必须绑定邮箱，用于启用/禁用操作的验证
-- 3. 非NULL的邮箱值必须唯一（通过唯一索引保证）
-- ============================================

USE cdiom_db;

-- 检查字段是否已存在，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = 'sys_user';
SET @columnname = 'email';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',  -- 字段已存在，不执行任何操作
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname, '` VARCHAR(100) DEFAULT NULL COMMENT ''邮箱地址'' AFTER `phone`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查索引是否已存在，如果不存在则添加唯一索引
-- 注意：MySQL的UNIQUE索引允许多个NULL值，所以即使email可以为NULL，非NULL值也会保证唯一性
SET @indexname = 'uk_email';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1',  -- 索引已存在，不执行任何操作
  CONCAT('CREATE UNIQUE INDEX `', @indexname, '` ON `', @tablename, '` (`email`)')
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- 如果上面的唯一索引创建失败（某些MySQL版本可能不支持），可以使用普通索引
-- 然后通过应用层代码确保邮箱唯一性
-- CREATE INDEX `idx_email` ON `sys_user` (`email`);

