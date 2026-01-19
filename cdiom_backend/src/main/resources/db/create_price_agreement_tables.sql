-- ============================================
-- 价格协议和价格历史表创建脚本
-- 用于确保价格与协议一致性，记录价格变更历史
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 创建价格协议表
-- 用于存储供应商-药品价格协议文件
-- ============================================
CREATE TABLE IF NOT EXISTS supplier_drug_agreement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    drug_id BIGINT NOT NULL COMMENT '药品ID',
    agreement_number VARCHAR(100) DEFAULT NULL COMMENT '协议编号',
    agreement_name VARCHAR(200) DEFAULT NULL COMMENT '协议名称',
    agreement_file_url VARCHAR(500) DEFAULT NULL COMMENT '协议文件URL（电子版或扫描件）',
    agreement_type VARCHAR(20) DEFAULT 'PRICE' COMMENT '协议类型：PRICE-价格协议/FRAMEWORK-框架协议',
    effective_date DATE DEFAULT NULL COMMENT '生效日期',
    expiry_date DATE DEFAULT NULL COMMENT '到期日期',
    unit_price DECIMAL(10, 2) DEFAULT NULL COMMENT '协议约定的单价',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币单位：CNY-人民币',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (id),
    KEY idx_supplier_drug (supplier_id, drug_id),
    KEY idx_agreement_number (agreement_number),
    KEY idx_effective_date (effective_date),
    KEY idx_expiry_date (expiry_date),
    CONSTRAINT fk_agreement_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE,
    CONSTRAINT fk_agreement_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_agreement_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商-药品价格协议表';

-- ============================================
-- 2. 创建价格历史记录表
-- 用于记录每次价格变更的完整历史
-- ============================================
CREATE TABLE IF NOT EXISTS supplier_drug_price_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    drug_id BIGINT NOT NULL COMMENT '药品ID',
    agreement_id BIGINT DEFAULT NULL COMMENT '关联的协议ID',
    price_before DECIMAL(10, 2) DEFAULT NULL COMMENT '变更前价格',
    price_after DECIMAL(10, 2) NOT NULL COMMENT '变更后价格',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    change_type VARCHAR(20) DEFAULT 'MANUAL' COMMENT '变更类型：MANUAL-手动修改/AGREEMENT-协议更新/AUTO-自动调整',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    operation_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_supplier_drug (supplier_id, drug_id),
    KEY idx_agreement_id (agreement_id),
    KEY idx_operator_id (operator_id),
    KEY idx_operation_time (operation_time),
    CONSTRAINT fk_history_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_agreement FOREIGN KEY (agreement_id) REFERENCES supplier_drug_agreement(id) ON DELETE SET NULL,
    CONSTRAINT fk_history_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商-药品价格历史记录表';

-- ============================================
-- 3. 修改supplier_drug表，增加协议关联字段
-- ============================================
ALTER TABLE supplier_drug 
ADD COLUMN current_agreement_id BIGINT DEFAULT NULL COMMENT '当前生效的协议ID' AFTER unit_price,
ADD KEY idx_agreement_id (current_agreement_id),
ADD CONSTRAINT fk_supplier_drug_agreement FOREIGN KEY (current_agreement_id) REFERENCES supplier_drug_agreement(id) ON DELETE SET NULL;

