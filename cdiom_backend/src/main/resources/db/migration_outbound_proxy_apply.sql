-- 出库申请：仓库管理员现场代医护人员录入（可追溯代录人）
-- 执行前请备份数据库

ALTER TABLE `outbound_apply`
    ADD COLUMN `proxy_registrar_id` BIGINT DEFAULT NULL COMMENT '代录人ID（仓库管理员代录时）' AFTER `applicant_id`,
    ADD KEY `idx_proxy_registrar_id` (`proxy_registrar_id`),
    ADD CONSTRAINT `fk_outbound_apply_proxy_registrar` FOREIGN KEY (`proxy_registrar_id`) REFERENCES `sys_user` (`id`);

-- 新权限：代录出库申请
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `sort_order`) VALUES
('出库代录', 'outbound:apply:on-behalf', 3, 0, 66)
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- 仓库管理员（role_id=2）授予代录权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission` WHERE `permission_code` = 'outbound:apply:on-behalf';
