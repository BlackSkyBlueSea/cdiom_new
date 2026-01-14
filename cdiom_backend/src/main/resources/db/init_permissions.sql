-- ============================================
-- 权限数据初始化脚本
-- 初始化系统权限和角色权限关联数据
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 插入权限数据（接口权限，permission_type=3）
-- ============================================

-- 用户管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('用户管理', 'user:manage', 3, 0, 1),
('用户查看', 'user:view', 3, 0, 2),
('用户创建', 'user:create', 3, 0, 3),
('用户更新', 'user:update', 3, 0, 4),
('用户删除', 'user:delete', 3, 0, 5)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 角色管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('角色管理', 'role:manage', 3, 0, 10),
('角色查看', 'role:view', 3, 0, 11),
('角色创建', 'role:create', 3, 0, 12),
('角色更新', 'role:update', 3, 0, 13),
('角色删除', 'role:delete', 3, 0, 14)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 药品管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('药品查看', 'drug:view', 3, 0, 20),
('药品管理', 'drug:manage', 3, 0, 21),
('药品创建', 'drug:create', 3, 0, 22),
('药品更新', 'drug:update', 3, 0, 23),
('药品删除', 'drug:delete', 3, 0, 24)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 供应商管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('供应商审核', 'supplier:audit', 3, 0, 25)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 系统配置权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('配置管理', 'config:manage', 3, 0, 30),
('配置查看', 'config:view', 3, 0, 31),
('配置创建', 'config:create', 3, 0, 32),
('配置更新', 'config:update', 3, 0, 33),
('配置删除', 'config:delete', 3, 0, 34)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 通知公告权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('通知查看', 'notice:view', 3, 0, 40),
('通知管理', 'notice:manage', 3, 0, 41),
('通知创建', 'notice:create', 3, 0, 42),
('通知更新', 'notice:update', 3, 0, 43),
('通知删除', 'notice:delete', 3, 0, 44)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 日志查看权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('操作日志查看', 'log:operation:view', 3, 0, 50),
('登录日志查看', 'log:login:view', 3, 0, 51)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- ============================================
-- 2. 插入角色权限关联数据
-- ============================================

-- 系统管理员（角色ID=1）：只拥有系统功能权限（用户管理、角色管理、配置管理、通知管理、日志查看）
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 1, id FROM sys_permission 
WHERE permission_code IN (
    'user:manage', 'user:view', 'user:create', 'user:update', 'user:delete',
    'role:manage', 'role:view', 'role:create', 'role:update', 'role:delete',
    'config:manage', 'config:view', 'config:create', 'config:update', 'config:delete',
    'notice:view', 'notice:manage', 'notice:create', 'notice:update', 'notice:delete',
    'log:operation:view', 'log:login:view'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 仓库管理员（角色ID=2）：药品管理、供应商审核和通知查看、创建
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 2, id FROM sys_permission 
WHERE permission_code IN (
    'drug:view', 'drug:manage', 'drug:create', 'drug:update', 'drug:delete',
    'supplier:audit', -- 供应商审核权限
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 采购专员（角色ID=3）：供应商管理（查看、创建、更新）和通知查看、创建
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 3, id FROM sys_permission 
WHERE permission_code IN (
    'drug:view', -- 查看供应商列表
    'drug:manage', -- 创建、更新供应商（但不包括删除和审核）
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 医护人员（角色ID=4）：通知查看、创建
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 4, id FROM sys_permission 
WHERE permission_code IN ('notice:view', 'notice:create')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 供应商（角色ID=5）：通知查看、创建
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 5, id FROM sys_permission 
WHERE permission_code IN ('notice:view', 'notice:create')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 3. 验证数据
-- ============================================

-- 查看所有权限
SELECT id, permission_name, permission_code, permission_type 
FROM sys_permission 
ORDER BY sort_order;

-- 查看各角色的权限分配
SELECT 
    r.role_name,
    p.permission_name,
    p.permission_code
FROM sys_role r
LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
LEFT JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id IN (1, 2, 3, 4, 5)
ORDER BY r.id, p.sort_order;

