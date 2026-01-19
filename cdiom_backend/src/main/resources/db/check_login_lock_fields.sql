-- ============================================
-- 检查登录锁定机制相关字段和配置
-- ============================================

USE cdiom_db;

-- 1. 检查sys_user表的字段
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = 'cdiom_db'
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME IN ('login_fail_count', 'last_login_fail_time', 'lock_time')
ORDER BY COLUMN_NAME;

-- 2. 检查登录安全配置项
SELECT 
    id,
    config_name,
    config_key,
    config_value,
    config_type,
    remark,
    deleted
FROM sys_config
WHERE 
    config_key IN ('login.fail.threshold', 'login.fail.time.window', 'login.lock.duration')
ORDER BY config_key;

-- 3. 查看某个用户的当前状态（替换USER_ID为实际用户ID）
-- SELECT 
--     id,
--     username,
--     login_fail_count,
--     last_login_fail_time,
--     lock_time,
--     status
-- FROM sys_user
-- WHERE id = 1;  -- 替换为实际用户ID

-- 4. 查看最近的登录日志
SELECT 
    id,
    user_id,
    username,
    status,
    msg,
    ip,
    create_time
FROM login_log
ORDER BY create_time DESC
LIMIT 10;


