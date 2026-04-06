-- 仅补充 outbound_apply.proxy_registrar_id（解决 Unknown column 'proxy_registrar_id'）
-- 在已运行的库上执行本文件即可；若已执行过 migration_outbound_proxy_apply.sql 则不必重复执行。
-- MySQL / MariaDB（若列或索引已存在会报错，可忽略或手动跳过对应语句）

ALTER TABLE `outbound_apply`
    ADD COLUMN `proxy_registrar_id` BIGINT DEFAULT NULL COMMENT '代录人ID（仓库管理员代录时）' AFTER `applicant_id`,
    ADD KEY `idx_proxy_registrar_id` (`proxy_registrar_id`),
    ADD CONSTRAINT `fk_outbound_apply_proxy_registrar` FOREIGN KEY (`proxy_registrar_id`) REFERENCES `sys_user` (`id`);
