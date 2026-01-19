-- ============================================
-- 完整角色权限更新脚本
-- 用于更新所有角色的权限分配，确保与细粒度权限系统一致
-- 执行此脚本前，请确保已执行 add_user_permission_system.sql
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 清除现有角色权限（可选，谨慎使用）
-- ============================================
-- 注意：如果只想更新特定角色，可以注释掉对应的DELETE语句

-- DELETE FROM sys_role_permission WHERE role_id = 1;
-- DELETE FROM sys_role_permission WHERE role_id = 2;
-- DELETE FROM sys_role_permission WHERE role_id = 3;
-- DELETE FROM sys_role_permission WHERE role_id = 4;
-- DELETE FROM sys_role_permission WHERE role_id = 5;

-- ============================================
-- 2. 系统管理员（角色ID=1）：系统功能权限
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 1, id FROM sys_permission 
WHERE permission_code IN (
    -- 用户管理
    'user:manage', 'user:view', 'user:create', 'user:update', 'user:delete',
    -- 角色管理
    'role:manage', 'role:view', 'role:create', 'role:update', 'role:delete',
    -- 系统配置
    'config:manage', 'config:view', 'config:create', 'config:update', 'config:delete',
    -- 通知管理
    'notice:view', 'notice:manage', 'notice:create', 'notice:update', 'notice:delete',
    -- 日志查看
    'log:operation:view', 'log:login:view'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 3. 仓库管理员（角色ID=2）：业务管理权限
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 2, id FROM sys_permission 
WHERE permission_code IN (
    -- 药品管理
    'drug:view', 'drug:manage', 'drug:create', 'drug:update', 'drug:delete',
    -- 供应商审核
    'supplier:audit',
    -- 出库管理（审核、执行，不包含申领）
    'outbound:view', 'outbound:approve', 'outbound:execute', 'outbound:reject',
    -- 入库管理
    'inbound:view', 'inbound:create', 'inbound:approve', 'inbound:execute',
    -- 库存管理
    'inventory:view', 'inventory:adjust',
    -- 通知
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 4. 采购专员（角色ID=3）：采购管理权限
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 3, id FROM sys_permission 
WHERE permission_code IN (
    -- 药品查看（用于查看供应商列表）
    'drug:view',
    -- 供应商管理（创建、更新，不包括删除和审核）
    'drug:manage',
    -- 采购订单管理
    'purchase:view', 'purchase:create', 'purchase:approve', 'purchase:execute',
    -- 通知
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 5. 医护人员（角色ID=4）：出库申领权限
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 4, id FROM sys_permission 
WHERE permission_code IN (
    -- 出库管理（查看和申领）
    'outbound:view', 'outbound:apply',
    -- 通知
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 6. 供应商（角色ID=5）：订单查看权限
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 5, id FROM sys_permission 
WHERE permission_code IN (
    -- 采购订单查看（后端会过滤，只显示该供应商的订单）
    'purchase:view',
    -- 通知
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 7. 验证权限分配
-- ============================================

-- 查看各角色的权限统计
SELECT 
    r.id AS role_id,
    r.role_name,
    COUNT(rp.permission_id) AS permission_count
FROM sys_role r
LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
WHERE r.id IN (1, 2, 3, 4, 5)
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- 查看各角色的详细权限列表
SELECT 
    r.id AS role_id,
    r.role_name,
    p.permission_code,
    p.permission_name,
    CASE 
        WHEN p.permission_code LIKE 'outbound:%' THEN '出库管理'
        WHEN p.permission_code LIKE 'inbound:%' THEN '入库管理'
        WHEN p.permission_code LIKE 'inventory:%' THEN '库存管理'
        WHEN p.permission_code LIKE 'purchase:%' THEN '采购管理'
        WHEN p.permission_code LIKE 'drug:%' THEN '药品管理'
        WHEN p.permission_code LIKE 'supplier:%' THEN '供应商管理'
        WHEN p.permission_code LIKE 'user:%' THEN '用户管理'
        WHEN p.permission_code LIKE 'role:%' THEN '角色管理'
        WHEN p.permission_code LIKE 'config:%' THEN '配置管理'
        WHEN p.permission_code LIKE 'notice:%' THEN '通知管理'
        WHEN p.permission_code LIKE 'log:%' THEN '日志管理'
        ELSE '其他'
    END AS permission_category
FROM sys_role r
INNER JOIN sys_role_permission rp ON r.id = rp.role_id
INNER JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id IN (1, 2, 3, 4, 5)
ORDER BY r.id, permission_category, p.sort_order;




