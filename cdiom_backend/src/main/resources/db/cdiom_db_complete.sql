-- ============================================
-- CDIOM系统完整数据库初始化脚本
-- 数据库：cdiom_db
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci
-- 存储引擎：InnoDB
-- 版本：1.0.0
-- 创建时间：2024
-- ============================================

-- 删除数据库（如果存在，谨慎使用）
-- DROP DATABASE IF EXISTS cdiom_db;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS cdiom_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cdiom_db;

-- ============================================
-- 系统表（6张）
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
-- 权限表（3张）
-- ============================================

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限代码',
    `permission_type` TINYINT NOT NULL COMMENT '1-菜单/2-按钮/3-接口',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `is_required` TINYINT DEFAULT 0 COMMENT '0-否/1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_permission_type` (`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`),
    CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================
-- 业务表（9张）
-- ============================================

-- 供应商表
CREATE TABLE IF NOT EXISTS `supplier` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
    `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '地址',
    `credit_code` VARCHAR(100) DEFAULT NULL COMMENT '统一社会信用代码',
    `license_image` VARCHAR(500) DEFAULT NULL COMMENT '营业执照图片路径',
    `license_expiry_date` DATE DEFAULT NULL COMMENT '营业执照到期日期',
    `status` TINYINT DEFAULT 1 COMMENT '0-禁用/1-启用/2-待审核',
    `audit_status` TINYINT DEFAULT 0 COMMENT '0-待审核/1-已通过/2-已驳回',
    `audit_reason` VARCHAR(500) DEFAULT NULL COMMENT '审核理由',
    `audit_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_status` (`status`),
    KEY `idx_credit_code` (`credit_code`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_supplier_audit_by` FOREIGN KEY (`audit_by`) REFERENCES `sys_user`(`id`),
    CONSTRAINT `fk_supplier_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- 药品信息表
CREATE TABLE IF NOT EXISTS `drug_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `national_code` VARCHAR(50) NOT NULL COMMENT '国家本位码',
    `trace_code` VARCHAR(100) DEFAULT NULL COMMENT '药品追溯码',
    `product_code` VARCHAR(100) DEFAULT NULL COMMENT '商品码',
    `drug_name` VARCHAR(200) NOT NULL COMMENT '通用名称',
    `dosage_form` VARCHAR(50) DEFAULT NULL COMMENT '剂型',
    `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `approval_number` VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
    `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    `supplier_name` VARCHAR(200) DEFAULT NULL COMMENT '供应商名称',
    `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
    `expiry_date` DATE DEFAULT NULL COMMENT '有效期',
    `is_special` TINYINT DEFAULT 0 COMMENT '0-普通药品/1-特殊药品',
    `storage_requirement` VARCHAR(100) DEFAULT NULL COMMENT '存储要求',
    `storage_location` VARCHAR(200) DEFAULT NULL COMMENT '存储位置',
    `unit` VARCHAR(20) DEFAULT '盒' COMMENT '单位',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_national_code` (`national_code`),
    UNIQUE KEY `uk_trace_code` (`trace_code`),
    KEY `idx_product_code` (`product_code`),
    KEY `idx_drug_name` (`drug_name`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_is_special` (`is_special`),
    KEY `idx_expiry_date` (`expiry_date`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_drug_info_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier`(`id`),
    CONSTRAINT `fk_drug_info_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品信息表';

-- 库存表（按批次管理）
CREATE TABLE IF NOT EXISTS `inventory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `batch_number` VARCHAR(100) NOT NULL COMMENT '批次号',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `expiry_date` DATE NOT NULL COMMENT '有效期至',
    `storage_location` VARCHAR(200) DEFAULT NULL COMMENT '存储位置',
    `production_date` DATE DEFAULT NULL COMMENT '生产日期',
    `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_drug_batch` (`drug_id`, `batch_number`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_expiry_date` (`expiry_date`),
    KEY `idx_quantity` (`quantity`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_inventory_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info`(`id`),
    CONSTRAINT `chk_quantity_non_negative` CHECK (`quantity` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- 采购订单表
CREATE TABLE IF NOT EXISTS `purchase_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_number` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
    `purchaser_id` BIGINT NOT NULL COMMENT '采购员ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待确认/REJECTED-已拒绝/CONFIRMED-待发货/SHIPPED-已发货/RECEIVED-已入库/CANCELLED-已取消',
    `expected_delivery_date` DATE DEFAULT NULL COMMENT '预计交货日期',
    `logistics_number` VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    `ship_date` DATETIME DEFAULT NULL COMMENT '发货日期',
    `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '订单总金额',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝理由',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_purchaser_id` (`purchaser_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_purchase_order_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier`(`id`),
    CONSTRAINT `fk_purchase_order_purchaser` FOREIGN KEY (`purchaser_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单表';

-- 采购订单明细表
CREATE TABLE IF NOT EXISTS `purchase_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `quantity` INT NOT NULL COMMENT '采购数量',
    `unit_price` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '单价',
    `total_price` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '小计金额',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_purchase_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_purchase_order_item_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

-- 入库记录表
CREATE TABLE IF NOT EXISTS `inbound_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_number` VARCHAR(50) NOT NULL COMMENT '入库单号',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联采购订单ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `batch_number` VARCHAR(100) NOT NULL COMMENT '批次号',
    `quantity` INT NOT NULL COMMENT '入库数量',
    `expiry_date` DATE NOT NULL COMMENT '有效期至',
    `arrival_date` DATE DEFAULT NULL COMMENT '到货日期',
    `production_date` DATE DEFAULT NULL COMMENT '生产日期',
    `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    `delivery_note_number` VARCHAR(100) DEFAULT NULL COMMENT '随货同行单编号',
    `delivery_note_image` VARCHAR(500) DEFAULT NULL COMMENT '随货同行单图片路径',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `second_operator_id` BIGINT DEFAULT NULL COMMENT '第二操作人ID（特殊药品）',
    `status` VARCHAR(20) DEFAULT 'QUALIFIED' COMMENT 'QUALIFIED-合格/UNQUALIFIED-不合格',
    `expiry_check_status` VARCHAR(20) DEFAULT 'PASS' COMMENT 'PASS-通过/WARNING-不足180天需确认/FORCE-强制入库',
    `expiry_check_reason` VARCHAR(500) DEFAULT NULL COMMENT '效期校验说明',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_number` (`record_number`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_inbound_record_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_inbound_record_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info`(`id`),
    CONSTRAINT `fk_inbound_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user`(`id`),
    CONSTRAINT `fk_inbound_record_second_operator` FOREIGN KEY (`second_operator_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库记录表';

-- 出库申请表
CREATE TABLE IF NOT EXISTS `outbound_apply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `apply_number` VARCHAR(50) NOT NULL COMMENT '申领单号',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `department` VARCHAR(100) DEFAULT NULL COMMENT '所属科室',
    `purpose` VARCHAR(200) DEFAULT NULL COMMENT '用途',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待审批/APPROVED-已通过/REJECTED-已驳回/OUTBOUND-已出库/CANCELLED-已取消',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `second_approver_id` BIGINT DEFAULT NULL COMMENT '第二审批人ID（特殊药品）',
    `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回理由',
    `outbound_time` DATETIME DEFAULT NULL COMMENT '出库时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_apply_number` (`apply_number`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_outbound_apply_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `sys_user`(`id`),
    CONSTRAINT `fk_outbound_apply_approver` FOREIGN KEY (`approver_id`) REFERENCES `sys_user`(`id`),
    CONSTRAINT `fk_outbound_apply_second_approver` FOREIGN KEY (`second_approver_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库申请表';

-- 出库申请明细表
CREATE TABLE IF NOT EXISTS `outbound_apply_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `apply_id` BIGINT NOT NULL COMMENT '申请ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `batch_number` VARCHAR(100) DEFAULT NULL COMMENT '批次号',
    `quantity` INT NOT NULL COMMENT '申领数量',
    `actual_quantity` INT DEFAULT NULL COMMENT '实际出库数量',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_apply_id` (`apply_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_outbound_apply_item_apply` FOREIGN KEY (`apply_id`) REFERENCES `outbound_apply`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_outbound_apply_item_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库申请明细表';

-- 库存调整记录表
CREATE TABLE IF NOT EXISTS `inventory_adjustment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `adjustment_number` VARCHAR(50) NOT NULL COMMENT '调整单号',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `batch_number` VARCHAR(100) NOT NULL COMMENT '批次号',
    `adjustment_type` VARCHAR(20) NOT NULL COMMENT 'PROFIT-盘盈/LOSS-盘亏',
    `quantity_before` INT NOT NULL COMMENT '调整前数量',
    `quantity_after` INT NOT NULL COMMENT '调整后数量',
    `adjustment_quantity` INT NOT NULL COMMENT '调整数量',
    `adjustment_reason` VARCHAR(500) DEFAULT NULL COMMENT '调整原因',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `second_operator_id` BIGINT DEFAULT NULL COMMENT '第二操作人ID（特殊药品）',
    `adjustment_image` VARCHAR(500) DEFAULT NULL COMMENT '盘点记录照片路径',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_adjustment_number` (`adjustment_number`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_batch_number` (`batch_number`),
    KEY `idx_adjustment_type` (`adjustment_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_inventory_adjustment_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info`(`id`),
    CONSTRAINT `fk_inventory_adjustment_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user`(`id`),
    CONSTRAINT `fk_inventory_adjustment_second_operator` FOREIGN KEY (`second_operator_id`) REFERENCES `sys_user`(`id`),
    CONSTRAINT `chk_adjustment_quantity_non_negative` CHECK (`quantity_after` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存调整记录表';

-- ============================================
-- 扩展表（1张）
-- ============================================

-- 常用药品收藏表
CREATE TABLE IF NOT EXISTS `favorite_drug` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `drug_id` BIGINT NOT NULL COMMENT '药品ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_drug` (`user_id`, `drug_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_drug_id` (`drug_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_favorite_drug_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_drug_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用药品收藏表';

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
(1, 'admin', '17728770236', '$2a$10$7WcJvZ.NHjMA8fadvg6qPeGeHnW0nfzZK2uKLIeaYXN2Rm.dunUIO', 1, 1)
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

-- ============================================
-- 脚本执行完成
-- ============================================
-- 说明：
-- 1. 本脚本包含CDIOM系统的所有数据库表结构（共19张表）
-- 2. 所有表均使用utf8mb4字符集和utf8mb4_unicode_ci排序规则
-- 3. 所有表均使用InnoDB存储引擎
-- 4. 已包含基础初始化数据（角色、管理员用户、系统配置、通知公告）
-- 5. 外键约束已正确设置，确保数据完整性
-- 6. 索引已优化，提升查询性能
-- ============================================



