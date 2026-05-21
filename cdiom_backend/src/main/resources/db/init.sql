-- =============================================================================
-- CDIOM 临床药品出入库管理系统 — 一体化数据库初始化脚本（完整表结构）
-- 数据库：cdiom_db | 字符集：utf8mb4 | 引擎：InnoDB
-- 合并来源：原 init.sql、init_business_tables.sql、add_user_permission_system.sql（建表+权限）、
--          create_price_agreement_tables.sql、create_supplier_approval_tables.sql，
--          以及迁移脚本中的列变更（supplier.remark、supplier_drug.current_agreement_id、
--          outbound_apply 代录与第二审批相关列、inbound_record 第二人确认与处置等 — 已并入 CREATE）。
-- 表数量：29 张（含 RBAC、业务、价格协议、供应商准入审批等）。
-- 可选后续脚本：init_super_admin.sql（超级管理员账号）、defense 目录下演练数据等。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS cdiom_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cdiom_db;

-- ============================================
-- 1. 系统核心表
-- ============================================

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

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `role_id` BIGINT DEFAULT NULL COMMENT '角色ID',
    `permission_customized` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=仅以sys_user_permission为有效权限，0=角色与直接权限并集',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用/1-正常',
    `lock_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
    `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `last_login_fail_time` DATETIME DEFAULT NULL COMMENT '最后登录失败时间',
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
-- 2. RBAC 与细粒度权限
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `permission_name` VARCHAR(100) NOT NULL,
    `permission_code` VARCHAR(100) NOT NULL,
    `permission_type` TINYINT NOT NULL COMMENT '1-菜单/2-按钮/3-接口',
    `parent_id` BIGINT DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `is_required` TINYINT DEFAULT 0 COMMENT '0-否/1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_permission_type` (`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`),
    CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS `sys_user_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_permission` (`user_id`, `permission_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_permission_id` (`permission_id`),
    CONSTRAINT `fk_user_permission_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户直接权限关联表';

-- ============================================
-- 3. 供应商、药品、价格协议
-- ============================================

CREATE TABLE IF NOT EXISTS `supplier` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL,
    `contact_person` VARCHAR(50) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `address` VARCHAR(500) DEFAULT NULL,
    `credit_code` VARCHAR(100) DEFAULT NULL,
    `license_image` VARCHAR(500) DEFAULT NULL,
    `license_expiry_date` DATE DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注/描述',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-禁用/1-启用（待审核由 audit_status 表示）',
    `audit_status` TINYINT DEFAULT 0 COMMENT '0-待审核/1-已通过/2-已驳回',
    `audit_reason` VARCHAR(500) DEFAULT NULL,
    `audit_by` BIGINT DEFAULT NULL,
    `audit_time` DATETIME DEFAULT NULL,
    `create_by` BIGINT DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_status` (`status`),
    KEY `idx_credit_code` (`credit_code`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_supplier_audit_by` FOREIGN KEY (`audit_by`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_supplier_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

CREATE TABLE IF NOT EXISTS `drug_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `national_code` VARCHAR(50) NOT NULL COMMENT '国家本位码',
    `trace_code` VARCHAR(100) DEFAULT NULL COMMENT '药品追溯码',
    `product_code` VARCHAR(100) DEFAULT NULL COMMENT '商品码',
    `drug_name` VARCHAR(200) NOT NULL COMMENT '通用名称',
    `dosage_form` VARCHAR(50) DEFAULT NULL COMMENT '剂型',
    `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `approval_number` VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
    `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    `expiry_date` DATE DEFAULT NULL COMMENT '有效期',
    `is_special` TINYINT DEFAULT 0 COMMENT '0-普通药品/1-特殊药品',
    `storage_requirement` VARCHAR(100) DEFAULT NULL COMMENT '存储要求',
    `storage_location` VARCHAR(200) DEFAULT NULL COMMENT '存储位置',
    `unit` VARCHAR(20) DEFAULT '盒' COMMENT '单位',
    `description` TEXT DEFAULT NULL,
    `create_by` BIGINT DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_national_code` (`national_code`),
    UNIQUE KEY `uk_trace_code` (`trace_code`),
    KEY `idx_product_code` (`product_code`),
    KEY `idx_drug_name` (`drug_name`),
    KEY `idx_is_special` (`is_special`),
    KEY `idx_expiry_date` (`expiry_date`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_drug_info_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品信息表';

CREATE TABLE IF NOT EXISTS `supplier_drug_agreement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `agreement_number` VARCHAR(100) DEFAULT NULL COMMENT '协议编号',
    `agreement_name` VARCHAR(200) DEFAULT NULL COMMENT '协议名称',
    `agreement_file_url` VARCHAR(500) DEFAULT NULL COMMENT '协议文件URL',
    `agreement_type` VARCHAR(20) DEFAULT 'PRICE' COMMENT '协议类型：PRICE/FRAMEWORK',
    `effective_date` DATE DEFAULT NULL COMMENT '生效日期',
    `expiry_date` DATE DEFAULT NULL COMMENT '到期日期',
    `unit_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '协议单价',
    `currency` VARCHAR(10) DEFAULT 'CNY' COMMENT '货币',
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_by` BIGINT DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_supplier_drug` (`supplier_id`, `drug_id`),
    KEY `idx_agreement_number` (`agreement_number`),
    KEY `idx_effective_date` (`effective_date`),
    KEY `idx_expiry_date` (`expiry_date`),
    CONSTRAINT `fk_agreement_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_agreement_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_agreement_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商-药品价格协议表';

CREATE TABLE IF NOT EXISTS `supplier_drug` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `unit_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '单价',
    `current_agreement_id` BIGINT DEFAULT NULL COMMENT '当前生效的协议ID',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用/1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplier_drug` (`supplier_id`, `drug_id`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_agreement_id` (`current_agreement_id`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_supplier_drug_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_supplier_drug_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_supplier_drug_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_supplier_drug_agreement` FOREIGN KEY (`current_agreement_id`) REFERENCES `supplier_drug_agreement` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商-药品关联表';

CREATE TABLE IF NOT EXISTS `supplier_drug_price_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_id` BIGINT NOT NULL,
    `drug_id` BIGINT NOT NULL,
    `agreement_id` BIGINT DEFAULT NULL,
    `price_before` DECIMAL(10, 2) DEFAULT NULL,
    `price_after` DECIMAL(10, 2) NOT NULL,
    `change_reason` VARCHAR(500) DEFAULT NULL,
    `change_type` VARCHAR(20) DEFAULT 'MANUAL',
    `operator_id` BIGINT NOT NULL,
    `operator_name` VARCHAR(50) DEFAULT NULL,
    `operation_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `ip_address` VARCHAR(50) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_supplier_drug` (`supplier_id`, `drug_id`),
    KEY `idx_agreement_id` (`agreement_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_time` (`operation_time`),
    CONSTRAINT `fk_history_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_history_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_history_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `supplier_drug_agreement` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_history_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商-药品价格历史';

-- ============================================
-- 4. 库存、采购、入库、出库、调整、收藏
-- ============================================

CREATE TABLE IF NOT EXISTS `inventory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `drug_id` BIGINT NOT NULL,
    `batch_number` VARCHAR(100) NOT NULL COMMENT '批次号',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `expiry_date` DATE NOT NULL COMMENT '有效期至',
    `storage_location` VARCHAR(200) DEFAULT NULL,
    `production_date` DATE DEFAULT NULL,
    `manufacturer` VARCHAR(200) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_drug_batch` (`drug_id`, `batch_number`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_expiry_date` (`expiry_date`),
    KEY `idx_quantity` (`quantity`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_inventory_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`),
    CONSTRAINT `chk_quantity_non_negative` CHECK (`quantity` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

CREATE TABLE IF NOT EXISTS `purchase_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_number` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `supplier_id` BIGINT NOT NULL,
    `purchaser_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/REJECTED/CONFIRMED/SHIPPED/RECEIVED/CANCELLED',
    `expected_delivery_date` DATE DEFAULT NULL,
    `logistics_number` VARCHAR(100) DEFAULT NULL,
    `ship_date` DATETIME DEFAULT NULL,
    `total_amount` DECIMAL(10, 2) DEFAULT 0.00,
    `remark` VARCHAR(500) DEFAULT NULL,
    `reject_reason` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_purchaser_id` (`purchaser_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_purchase_order_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`),
    CONSTRAINT `fk_purchase_order_purchaser` FOREIGN KEY (`purchaser_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单';

CREATE TABLE IF NOT EXISTS `purchase_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `drug_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `unit_price` DECIMAL(10, 2) DEFAULT 0.00,
    `total_price` DECIMAL(10, 2) DEFAULT 0.00,
    `remark` VARCHAR(200) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_purchase_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_purchase_order_item_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细';

CREATE TABLE IF NOT EXISTS `inbound_receipt_batch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `batch_code` VARCHAR(50) NOT NULL COMMENT '到货批次号',
    `order_id` BIGINT NOT NULL,
    `delivery_note_number` VARCHAR(100) NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `delivery_note_image` VARCHAR(500) DEFAULT NULL,
    `operator_id` BIGINT DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receipt_batch_code` (`batch_code`),
    KEY `idx_receipt_batch_order` (`order_id`),
    CONSTRAINT `fk_inbound_receipt_batch_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库到货批次头';

CREATE TABLE IF NOT EXISTS `inbound_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_number` VARCHAR(50) NOT NULL,
    `order_id` BIGINT DEFAULT NULL,
    `receipt_batch_id` BIGINT DEFAULT NULL,
    `drug_id` BIGINT NOT NULL,
    `batch_number` VARCHAR(100) NOT NULL,
    `quantity` INT NOT NULL,
    `expiry_date` DATE NOT NULL,
    `arrival_date` DATE DEFAULT NULL,
    `production_date` DATE DEFAULT NULL,
    `manufacturer` VARCHAR(200) DEFAULT NULL,
    `storage_location` VARCHAR(200) DEFAULT NULL,
    `delivery_note_number` VARCHAR(100) DEFAULT NULL,
    `delivery_note_image` VARCHAR(500) DEFAULT NULL,
    `operator_id` BIGINT NOT NULL,
    `second_operator_id` BIGINT DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'QUALIFIED',
    `second_confirm_status` VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    `second_confirm_time` DATETIME DEFAULT NULL,
    `second_confirm_deadline` DATETIME DEFAULT NULL,
    `second_reject_reason` VARCHAR(500) DEFAULT NULL,
    `expiry_check_status` VARCHAR(20) DEFAULT 'PASS',
    `expiry_check_reason` VARCHAR(500) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `disposition_code` VARCHAR(32) DEFAULT NULL,
    `disposition_remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_number` (`record_number`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_receipt_batch_id` (`receipt_batch_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_status` (`status`),
    KEY `idx_second_confirm` (`second_confirm_status`, `second_operator_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_disposition_code` (`disposition_code`),
    CONSTRAINT `fk_inbound_record_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_inbound_record_receipt_batch` FOREIGN KEY (`receipt_batch_id`) REFERENCES `inbound_receipt_batch` (`id`),
    CONSTRAINT `fk_inbound_record_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`),
    CONSTRAINT `fk_inbound_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_inbound_record_second_operator` FOREIGN KEY (`second_operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库记录';

CREATE TABLE IF NOT EXISTS `outbound_apply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `apply_number` VARCHAR(50) NOT NULL,
    `applicant_id` BIGINT NOT NULL,
    `proxy_registrar_id` BIGINT DEFAULT NULL,
    `department` VARCHAR(100) DEFAULT NULL,
    `purpose` VARCHAR(200) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PENDING_SECOND/APPROVED/REJECTED/OUTBOUND/CANCELLED',
    `approver_id` BIGINT DEFAULT NULL,
    `second_approver_id` BIGINT DEFAULT NULL,
    `approve_time` DATETIME DEFAULT NULL,
    `first_approve_time` DATETIME DEFAULT NULL,
    `reject_reason` VARCHAR(500) DEFAULT NULL,
    `reject_operator_id` BIGINT DEFAULT NULL,
    `outbound_time` DATETIME DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_apply_number` (`apply_number`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_proxy_registrar_id` (`proxy_registrar_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_outbound_apply_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_outbound_apply_proxy_registrar` FOREIGN KEY (`proxy_registrar_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_outbound_apply_approver` FOREIGN KEY (`approver_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_outbound_apply_second_approver` FOREIGN KEY (`second_approver_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_outbound_apply_reject_operator` FOREIGN KEY (`reject_operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库申请';

CREATE TABLE IF NOT EXISTS `outbound_apply_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `apply_id` BIGINT NOT NULL,
    `drug_id` BIGINT NOT NULL,
    `batch_number` VARCHAR(100) DEFAULT NULL,
    `quantity` INT NOT NULL,
    `actual_quantity` INT DEFAULT NULL,
    `remark` VARCHAR(200) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_apply_id` (`apply_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_outbound_apply_item_apply` FOREIGN KEY (`apply_id`) REFERENCES `outbound_apply` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_outbound_apply_item_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库申请明细';

CREATE TABLE IF NOT EXISTS `inventory_adjustment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `adjustment_number` VARCHAR(50) NOT NULL,
    `drug_id` BIGINT NOT NULL,
    `batch_number` VARCHAR(100) NOT NULL,
    `adjustment_type` VARCHAR(20) NOT NULL,
    `quantity_before` INT NOT NULL,
    `quantity_after` INT NOT NULL,
    `adjustment_quantity` INT NOT NULL,
    `adjustment_reason` VARCHAR(500) DEFAULT NULL,
    `operator_id` BIGINT NOT NULL,
    `second_operator_id` BIGINT DEFAULT NULL,
    `adjustment_image` VARCHAR(500) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_adjustment_number` (`adjustment_number`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_adjustment_type` (`adjustment_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_inventory_adjustment_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`),
    CONSTRAINT `fk_inventory_adjustment_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_inventory_adjustment_second_operator` FOREIGN KEY (`second_operator_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `chk_adjustment_quantity_non_negative` CHECK (`quantity_after` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存调整';

CREATE TABLE IF NOT EXISTS `favorite_drug` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `drug_id` BIGINT NOT NULL,
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_drug` (`user_id`, `drug_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_favorite_drug_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_drug_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用药品收藏';

-- ============================================
-- 5. 供应商准入审批扩展
-- ============================================

CREATE TABLE IF NOT EXISTS `supplier_approval_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_id` BIGINT DEFAULT NULL,
    `supplier_name` VARCHAR(200) NOT NULL,
    `contact_person` VARCHAR(50) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `address` VARCHAR(500) DEFAULT NULL,
    `credit_code` VARCHAR(50) DEFAULT NULL,
    `license_image` VARCHAR(500) DEFAULT NULL,
    `license_expiry_date` DATE DEFAULT NULL,
    `application_type` VARCHAR(20) DEFAULT 'NEW',
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `applicant_id` BIGINT NOT NULL,
    `applicant_name` VARCHAR(50) DEFAULT NULL,
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `quality_checker_id` BIGINT DEFAULT NULL,
    `quality_checker_name` VARCHAR(50) DEFAULT NULL,
    `quality_check_time` DATETIME DEFAULT NULL,
    `quality_check_result` VARCHAR(20) DEFAULT NULL,
    `quality_check_opinion` VARCHAR(500) DEFAULT NULL,
    `price_reviewer_id` BIGINT DEFAULT NULL,
    `price_reviewer_name` VARCHAR(50) DEFAULT NULL,
    `price_review_time` DATETIME DEFAULT NULL,
    `price_review_result` VARCHAR(20) DEFAULT NULL,
    `price_review_opinion` VARCHAR(500) DEFAULT NULL,
    `price_warning` VARCHAR(500) DEFAULT NULL,
    `final_approver_id` BIGINT DEFAULT NULL,
    `final_approver_name` VARCHAR(50) DEFAULT NULL,
    `final_approve_time` DATETIME DEFAULT NULL,
    `final_approve_result` VARCHAR(20) DEFAULT NULL,
    `final_approve_opinion` VARCHAR(500) DEFAULT NULL,
    `reject_reason` VARCHAR(500) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_status` (`status`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_apply_time` (`apply_time`),
    CONSTRAINT `fk_approval_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_approval_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_approval_quality_checker` FOREIGN KEY (`quality_checker_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_approval_price_reviewer` FOREIGN KEY (`price_reviewer_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_approval_final_approver` FOREIGN KEY (`final_approver_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商准入审批申请';

CREATE TABLE IF NOT EXISTS `supplier_approval_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT NOT NULL,
    `drug_id` BIGINT NOT NULL,
    `drug_name` VARCHAR(200) DEFAULT NULL,
    `proposed_price` DECIMAL(10, 2) NOT NULL,
    `reference_price` DECIMAL(10, 2) DEFAULT NULL,
    `price_difference_rate` DECIMAL(5, 2) DEFAULT NULL,
    `price_warning_level` VARCHAR(20) DEFAULT NULL,
    `price_difference_reason` VARCHAR(500) DEFAULT NULL,
    `agreement_file_url` VARCHAR(500) DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_drug_id` (`drug_id`),
    CONSTRAINT `fk_approval_item_application` FOREIGN KEY (`application_id`) REFERENCES `supplier_approval_application` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_approval_item_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商准入审批明细';

CREATE TABLE IF NOT EXISTS `supplier_blacklist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `supplier_id` BIGINT DEFAULT NULL,
    `supplier_name` VARCHAR(200) NOT NULL,
    `credit_code` VARCHAR(50) DEFAULT NULL,
    `blacklist_reason` VARCHAR(500) NOT NULL,
    `blacklist_type` VARCHAR(20) DEFAULT 'FULL',
    `effective_date` DATE DEFAULT NULL,
    `expiry_date` DATE DEFAULT NULL,
    `creator_id` BIGINT NOT NULL,
    `creator_name` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_supplier_name` (`supplier_name`),
    KEY `idx_credit_code` (`credit_code`),
    KEY `idx_effective_date` (`effective_date`),
    KEY `idx_expiry_date` (`expiry_date`),
    CONSTRAINT `fk_blacklist_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_blacklist_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商黑名单';

CREATE TABLE IF NOT EXISTS `price_warning_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `drug_id` BIGINT DEFAULT NULL,
    `drug_name` VARCHAR(200) DEFAULT NULL,
    `reference_price_type` VARCHAR(20) DEFAULT 'MARKET',
    `reference_price` DECIMAL(10, 2) DEFAULT NULL,
    `warning_threshold` DECIMAL(5, 2) DEFAULT 10.00,
    `critical_threshold` DECIMAL(5, 2) DEFAULT 20.00,
    `is_active` TINYINT DEFAULT 1,
    `creator_id` BIGINT DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_is_active` (`is_active`),
    CONSTRAINT `fk_price_warning_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_price_warning_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格预警配置';

CREATE TABLE IF NOT EXISTS `supplier_approval_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT NOT NULL,
    `step_name` VARCHAR(50) NOT NULL,
    `operator_id` BIGINT NOT NULL,
    `operator_name` VARCHAR(50) DEFAULT NULL,
    `operator_role` VARCHAR(50) DEFAULT NULL,
    `operation_type` VARCHAR(20) NOT NULL,
    `operation_result` VARCHAR(20) DEFAULT NULL,
    `operation_opinion` VARCHAR(500) DEFAULT NULL,
    `ip_address` VARCHAR(50) DEFAULT NULL,
    `operation_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_time` (`operation_time`),
    CONSTRAINT `fk_approval_log_application` FOREIGN KEY (`application_id`) REFERENCES `supplier_approval_application` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_approval_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商审批流程日志';

-- ============================================
-- 6. 基础种子数据（角色、管理员、配置、公告）
-- ============================================

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`) VALUES
(1, '系统管理员', 'SUPER_ADMIN', '系统管理员，拥有所有权限', 1),
(2, '仓库管理员', 'WAREHOUSE_ADMIN', '仓库管理员，负责药品入库、出库、库存管理', 1),
(3, '采购专员', 'PURCHASER', '采购专员，负责供应商管理和采购订单管理', 1),
(4, '医护人员', 'MEDICAL_STAFF', '医护人员，负责药品申领', 1),
(5, '供应商', 'SUPPLIER', '供应商，负责订单管理', 1)
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`);

INSERT INTO `sys_user` (`id`, `username`, `phone`, `password`, `role_id`, `status`) VALUES
(1, 'admin', '17728770236', '$2a$10$miCOohBEJc0JYzL7OLFgQOTUQETRofEoR46sXfOmiG7MeBzyblhHm', 1, 1)
ON DUPLICATE KEY UPDATE `username`=VALUES(`username`);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`) VALUES
('近效期预警阈值（天）', 'expiry_warning_days', '180', 2, '药品有效期至当前日期的天数差，小于等于此值将预警'),
('红色预警阈值（天）', 'expiry_critical_days', '90', 2, '药品有效期至当前日期的天数差，小于等于此值将红色预警'),
('日志保留期（年）', 'log_retention_years', '5', 1, '操作日志和登录日志的保留年限'),
('JWT过期时间（毫秒）', 'jwt_expiration', '28800000', 1, 'JWT Token的有效期，默认8小时')
ON DUPLICATE KEY UPDATE `config_value`=VALUES(`config_value`);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT '登录失败次数阈值', 'login.fail.threshold', '5', 1, '时间窗口内连续登录失败次数上限，达到此值将触发账号锁定', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'login.fail.threshold' AND `deleted` = 0);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT '登录失败时间窗口', 'login.fail.time.window', '10', 1, '判定暴力破解的时间间隔（单位：分钟），窗口内失败才累加次数', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'login.fail.time.window' AND `deleted` = 0);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT '账号锁定时长', 'login.lock.duration', '1', 1, '账号锁定持续时间（单位：小时），触发锁定后禁止登录的时长', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'login.lock.duration' AND `deleted` = 0);

INSERT INTO `sys_notice` (`notice_title`, `notice_type`, `notice_content`, `status`) VALUES
('欢迎使用CDIOM系统', 1, '欢迎使用临床药品出入库管理系统！', 1),
('系统使用说明', 2, '请各用户按照系统规范操作，确保数据准确性。', 1)
ON DUPLICATE KEY UPDATE `notice_title`=VALUES(`notice_title`);

-- ============================================
-- 7. 权限定义与角色授权（合并 init_permissions.sql + add_user_permission_system.sql 核心部分）
-- ============================================

INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `sort_order`) VALUES
('用户管理', 'user:manage', 3, 0, 1),
('用户查看', 'user:view', 3, 0, 2),
('用户创建', 'user:create', 3, 0, 3),
('用户更新', 'user:update', 3, 0, 4),
('用户删除', 'user:delete', 3, 0, 5),
('角色管理', 'role:manage', 3, 0, 10),
('角色查看', 'role:view', 3, 0, 11),
('角色创建', 'role:create', 3, 0, 12),
('角色更新', 'role:update', 3, 0, 13),
('角色删除', 'role:delete', 3, 0, 14),
('药品查看', 'drug:view', 3, 0, 20),
('药品管理', 'drug:manage', 3, 0, 21),
('药品创建', 'drug:create', 3, 0, 22),
('药品更新', 'drug:update', 3, 0, 23),
('药品删除', 'drug:delete', 3, 0, 24),
('供应商审核', 'supplier:audit', 3, 0, 25),
('配置管理', 'config:manage', 3, 0, 30),
('配置查看', 'config:view', 3, 0, 31),
('配置创建', 'config:create', 3, 0, 32),
('配置更新', 'config:update', 3, 0, 33),
('配置删除', 'config:delete', 3, 0, 34),
('通知查看', 'notice:view', 3, 0, 40),
('通知管理', 'notice:manage', 3, 0, 41),
('通知创建', 'notice:create', 3, 0, 42),
('通知更新', 'notice:update', 3, 0, 43),
('通知删除', 'notice:delete', 3, 0, 44),
('操作日志查看', 'log:operation:view', 3, 0, 50),
('登录日志查看', 'log:login:view', 3, 0, 51),
('出库查看', 'outbound:view', 3, 0, 60),
('出库申领', 'outbound:apply', 3, 0, 61),
('出库审核', 'outbound:approve', 3, 0, 62),
('出库执行', 'outbound:execute', 3, 0, 63),
('特殊药品审核', 'outbound:approve:special', 3, 0, 64),
('出库驳回', 'outbound:reject', 3, 0, 65),
('出库代录', 'outbound:apply:on-behalf', 3, 0, 66),
('入库查看', 'inbound:view', 3, 0, 70),
('入库创建', 'inbound:create', 3, 0, 71),
('入库审核', 'inbound:approve', 3, 0, 72),
('入库执行', 'inbound:execute', 3, 0, 73),
('库存查看', 'inventory:view', 3, 0, 80),
('库存调整', 'inventory:adjust', 3, 0, 81),
('库存调整审核', 'inventory:adjust:approve', 3, 0, 82),
('采购订单查看', 'purchase:view', 3, 0, 90),
('采购订单创建', 'purchase:create', 3, 0, 91),
('采购订单审核', 'purchase:approve', 3, 0, 92),
('采购订单执行', 'purchase:execute', 3, 0, 93)
ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`
WHERE `permission_code` IN (
    'user:manage', 'user:view', 'user:create', 'user:update', 'user:delete',
    'role:manage', 'role:view', 'role:create', 'role:update', 'role:delete',
    'config:manage', 'config:view', 'config:create', 'config:update', 'config:delete',
    'notice:view', 'notice:manage', 'notice:create', 'notice:update', 'notice:delete',
    'log:operation:view', 'log:login:view'
)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`), `permission_id`=VALUES(`permission_id`);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission`
WHERE `permission_code` IN (
    'drug:view', 'drug:manage', 'drug:create', 'drug:update', 'drug:delete',
    'supplier:audit',
    'outbound:view', 'outbound:approve', 'outbound:approve:special', 'outbound:execute', 'outbound:reject', 'outbound:apply:on-behalf',
    'inbound:view', 'inbound:create', 'inbound:approve', 'inbound:execute',
    'inventory:view', 'inventory:adjust',
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`), `permission_id`=VALUES(`permission_id`);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 3, `id` FROM `sys_permission`
WHERE `permission_code` IN (
    'drug:view', 'drug:manage',
    'purchase:view', 'purchase:create', 'purchase:approve', 'purchase:execute',
    'notice:view', 'notice:create'
)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`), `permission_id`=VALUES(`permission_id`);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 4, `id` FROM `sys_permission`
WHERE `permission_code` IN ('outbound:view', 'outbound:apply', 'notice:view', 'notice:create')
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`), `permission_id`=VALUES(`permission_id`);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 5, `id` FROM `sys_permission`
WHERE `permission_code` IN ('purchase:view', 'notice:view', 'notice:create')
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`), `permission_id`=VALUES(`permission_id`);

-- 价格预警全局默认（与 create_supplier_approval_tables.sql 一致，修正列顺序）
INSERT INTO `price_warning_config` (`drug_id`, `reference_price_type`, `reference_price`, `warning_threshold`, `critical_threshold`, `is_active`, `creator_id`) VALUES
(NULL, 'MARKET', NULL, 10.00, 20.00, 1, NULL)
ON DUPLICATE KEY UPDATE `warning_threshold`=VALUES(`warning_threshold`), `critical_threshold`=VALUES(`critical_threshold`);

-- =============================================================================
-- 脚本结束
-- 说明：若需超级管理员账号（role_id=6）等，请继续执行 init_super_admin.sql（与本脚本兼容）。
-- =============================================================================
