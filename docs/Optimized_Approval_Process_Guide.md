# 优化后的供应商审批流程与权限配置指南

## 一、优化概述

基于当前系统的用户角色权限情况，优化了价格、资质审核业务流程。**不新增角色**，通过**新增权限标签**的方式，灵活配置各角色的审批权限，实现分权制衡的审批流程。

### 优化原则

1. **不新增角色**：保持现有5个角色（系统管理员、仓库管理员、采购专员、医护人员、供应商）+ 超级管理员
2. **新增权限标签**：通过细粒度权限控制，实现灵活的权限配置
3. **分权制衡**：不同角色负责不同审批环节，相互制衡
4. **超级管理员**：拥有所有权限，主要用于系统测试和维护
5. **系统管理员**：只拥有系统功能权限，不参与业务审批

## 二、角色与权限配置

### 2.1 角色说明

| 角色ID | 角色名称 | 主要职责 | 是否参与审批 |
|--------|----------|----------|--------------|
| 1 | 系统管理员 | 系统功能维护（用户管理、角色管理、配置管理等） | ❌ 不参与 |
| 2 | 仓库管理员 | 药品管理、库存管理、资质核验、价格审核、最终审批 | ✅ 参与 |
| 3 | 采购专员 | 采购管理、发起审批申请、价格审核 | ✅ 参与 |
| 4 | 医护人员 | 出库申领、通知查看 | ❌ 不参与 |
| 5 | 供应商 | 订单查看、通知查看 | ❌ 不参与 |
| 6 | 超级管理员 | 系统测试和维护，拥有所有权限 | ✅ 可参与（主要用于测试） |

### 2.2 新增权限标签

#### 供应商审批流程权限

| 权限代码 | 权限名称 | 说明 |
|---------|---------|------|
| `supplier:approval:apply` | 供应商审批申请 | 发起供应商准入审批申请 |
| `supplier:approval:quality` | 供应商资质核验 | 核验供应商资质真实性 |
| `supplier:approval:price` | 供应商价格审核 | 审核供应商报价合理性 |
| `supplier:approval:final` | 供应商最终审批 | 最终审批供应商准入 |
| `supplier:approval:view` | 供应商审批查看 | 查看审批申请和审批日志 |

#### 供应商黑名单权限

| 权限代码 | 权限名称 | 说明 |
|---------|---------|------|
| `supplier:blacklist:manage` | 供应商黑名单管理 | 管理供应商黑名单 |
| `supplier:blacklist:view` | 供应商黑名单查看 | 查看供应商黑名单 |

#### 价格管理权限

| 权限代码 | 权限名称 | 说明 |
|---------|---------|------|
| `price:agreement:manage` | 价格协议管理 | 创建、更新、删除价格协议 |
| `price:agreement:view` | 价格协议查看 | 查看价格协议 |
| `price:history:view` | 价格历史查看 | 查看价格变更历史 |
| `price:warning:config` | 价格预警配置 | 配置价格预警规则 |
| `price:warning:view` | 价格预警查看 | 查看价格预警信息 |

### 2.3 角色权限分配

#### 采购专员（角色ID=3）

**分配的审批权限**：
- ✅ `supplier:approval:apply` - 发起审批申请
- ✅ `supplier:approval:view` - 查看审批
- ✅ `supplier:approval:price` - 价格审核
- ✅ `price:agreement:view` - 查看价格协议
- ✅ `price:history:view` - 查看价格历史

**职责**：
- 发起供应商准入审批申请
- 提交供应商资质文件和报价单
- 审核供应商报价合理性（价格审核环节）
- 查看审批进度和审批日志

#### 仓库管理员（角色ID=2）

**分配的审批权限**：
- ✅ `supplier:approval:quality` - 资质核验
- ✅ `supplier:approval:price` - 价格审核
- ✅ `supplier:approval:final` - 最终审批
- ✅ `supplier:approval:view` - 查看审批
- ✅ `price:agreement:manage` - 价格协议管理
- ✅ `price:agreement:view` - 查看价格协议
- ✅ `price:warning:config` - 价格预警配置
- ✅ `price:warning:view` - 查看价格预警
- ✅ `price:history:view` - 查看价格历史

**职责**：
- 核验供应商资质真实性（资质核验环节）
- 审核供应商报价合理性（价格审核环节）
- 最终审批供应商准入（最终审批环节）
- 管理价格协议
- 配置价格预警规则

#### 系统管理员（角色ID=1）

**分配的审批权限**：
- ❌ 不分配任何审批权限

**职责**：
- 系统功能维护（用户管理、角色管理、配置管理等）
- 不参与业务审批流程

#### 超级管理员（角色ID=6）

**分配的审批权限**：
- ✅ 拥有所有权限（通过代码判断，返回通配符"*"）

**职责**：
- 系统测试和维护
- 可以参与所有审批环节（主要用于测试）
- 拥有所有业务权限

## 三、优化后的审批流程

### 3.1 审批流程设计

```
步骤1：采购专员发起审批申请
       ↓ （提交资质文件+报价单）
       权限：supplier:approval:apply
       
步骤2：仓库管理员核验资质
       ↓ （核对资质原件与复印件是否一致）
       权限：supplier:approval:quality
       
步骤3：价格审核（采购专员或仓库管理员）
       ↓ （对比集采价/市场均价/历史协议价）
       权限：supplier:approval:price
       
步骤4：仓库管理员最终审批
       ↓ （确认流程合规性，存档协议）
       权限：supplier:approval:final
       
步骤5：审批通过，协议生效
       ↓ （价格协议自动关联，价格可录入系统）
```

### 3.2 流程特点

1. **分权制衡**：
   - 采购专员负责发起申请和价格审核
   - 仓库管理员负责资质核验、价格审核和最终审批
   - 不同环节由不同角色负责，相互制衡

2. **灵活配置**：
   - 通过权限标签配置，可以灵活调整各角色的权限
   - 不依赖硬编码的角色ID判断
   - 支持通过用户直接权限实现特殊配置

3. **权限隔离**：
   - 系统管理员不参与业务审批
   - 医护人员和供应商不参与审批流程
   - 超级管理员主要用于测试和维护

## 四、API接口权限配置

### 4.1 审批流程接口

| 接口 | 方法 | 权限要求 | 说明 |
|------|------|---------|------|
| `/api/v1/supplier-approvals` | POST | `supplier:approval:apply` | 创建审批申请 |
| `/api/v1/supplier-approvals/{id}/quality-check` | POST | `supplier:approval:quality` | 资质核验 |
| `/api/v1/supplier-approvals/{id}/price-review` | POST | `supplier:approval:price` | 价格审核 |
| `/api/v1/supplier-approvals/{id}/final-approve` | POST | `supplier:approval:final` | 最终审批 |
| `/api/v1/supplier-approvals/{id}` | GET | `supplier:approval:view` | 查询申请详情 |
| `/api/v1/supplier-approvals/{id}/logs` | GET | `supplier:approval:view` | 查询审批日志 |

### 4.2 价格协议接口

| 接口 | 方法 | 权限要求 | 说明 |
|------|------|---------|------|
| `/api/v1/supplier-drug-agreements` | POST | `price:agreement:manage` | 创建协议 |
| `/api/v1/supplier-drug-agreements/{id}` | GET | `price:agreement:view` | 查询协议 |
| `/api/v1/supplier-drug-agreements/current` | GET | `price:agreement:view` | 查询当前协议 |
| `/api/v1/supplier-drug-agreements/list` | GET | `price:agreement:view` | 查询所有协议 |
| `/api/v1/supplier-drug-agreements/{id}` | PUT | `price:agreement:manage` | 更新协议 |
| `/api/v1/supplier-drug-agreements/{id}` | DELETE | `price:agreement:manage` | 删除协议 |

### 4.3 价格历史接口

| 接口 | 方法 | 权限要求 | 说明 |
|------|------|---------|------|
| `/api/v1/supplier-drug-price-history/list` | GET | `price:history:view` | 查询价格历史 |
| `/api/v1/supplier-drug-price-history/agreement/{agreementId}` | GET | `price:history:view` | 根据协议查询历史 |

## 五、实施步骤

### 5.1 数据库初始化

1. **执行权限初始化脚本**：
   ```sql
   -- 执行 add_supplier_approval_permissions.sql
   -- 新增审批流程相关权限
   -- 为各角色分配权限
   ```

2. **验证权限分配**：
   ```sql
   -- 查看新增的权限
   SELECT * FROM sys_permission 
   WHERE permission_code LIKE 'supplier:approval:%'
      OR permission_code LIKE 'price:%';
   
   -- 查看各角色的权限分配
   SELECT r.role_name, p.permission_code, p.permission_name
   FROM sys_role r
   INNER JOIN sys_role_permission rp ON r.id = rp.role_id
   INNER JOIN sys_permission p ON rp.permission_id = p.id
   WHERE p.permission_code LIKE 'supplier:approval:%'
      OR p.permission_code LIKE 'price:%'
   ORDER BY r.id, p.sort_order;
   ```

### 5.2 代码部署

1. **后端代码**：
   - 所有Controller已更新为使用权限标签而非硬编码角色ID
   - 权限检查通过 `@RequiresPermission` 注解实现
   - 支持权限拦截器自动校验

2. **前端代码**：
   - 需要更新前端权限检查逻辑
   - 使用权限代码而非角色ID判断
   - 更新权限常量定义

### 5.3 权限配置调整

如果需要调整权限分配，可以通过以下方式：

1. **通过数据库直接调整**：
   ```sql
   -- 为某个角色添加权限
   INSERT INTO sys_role_permission (role_id, permission_id) 
   SELECT 2, id FROM sys_permission 
   WHERE permission_code = 'supplier:approval:quality';
   
   -- 移除某个角色的权限
   DELETE FROM sys_role_permission 
   WHERE role_id = 2 
     AND permission_id IN (
       SELECT id FROM sys_permission 
       WHERE permission_code = 'supplier:approval:quality'
     );
   ```

2. **通过用户直接权限**：
   - 使用 `sys_user_permission` 表
   - 可以为特定用户分配特殊权限
   - 适用于临时授权或特殊场景

## 六、权限配置示例

### 6.1 标准配置（推荐）

**采购专员**：
- 发起审批申请
- 价格审核
- 查看审批进度

**仓库管理员**：
- 资质核验
- 价格审核
- 最终审批
- 价格协议管理
- 价格预警配置

### 6.2 灵活配置示例

如果需要调整审批流程，可以通过权限配置实现：

**场景1：增加财务部门参与价格审核**
- 为财务部门用户分配 `supplier:approval:price` 权限
- 通过用户直接权限实现，不修改角色权限

**场景2：采购专员也可以进行最终审批**
- 为采购专员角色添加 `supplier:approval:final` 权限
- 通过数据库更新角色权限配置

**场景3：系统管理员参与审批（特殊场景）**
- 为系统管理员用户分配审批相关权限
- 通过用户直接权限实现

## 七、优势总结

### 7.1 灵活性
- 不依赖硬编码的角色ID
- 通过权限标签灵活配置
- 支持用户直接权限，实现特殊配置

### 7.2 可维护性
- 权限配置集中管理
- 易于调整和扩展
- 清晰的权限定义

### 7.3 安全性
- 权限检查在拦截器层面实现
- 支持细粒度权限控制
- 权限不可绕过

### 7.4 合规性
- 符合最小权限原则
- 系统管理员不参与业务
- 超级管理员主要用于测试

## 八、注意事项

1. **权限初始化**：首次部署需要执行权限初始化脚本
2. **权限验证**：确保所有接口都有正确的权限注解
3. **前端同步**：前端需要同步更新权限检查逻辑
4. **测试验证**：部署后需要测试各角色的权限是否正确
5. **文档更新**：权限调整后需要更新相关文档

## 九、常见问题

### Q1: 如何为特定用户分配特殊权限？
A: 使用 `sys_user_permission` 表，为特定用户直接分配权限，优先级高于角色权限。

### Q2: 系统管理员可以参与审批吗？
A: 默认情况下，系统管理员不参与审批。如果需要，可以通过用户直接权限为其分配审批权限。

### Q3: 如何调整审批流程？
A: 通过调整角色权限配置或用户直接权限，可以灵活调整各环节的审批人员。

### Q4: 超级管理员的作用是什么？
A: 超级管理员主要用于系统测试和维护，拥有所有权限，但不应该用于日常业务操作。

## 十、相关文档

- `Supplier_Approval_Anti_Corruption_Solution.md` - 供应商审批与廉政风险防控方案
- `Complete_Price_Management_Solution.md` - 完整价格管理方案
- `Fine_Grained_Permission_System_Guide.md` - 细粒度权限系统指南

