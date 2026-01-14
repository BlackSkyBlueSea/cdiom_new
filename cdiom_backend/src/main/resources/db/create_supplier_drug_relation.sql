-- ============================================
-- 供应商-药品关联表创建脚本
-- 用于支持供应商和药品的多对多关系
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 创建供应商-药品关联表（中间表）
-- ============================================
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

-- ============================================
-- 2. 迁移现有数据（如果有的话）
-- 将 drug_info 表中的 supplier_id 数据迁移到中间表
-- ============================================
INSERT INTO supplier_drug (supplier_id, drug_id, create_by, create_time)
SELECT DISTINCT 
    di.supplier_id,
    di.id AS drug_id,
    di.create_by,
    di.create_time
FROM drug_info di
WHERE di.supplier_id IS NOT NULL
  AND di.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM supplier_drug sd 
    WHERE sd.supplier_id = di.supplier_id 
      AND sd.drug_id = di.id
  );

-- ============================================
-- 3. 更新 drug_info 表
-- 移除 supplier_id 和 supplier_name 字段
-- 注意：如果表已存在且没有这些字段，执行这些语句会报错，可以忽略
-- ============================================

-- 先删除外键约束（如果存在）
-- 如果外键不存在，执行会报错，可以手动检查后执行
ALTER TABLE drug_info DROP FOREIGN KEY fk_drug_info_supplier;

-- 删除索引（如果存在）
ALTER TABLE drug_info DROP INDEX idx_supplier_id;

-- 移除 supplier_id 字段
-- 如果字段不存在，执行会报错，可以手动检查后执行
ALTER TABLE drug_info DROP COLUMN supplier_id;

-- 移除 supplier_name 字段（不再需要，因为一个药品可以有多个供应商，通过 supplier_drug 中间表关联）
-- 如果字段不存在，执行会报错，可以手动检查后执行
ALTER TABLE drug_info DROP COLUMN supplier_name;

-- ============================================
-- 4. 验证数据
-- ============================================
-- 查看中间表数据
SELECT 
    sd.id,
    s.name AS supplier_name,
    di.drug_name,
    di.specification,
    sd.unit_price,
    sd.is_active,
    sd.create_time
FROM supplier_drug sd
JOIN supplier s ON sd.supplier_id = s.id
JOIN drug_info di ON sd.drug_id = di.id
WHERE sd.deleted = 0
ORDER BY sd.create_time DESC
LIMIT 10;

-- 查看每个供应商提供的药品数量
SELECT 
    s.name AS supplier_name,
    COUNT(sd.id) AS drug_count
FROM supplier s
LEFT JOIN supplier_drug sd ON s.id = sd.supplier_id AND sd.deleted = 0
WHERE s.deleted = 0
GROUP BY s.id, s.name
ORDER BY drug_count DESC;

-- 查看每个药品的供应商数量
SELECT 
    di.drug_name,
    di.specification,
    COUNT(sd.id) AS supplier_count
FROM drug_info di
LEFT JOIN supplier_drug sd ON di.id = sd.drug_id AND sd.deleted = 0
WHERE di.deleted = 0
GROUP BY di.id, di.drug_name, di.specification
HAVING supplier_count > 0
ORDER BY supplier_count DESC;

