USE cdiom_db;

-- ============================================
-- 权限表
-- ============================================

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_type TINYINT NOT NULL COMMENT '1-菜单/2-按钮/3-接口',
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_required TINYINT DEFAULT 0 COMMENT '0-否/1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_parent_id (parent_id),
    KEY idx_permission_type (permission_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 业务表
-- ============================================

-- 供应商表
CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(50) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    address VARCHAR(500) DEFAULT NULL,
    credit_code VARCHAR(100) DEFAULT NULL,
    license_image VARCHAR(500) DEFAULT NULL,
    license_expiry_date DATE DEFAULT NULL,
    status TINYINT DEFAULT 1 COMMENT '0-禁用/1-启用/2-待审核',
    audit_status TINYINT DEFAULT 0 COMMENT '0-待审核/1-已通过/2-已驳回',
    audit_reason VARCHAR(500) DEFAULT NULL,
    audit_by BIGINT DEFAULT NULL,
    audit_time DATETIME DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_name (name),
    KEY idx_status (status),
    KEY idx_credit_code (credit_code),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_supplier_audit_by FOREIGN KEY (audit_by) REFERENCES sys_user(id),
    CONSTRAINT fk_supplier_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 药品信息表
CREATE TABLE IF NOT EXISTS drug_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    national_code VARCHAR(50) NOT NULL COMMENT '国家本位码',
    trace_code VARCHAR(100) DEFAULT NULL COMMENT '药品追溯码',
    product_code VARCHAR(100) DEFAULT NULL COMMENT '商品码',
    drug_name VARCHAR(200) NOT NULL COMMENT '通用名称',
    dosage_form VARCHAR(50) DEFAULT NULL COMMENT '剂型',
    specification VARCHAR(100) DEFAULT NULL COMMENT '规格',
    approval_number VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
    manufacturer VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    expiry_date DATE DEFAULT NULL COMMENT '有效期',
    is_special TINYINT DEFAULT 0 COMMENT '0-普通药品/1-特殊药品',
    storage_requirement VARCHAR(100) DEFAULT NULL COMMENT '存储要求',
    storage_location VARCHAR(200) DEFAULT NULL COMMENT '存储位置',
    unit VARCHAR(20) DEFAULT '盒' COMMENT '单位',
    description TEXT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_national_code (national_code),
    UNIQUE KEY uk_trace_code (trace_code),
    KEY idx_product_code (product_code),
    KEY idx_drug_name (drug_name),
    KEY idx_is_special (is_special),
    KEY idx_expiry_date (expiry_date),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_drug_info_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 注意：supplier_drug 表必须在 drug_info 表之后创建，因为 drug_info 表是外键引用
-- 供应商-药品关联表（中间表，支持多对多关系）
CREATE TABLE IF NOT EXISTS supplier_drug (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    drug_id BIGINT NOT NULL COMMENT '药品ID',
    unit_price DECIMAL(10, 2) DEFAULT NULL COMMENT '该供应商提供该药品的单价',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用/1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_drug (supplier_id, drug_id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_drug_id (drug_id),
    KEY idx_is_active (is_active),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_supplier_drug_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_drug_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_drug_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商-药品关联表';

-- 库存表
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drug_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL COMMENT '批次号',
    quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    expiry_date DATE NOT NULL COMMENT '有效期至',
    storage_location VARCHAR(200) DEFAULT NULL COMMENT '存储位置',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    manufacturer VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_drug_batch (drug_id, batch_number),
    KEY idx_drug_id (drug_id),
    KEY idx_batch_number (batch_number),
    KEY idx_expiry_date (expiry_date),
    KEY idx_quantity (quantity),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_inventory_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id),
    CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 采购订单表
CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL COMMENT '订单编号',
    supplier_id BIGINT NOT NULL,
    purchaser_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待确认/REJECTED-已拒绝/CONFIRMED-待发货/SHIPPED-已发货/RECEIVED-已入库/CANCELLED-已取消',
    expected_delivery_date DATE DEFAULT NULL COMMENT '预计交货日期',
    logistics_number VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    ship_date DATETIME DEFAULT NULL COMMENT '发货日期',
    total_amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '订单总金额',
    remark VARCHAR(500) DEFAULT NULL,
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝理由',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_number (order_number),
    KEY idx_supplier_id (supplier_id),
    KEY idx_purchaser_id (purchaser_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    CONSTRAINT fk_purchase_order_purchaser FOREIGN KEY (purchaser_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 采购订单明细表
CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    drug_id BIGINT NOT NULL,
    quantity INT NOT NULL COMMENT '采购数量',
    unit_price DECIMAL(10, 2) DEFAULT 0.00 COMMENT '单价',
    total_price DECIMAL(10, 2) DEFAULT 0.00 COMMENT '小计金额',
    remark VARCHAR(200) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_drug_id (drug_id),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_purchase_order_item_order FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_item_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 入库记录表
CREATE TABLE IF NOT EXISTS inbound_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    record_number VARCHAR(50) NOT NULL COMMENT '入库单号',
    order_id BIGINT DEFAULT NULL COMMENT '关联采购订单ID',
    drug_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL COMMENT '批次号',
    quantity INT NOT NULL COMMENT '入库数量',
    expiry_date DATE NOT NULL COMMENT '有效期至',
    arrival_date DATE DEFAULT NULL COMMENT '到货日期',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    manufacturer VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    delivery_note_number VARCHAR(100) DEFAULT NULL COMMENT '随货同行单编号',
    delivery_note_image VARCHAR(500) DEFAULT NULL COMMENT '随货同行单图片路径',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    second_operator_id BIGINT DEFAULT NULL COMMENT '第二操作人ID（特殊药品）',
    status VARCHAR(20) DEFAULT 'QUALIFIED' COMMENT 'QUALIFIED-合格/UNQUALIFIED-不合格',
    expiry_check_status VARCHAR(20) DEFAULT 'PASS' COMMENT 'PASS-通过/WARNING-不足180天需确认/FORCE-强制入库',
    expiry_check_reason VARCHAR(500) DEFAULT NULL COMMENT '效期校验说明',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_number (record_number),
    KEY idx_order_id (order_id),
    KEY idx_drug_id (drug_id),
    KEY idx_batch_number (batch_number),
    KEY idx_operator_id (operator_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_inbound_record_order FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE SET NULL,
    CONSTRAINT fk_inbound_record_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id),
    CONSTRAINT fk_inbound_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
    CONSTRAINT fk_inbound_record_second_operator FOREIGN KEY (second_operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 出库申请表
CREATE TABLE IF NOT EXISTS outbound_apply (
    id BIGINT NOT NULL AUTO_INCREMENT,
    apply_number VARCHAR(50) NOT NULL COMMENT '申领单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    department VARCHAR(100) DEFAULT NULL COMMENT '所属科室',
    purpose VARCHAR(200) DEFAULT NULL COMMENT '用途',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待审批/APPROVED-已通过/REJECTED-已驳回/OUTBOUND-已出库/CANCELLED-已取消',
    approver_id BIGINT DEFAULT NULL COMMENT '审批人ID',
    second_approver_id BIGINT DEFAULT NULL COMMENT '第二审批人ID（特殊药品）',
    approve_time DATETIME DEFAULT NULL COMMENT '审批时间',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回理由',
    outbound_time DATETIME DEFAULT NULL COMMENT '出库时间',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_apply_number (apply_number),
    KEY idx_applicant_id (applicant_id),
    KEY idx_approver_id (approver_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_outbound_apply_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
    CONSTRAINT fk_outbound_apply_approver FOREIGN KEY (approver_id) REFERENCES sys_user(id),
    CONSTRAINT fk_outbound_apply_second_approver FOREIGN KEY (second_approver_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 出库申请明细表
CREATE TABLE IF NOT EXISTS outbound_apply_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    apply_id BIGINT NOT NULL,
    drug_id BIGINT NOT NULL,
    batch_number VARCHAR(100) DEFAULT NULL COMMENT '批次号',
    quantity INT NOT NULL COMMENT '申领数量',
    actual_quantity INT DEFAULT NULL COMMENT '实际出库数量',
    remark VARCHAR(200) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_apply_id (apply_id),
    KEY idx_drug_id (drug_id),
    KEY idx_batch_number (batch_number),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_outbound_apply_item_apply FOREIGN KEY (apply_id) REFERENCES outbound_apply(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_apply_item_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 库存调整记录表
CREATE TABLE IF NOT EXISTS inventory_adjustment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    adjustment_number VARCHAR(50) NOT NULL COMMENT '调整单号',
    drug_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL COMMENT '批次号',
    adjustment_type VARCHAR(20) NOT NULL COMMENT 'PROFIT-盘盈/LOSS-盘亏',
    quantity_before INT NOT NULL COMMENT '调整前数量',
    quantity_after INT NOT NULL COMMENT '调整后数量',
    adjustment_quantity INT NOT NULL COMMENT '调整数量',
    adjustment_reason VARCHAR(500) DEFAULT NULL COMMENT '调整原因',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    second_operator_id BIGINT DEFAULT NULL COMMENT '第二操作人ID（特殊药品）',
    adjustment_image VARCHAR(500) DEFAULT NULL COMMENT '盘点记录照片路径',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_adjustment_number (adjustment_number),
    KEY idx_drug_id (drug_id),
    KEY idx_batch_number (batch_number),
    KEY idx_adjustment_type (adjustment_type),
    KEY idx_operator_id (operator_id),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_inventory_adjustment_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id),
    CONSTRAINT fk_inventory_adjustment_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
    CONSTRAINT fk_inventory_adjustment_second_operator FOREIGN KEY (second_operator_id) REFERENCES sys_user(id),
    CONSTRAINT chk_adjustment_quantity_non_negative CHECK (quantity_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 扩展表
-- ============================================

-- 常用药品收藏表
CREATE TABLE IF NOT EXISTS favorite_drug (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    drug_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_drug (user_id, drug_id),
    KEY idx_user_id (user_id),
    KEY idx_drug_id (drug_id),
    KEY idx_sort_order (sort_order),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_favorite_drug_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_drug_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;






