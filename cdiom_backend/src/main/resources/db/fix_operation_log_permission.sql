-- 修复操作日志权限：确保只有系统管理员可以查看操作日志
-- 执行此脚本以从仓库管理员角色中移除操作日志查看权限

USE cdiom_db;

-- 查找操作日志查看权限的ID
SET @permission_id = (SELECT id FROM sys_permission WHERE permission_code = 'log:operation:view' LIMIT 1);

-- 如果权限存在，则从仓库管理员（角色ID=2）中移除该权限
DELETE FROM sys_role_permission 
WHERE role_id = 2 
  AND permission_id = @permission_id;

-- 验证结果：查询仓库管理员的权限列表，确认不包含操作日志权限
SELECT 
    r.role_name,
    p.permission_name,
    p.permission_code
FROM sys_role r
LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
LEFT JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id = 2
  AND p.permission_code = 'log:operation:view';

-- 如果上面的查询返回空结果，说明权限已成功移除

