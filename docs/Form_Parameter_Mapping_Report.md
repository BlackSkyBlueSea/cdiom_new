# 表单参数映射检查报告

## 检查日期：2026-01-12

本报告详细检查了前后端代码以及数据库实体类之间的参数映射对应关系。

---

## 1. 用户管理表单 ✅

### 前端字段（UserManagement.jsx）
- `username` - 用户名
- `phone` - 手机号
- `email` - 邮箱
- `password` - 密码
- `roleId` - 角色ID
- `status` - 状态

### 后端DTO（SysUserController.java）
- `CreateUserRequest`: username, phone, email, password, roleId, status ✅
- `UpdateUserRequest`: username, phone, email, password, roleId, status ✅

### 实体类（SysUser.java）
- username, phone, email, password, roleId, status ✅

### 检查结果
✅ **所有字段映射正确，无需修复**

---

## 2. 供应商管理表单 ✅

### 前端字段（SupplierManagement.jsx）
- `name` - 供应商名称
- `contactPerson` - 联系人
- `phone` - 联系电话
- `address` - 地址
- `creditCode` - 统一社会信用代码
- `licenseImage` - 许可证图片
- `licenseExpiryDate` - 许可证到期日期（已格式化：YYYY-MM-DD）
- `remark` - 备注
- `status` - 状态（编辑时显示）

### 后端DTO（SupplierController.java）
- `SupplierRequest`: name, contactPerson, phone, address, creditCode, licenseImage, licenseExpiryDate, remark, status ✅

### 实体类（Supplier.java）
- name, contactPerson, phone, address, creditCode, licenseImage, licenseExpiryDate, remark, status ✅

### 检查结果
✅ **所有字段映射正确，日期格式已正确处理**

---

## 3. 药品管理表单 ✅

### 前端字段（DrugManagement.jsx）
- `drugName` - 药品名称
- `nationalCode` - 国家本位码
- `traceCode` - 药品追溯码
- `productCode` - 商品码
- `approvalNumber` - 批准文号
- `manufacturer` - 生产厂家
- `dosageForm` - 剂型
- `specification` - 规格
- `expiryDate` - 有效期（已格式化：YYYY-MM-DD）
- `isSpecial` - 是否特殊药品
- `unit` - 单位
- `storageRequirement` - 存储要求
- `storageLocation` - 存储位置
- `description` - 描述

### 后端接收（DrugInfoController.java）
- 直接使用 `@RequestBody DrugInfo drugInfo` ✅

### 实体类（DrugInfo.java）
- drugName, nationalCode, traceCode, productCode, approvalNumber, manufacturer, dosageForm, specification, expiryDate, isSpecial, unit, storageRequirement, storageLocation, description ✅

### 检查结果
✅ **所有字段映射正确，日期格式已正确处理**

---

## 4. 入库管理表单 ✅

### 前端字段（InboundManagement.jsx）
- `orderId` - 采购订单ID（订单入库时）
- `drugId` - 药品ID
- `batchNumber` - 批次号
- `quantity` - 入库数量
- `expiryDate` - 有效期至（已格式化：YYYY-MM-DD）
- `arrivalDate` - 到货日期（已格式化：YYYY-MM-DD）
- `productionDate` - 生产日期（已格式化：YYYY-MM-DD）
- `manufacturer` - 生产厂家
- `deliveryNoteNumber` - 随货同行单编号
- `status` - 验收状态（QUALIFIED/UNQUALIFIED）
- `expiryCheckReason` - 效期校验说明
- `remark` - 备注

### 后端DTO（InboundRecordController.java）
- `InboundRecordRequest`: orderId, drugId, batchNumber, quantity, expiryDate, arrivalDate, productionDate, manufacturer, deliveryNoteNumber, deliveryNoteImage, secondOperatorId, status, expiryCheckStatus, expiryCheckReason, remark ✅

### 实体类（InboundRecord.java）
- orderId, drugId, batchNumber, quantity, expiryDate, arrivalDate, productionDate, manufacturer, deliveryNoteNumber, deliveryNoteImage, operatorId, secondOperatorId, status, expiryCheckStatus, expiryCheckReason, remark ✅

### 检查结果
✅ **所有字段映射正确，日期格式已正确处理**
- 注意：`expiryCheckStatus` 由后端自动计算，前端无需提交
- 注意：`operatorId` 由后端从当前用户获取，前端无需提交

---

## 5. 出库管理表单 ✅

### 前端字段（OutboundManagement.jsx）
#### 创建申请表单
- `department` - 所属科室
- `purpose` - 用途说明
- `remark` - 备注
- `items` - 申请明细数组：[{drugId, batchNumber(可选), quantity}]

#### 审批表单
- `secondApproverId` - 第二审批人ID（特殊药品必填）

#### 执行出库表单
- `outboundItems` - 出库明细数组：[{drugId, batchNumber(可选), actualQuantity}]

### 后端DTO（OutboundApplyController.java）
- `OutboundApplyRequest`: department, purpose, remark, items ✅
- `ApproveRequest`: secondApproverId ✅
- `ExecuteOutboundRequest`: outboundItems ✅

### 实体类（OutboundApply.java）
- department, purpose, remark ✅
- applicantId（后端自动获取）✅
- approverId（后端自动获取）✅
- secondApproverId ✅

### 检查结果
✅ **所有字段映射正确**

---

## 6. 采购订单管理表单 ✅

### 前端字段（PurchaseOrderManagement.jsx）
#### 创建订单表单
- `supplierId` - 供应商ID
- `expectedDeliveryDate` - 预计交货日期（已格式化：YYYY-MM-DD）
- `remark` - 备注
- `items` - 订单明细数组：[{drugId, quantity, unitPrice, totalPrice}]

#### 拒绝订单表单
- `reason` - 拒绝理由

#### 发货表单
- `logisticsNumber` - 物流单号

#### 取消订单表单
- `reason` - 取消原因

#### 更新物流单号表单
- `logisticsNumber` - 物流单号

### 后端DTO（PurchaseOrderController.java）
- `PurchaseOrderRequest`: supplierId, expectedDeliveryDate, remark, items ✅
- `RejectOrderRequest`: reason ✅
- `ShipOrderRequest`: logisticsNumber ✅
- `CancelOrderRequest`: reason ✅
- `UpdateLogisticsRequest`: logisticsNumber ✅

### 实体类（PurchaseOrder.java）
- supplierId, expectedDeliveryDate, remark ✅
- purchaserId（后端自动获取）✅
- status（后端自动设置）✅

### 实体类（PurchaseOrderItem.java）
- drugId, quantity, unitPrice, totalPrice ✅

### 检查结果
✅ **所有字段映射正确，日期格式已正确处理**

---

## 总结

### ✅ 检查通过的表单
1. 用户管理表单
2. 供应商管理表单
3. 药品管理表单
4. 入库管理表单
5. 出库管理表单
6. 采购订单管理表单

### 📝 注意事项
1. **日期字段处理**：所有日期字段在前端提交时都已正确格式化为 `YYYY-MM-DD` 格式
2. **自动字段**：以下字段由后端自动处理，前端无需提交：
   - `operatorId`（操作人ID）
   - `applicantId`（申请人ID）
   - `approverId`（审批人ID）
   - `purchaserId`（采购员ID）
   - `createTime`（创建时间）
   - `updateTime`（更新时间）
   - `expiryCheckStatus`（效期校验状态，由后端计算）
3. **数组字段**：明细项数组字段（items, outboundItems）格式正确
4. **可选字段**：所有可选字段都正确处理了 `undefined` 或 `null` 的情况

### 🎯 结论
**所有表单的参数映射都是正确的，前后端字段名一致，日期格式转换正确，前端显示参数值正确。**







