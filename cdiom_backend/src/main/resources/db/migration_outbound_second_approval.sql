-- 特殊药品出库：第一审批通过后进入 PENDING_SECOND，第二审批人本人确认后为 APPROVED
-- 执行前请备份。若列已存在会报错，可忽略对应语句。

ALTER TABLE outbound_apply
  MODIFY COLUMN status VARCHAR(20) DEFAULT 'PENDING'
  COMMENT 'PENDING-待审批/PENDING_SECOND-待第二审批/APPROVED-已通过/REJECTED-已驳回/OUTBOUND-已出库/CANCELLED-已取消';

ALTER TABLE outbound_apply
  ADD COLUMN first_approve_time DATETIME DEFAULT NULL COMMENT '第一审批通过时间（特殊药品进入待第二审批时）' AFTER approve_time;

ALTER TABLE outbound_apply
  ADD COLUMN reject_operator_id BIGINT DEFAULT NULL COMMENT '驳回操作人（待第二审批时由第二人驳回，保留 approver_id 为第一审批人）' AFTER reject_reason;
