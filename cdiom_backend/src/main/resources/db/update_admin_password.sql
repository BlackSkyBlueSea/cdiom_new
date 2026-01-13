-- ============================================
-- 更新超级管理员密码
-- ============================================
-- 说明：将超级管理员（admin）的密码更新为新的BCrypt加密值
-- ============================================

USE cdiom_db;

-- 更新超级管理员密码
UPDATE `sys_user` 
SET `password` = '$2a$10$7WcJvZ.NHjMA8fadvg6qPeGeHnW0nfzZK2uKLIeaYXN2Rm.dunUIO',
    `update_time` = NOW()
WHERE `id` = 1 
  AND `username` = 'admin';

-- 验证更新结果
SELECT `id`, `username`, `password`, `update_time` 
FROM `sys_user` 
WHERE `id` = 1;



