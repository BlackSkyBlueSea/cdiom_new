-- 用户权限「与用户管理勾选一致」模式：为 1 时有效权限仅以 sys_user_permission 为准（可少于角色默认），为 0 时为角色权限与直接权限并集（历史行为）
-- 可重复执行：若列已存在则跳过（若曾手动执行过裸 ALTER 并报 1060，说明列已在，无需再处理）

SET @db := DATABASE();
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'permission_customized'
);

SET @sql := IF(
  @exists = 0,
  'ALTER TABLE sys_user ADD COLUMN permission_customized TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=仅以sys_user_permission为有效权限清单，0=角色与直接权限并集'' AFTER role_id',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
