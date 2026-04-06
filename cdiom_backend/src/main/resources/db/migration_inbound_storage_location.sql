-- 入库明细：记录本次入库指定的存储位置（合格入库必填；入账时写入 inventory）
ALTER TABLE inbound_record
    ADD COLUMN storage_location VARCHAR(200) DEFAULT NULL COMMENT '入库指定存储位置' AFTER manufacturer;
