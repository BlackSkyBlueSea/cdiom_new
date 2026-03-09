-- 供应商「状态」与「审核状态」语义分离
-- 状态(status)仅表示：0-禁用/1-启用；待审核由审核状态(audit_status)表示，避免重复。
-- 执行前请备份 supplier 表。

-- 1. 迁移已有数据：将 status=2(待审核) 按 audit_status 转为 0 或 1
UPDATE supplier
SET status = CASE
    WHEN audit_status = 1 THEN 1   -- 已通过 -> 启用
    ELSE 0                         -- 待审核/已驳回 -> 禁用
END
WHERE status = 2;

-- 2. 更新字段定义与注释（MySQL 5.7+）
ALTER TABLE supplier
MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '状态：0-禁用/1-启用（待审核由 audit_status 表示）';
