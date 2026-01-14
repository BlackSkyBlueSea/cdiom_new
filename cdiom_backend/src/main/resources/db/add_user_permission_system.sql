-- ============================================
-- 细粒度权限系统升级脚本
-- 支持用户直接拥有权限，实现更细粒度的权限控制
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 创建用户权限关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_user_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_permission` (`user_id`, `permission_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_permission_id` (`permission_id`),
    CONSTRAINT `fk_user_permission_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限关联表';

-- ============================================
-- 2. 添加细粒度出库相关权限
-- ============================================

-- 出库管理基础权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('出库查看', 'outbound:view', 3, 0, 60),
('出库申领', 'outbound:apply', 3, 0, 61),
('出库审核', 'outbound:approve', 3, 0, 62),
('出库执行', 'outbound:execute', 3, 0, 63),
('特殊药品审核', 'outbound:approve:special', 3, 0, 64),
('出库驳回', 'outbound:reject', 3, 0, 65)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 入库管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('入库查看', 'inbound:view', 3, 0, 70),
('入库创建', 'inbound:create', 3, 0, 71),
('入库审核', 'inbound:approve', 3, 0, 72),
('入库执行', 'inbound:execute', 3, 0, 73)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 库存管理权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('库存查看', 'inventory:view', 3, 0, 80),
('库存调整', 'inventory:adjust', 3, 0, 81),
('库存调整审核', 'inventory:adjust:approve', 3, 0, 82)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- 采购订单权限
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, sort_order) VALUES
('采购订单查看', 'purchase:view', 3, 0, 90),
('采购订单创建', 'purchase:create', 3, 0, 91),
('采购订单审核', 'purchase:approve', 3, 0, 92),
('采购订单执行', 'purchase:execute', 3, 0, 93)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

-- ============================================
-- 3. 为现有角色分配默认权限（保持向后兼容）
-- ============================================

-- 系统管理员（角色ID=1）：系统功能权限（已在init_permissions.sql中配置，这里确保完整性）
-- 注意：系统管理员不拥有业务权限（出库、入库、库存、采购等）

-- 仓库管理员（角色ID=2）：药品管理、供应商审核、出库审核执行、入库、库存管理
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 2, id FROM sys_permission 
WHERE permission_code IN (
    -- 药品管理（保持原有权限）
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

-- 采购专员（角色ID=3）：供应商管理、采购订单管理
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

-- 医护人员（角色ID=4）：出库查看、申领
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 4, id FROM sys_permission 
WHERE permission_code IN (
    -- 出库管理（查看和申领）
    'outbound:view', 'outbound:apply',
    -- 通知
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id);

-- 供应商（角色ID=5）：采购订单查看（仅查看自己的订单）
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
-- 4. 用户直接权限分配示例
-- ============================================
-- 注意：以下示例都是注释掉的，需要时取消注释并修改用户ID

-- 示例1：为某个医护人员添加特殊药品审核权限
-- INSERT INTO sys_user_permission (user_id, permission_id) 
-- SELECT 10, id FROM sys_permission WHERE permission_code = 'outbound:approve:special';

-- 示例2：为某个医护人员添加库存查看权限
-- INSERT INTO sys_user_permission (user_id, permission_id) 
-- SELECT 10, id FROM sys_permission WHERE permission_code = 'inventory:view';

-- 示例3：为某个仓库管理员添加采购订单查看权限
-- INSERT INTO sys_user_permission (user_id, permission_id) 
-- SELECT 15, id FROM sys_permission WHERE permission_code = 'purchase:view';

-- 示例4：移除用户的某个权限（删除用户直接权限）
-- DELETE FROM sys_user_permission 
-- WHERE user_id = 10 AND permission_id = (SELECT id FROM sys_permission WHERE permission_code = 'outbound:approve:special');

-- ============================================
-- 5. 验证数据
-- ============================================

-- 查看所有业务权限（出库、入库、库存、采购）
SELECT id, permission_name, permission_code, permission_type 
FROM sys_permission 
WHERE permission_code LIKE 'outbound:%' 
   OR permission_code LIKE 'inbound:%' 
   OR permission_code LIKE 'inventory:%'
   OR permission_code LIKE 'purchase:%'
ORDER BY sort_order;

-- 查看各角色的完整权限分配
SELECT 
    r.id AS role_id,
    r.role_name,
    p.permission_code,
    p.permission_name
FROM sys_role r
LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
LEFT JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id IN (1, 2, 3, 4, 5)
ORDER BY r.id, p.sort_order;

-- 查看用户权限（角色权限 + 用户直接权限）
SELECT 
    u.id AS user_id,
    u.username,
    r.role_name,
    p.permission_code,
    'role' AS permission_source
FROM sys_user u
INNER JOIN sys_role r ON u.role_id = r.id
INNER JOIN sys_role_permission rp ON r.id = rp.role_id
INNER JOIN sys_permission p ON rp.permission_id = p.id
WHERE u.id = 1  -- 替换为实际用户ID
UNION ALL
SELECT 
    u.id AS user_id,
    u.username,
    NULL AS role_name,
    p.permission_code,
    'user' AS permission_source
FROM sys_user u
INNER JOIN sys_user_permission up ON u.id = up.user_id
INNER JOIN sys_permission p ON up.permission_id = p.id
WHERE u.id = 1  -- 替换为实际用户ID
ORDER BY permission_source, permission_code;

