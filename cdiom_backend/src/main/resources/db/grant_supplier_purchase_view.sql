-- 供应商角色（role_id=5）绑定 purchase:view（与供应商工作台、采购订单等 @RequiresPermission 一致）
-- 可重复执行：已存在 (role_id, permission_id) 时不插入，避免 INSERT IGNORE 在部分客户端产生 1062 告警

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 5, p.id
FROM sys_permission p
WHERE p.permission_code = 'purchase:view'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.role_id = 5
      AND rp.permission_id = p.id
  );
