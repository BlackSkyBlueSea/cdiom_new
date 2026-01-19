# 药品价格管理完整解决方案

## 一、方案概述

本方案实现了完整的药品价格管理体系，包括：
1. **价格协议管理**：协议文件存档、协议关联、价格设定
2. **价格历史记录**：完整记录每次价格变更历史
3. **操作日志**：记录所有价格相关操作
4. **分权制衡审批**：多角色协同审批，防止廉政风险
5. **价格预警**：自动检测价格异常
6. **黑名单机制**：拦截违规供应商

## 二、核心功能模块

### 2.1 价格协议管理模块

**功能**：
- 协议文件上传（支持PDF、DOC、DOCX等格式）
- 协议信息管理（编号、名称、有效期、价格等）
- 协议与价格关联
- 协议查询和追溯

**相关文件**：
- `SupplierDrugAgreement.java` - 协议实体类
- `SupplierDrugAgreementService.java` - 协议服务
- `SupplierDrugAgreementController.java` - 协议控制器
- `create_price_agreement_tables.sql` - 数据库表结构

### 2.2 价格历史记录模块

**功能**：
- 记录每次价格变更的完整历史
- 记录变更前后价格对比
- 记录变更原因、操作人、操作时间
- 支持价格历史查询

**相关文件**：
- `SupplierDrugPriceHistory.java` - 价格历史实体类
- `SupplierDrugPriceHistoryService.java` - 价格历史服务
- `SupplierDrugPriceHistoryController.java` - 价格历史控制器

### 2.3 分权制衡审批模块

**功能**：
- 多角色协同审批流程
- 资质核验（仓库管理员）
- 价格审核（采购/财务负责人）
- 最终审批（超级管理员）
- 审批流程日志记录

**相关文件**：
- `SupplierApprovalApplication.java` - 审批申请实体类
- `SupplierApprovalService.java` - 审批服务
- `SupplierApprovalController.java` - 审批控制器
- `create_supplier_approval_tables.sql` - 数据库表结构

### 2.4 价格预警模块

**功能**：
- 自动对比参考价格（集采价/市场价/历史最低价）
- 计算价格差异率
- 触发价格预警（警告/严重）
- 强制填写价格差异说明

**相关文件**：
- `PriceWarningConfig.java` - 预警配置实体类
- `PriceWarningService.java` - 预警服务
- `PriceWarningResult.java` - 预警结果模型

### 2.5 供应商黑名单模块

**功能**：
- 供应商黑名单管理
- 自动拦截黑名单供应商
- 支持完全禁止和部分药品禁止
- 支持有效期管理

**相关文件**：
- `SupplierBlacklist.java` - 黑名单实体类
- `SupplierBlacklistMapper.java` - 黑名单Mapper

## 三、审批流程设计

### 3.1 流程步骤

```
1. 采购员发起申请
   ↓
2. 系统自动检查（黑名单、价格预警）
   ↓
3. 仓库管理员核验资质
   ↓
4. 采购/财务负责人审核价格
   ↓
5. 超级管理员最终审批
   ↓
6. 审批通过，协议生效
```

### 3.2 权限控制

| 角色 | 可操作环节 | 禁止操作 |
|------|-----------|----------|
| 采购员 | 发起申请 | 审核、审批 |
| 仓库管理员 | 资质核验 | 价格审核、最终审批 |
| 采购/财务负责人 | 价格审核 | 资质核验、最终审批 |
| 超级管理员 | 最终审批 | 发起申请 |

## 四、数据库表结构

### 4.1 价格协议相关表

1. **supplier_drug_agreement** - 价格协议表
2. **supplier_drug_price_history** - 价格历史记录表
3. **supplier_drug** - 供应商-药品关联表（已扩展协议关联字段）

### 4.2 审批流程相关表

1. **supplier_approval_application** - 审批申请表
2. **supplier_approval_item** - 审批明细表（价格明细）
3. **supplier_approval_log** - 审批流程日志表
4. **supplier_blacklist** - 供应商黑名单表
5. **price_warning_config** - 价格预警配置表

## 五、API接口清单

### 5.1 价格协议管理

- `POST /api/v1/supplier-drug-agreements` - 创建协议
- `GET /api/v1/supplier-drug-agreements/{id}` - 查询协议
- `GET /api/v1/supplier-drug-agreements/current` - 查询当前生效协议
- `GET /api/v1/supplier-drug-agreements/list` - 查询所有协议
- `PUT /api/v1/supplier-drug-agreements/{id}` - 更新协议
- `DELETE /api/v1/supplier-drug-agreements/{id}` - 删除协议

### 5.2 价格更新（增强版）

- `PUT /api/v1/supplier-drugs/price` - 更新价格（支持协议关联、历史记录、操作日志）

### 5.3 价格历史查询

- `GET /api/v1/supplier-drug-price-history/list` - 查询价格历史
- `GET /api/v1/supplier-drug-price-history/agreement/{agreementId}` - 根据协议查询历史

### 5.4 审批流程

- `POST /api/v1/supplier-approvals` - 创建审批申请
- `POST /api/v1/supplier-approvals/{id}/quality-check` - 资质核验
- `POST /api/v1/supplier-approvals/{id}/price-review` - 价格审核
- `POST /api/v1/supplier-approvals/{id}/final-approve` - 最终审批
- `GET /api/v1/supplier-approvals/{id}` - 查询申请详情
- `GET /api/v1/supplier-approvals/{id}/logs` - 查询审批日志

## 六、安全保障

### 6.1 流程保障
- 多环节审批，分权制衡
- 状态流转控制，不允许跳步
- 角色权限隔离

### 6.2 技术保障
- 价格预警自动检测
- 黑名单自动拦截
- 操作日志全程留痕
- IP地址记录

### 6.3 数据保障
- 协议文档防篡改
- 价格历史永久保存
- 操作日志不可删除
- 数据定期备份

## 七、实施步骤

### 7.1 数据库初始化

1. 执行 `create_price_agreement_tables.sql` 创建价格协议相关表
2. 执行 `create_supplier_approval_tables.sql` 创建审批流程相关表
3. 初始化价格预警全局配置

### 7.2 后端部署

1. 部署所有新增的实体类、Mapper、Service、Controller
2. 配置权限拦截器，确保权限控制生效
3. 测试所有API接口

### 7.3 前端开发

1. 开发价格协议管理界面
2. 开发审批流程界面
3. 开发价格历史查询界面
4. 开发价格预警展示
5. 开发审批日志查询界面

### 7.4 测试验证

1. 测试完整的审批流程
2. 测试价格预警功能
3. 测试黑名单拦截
4. 测试权限控制
5. 测试操作日志记录

## 八、文档清单

1. **Price_Agreement_Management_Solution.md** - 价格协议管理解决方案
2. **Supplier_Approval_Anti_Corruption_Solution.md** - 供应商准入审批与廉政风险防控解决方案
3. **Complete_Price_Management_Solution.md** - 本文档，完整方案总结

## 九、关键特性

### 9.1 确保价格与协议一致
- 协议文件存档
- 价格关联协议
- 协议有效期管理

### 9.2 防止廉政风险
- 分权制衡审批
- 价格预警校验
- 黑名单拦截
- 全程留痕

### 9.3 完整追溯能力
- 价格历史记录
- 操作日志记录
- 审批流程日志
- 协议文档存档

### 9.4 符合合规要求
- 符合GSP规范
- 操作日志保留5年
- 数据不可篡改
- 完整的审计能力

## 十、总结

本方案实现了完整的药品价格管理体系，通过**协议管理、历史记录、分权审批、价格预警、黑名单机制**等多重保障，既确保了价格与协议的一致性，又有效防范了廉政风险，同时提供了完整的追溯能力，符合药品采购管理的合规要求。

