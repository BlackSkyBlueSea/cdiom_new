-- ============================================
-- 供应商管理权限更新脚本
-- 1. 添加供应商审核权限
-- 2. 给采购专员添加供应商管理权限（可创建供应商）
-- 3. 给仓库管理员添加供应商审核权限（可审核供应商）
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 添加供应商审核权限
-- ============================================
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('供应商审核', 'supplier:audit', 3, 0, 25)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- ============================================
-- 2. 给采购专员（角色ID=3）添加供应商管理权限
-- 可以查看、创建、更新供应商，但不能删除和审核
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 3, id FROM sys_permission 
WHERE permission_code IN (
    'drug:view',  -- 查看供应商列表
    'drug:manage' -- 创建、更新供应商（但不包括删除和审核）
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 3. 给仓库管理员（角色ID=2）添加供应商审核权限
-- 可以审核供应商
-- ============================================
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 2, id FROM sys_permission 
WHERE permission_code IN ('supplier:audit')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- ============================================
-- 4. 验证数据
-- ============================================
-- 查看采购专员的权限
SELECT 
    r.role_name,
    p.permission_name,
    p.permission_code
FROM sys_role r
JOIN sys_role_permission rp ON r.id = rp.role_id
JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id = 3
ORDER BY p.sort_order;

-- 查看仓库管理员的权限
SELECT 
    r.role_name,
    p.permission_name,
    p.permission_code
FROM sys_role r
JOIN sys_role_permission rp ON r.id = rp.role_id
JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id = 2
ORDER BY p.sort_order;



