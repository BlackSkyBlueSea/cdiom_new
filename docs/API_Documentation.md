# CDIOM - API接口文档

本文档详细描述了CDIOM系统中所有API接口的详细说明，包括请求参数、响应格式、权限要求、业务说明等完整的接口文档内容。

## 目录

- [统一响应格式](#统一响应格式)
- [认证接口](#认证接口)
- [超级管理员管理接口](#超级管理员管理接口)
- [用户管理接口](#用户管理接口)
- [角色管理接口](#角色管理接口)
- [参数配置接口](#参数配置接口)
- [通知公告接口](#通知公告接口)
- [操作日志接口](#操作日志接口)
- [登录日志接口](#登录日志接口)
- [库存管理接口](#库存管理接口)
- [药品信息管理接口](#药品信息管理接口)
- [入库管理接口](#入库管理接口)
- [出库管理接口](#出库管理接口)
- [采购订单管理接口](#采购订单管理接口)
- [供应商管理接口](#供应商管理接口)
- [库存调整接口](#库存调整接口)
- [仪表盘接口](#仪表盘接口)
- [供应商-药品关联管理接口](#供应商-药品关联管理接口)
- [文件上传接口](#文件上传接口)
- [后端监控接口](#后端监控接口)
- [供应商审核接口](#供应商审核接口)
- [供应商药品协议接口](#供应商药品协议接口)
- [供应商药品价格历史接口](#供应商药品价格历史接口)
- [第三方API集成](#第三方api集成)

---

## 统一响应格式

所有API接口统一使用 `Result<T>` 格式：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

**状态码说明：**
- `200`: 操作成功
- `401`: 未授权（Token无效或过期）
- `500`: 操作失败

**认证方式：**
- **Token认证**：JWT Token，8小时有效期，存储在Cookie中（key: cdiom_token）
- **权限控制**：基于RBAC的权限系统，支持接口级权限控制

---

## 认证接口

### 用户登录

- **接口**: `POST /api/v1/auth/login`
- **权限**: 无需认证（公开接口）
- **请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- **响应**: 返回Token和用户信息，Token自动存储到Cookie（key: cdiom_token）
- **响应格式**:
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "phone": "13800138000",
      "email": "admin@example.com",
      "roleId": 1,
      "status": 1
    }
  }
}
```
- **说明**: 
  - 支持用户名或手机号登录
  - 自动记录登录日志（IP地址、浏览器、操作系统）
  - 连续5次登录失败会锁定账户1小时

### 获取当前用户信息

- **接口**: `GET /api/v1/auth/current`
- **权限**: 需要Token认证
- **响应**: 返回当前登录用户信息
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "phone": "13800138000",
    "email": "admin@example.com",
    "roleId": 1,
    "status": 1
  }
}
```

### 获取当前用户权限列表

- **接口**: `GET /api/v1/auth/permissions`
- **权限**: 需要Token认证
- **说明**: 返回当前用户的所有权限代码集合
- **响应**: 返回权限代码Set<String>
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": ["user:manage", "drug:view", "drug:manage", ...]
}
```

### 用户登出

- **接口**: `POST /api/v1/auth/logout`
- **权限**: 需要Token认证
- **说明**: 清除Token和Cookie
- **响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

## 超级管理员管理接口

### 查询超级管理员状态

- **接口**: `GET /api/v1/super-admin/status`
- **权限**: 需要 `user:manage` 权限
- **响应**: 返回超级管理员账户的状态信息（用户名、状态、邮箱、创建时间）
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "username": "super_admin",
    "status": 1,
    "email": "super_admin@example.com",
    "createTime": "2026-01-01T10:00:00"
  }
}
```

### 发送验证码

- **接口**: `POST /api/v1/super-admin/send-verification-code`
- **权限**: 需要 `user:manage` 权限
- **请求体**:
```json
{
  "email": "user@example.com"
}
```
- **说明**: 
  - 验证码将发送到指定邮箱，验证码有效期为5分钟
  - 必须使用当前登录用户绑定的邮箱
  - 验证码格式：6位数字

### 启用超级管理员

- **接口**: `POST /api/v1/super-admin/enable`
- **权限**: 需要 `user:manage` 权限
- **请求体**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```
- **说明**: 启用超级管理员账户，需要邮箱验证码验证

### 停用超级管理员

- **接口**: `POST /api/v1/super-admin/disable`
- **权限**: 需要 `user:manage` 权限
- **请求体**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```
- **说明**: 
  - 停用超级管理员账户，需要邮箱验证码验证
  - 如果停用的是当前登录用户，系统会自动退出登录

---

## 用户管理接口

### 获取用户列表

- **接口**: `GET /api/v1/users`
- **权限**: 需要 `user:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（用户名/手机号）
  - `roleId`: 角色ID（可选）
  - `status`: 状态（0-禁用/1-正常，可选）
- **响应**: 返回分页用户列表

### 获取用户详情

- **接口**: `GET /api/v1/users/{id}`
- **权限**: 需要 `user:manage` 权限
- **响应**: 返回用户详细信息

### 创建用户

- **接口**: `POST /api/v1/users`
- **权限**: 需要 `user:manage` 权限
- **请求体**: 用户信息（密码会自动BCrypt加密）
```json
{
  "username": "testuser",
  "phone": "13900139000",
  "email": "test@example.com",
  "password": "password123",
  "roleId": 2,
  "status": 1
}
```

### 更新用户

- **接口**: `PUT /api/v1/users/{id}`
- **权限**: 需要 `user:manage` 权限
- **说明**: 如果提供密码，会自动加密

### 删除用户

- **接口**: `DELETE /api/v1/users/{id}`
- **权限**: 需要 `user:manage` 权限
- **说明**: 逻辑删除

### 更新用户状态

- **接口**: `PUT /api/v1/users/{id}/status`
- **权限**: 需要 `user:manage` 权限
- **请求体**: `{"status": 0}` 或 `{"status": 1}`

### 解锁用户

- **接口**: `PUT /api/v1/users/{id}/unlock`
- **权限**: 需要 `user:manage` 权限
- **说明**: 清除锁定时间和失败次数

### 获取已删除用户列表（回收站）

- **接口**: `GET /api/v1/users/deleted`
- **权限**: 需要 `user:manage` 权限
- **参数**: `page`, `size`, `keyword`

### 恢复用户

- **接口**: `PUT /api/v1/users/{id}/restore`
- **权限**: 需要 `user:manage` 权限
- **说明**: 从回收站恢复已删除的用户

### 永久删除用户

- **接口**: `DELETE /api/v1/users/{id}/permanent`
- **权限**: 需要 `user:manage` 权限
- **请求体**: `{"confirmText": "DELETE"}`
- **说明**: 物理删除用户，需要输入"DELETE"确认

### 获取所有权限列表

- **接口**: `GET /api/v1/users/permissions/all`
- **权限**: 需要 `user:manage` 权限
- **说明**: 获取系统中所有可用的权限列表

### 获取用户权限

- **接口**: `GET /api/v1/users/{id}/permissions`
- **权限**: 需要 `user:manage` 权限
- **说明**: 获取指定用户的所有权限

### 更新用户权限

- **接口**: `PUT /api/v1/users/{id}/permissions`
- **权限**: 需要 `user:manage` 权限
- **请求体**: `{"permissionIds": [1, 2, 3]}`
- **说明**: 更新用户的权限列表

---

## 角色管理接口

### 获取角色列表

- **接口**: `GET /api/v1/roles`
- **权限**: 需要 `user:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（角色名称）
  - `status`: 状态（0-禁用/1-正常，可选）

### 获取角色详情

- **接口**: `GET /api/v1/roles/{id}`
- **权限**: 需要 `user:manage` 权限

### 创建角色

- **接口**: `POST /api/v1/roles`
- **权限**: 需要 `user:manage` 权限
- **请求体**: 角色信息（role_code必须唯一）
```json
{
  "roleName": "测试角色",
  "roleCode": "TEST_ROLE",
  "description": "测试角色描述",
  "status": 1
}
```

### 更新角色

- **接口**: `PUT /api/v1/roles/{id}`
- **权限**: 需要 `user:manage` 权限

### 删除角色

- **接口**: `DELETE /api/v1/roles/{id}`
- **权限**: 需要 `user:manage` 权限
- **说明**: 逻辑删除

### 更新角色状态

- **接口**: `PUT /api/v1/roles/{id}/status`
- **权限**: 需要 `user:manage` 权限
- **请求体**: `{"status": 0}` 或 `{"status": 1}`

---

## 参数配置接口

### 获取配置列表

- **接口**: `GET /api/v1/configs`
- **权限**: 需要 `config:view` 或 `config:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（配置键名/配置值）
  - `configType`: 配置类型（可选）

### 获取配置详情

- **接口**: `GET /api/v1/configs/{id}`
- **权限**: 需要 `config:view` 或 `config:manage` 权限

### 根据键名获取配置

- **接口**: `GET /api/v1/configs/key/{configKey}`
- **权限**: 需要 `config:view` 或 `config:manage` 权限
- **说明**: 用于获取特定配置值

### 创建配置

- **接口**: `POST /api/v1/configs`
- **权限**: 需要 `config:manage` 权限
- **说明**: config_key必须唯一

### 更新配置

- **接口**: `PUT /api/v1/configs/{id}`
- **权限**: 需要 `config:manage` 权限

### 删除配置

- **接口**: `DELETE /api/v1/configs/{id}`
- **权限**: 需要 `config:manage` 权限
- **说明**: 逻辑删除

---

## 通知公告接口

### 获取通知公告列表

- **接口**: `GET /api/v1/notices`
- **权限**: 需要 `notice:view` 或 `notice:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（公告标题/内容）
  - `noticeType`: 公告类型（可选）
  - `status`: 状态（0-关闭/1-开启，可选）

### 获取通知公告详情

- **接口**: `GET /api/v1/notices/{id}`
- **权限**: 需要 `notice:view` 或 `notice:manage` 权限

### 创建通知公告

- **接口**: `POST /api/v1/notices`
- **权限**: 需要 `notice:manage` 权限
- **请求体**:
```json
{
  "noticeTitle": "系统通知",
  "noticeType": 1,
  "noticeContent": "通知内容",
  "status": 1
}
```

### 更新通知公告

- **接口**: `PUT /api/v1/notices/{id}`
- **权限**: 需要 `notice:manage` 权限

### 删除通知公告

- **接口**: `DELETE /api/v1/notices/{id}`
- **权限**: 需要 `notice:manage` 权限
- **说明**: 逻辑删除

### 更新通知公告状态

- **接口**: `PUT /api/v1/notices/{id}/status`
- **权限**: 需要 `notice:manage` 权限
- **请求体**: `{"status": 0}` 或 `{"status": 1}`

---

## 操作日志接口

### 获取操作日志列表

- **接口**: `GET /api/v1/operation-logs`
- **权限**: 需要 `log:operation:view` 权限（仅系统管理员）
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（用户名/操作内容）
  - `userId`: 用户ID（可选）
  - `module`: 操作模块（可选）
  - `operationType`: 操作类型（INSERT/UPDATE/DELETE/SELECT，可选）
  - `status`: 操作状态（0-失败/1-成功，可选）

### 获取操作日志详情

- **接口**: `GET /api/v1/operation-logs/{id}`
- **权限**: 需要 `log:operation:view` 权限（仅系统管理员）

---

## 登录日志接口

### 获取登录日志列表

- **接口**: `GET /api/v1/login-logs`
- **权限**: 需要 `log:login:view` 权限（仅系统管理员）
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（用户名）
  - `userId`: 用户ID（可选）
  - `status`: 登录状态（0-失败/1-成功，可选）

### 获取登录日志详情

- **接口**: `GET /api/v1/login-logs/{id}`
- **权限**: 需要 `log:login:view` 权限（仅系统管理员）

---

## 库存管理接口

### 获取库存列表

- **接口**: `GET /api/v1/inventory`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（药品名称、批次号）
  - `drugId`: 药品ID（可选）
  - `batchNumber`: 批次号（可选）
  - `storageLocation`: 存储位置（可选）
  - `expiryDateStart`: 有效期开始日期（可选，格式：YYYY-MM-DD）
  - `expiryDateEnd`: 有效期结束日期（可选，格式：YYYY-MM-DD）
  - `isSpecial`: 是否特殊药品（0-否/1-是，可选）

### 获取库存详情

- **接口**: `GET /api/v1/inventory/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 获取近效期预警

- **接口**: `GET /api/v1/inventory/near-expiry-warning`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 返回近效期药品列表（默认90天内到期）
- **响应格式**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 10,
    "warning90": 5,
    "warning180": 3
  }
}
```

### 获取库存总数

- **接口**: `GET /api/v1/inventory/total`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 返回库存总数量

### 导出库存列表

- **接口**: `GET /api/v1/inventory/export`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 与获取库存列表接口相同
- **说明**: 导出Excel文件，包含导出人和导出时间水印

---

## 药品信息管理接口

### 获取药品信息列表

- **接口**: `GET /api/v1/drugs`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（药品名称、商品码等）
  - `isSpecial`: 是否特殊药品（0-否/1-是，可选）
  - `supplierId`: 供应商ID（可选，根据供应商ID查询该供应商提供的药品）

### 获取药品信息详情

- **接口**: `GET /api/v1/drugs/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 创建药品信息

- **接口**: `POST /api/v1/drugs`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: 药品信息对象

### 更新药品信息

- **接口**: `PUT /api/v1/drugs/{id}`
- **权限**: 需要 `drug:manage` 权限

### 删除药品信息

- **接口**: `DELETE /api/v1/drugs/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 逻辑删除

### 根据商品码/本位码/追溯码查询

- **接口**: `GET /api/v1/drugs/search?code={code}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 
  - 先查询本地数据库，如果未找到则调用极速数据API
  - 支持商品码、本位码、追溯码三种编码查询

### 根据药品名称查询

- **接口**: `GET /api/v1/drugs/search/name?drugName={drugName}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 
  - 调用万维易源API获取基本信息
  - 然后自动调用极速数据API补充信息
  - 自动合并两个API的数据

### 根据批准文号查询

- **接口**: `GET /api/v1/drugs/search/approval?approvalNumber={approvalNumber}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 
  - 调用万维易源API获取基本信息
  - 然后自动调用极速数据API补充信息
  - 自动合并两个API的数据

### 根据供应商ID查询药品列表

- **接口**: `GET /api/v1/drugs/supplier/{supplierId}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 查询指定供应商关联的所有药品

### 导出药品列表

- **接口**: `GET /api/v1/drugs/export`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: `keyword`, `isSpecial`
- **说明**: 导出Excel文件，包含导出人和导出时间水印

---

## 入库管理接口

### 获取入库记录列表

- **接口**: `GET /api/v1/inbound`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（入库单号、药品名称、批次号）
  - `orderId`: 采购订单ID（可选）
  - `drugId`: 药品ID（可选）
  - `batchNumber`: 批次号（可选）
  - `operatorId`: 操作人ID（可选）
  - `startDate`: 开始日期（可选，格式：YYYY-MM-DD）
  - `endDate`: 结束日期（可选，格式：YYYY-MM-DD）
  - `status`: 验收状态（QUALIFIED/UNQUALIFIED，可选）
  - `expiryCheckStatus`: 效期校验状态（PASS/WARNING/FORCE，可选）

### 获取入库记录详情

- **接口**: `GET /api/v1/inbound/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 根据入库单号查询入库记录

- **接口**: `GET /api/v1/inbound/record-number/{recordNumber}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 创建入库记录（从采购订单入库）

- **接口**: `POST /api/v1/inbound/from-order`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "orderId": 1,
  "drugId": 1,
  "batchNumber": "BATCH001",
  "quantity": 100,
  "expiryDate": "2026-12-31",
  "arrivalDate": "2026-01-20",
  "productionDate": "2025-12-01",
  "manufacturer": "生产厂家",
  "deliveryNoteNumber": "DN001",
  "deliveryNoteImage": "/uploads/2026/01/20/image.jpg",
  "secondOperatorId": 2,
  "status": "QUALIFIED",
  "expiryCheckStatus": "PASS",
  "expiryCheckReason": "效期检查通过",
  "remark": "备注信息"
}
```
- **说明**: 
  - 从采购订单创建入库记录
  - 包含效期校验和特殊药品双人操作
  - 自动验证订单状态和可入库数量

### 创建临时入库记录

- **接口**: `POST /api/v1/inbound/temporary`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: 与从订单入库类似，但不包含orderId
- **说明**: 不关联采购订单的临时入库

### 效期校验

- **接口**: `POST /api/v1/inbound/check-expiry`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "expiryDate": "2026-12-31"
}
```
- **响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "status": "PASS",
    "message": "效期检查通过"
  }
}
```
- **说明**: 校验药品有效期，返回校验结果（PASS/WARNING/FORCE）

### 获取订单药品可入库数量

- **接口**: `GET /api/v1/inbound/order/{orderId}/drug/{drugId}/quantity`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取指定订单中指定药品的可入库数量

### 获取今日入库数量

- **接口**: `GET /api/v1/inbound/today-count`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 返回今日入库记录总数

---

## 出库管理接口

### 获取出库申请列表

- **接口**: `GET /api/v1/outbound`
- **权限**: 需要 `outbound:view`、`outbound:apply`、`outbound:approve` 或 `outbound:execute` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（申领单号、申请人、科室）
  - `applicantId`: 申请人ID（可选）
  - `approverId`: 审批人ID（可选）
  - `department`: 所属科室（可选）
  - `status`: 申请状态（PENDING/APPROVED/REJECTED/OUTBOUND/CANCELLED，可选）
  - `startDate`: 开始日期（可选，格式：YYYY-MM-DD）
  - `endDate`: 结束日期（可选，格式：YYYY-MM-DD）

### 获取出库申请详情

- **接口**: `GET /api/v1/outbound/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 根据申领单号查询出库申请

- **接口**: `GET /api/v1/outbound/apply-number/{applyNumber}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 创建出库申请（医护人员申领）

- **接口**: `POST /api/v1/outbound`
- **权限**: 需要 `outbound:apply` 权限
- **请求体**:
```json
{
  "department": "内科",
  "purpose": "日常用药",
  "items": [
    {
      "drugId": 1,
      "batchNumber": "BATCH001",
      "quantity": 10,
      "remark": "备注"
    }
  ],
  "remark": "申请备注"
}
```

### 审批出库申请（通过）

- **接口**: `POST /api/v1/outbound/{id}/approve`
- **权限**: 需要 `outbound:approve` 或 `outbound:approve:special` 权限
- **请求体**:
```json
{
  "secondApproverId": 2
}
```
- **说明**: 特殊药品需要提供 `secondApproverId`（第二审批人ID）

### 审批出库申请（驳回）

- **接口**: `POST /api/v1/outbound/{id}/reject`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "rejectReason": "驳回理由"
}
```

### 执行出库

- **接口**: `POST /api/v1/outbound/{id}/execute`
- **权限**: 需要 `outbound:execute` 权限
- **请求体**:
```json
{
  "outboundItems": [
    {
      "drugId": 1,
      "batchNumber": "BATCH001",
      "quantity": 10
    }
  ]
}
```
- **说明**: 执行出库操作，自动扣减库存（先进先出FIFO）

### 取消出库申请

- **接口**: `POST /api/v1/outbound/{id}/cancel`
- **权限**: 需要 `outbound:apply` 权限
- **说明**: 取消待审批的出库申请

### 获取待审批数量

- **接口**: `GET /api/v1/outbound/pending-count`
- **权限**: 需要 `outbound:approve` 权限
- **说明**: 返回待审批的出库申请数量

### 获取今日出库数量

- **接口**: `GET /api/v1/outbound/today-count`
- **权限**: 需要 `outbound:view` 或 `outbound:execute` 权限
- **说明**: 返回今日出库记录总数

### 获取出库申请明细

- **接口**: `GET /api/v1/outbound/{id}/items`
- **权限**: 需要 `outbound:view` 权限
- **说明**: 获取指定出库申请的所有明细项

---

## 采购订单管理接口

### 获取采购订单列表

- **接口**: `GET /api/v1/purchase-orders`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（订单编号、供应商名称）
  - `supplierId`: 供应商ID（可选）
  - `purchaserId`: 采购员ID（可选）
  - `status`: 订单状态（PENDING/CONFIRMED/SHIPPED/RECEIVED/CANCELLED，可选）
  - `startDate`: 开始日期（可选，格式：YYYY-MM-DD）
  - `endDate`: 结束日期（可选，格式：YYYY-MM-DD）
- **说明**: 供应商角色只能查看自己的订单

### 获取采购订单详情

- **接口**: `GET /api/v1/purchase-orders/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 根据订单编号查询

- **接口**: `GET /api/v1/purchase-orders/order-number/{orderNumber}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 根据订单编号查询订单详情（用于条形码扫描）

### 创建采购订单

- **接口**: `POST /api/v1/purchase-orders`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "supplierId": 1,
  "expectedDeliveryDate": "2026-02-01",
  "items": [
    {
      "drugId": 1,
      "quantity": 100,
      "unitPrice": 10.50,
      "remark": "备注"
    }
  ],
  "remark": "订单备注"
}
```

### 更新采购订单

- **接口**: `PUT /api/v1/purchase-orders/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 只能更新待确认状态的订单

### 删除采购订单

- **接口**: `DELETE /api/v1/purchase-orders/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 逻辑删除，只能删除待确认状态的订单

### 更新订单状态

- **接口**: `PUT /api/v1/purchase-orders/{id}/status`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "status": "CONFIRMED",
  "logisticsNumber": "SF1234567890",
  "shipDate": "2026-01-20T10:00:00"
}
```
- **说明**: 订单状态流转：PENDING → CONFIRMED → SHIPPED → RECEIVED

### 确认订单

- **接口**: `POST /api/v1/purchase-orders/{id}/confirm`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 供应商确认订单（PENDING → CONFIRMED）

### 驳回订单

- **接口**: `POST /api/v1/purchase-orders/{id}/reject`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: `{"reason": "驳回原因"}`
- **说明**: 供应商驳回订单（PENDING → REJECTED）

### 发货

- **接口**: `POST /api/v1/purchase-orders/{id}/ship`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: `{"logisticsNumber": "SF1234567890"}`
- **说明**: 供应商发货（CONFIRMED → SHIPPED）

### 取消订单

- **接口**: `POST /api/v1/purchase-orders/{id}/cancel`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: `{"reason": "取消原因"}`
- **说明**: 取消订单（状态变为CANCELLED）

### 更新物流单号

- **接口**: `PUT /api/v1/purchase-orders/{id}/logistics`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: `{"logisticsNumber": "SF1234567890"}`
- **说明**: 更新订单的物流单号（供应商可更新自己订单的物流单号）

### 生成订单条形码

- **接口**: `GET /api/v1/purchase-orders/{id}/barcode`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 返回订单条形码图片（Base64格式）

### 下载订单条形码

- **接口**: `GET /api/v1/purchase-orders/{id}/barcode/download`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 下载订单条形码图片文件

### 获取订单明细

- **接口**: `GET /api/v1/purchase-orders/{id}/items`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取订单的所有明细项

### 获取订单药品可入库数量

- **接口**: `GET /api/v1/purchase-orders/{id}/inbound-quantities`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取订单中每个药品的可入库数量

### 检查订单是否可以入库

- **接口**: `GET /api/v1/purchase-orders/{id}/can-inbound`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 检查订单是否可以进行入库操作

### 检查订单是否已全部入库

- **接口**: `GET /api/v1/purchase-orders/{id}/is-fully-inbound`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 检查订单是否已全部入库完成

### 获取已发货订单列表

- **接口**: `GET /api/v1/purchase-orders/shipped-orders`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取所有已发货的订单列表

### 导出采购订单列表

- **接口**: `GET /api/v1/purchase-orders/export`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: `keyword`, `supplierId`, `purchaserId`, `status`
- **说明**: 导出Excel文件，包含导出人和导出时间水印

---

## 供应商管理接口

### 获取供应商列表

- **接口**: `GET /api/v1/suppliers`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（供应商名称、联系人、电话）
  - `status`: 状态（0-禁用/1-启用/2-待审核，可选）
  - `auditStatus`: 审核状态（0-待审核/1-已通过/2-已驳回，可选）

### 获取供应商详情

- **接口**: `GET /api/v1/suppliers/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 创建供应商

- **接口**: `POST /api/v1/suppliers`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "name": "供应商名称",
  "contactPerson": "联系人",
  "phone": "13800138000",
  "address": "地址",
  "creditCode": "统一社会信用代码",
  "licenseImage": "/uploads/license.jpg",
  "licenseExpiryDate": "2026-12-31",
  "remark": "备注信息",
  "status": 2
}
```
- **说明**: 采购专员可创建，但需要审核（默认status=2，auditStatus=0）

### 更新供应商

- **接口**: `PUT /api/v1/suppliers/{id}`
- **权限**: 需要 `drug:manage` 权限

### 删除供应商

- **接口**: `DELETE /api/v1/suppliers/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 逻辑删除

### 更新供应商状态

- **接口**: `POST /api/v1/suppliers/{id}/status`
- **权限**: 需要 `drug:manage` 权限
- **请求体**: `{"status": 1}` 或 `{"status": 0}`
- **说明**: 启用或禁用供应商

### 审核供应商

- **接口**: `POST /api/v1/suppliers/{id}/audit`
- **权限**: 需要 `supplier:audit` 权限
- **请求体**:
```json
{
  "auditStatus": 1,
  "auditReason": "审核通过"
}
```
- **说明**: 
  - `auditStatus`: 1-已通过/2-已驳回
  - 仅仓库管理员可以审核供应商

### 获取供应商关联的药品列表

- **接口**: `GET /api/v1/suppliers/{id}/drugs`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取指定供应商关联的所有药品

---

## 库存调整接口

### 获取库存调整记录列表

- **接口**: `GET /api/v1/inventory-adjustments`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（调整单号、药品名称、批次号）
  - `drugId`: 药品ID（可选）
  - `batchNumber`: 批次号（可选）
  - `adjustmentType`: 调整类型（PROFIT-盘盈/LOSS-盘亏，可选）
  - `operatorId`: 操作人ID（可选）
  - `startDate`: 开始日期（可选，格式：YYYY-MM-DD）
  - `endDate`: 结束日期（可选，格式：YYYY-MM-DD）

### 获取库存调整记录详情

- **接口**: `GET /api/v1/inventory-adjustments/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 创建库存调整记录

- **接口**: `POST /api/v1/inventory-adjustments`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "drugId": 1,
  "batchNumber": "BATCH001",
  "adjustmentType": "PROFIT",
  "quantityBefore": 100,
  "quantityAfter": 120,
  "adjustmentReason": "盘点发现多出20盒",
  "secondOperatorId": 2,
  "adjustmentImage": "/uploads/2026/01/15/image.jpg",
  "remark": "备注信息"
}
```
- **说明**: 
  - `adjustmentType`: PROFIT-盘盈/LOSS-盘亏
  - 特殊药品需要提供 `secondOperatorId`（第二操作人ID）
  - 调整数量自动计算：`adjustmentQuantity = quantityAfter - quantityBefore`

---

## 仪表盘接口

### 获取基础统计数据

- **接口**: `GET /api/v1/dashboard/statistics`
- **权限**: 所有登录用户都可以访问
- **说明**: 返回基础统计数据（用户数、药品数、订单数等）

### 获取登录趋势

- **接口**: `GET /api/v1/dashboard/login-trend`
- **权限**: 需要 `log:login:view` 权限（仅系统管理员）
- **说明**: 返回登录趋势数据（最近7天登录统计）

### 获取操作日志统计

- **接口**: `GET /api/v1/dashboard/operation-statistics`
- **权限**: 需要 `log:operation:view` 权限（仅系统管理员）
- **说明**: 返回操作日志统计数据

### 获取仓库管理员仪表盘数据

- **接口**: `GET /api/v1/dashboard/warehouse`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 返回仓库管理员专用仪表盘数据（近效期预警、待办任务、出入库统计等）

### 获取采购专员仪表盘数据

- **接口**: `GET /api/v1/dashboard/purchaser`
- **权限**: 所有登录用户都可以访问（但仅采购专员角色返回有效数据）
- **说明**: 返回采购专员的订单统计、供应商统计、订单趋势等数据

### 获取医护人员仪表盘数据

- **接口**: `GET /api/v1/dashboard/medical-staff`
- **权限**: 所有登录用户都可以访问（但仅医护人员角色返回有效数据）
- **说明**: 返回医护人员的申领统计、状态分布、申领趋势等数据

### 获取供应商仪表盘数据

- **接口**: `GET /api/v1/dashboard/supplier`
- **权限**: 所有登录用户都可以访问（但仅供应商角色返回有效数据）
- **说明**: 返回供应商的订单统计、状态分布、金额统计、订单趋势等数据

---

## 供应商-药品关联管理接口

### 添加供应商-药品关联

- **接口**: `POST /api/v1/supplier-drugs`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "supplierId": 1,
  "drugId": 1,
  "unitPrice": 10.50
}
```

### 删除供应商-药品关联

- **接口**: `DELETE /api/v1/supplier-drugs?supplierId={supplierId}&drugId={drugId}`
- **权限**: 需要 `drug:manage` 权限

### 更新供应商-药品关联单价

- **接口**: `PUT /api/v1/supplier-drugs/price`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "supplierId": 1,
  "drugId": 1,
  "unitPrice": 12.00
}
```

---

## 文件上传接口

### 上传文件

- **接口**: `POST /api/v1/upload`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **请求**: multipart/form-data
  - `file`: 文件（仅支持图片格式：jpg, jpeg, png, gif, bmp, webp）
  - 文件大小限制：最大10MB
- **响应**: 返回文件访问URL
```json
{
  "code": 200,
  "msg": "上传成功",
  "data": "/uploads/2026/01/20/uuid-filename.jpg"
}
```
- **说明**: 
  - 文件按日期分类存储（yyyy/MM/dd目录结构）
  - 生成唯一文件名（UUID），防止文件名冲突

### 删除文件

- **接口**: `DELETE /api/v1/upload?url={fileUrl}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 根据文件URL删除已上传的文件

---

## 后端监控接口

### 获取系统信息

- **接口**: `GET /api/v1/system/info`
- **权限**: 无需登录即可访问（公开接口）
- **响应**: 返回系统信息（JVM信息、内存使用、线程信息等）
- **说明**: 提供系统运行状态信息，包括CPU、内存、磁盘使用情况

### 获取最近的日志

- **接口**: `GET /api/v1/logs/recent`
- **权限**: 无需登录即可访问（公开接口）
- **参数**: 
  - `limit`: 返回的日志条数（默认100）
  - `level`: 日志级别过滤（可选：DEBUG、INFO、WARN、ERROR）
- **响应**: 返回最近的日志列表
- **说明**: 用于查询历史日志，实时日志通过WebSocket推送

### 健康检查

- **接口**: `GET /api/v1/health`
- **权限**: 无需登录即可访问（公开接口）
- **响应**: 返回系统健康状态
- **说明**: 用于监控系统健康状态，可用于负载均衡健康检查

### WebSocket实时日志流

- **接口**: `WS /api/v1/logs/stream`
- **权限**: 无需登录即可访问（公开接口）
- **说明**: WebSocket连接，实时推送系统日志
- **功能**:
  - 实时日志推送（通过Logback Appender）
  - 日志级别过滤
  - 心跳保持连接（每30秒）
  - 支持多客户端同时连接
- **消息格式**:
```json
{
  "timestamp": "2026/01/20 10:30:45.123",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.cdiom.backend.controller",
  "message": "日志消息内容"
}
```

---

## 供应商审核接口

### 创建供应商审核申请

- **接口**: `POST /api/v1/supplier-approvals`
- **权限**: 需要 `supplier:approval:apply` 权限
- **说明**: 创建供应商审核申请

### 质量检查

- **接口**: `POST /api/v1/supplier-approvals/{id}/quality-check`
- **权限**: 需要 `supplier:approval:quality` 权限
- **说明**: 对供应商进行质量检查

### 价格审核

- **接口**: `POST /api/v1/supplier-approvals/{id}/price-review`
- **权限**: 需要 `supplier:approval:price` 权限
- **说明**: 对供应商进行价格审核

### 最终审核

- **接口**: `POST /api/v1/supplier-approvals/{id}/final-approve`
- **权限**: 需要 `supplier:approval:final` 权限
- **说明**: 对供应商进行最终审核

### 获取审核详情

- **接口**: `GET /api/v1/supplier-approvals/{id}`
- **权限**: 需要 `supplier:approval:view` 权限
- **说明**: 获取供应商审核申请的详细信息

### 获取审核日志

- **接口**: `GET /api/v1/supplier-approvals/{id}/logs`
- **权限**: 需要 `supplier:approval:view` 权限
- **说明**: 获取供应商审核的日志记录

---

## 供应商药品协议接口

### 创建供应商药品协议

- **接口**: `POST /api/v1/supplier-drug-agreements`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 创建供应商与药品的协议

### 获取协议详情

- **接口**: `GET /api/v1/supplier-drug-agreements/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取协议的详细信息

### 获取当前协议

- **接口**: `GET /api/v1/supplier-drug-agreements/current`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: `supplierId`, `drugId`
- **说明**: 获取供应商和药品的当前有效协议

### 获取协议列表

- **接口**: `GET /api/v1/supplier-drug-agreements/list`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: `supplierId`, `drugId`, `status`
- **说明**: 获取协议列表

### 更新协议

- **接口**: `PUT /api/v1/supplier-drug-agreements/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 更新协议信息

### 删除协议

- **接口**: `DELETE /api/v1/supplier-drug-agreements/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 删除协议（逻辑删除）

---

## 供应商药品价格历史接口

### 获取价格历史列表

- **接口**: `GET /api/v1/supplier-drug-price-history/list`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: `supplierId`, `drugId`, `startDate`, `endDate`
- **说明**: 获取供应商药品的价格历史记录

### 根据协议ID获取价格历史

- **接口**: `GET /api/v1/supplier-drug-price-history/agreement/{agreementId}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 获取指定协议的价格历史记录

---

## 第三方API集成

系统集成了两个第三方药品信息API，用于补充和丰富药品数据。这些API在药品信息查询接口中自动调用，无需前端单独调用。

### 万维易源API（1468-4）

#### API基本信息

- **API名称**: 万维易源药品信息查询API
- **接口版本**: 1468-4（简化版）
- **接口地址**: `https://route.showapi.com/1468-4`
- **请求方式**: POST
- **数据格式**: JSON

#### 配置说明

在 `application.yml` 中配置：

```yaml
yuanyanyao:
  api:
    base-url: https://route.showapi.com/1468-4  # API基础地址
    app-key: your-yuanyanyao-api-key            # API密钥（必填）
    default-classify-id: 599ad2a0600b2149d689b75a  # 默认分类ID（可选）
```

#### 支持的查询方式

1. **按药品名称查询**
   - 搜索类型：`searchType=1`
   - 查询参数：`searchKey`（药品名称）

2. **按批准文号查询**
   - 搜索类型：`searchType=3`
   - 查询参数：`searchKey`（批准文号）

#### 返回字段

| 字段名 | 说明 | 示例 |
|--------|------|------|
| approvalNumber | 批准文号 | 国药准字H20123456 |
| manufacturer | 生产厂家 | 某某制药有限公司 |
| dosageForm | 剂型 | 片剂 |
| specification | 规格 | 0.5g*24片 |
| expiryDate | 有效期 | 24个月 |
| storageRequirement | 存储要求 | 密封，在干燥处保存 |
| drugName | 药品名称 | 阿莫西林胶囊 |

#### 使用场景

- **通过药品名称搜索药品信息**：用户输入药品名称，系统调用API获取基本信息
- **通过批准文号搜索药品信息**：用户输入批准文号，系统调用API获取详细信息

#### 在系统中的应用

- **接口**: `GET /api/v1/drugs/search/name?drugName={drugName}`
- **接口**: `GET /api/v1/drugs/search/approval?approvalNumber={approvalNumber}`
- **说明**: 这两个接口会自动调用万维易源API获取药品基本信息

---

### 极速数据API（JisuAPI）

#### API基本信息

- **API名称**: 极速数据药品信息查询API
- **接口地址**: `https://api.jisuapi.com/medicine/detail`
- **请求方式**: GET
- **数据格式**: JSON
- **API文档**: https://api.jisuapi.com/medicine/detail

#### 配置说明

在 `application.yml` 中配置：

```yaml
jisuapi:
  api:
    base-url: https://api.jisuapi.com/medicine/detail  # API基础地址
    app-key: your-jisuapi-key                           # API密钥（必填）
```

#### 支持的查询方式

1. **按商品码查询**
   - 查询参数：`code`（商品码/本位码/追溯码）
   - 支持商品码、国家本位码、药品追溯码三种编码

2. **按批准文号查询**
   - 查询参数：`code`（批准文号）

#### 返回字段

| 字段名 | 说明 | 示例 |
|--------|------|------|
| nationalCode | 国家本位码 | 8690123456789 |
| barcode | 条形码 | 8690123456789 |
| description | 药品描述 | 用于治疗... |
| specification | 包装规格 | 0.5g*24片/盒 |
| approvalNumber | 批准文号 | 国药准字H20123456 |
| manufacturer | 生产厂家 | 某某制药有限公司 |

#### 使用场景

- **通过商品码/本位码/追溯码搜索药品信息**：扫码识别时，系统调用API查询药品信息
- **补充万维易源API未返回的信息**：国家本位码、条形码、描述等字段
- **用包装规格替换万维易源API返回的规格**：极速数据API的规格字段更准确

#### 在系统中的应用

- **接口**: `GET /api/v1/drugs/search?code={code}`
- **说明**: 先查询本地数据库，如果未找到则调用极速数据API
- **接口**: `GET /api/v1/drugs/search/name?drugName={drugName}`
- **接口**: `GET /api/v1/drugs/search/approval?approvalNumber={approvalNumber}`
- **说明**: 这两个接口会自动调用极速数据API补充信息

---

### API数据合并逻辑

系统实现了智能的数据合并机制，自动整合两个API返回的数据，确保药品信息的完整性和准确性。

#### 通过药品名称/批准文号查询的合并流程

**流程说明**：

1. **第一步：调用万维易源API**
   - 根据药品名称或批准文号调用万维易源API
   - 获取药品基本信息（批准文号、生产厂家、剂型、规格等）

2. **第二步：自动调用极速数据API补充信息**
   - 如果万维易源API返回了批准文号，自动调用极速数据API
   - 使用批准文号作为查询参数

3. **第三步：智能合并数据**
   - 将极速数据API返回的信息合并到万维易源API返回的信息中
   - **合并规则**：
     - 极速数据API的字段优先（用于补充国家本位码、条形码、描述等）
     - 极速数据API的规格字段会替换万维易源API的规格字段（更准确）
     - 其他字段如果万维易源API已有，则保留；如果极速数据API有补充，则使用极速数据API的值

**示例**：

```json
// 万维易源API返回
{
  "drugName": "阿莫西林胶囊",
  "approvalNumber": "国药准字H20123456",
  "manufacturer": "某某制药有限公司",
  "specification": "0.5g*24片"
}

// 极速数据API返回
{
  "nationalCode": "8690123456789",
  "barcode": "8690123456789",
  "description": "用于治疗...",
  "specification": "0.5g*24片/盒"
}

// 合并后的结果
{
  "drugName": "阿莫西林胶囊",
  "approvalNumber": "国药准字H20123456",
  "manufacturer": "某某制药有限公司",
  "specification": "0.5g*24片/盒",  // 使用极速数据API的规格
  "nationalCode": "8690123456789",  // 补充国家本位码
  "barcode": "8690123456789",       // 补充条形码
  "description": "用于治疗..."      // 补充描述
}
```

#### 通过商品码/本位码/追溯码查询的流程

**流程说明**：

1. **第一步：查询本地数据库**
   - 根据商品码、本位码或追溯码查询本地数据库
   - 如果找到，直接返回本地数据

2. **第二步：调用极速数据API**
   - 如果本地数据库未找到，调用极速数据API查询
   - 使用商品码/本位码/追溯码作为查询参数

3. **第三步：返回查询结果**
   - 如果极速数据API返回了数据，返回API数据
   - 如果仍未找到，返回null

**优势**：
- 优先使用本地数据，减少API调用次数
- 本地数据查询速度快，用户体验好
- API作为补充，确保数据完整性

---

### API配置注意事项

#### 1. API密钥配置

- **万维易源API**：需要在万维易源官网注册账号并获取API密钥
- **极速数据API**：需要在极速数据官网注册账号并获取API密钥
- **配置位置**：`application.yml` 或 `application-local.yml`
- **安全建议**：生产环境建议使用环境变量或配置中心管理API密钥

#### 2. API调用限制

- **万维易源API**：根据购买的套餐有不同的调用次数限制
- **极速数据API**：根据购买的套餐有不同的调用次数限制
- **建议**：系统已实现本地数据库优先查询，减少API调用次数

#### 3. 错误处理

- **API调用失败**：系统会记录警告日志，但不影响其他功能
- **API返回错误**：系统会返回null，前端会提示"未找到药品信息"
- **网络超时**：系统设置了合理的超时时间，避免长时间等待

#### 4. 数据缓存建议

- **本地数据库**：系统会将查询到的药品信息保存到本地数据库
- **后续查询**：相同药品的后续查询会优先使用本地数据
- **数据更新**：可以通过药品管理页面手动更新药品信息

---

### API集成优势

1. **数据完整性**：整合两个API的数据，确保药品信息完整
2. **数据准确性**：优先使用更准确的字段（如极速数据API的规格字段）
3. **用户体验**：自动合并数据，用户无需关心API调用细节
4. **性能优化**：本地数据库优先，减少API调用次数
5. **容错机制**：API调用失败不影响系统其他功能

---

## 注意事项

1. **Token认证**: 除登录接口和公开接口外，所有接口都需要在请求头中携带Token（通过Cookie自动携带）
2. **权限控制**: 接口支持细粒度权限控制，请确保用户拥有相应权限
3. **参数验证**: 所有请求参数都会进行验证，请确保参数格式正确
4. **错误处理**: 统一使用Result<T>格式返回错误信息，请根据code和msg字段判断错误类型
5. **分页参数**: 列表接口默认page=1，size=10，可根据需要调整
6. **日期格式**: 日期参数统一使用YYYY-MM-DD格式（ISO日期格式）
7. **逻辑删除**: 所有删除操作均为逻辑删除，数据不会真正删除

---

**文档版本**: v1.0.0  
**最后更新**: 2026年1月20日

