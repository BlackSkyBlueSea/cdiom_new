-- ============================================
-- 登录防暴力破解机制数据库迁移脚本
-- ============================================
-- 说明：实现登录失败锁定机制，防止暴力破解攻击
-- 功能：
-- 1. 为sys_user表添加last_login_fail_time字段，记录最后登录失败时间
-- 2. 初始化登录安全配置项到sys_config表
-- 注意：
-- 1. lockTime和loginFailCount字段已存在于sys_user表，无需添加
-- 2. 配置项可通过配置管理界面动态修改，无需重启服务
-- ============================================

USE cdiom_db;

-- ============================================
-- 第一部分：为sys_user表添加last_login_fail_time字段
-- ============================================

SET @dbname = DATABASE();
SET @tablename = 'sys_user';
SET @columnname = 'last_login_fail_time';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',  -- 字段已存在，不执行任何操作
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname, '` DATETIME NULL COMMENT ''最后登录失败时间'' AFTER `login_fail_count`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- 第二部分：初始化登录安全配置项
-- ============================================

-- 配置项1：登录失败次数阈值
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time, deleted)
SELECT 
    '登录失败次数阈值',
    'login.fail.threshold',
    '5',
    1,
    '时间窗口内连续登录失败次数上限，达到此值将触发账号锁定',
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'login.fail.threshold' AND deleted = 0
);

-- 配置项2：登录失败时间窗口
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time, deleted)
SELECT 
    '登录失败时间窗口',
    'login.fail.time.window',
    '10',
    1,
    '判定暴力破解的时间间隔（单位：分钟），窗口内失败才累加次数',
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'login.fail.time.window' AND deleted = 0
);

-- 配置项3：账号锁定时长
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time, deleted)
SELECT 
    '账号锁定时长',
    'login.lock.duration',
    '1',
    1,
    '账号锁定持续时间（单位：小时），触发锁定后禁止登录的时长',
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'login.lock.duration' AND deleted = 0
);

-- ============================================
-- 验证脚本执行结果
-- ============================================

-- 验证字段是否添加成功
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = @columnname;

-- 验证配置项是否添加成功
SELECT 
    config_name,
    config_key,
    config_value,
    config_type,
    remark
FROM sys_config
WHERE 
    config_key IN ('login.fail.threshold', 'login.fail.time.window', 'login.lock.duration')
    AND deleted = 0;

-- ============================================
-- 脚本执行完成
-- ============================================

