-- 不合格入库：处置意向（最简枚举，后续可扩展退货单/销毁单等）
ALTER TABLE inbound_record
ADD COLUMN disposition_code VARCHAR(32) NULL COMMENT '不合格处置意向：PENDING/RETURN_SUPPLIER/DESTROY/HOLD/OTHER' AFTER remark,
ADD COLUMN disposition_remark VARCHAR(500) NULL COMMENT '处置补充说明' AFTER disposition_code;

ALTER TABLE inbound_record ADD INDEX idx_disposition_code (disposition_code);
