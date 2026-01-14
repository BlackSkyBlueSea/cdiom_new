# 权限问题修复说明

## 发现的问题

1. **数据库权限数据缺失**
   - `sys_permission` 表可能没有权限数据
   - `sys_role_permission` 表可能没有角色权限关联数据
   - 导致非系统管理员用户无法访问任何需要权限的接口

2. **空指针风险**
   - 权限查询可能返回null或空列表
   - 缺少异常处理和日志记录

3. **异常处理不完善**
   - 权限检查时如果出现异常，没有适当的错误处理

## 已修复的问题

### 1. 权限服务增强 (`PermissionServiceImpl.java`)
- ✅ 添加了完整的异常处理和日志记录
- ✅ 添加了空值检查，防止空指针异常
- ✅ 添加了空集合检查，处理权限数据缺失的情况
- ✅ 系统管理员（roleId=1）自动拥有所有权限（通配符"*"）

### 2. 权限拦截器增强 (`PermissionInterceptor.java`)
- ✅ 添加了异常处理，防止权限检查异常导致系统崩溃
- ✅ 添加了日志记录，便于排查权限问题
- ✅ 添加了空值检查，处理权限代码为空的情况

### 3. 权限数据初始化脚本 (`init_permissions.sql`)
- ✅ 创建了完整的权限数据初始化脚本
- ✅ 包含所有系统权限的定义
- ✅ 包含各角色的权限分配

## 需要执行的步骤

### 步骤1：执行权限数据初始化脚本

```sql
-- 执行以下SQL脚本初始化权限数据
source cdiom_backend/src/main/resources/db/init_permissions.sql;
```

或者在MySQL客户端中执行：
```bash
mysql -u root -p cdiom_db < cdiom_backend/src/main/resources/db/init_permissions.sql
```

### 步骤2：验证权限数据

执行以下SQL验证权限数据是否正确：

```sql
-- 查看所有权限
SELECT id, permission_name, permission_code, permission_type 
FROM sys_permission 
ORDER BY sort_order;

-- 查看各角色的权限分配
SELECT 
    r.id as role_id,
    r.role_name,
    p.permission_name,
    p.permission_code
FROM sys_role r
LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
LEFT JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id IN (1, 2, 3, 4, 5)
ORDER BY r.id, p.sort_order;
```

### 步骤3：重启后端服务

执行权限数据初始化后，需要重启后端服务使权限配置生效。

## 权限配置说明

### 系统管理员（角色ID=1）
- **权限**：拥有所有权限（代码中通过roleId=1判断，返回通配符"*"）
- **菜单**：可以看到所有菜单项
- **功能**：可以访问所有API接口

### 仓库管理员（角色ID=2）
- **权限**：
  - `drug:view`, `drug:manage`, `drug:create`, `drug:update`, `drug:delete` - 药品管理
  - `notice:view` - 通知查看
- **菜单**：仪表盘、药品信息管理、通知公告
- **功能**：可以管理药品信息，查看通知公告

### 采购专员（角色ID=3）
- **权限**：
  - `notice:view` - 通知查看
- **菜单**：仪表盘、通知公告
- **功能**：只能查看通知公告

### 医护人员（角色ID=4）
- **权限**：
  - `notice:view` - 通知查看
- **菜单**：仪表盘、通知公告
- **功能**：只能查看通知公告

### 供应商（角色ID=5）
- **权限**：
  - `notice:view` - 通知查看
- **菜单**：仪表盘、通知公告
- **功能**：只能查看通知公告

## 常见问题排查

### 问题1：用户提示"权限不足"
**原因**：
- 数据库中没有该用户的权限数据
- 角色权限关联表没有配置

**解决方法**：
1. 执行权限数据初始化脚本
2. 检查用户角色是否正确
3. 检查 `sys_role_permission` 表中是否有该角色的权限关联

### 问题2：系统管理员无法访问某些接口
**原因**：
- 系统管理员判断逻辑可能有问题

**解决方法**：
1. 检查用户角色ID是否为1
2. 查看后端日志，确认权限检查过程
3. 确认 `PermissionServiceImpl.getPermissionCodesByRoleId()` 方法是否正确返回"*"

### 问题3：前端菜单显示但无法访问
**原因**：
- 前端菜单配置和数据库权限不一致

**解决方法**：
1. 检查前端 `Layout.jsx` 中的菜单角色配置
2. 检查数据库中的权限配置
3. 确保前后端权限代码一致

## 权限代码对照表

| 权限代码 | 权限名称 | 说明 |
|---------|---------|------|
| user:manage | 用户管理 | 用户管理相关操作 |
| user:view | 用户查看 | 查看用户列表 |
| user:create | 用户创建 | 创建新用户 |
| user:update | 用户更新 | 更新用户信息 |
| user:delete | 用户删除 | 删除用户 |
| role:manage | 角色管理 | 角色管理相关操作 |
| drug:view | 药品查看 | 查看药品列表 |
| drug:manage | 药品管理 | 药品管理相关操作 |
| drug:create | 药品创建 | 创建新药品 |
| drug:update | 药品更新 | 更新药品信息 |
| drug:delete | 药品删除 | 删除药品 |
| config:manage | 配置管理 | 系统配置管理 |
| notice:view | 通知查看 | 查看通知公告 |
| notice:manage | 通知管理 | 通知公告管理 |
| log:operation:view | 操作日志查看 | 查看操作日志（仅系统管理员） |
| log:login:view | 登录日志查看 | 查看登录日志（仅系统管理员） |

## 注意事项

1. **系统管理员特权**：系统管理员（roleId=1）在代码中自动拥有所有权限，不需要在数据库中配置
2. **权限数据一致性**：确保前后端权限代码保持一致
3. **权限缓存**：当前实现没有缓存，每次请求都会查询数据库，后续可以考虑添加缓存提高性能
4. **动态权限管理**：当前权限是静态配置的，后续可以实现通过管理界面动态分配权限

---

**最后更新**：2026年1月12日
