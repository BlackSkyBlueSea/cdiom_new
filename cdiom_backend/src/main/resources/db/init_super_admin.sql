-- ============================================
-- 超级管理员角色和用户初始化脚本
-- 超级管理员（roleId=6）拥有所有权限，主要用于系统测试
-- ============================================

USE cdiom_db;

-- 插入超级管理员角色（角色ID=6）
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`) VALUES
(6, '超级管理员', 'SUPER_ADMIN_TEST', '超级管理员，拥有所有权限，主要用于系统测试和维护', 1)
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`);

-- 插入超级管理员用户（用户名：super_admin，密码：super123，BCrypt加密后的值）
-- 注意：实际使用时需要替换为BCrypt加密后的密码
-- 默认状态为停用（status=0），需要通过邮箱验证码启用
INSERT INTO `sys_user` (`id`, `username`, `phone`, `password`, `role_id`, `status`) VALUES
(2, 'super_admin', '13827679411', '$2a$10$bTjn2BmZE5vbY7IzUaquhuvweupKL/J7aA067LVv/vdp/pKXdZad.', 6, 0)
ON DUPLICATE KEY UPDATE `username`=VALUES(`username`), `role_id`=VALUES(`role_id`);

-- 超级管理员拥有所有权限（通过代码中roleId=6判断，返回通配符"*"）
-- 不需要在数据库中配置权限关联

-- ============================================
-- 验证数据
-- ============================================

-- 查看超级管理员角色
SELECT * FROM sys_role WHERE id = 6;

-- 查看超级管理员用户
SELECT id, username, role_id, status, create_time FROM sys_user WHERE username = 'super_admin';

