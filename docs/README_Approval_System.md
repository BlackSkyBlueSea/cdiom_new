# 供应商审批系统文档索引

## 文档清单

### 1. 核心方案文档

#### [Optimized_Approval_Process_Guide.md](./Optimized_Approval_Process_Guide.md)
**优化后的审批流程与权限配置指南**

- 详细说明优化后的审批流程设计
- 完整的权限标签定义和角色权限分配
- API接口权限配置
- 实施步骤和配置示例
- **推荐首先阅读此文档**

#### [Supplier_Approval_Anti_Corruption_Solution.md](./Supplier_Approval_Anti_Corruption_Solution.md)
**供应商审批与廉政风险防控方案**

- 分权制衡的审批流程设计
- 价格预警、黑名单等安全保障机制
- 系统约束功能说明
- 数据库设计

#### [Complete_Price_Management_Solution.md](./Complete_Price_Management_Solution.md)
**完整价格管理方案总结**

- 价格协议管理
- 价格历史记录
- 审批流程
- 价格预警
- 黑名单机制

### 2. 数据库脚本

#### `add_supplier_approval_permissions.sql`
**权限初始化脚本**

- 新增审批流程相关权限标签
- 为各角色分配权限
- 执行此脚本初始化权限配置

#### `create_supplier_approval_tables.sql`
**审批流程表结构脚本**

- 创建审批申请表、审批明细表、审批日志表
- 创建黑名单表、价格预警配置表

#### `create_price_agreement_tables.sql`
**价格协议表结构脚本**

- 创建价格协议表
- 创建价格历史记录表
- 扩展供应商-药品关联表

### 3. 快速开始

#### 步骤1：执行数据库脚本
```sql
-- 1. 创建审批流程表
SOURCE create_supplier_approval_tables.sql;

-- 2. 创建价格协议表
SOURCE create_price_agreement_tables.sql;

-- 3. 初始化权限配置
SOURCE add_supplier_approval_permissions.sql;
```

#### 步骤2：部署后端代码
- 所有Controller已更新为使用权限标签
- 权限检查通过 `@RequiresPermission` 注解实现

#### 步骤3：验证权限配置
```sql
-- 查看各角色的审批权限
SELECT r.role_name, p.permission_code, p.permission_name
FROM sys_role r
INNER JOIN sys_role_permission rp ON r.id = rp.role_id
INNER JOIN sys_permission p ON rp.permission_id = p.id
WHERE p.permission_code LIKE 'supplier:approval:%'
   OR p.permission_code LIKE 'price:%'
ORDER BY r.id, p.sort_order;
```

### 4. 权限标签说明

#### 审批流程权限
- `supplier:approval:apply` - 发起审批申请
- `supplier:approval:quality` - 资质核验
- `supplier:approval:price` - 价格审核
- `supplier:approval:final` - 最终审批
- `supplier:approval:view` - 查看审批

#### 价格管理权限
- `price:agreement:manage` - 价格协议管理
- `price:agreement:view` - 价格协议查看
- `price:history:view` - 价格历史查看
- `price:warning:config` - 价格预警配置
- `price:warning:view` - 价格预警查看

### 5. 角色权限分配

| 角色 | 审批权限 | 价格管理权限 |
|------|---------|-------------|
| **采购专员** | 发起申请、价格审核、查看 | 查看协议、查看历史 |
| **仓库管理员** | 资质核验、价格审核、最终审批、查看 | 协议管理、预警配置、查看历史 |
| **系统管理员** | 无 | 无 |
| **超级管理员** | 所有权限 | 所有权限 |

### 6. 审批流程

```
采购专员发起申请
    ↓
仓库管理员核验资质
    ↓
采购专员/仓库管理员价格审核
    ↓
仓库管理员最终审批
    ↓
审批通过，协议生效
```

### 7. 常见问题

**Q: 如何调整审批流程？**
A: 通过调整角色权限配置或用户直接权限，可以灵活调整各环节的审批人员。

**Q: 系统管理员可以参与审批吗？**
A: 默认情况下，系统管理员不参与审批。如果需要，可以通过用户直接权限为其分配审批权限。

**Q: 如何为特定用户分配特殊权限？**
A: 使用 `sys_user_permission` 表，为特定用户直接分配权限，优先级高于角色权限。

### 8. 技术支持

如有问题，请参考：
- `Optimized_Approval_Process_Guide.md` - 详细的权限配置指南
- `Supplier_Approval_Anti_Corruption_Solution.md` - 完整的方案说明
- `Fine_Grained_Permission_System_Guide.md` - 细粒度权限系统指南

