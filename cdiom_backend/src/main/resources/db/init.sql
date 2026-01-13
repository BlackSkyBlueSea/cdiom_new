-- CDIOM系统数据库初始化脚本
-- 数据库：cdiom_db
-- 字符集：utf8mb4
-- 存储引擎：InnoDB

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS cdiom_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cdiom_db;

-- ============================================
-- 系统表
-- ============================================

-- 系统角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用/1-正常',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `role_id` BIGINT DEFAULT NULL COMMENT '角色ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用/1-正常',
    `lock_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
    `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 系统参数配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_name` VARCHAR(100) NOT NULL COMMENT '参数名称',
    `config_key` VARCHAR(100) NOT NULL COMMENT '参数键名',
    `config_value` TEXT DEFAULT NULL COMMENT '参数值',
    `config_type` TINYINT DEFAULT 1 COMMENT '参数类型：1-系统参数/2-业务参数',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_type` (`config_type`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数配置表';

-- 系统通知公告表
CREATE TABLE IF NOT EXISTS `sys_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `notice_type` TINYINT DEFAULT 1 COMMENT '公告类型：1-通知/2-公告',
    `notice_content` TEXT DEFAULT NULL COMMENT '公告内容',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-关闭/1-正常',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_notice_type` (`notice_type`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知公告表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
    `operation_type` VARCHAR(20) DEFAULT NULL COMMENT '操作类型：INSERT/UPDATE/DELETE/SELECT',
    `operation_content` VARCHAR(500) DEFAULT NULL COMMENT '操作内容',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    `status` TINYINT DEFAULT 1 COMMENT '操作状态：0-失败/1-成功',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
    `operation_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_status` (`status`),
    KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS `login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器类型',
    `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
    `status` TINYINT DEFAULT 1 COMMENT '登录状态：0-失败/1-成功',
    `msg` VARCHAR(500) DEFAULT NULL COMMENT '登录消息',
    `login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入系统角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`) VALUES
(1, '系统管理员', 'SUPER_ADMIN', '系统管理员，拥有所有权限', 1),
(2, '仓库管理员', 'WAREHOUSE_ADMIN', '仓库管理员，负责药品入库、出库、库存管理', 1),
(3, '采购专员', 'PURCHASER', '采购专员，负责供应商管理和采购订单管理', 1),
(4, '医护人员', 'MEDICAL_STAFF', '医护人员，负责药品申领', 1),
(5, '供应商', 'SUPPLIER', '供应商，负责订单管理', 1)
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`);

-- 插入系统管理员用户（密码：admin123，BCrypt加密后的值）
-- 注意：实际使用时需要替换为BCrypt加密后的密码
INSERT INTO `sys_user` (`id`, `username`, `phone`, `password`, `role_id`, `status`) VALUES
(1, 'admin', '17728770236', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5C', 1, 1)
ON DUPLICATE KEY UPDATE `username`=VALUES(`username`);

-- 插入系统参数配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`) VALUES
('近效期预警阈值（天）', 'expiry_warning_days', '180', 2, '药品有效期至当前日期的天数差，小于等于此值将预警'),
('红色预警阈值（天）', 'expiry_critical_days', '90', 2, '药品有效期至当前日期的天数差，小于等于此值将红色预警'),
('日志保留期（年）', 'log_retention_years', '5', 1, '操作日志和登录日志的保留年限'),
('JWT过期时间（毫秒）', 'jwt_expiration', '28800000', 1, 'JWT Token的有效期，默认8小时')
ON DUPLICATE KEY UPDATE `config_value`=VALUES(`config_value`);

-- 插入示例通知公告
INSERT INTO `sys_notice` (`notice_title`, `notice_type`, `notice_content`, `status`) VALUES
('欢迎使用CDIOM系统', 1, '欢迎使用临床药品出入库管理系统！', 1),
('系统使用说明', 2, '请各用户按照系统规范操作，确保数据准确性。', 1)
ON DUPLICATE KEY UPDATE `notice_title`=VALUES(`notice_title`);

