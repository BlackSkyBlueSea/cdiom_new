# 细粒度权限系统说明

## 概述

系统已升级为细粒度权限标签系统，支持：
- 用户直接拥有权限（不限于角色）
- 同一用户可拥有多个权限标签
- 同一角色内不同用户权限可差异化
- 特殊药品申请人和审核人不能是同一人的验证
- 符合RBAC扩展模型（混合RBAC：角色权限 + 用户直接权限）

## 数据库结构

### 1. 用户权限关联表

```sql
CREATE TABLE `sys_user_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_permission` (`user_id`, `permission_id`)
)
```

### 2. 细粒度权限定义

#### 出库管理权限
- `outbound:view` - 出库查看
- `outbound:apply` - 出库申领（医护人员）
- `outbound:approve` - 出库审核（仓库管理员）
- `outbound:approve:special` - 特殊药品审核（可分配给医护人员）
- `outbound:execute` - 出库执行（仓库管理员）
- `outbound:reject` - 出库驳回

#### 入库管理权限
- `inbound:view` - 入库查看
- `inbound:create` - 入库创建
- `inbound:approve` - 入库审核
- `inbound:execute` - 入库执行

#### 库存管理权限
- `inventory:view` - 库存查看
- `inventory:adjust` - 库存调整
- `inventory:adjust:approve` - 库存调整审核

## 权限查询逻辑

系统按以下顺序查询用户权限：
1. **角色权限**：从 `sys_role_permission` 表查询角色拥有的权限
2. **用户直接权限**：从 `sys_user_permission` 表查询用户直接拥有的权限
3. **合并结果**：将两种权限合并，去重后返回

## 使用示例

### 为医护人员添加特殊药品审核权限

```sql
-- 假设用户ID为10的医护人员需要特殊药品审核权限
INSERT INTO sys_user_permission (user_id, permission_id) 
SELECT 10, id FROM sys_permission WHERE permission_code = 'outbound:approve:special';
```

### 查询用户的所有权限

```sql
SELECT 
    u.id AS user_id,
    u.username,
    p.permission_code,
    CASE 
        WHEN up.id IS NOT NULL THEN 'user'
        ELSE 'role'
    END AS permission_source
FROM sys_user u
LEFT JOIN sys_role_permission rp ON u.role_id = rp.role_id
LEFT JOIN sys_permission p ON rp.permission_id = p.id
LEFT JOIN sys_user_permission up ON u.id = up.user_id AND up.permission_id = p.id
WHERE u.id = 10
ORDER BY permission_source, permission_code;
```

## 前端使用

### 1. 权限检查

```javascript
import { hasPermission, PERMISSIONS } from '../utils/permission'

// 检查单个权限
if (hasPermission(PERMISSIONS.OUTBOUND_APPLY)) {
  // 显示申领按钮
}

// 检查多个权限（任意一个）
if (hasPermission([PERMISSIONS.OUTBOUND_APPROVE, PERMISSIONS.OUTBOUND_APPROVE_SPECIAL])) {
  // 显示审核按钮
}
```

### 2. 获取用户权限

系统会在登录时自动获取用户权限并缓存。也可以手动获取：

```javascript
import { fetchUserPermissions } from '../utils/permission'

const permissions = await fetchUserPermissions()
```

## 后端API

### 获取当前用户权限

- **接口**: `GET /api/v1/auth/permissions`
- **权限**: 无需权限（已登录即可）
- **返回**: 权限代码集合

```json
{
  "code": 200,
  "data": [
    "outbound:view",
    "outbound:apply",
    "outbound:approve:special"
  ]
}
```

## 业务规则

### 1. 出库申请权限控制

- **申领权限** (`outbound:apply`)：只有拥有此权限的用户可以创建出库申请
- **审核权限** (`outbound:approve` 或 `outbound:approve:special`)：只有拥有此权限的用户可以审核出库申请
- **执行权限** (`outbound:execute`)：只有拥有此权限的用户可以执行出库

### 2. 特殊药品双人审批

- 如果申请包含特殊药品，必须指定第二审批人
- 第二审批人必须拥有 `outbound:approve:special` 权限
- **验证规则**：
  - 申请人和第一审批人不能是同一人
  - 申请人和第二审批人不能是同一人
  - 第一审批人和第二审批人不能是同一人

### 3. 权限继承

- 用户权限 = 角色权限 + 用户直接权限
- 如果用户拥有通配符权限 (`*`)，则拥有所有权限（超级管理员）

## 初始化脚本

### 执行顺序

**首次部署**：
```sql
-- 1. 执行基础权限初始化（如果还没有执行过）
source cdiom_backend/src/main/resources/db/init_permissions.sql

-- 2. 执行细粒度权限系统升级
source cdiom_backend/src/main/resources/db/add_user_permission_system.sql
```

**更新权限**（如果已执行过基础脚本）：
```sql
-- 直接执行完整权限更新
source cdiom_backend/src/main/resources/db/update_role_permissions_complete.sql
```

## 各角色完整权限分配

### 系统管理员（roleId=1）
**职责**：系统配置和维护，不涉及业务操作

**权限**：
- `user:manage`, `user:view`, `user:create`, `user:update`, `user:delete`
- `role:manage`, `role:view`, `role:create`, `role:update`, `role:delete`
- `config:manage`, `config:view`, `config:create`, `config:update`, `config:delete`
- `notice:view`, `notice:manage`, `notice:create`, `notice:update`, `notice:delete`
- `log:operation:view`, `log:login:view`

### 仓库管理员（roleId=2）
**职责**：药品入库、出库审核执行、库存管理

**权限**：
- **药品管理**：`drug:view`, `drug:manage`, `drug:create`, `drug:update`, `drug:delete`
- **供应商审核**：`supplier:audit`
- **出库管理**：`outbound:view`, `outbound:approve`, `outbound:execute`, `outbound:reject`
- **入库管理**：`inbound:view`, `inbound:create`, `inbound:approve`, `inbound:execute`
- **库存管理**：`inventory:view`, `inventory:adjust`
- **通知**：`notice:view`, `notice:create`

**注意**：不拥有 `outbound:apply`（不能申领）和 `outbound:approve:special`（特殊药品审核需单独分配）

### 采购专员（roleId=3）
**职责**：供应商管理、采购订单管理

**权限**：
- **药品查看**：`drug:view`（查看供应商列表）
- **供应商管理**：`drug:manage`（创建、更新供应商，不含删除和审核）
- **采购订单**：`purchase:view`, `purchase:create`, `purchase:approve`, `purchase:execute`
- **通知**：`notice:view`, `notice:create`

### 医护人员（roleId=4）
**职责**：药品申领

**权限**：
- **出库管理**：`outbound:view`, `outbound:apply`
- **通知**：`notice:view`, `notice:create`

**扩展权限**（通过用户直接权限分配）：
- 部分医护人员可拥有：`outbound:approve:special`（特殊药品审核）

### 供应商（roleId=5）
**职责**：查看和管理自己的订单

**权限**：
- **采购订单查看**：`purchase:view`（仅查看自己的订单）
- **通知**：`notice:view`, `notice:create`

### 超级管理员（roleId=6）
**职责**：系统最高权限

**权限**：
- 所有权限（通配符 `*`）

## 配置示例

### 场景1：医护人员同时拥有申领和特殊药品审核权限

```sql
-- 用户ID=10是医护人员（roleId=4），默认只有 outbound:view 和 outbound:apply
-- 添加特殊药品审核权限
INSERT INTO sys_user_permission (user_id, permission_id) 
SELECT 10, id FROM sys_permission WHERE permission_code = 'outbound:approve:special';
```

### 场景2：仓库管理员只负责审核，不负责申领

```sql
-- 仓库管理员（roleId=2）默认有 outbound:approve 和 outbound:execute
-- 角色权限中不包含 outbound:apply，所以默认不能申领
-- 如果某个仓库管理员需要申领权限，可以单独添加：
INSERT INTO sys_user_permission (user_id, permission_id) 
SELECT 15, id FROM sys_permission WHERE permission_code = 'outbound:apply';
```

### 场景3：医护人员需要查看库存信息

```sql
-- 为医护人员添加库存查看权限
INSERT INTO sys_user_permission (user_id, permission_id) 
SELECT 10, id FROM sys_permission WHERE permission_code = 'inventory:view';
```

### 场景4：移除用户的某个权限

```sql
-- 移除用户ID=10的特殊药品审核权限
DELETE FROM sys_user_permission 
WHERE user_id = 10 AND permission_id = (
    SELECT id FROM sys_permission WHERE permission_code = 'outbound:approve:special'
);
```

## 注意事项

1. **权限缓存**：前端会缓存用户权限，登录/登出时会自动清除缓存
2. **向后兼容**：如果后端权限API请求失败，前端会回退到基于角色的权限判断
3. **权限验证**：后端会在接口层面验证权限，前端权限控制主要用于UI显示
4. **特殊药品验证**：系统会在审批时自动验证申请人和审核人不能是同一人

## 常见问题

### Q: 如何查看某个用户的所有权限？

A: 调用 `/api/v1/auth/permissions` API，或执行SQL查询（见使用示例）

### Q: 如何批量给多个用户添加权限？

A: 使用INSERT语句批量插入到 `sys_user_permission` 表

### Q: 权限修改后需要重新登录吗？

A: 建议重新登录，系统会在登录时刷新权限缓存

### Q: 如何移除用户的某个权限？

A: 从 `sys_user_permission` 表中删除对应记录

```sql
DELETE FROM sys_user_permission 
WHERE user_id = 10 AND permission_id = (
    SELECT id FROM sys_permission WHERE permission_code = 'outbound:approve:special'
);
```

### Q: 如何查看某个角色的所有权限？

A: 执行以下SQL查询：

```sql
SELECT 
    r.role_name,
    p.permission_code,
    p.permission_name
FROM sys_role r
INNER JOIN sys_role_permission rp ON r.id = rp.role_id
INNER JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.id = 2  -- 替换为实际角色ID
ORDER BY p.sort_order;
```

### Q: 如何批量给多个用户添加权限？

A: 使用INSERT语句批量插入，例如给用户ID 10, 11, 12都添加特殊药品审核权限：

```sql
INSERT INTO sys_user_permission (user_id, permission_id) 
SELECT u.id, p.id 
FROM sys_user u
CROSS JOIN sys_permission p
WHERE u.id IN (10, 11, 12)
  AND p.permission_code = 'outbound:approve:special';
```

## RBAC模型说明

本系统采用**混合RBAC模型（Hybrid RBAC）**，也称为**RBAC with User-Permission Assignment**：

- **传统RBAC**：用户 → 角色 → 权限
- **扩展RBAC**：用户 → 角色 → 权限 + 用户 → 权限（直接分配）

**优势**：
- 保持RBAC的核心思想（角色批量管理）
- 支持用户个性化权限（灵活扩展）
- 权限合并机制（角色权限 + 用户权限）
- 符合最小权限原则

**权限查询逻辑**：
1. 先查询角色权限（从 `sys_role_permission` 表）
2. 再查询用户直接权限（从 `sys_user_permission` 表）
3. 合并去重后返回最终权限列表

---

**最后更新时间**：2025年1月14日

