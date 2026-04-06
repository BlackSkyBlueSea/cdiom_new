-- 方案 A：到货批次头（随货单、到货时间挂在批次上，明细行 inbound_record.receipt_batch_id 关联）
-- 已有库升级执行一次

CREATE TABLE IF NOT EXISTS inbound_receipt_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_code VARCHAR(50) NOT NULL COMMENT '到货批次号',
    order_id BIGINT NOT NULL COMMENT '采购订单ID',
    delivery_note_number VARCHAR(100) NOT NULL COMMENT '随货同行单编号',
    arrival_time DATETIME NOT NULL COMMENT '本批到货时间',
    delivery_note_image VARCHAR(500) DEFAULT NULL COMMENT '随货同行单图片路径',
    operator_id BIGINT DEFAULT NULL COMMENT '登记人',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_receipt_batch_code (batch_code),
    KEY idx_receipt_batch_order (order_id),
    CONSTRAINT fk_inbound_receipt_batch_order FOREIGN KEY (order_id) REFERENCES purchase_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库到货批次头';

ALTER TABLE inbound_record
ADD COLUMN receipt_batch_id BIGINT DEFAULT NULL COMMENT '到货批次头ID' AFTER order_id,
ADD KEY idx_receipt_batch_id (receipt_batch_id),
ADD CONSTRAINT fk_inbound_record_receipt_batch FOREIGN KEY (receipt_batch_id) REFERENCES inbound_receipt_batch(id);
