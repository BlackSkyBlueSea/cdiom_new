-- ============================================
-- 供应商审批流程权限初始化脚本
-- 新增审批流程相关权限，并分配给相应角色
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 新增供应商审批流程权限
-- ============================================

-- 供应商审批申请权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('供应商审批申请', 'supplier:approval:apply', 3, 0, 60),
('供应商资质核验', 'supplier:approval:quality', 3, 0, 61),
('供应商价格审核', 'supplier:approval:price', 3, 0, 62),
('供应商最终审批', 'supplier:approval:final', 3, 0, 63),
('供应商审批查看', 'supplier:approval:view', 3, 0, 64)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 供应商黑名单管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('供应商黑名单管理', 'supplier:blacklist:manage', 3, 0, 65),
('供应商黑名单查看', 'supplier:blacklist:view', 3, 0, 66)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 价格预警配置权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('价格预警配置', 'price:warning:config', 3, 0, 67),
('价格预警查看', 'price:warning:view', 3, 0, 68)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 价格协议管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('价格协议管理', 'price:agreement:manage', 3, 0, 69),
('价格协议查看', 'price:agreement:view', 3, 0, 70)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 价格历史查看权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('价格历史查看', 'price:history:view', 3, 0, 71)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- ============================================
-- 2. 为各角色分配审批流程权限
-- ============================================

-- 采购专员（角色ID=3）：发起审批申请、查看审批、价格审核
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 3, id FROM sys_permission 
WHERE permission_code IN (
    'supplier:approval:apply',  -- 发起审批申请
    'supplier:approval:view',  -- 查看审批
    'supplier:approval:price', -- 价格审核（采购专员可以审核价格）
    'price:agreement:view',     -- 查看价格协议
    'price:history:view'        -- 查看价格历史
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 仓库管理员（角色ID=2）：资质核验、价格审核、最终审批、价格协议管理、价格预警配置
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 2, id FROM sys_permission 
WHERE permission_code IN (
    'supplier:approval:quality',  -- 资质核验
    'supplier:approval:price',     -- 价格审核（仓库管理员也可以审核价格）
    'supplier:approval:final',     -- 最终审批（仓库管理员可以最终审批）
    'supplier:approval:view',      -- 查看审批
    'price:agreement:manage',      -- 价格协议管理
    'price:agreement:view',        -- 查看价格协议
    'price:warning:config',        -- 价格预警配置
    'price:warning:view',          -- 查看价格预警
    'price:history:view'           -- 查看价格历史
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 系统管理员（角色ID=1）：不分配业务权限，只保留系统功能权限
-- 系统管理员主要用于系统维护，不参与业务审批流程

-- 医护人员（角色ID=4）：不分配审批权限
-- 医护人员不参与供应商审批流程

-- 供应商（角色ID=5）：不分配审批权限
-- 供应商不参与审批流程

-- 超级管理员（角色ID=6）：拥有所有权限（通过代码判断，返回通配符"*"）
-- 超级管理员主要用于系统测试和维护，拥有所有权限

-- ============================================
-- 3. 验证权限分配
-- ============================================

-- 查看新增的权限
SELECT id, permission_name, permission_code, permission_type 
FROM sys_permission 
WHERE permission_code LIKE 'supplier:approval:%' 
   OR permission_code LIKE 'supplier:blacklist:%'
   OR permission_code LIKE 'price:warning:%'
   OR permission_code LIKE 'price:agreement:%'
   OR permission_code LIKE 'price:history:%'
ORDER BY sort_order;

-- 查看各角色的审批权限分配
SELECT 
    r.id AS role_id,
    r.role_name,
    p.permission_code,
    p.permission_name
FROM sys_role r
INNER JOIN sys_role_permission rp ON r.id = rp.role_id
INNER JOIN sys_permission p ON rp.permission_id = p.id
WHERE p.permission_code LIKE 'supplier:approval:%'
   OR p.permission_code LIKE 'supplier:blacklist:%'
   OR p.permission_code LIKE 'price:warning:%'
   OR p.permission_code LIKE 'price:agreement:%'
   OR p.permission_code LIKE 'price:history:%'
ORDER BY r.id, p.sort_order;

