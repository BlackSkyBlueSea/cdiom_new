-- 特殊药品采购/临时入库：第二仓库管理员确认流程（已有库升级执行一次）
-- NONE=不适用 CONFIRMED=已入账 PENDING_SECOND=待第二人确认 REJECTED=已驳回 WITHDRAWN=已撤回 TIMEOUT=超时关闭

ALTER TABLE inbound_record
ADD COLUMN second_confirm_status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED' COMMENT '第二人确认流程状态' AFTER status,
ADD COLUMN second_confirm_time DATETIME NULL COMMENT '第二人确认时间' AFTER second_confirm_status,
ADD COLUMN second_confirm_deadline DATETIME NULL COMMENT '第二人确认截止时间' AFTER second_confirm_time,
ADD COLUMN second_reject_reason VARCHAR(500) NULL COMMENT '第二人驳回原因' AFTER second_confirm_deadline;

UPDATE inbound_record SET second_confirm_status = 'NONE' WHERE status = 'UNQUALIFIED';

ALTER TABLE inbound_record ADD INDEX idx_second_confirm (second_confirm_status, second_operator_id);
