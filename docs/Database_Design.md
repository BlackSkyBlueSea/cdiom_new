# CDIOM - 数据库设计文档

本文档详细描述了CDIOM系统的数据库设计，包含数据库表分类、表结构说明、表关系、设计特点等完整的数据库设计内容。

## 目录

- [数据库基本信息](#数据库基本信息)
- [数据库表分类](#数据库表分类)
- [系统表字段说明](#系统表字段说明)
- [权限表字段说明](#权限表字段说明)
- [业务表字段说明](#业务表字段说明)
- [扩展表字段说明](#扩展表字段说明)
- [表关系说明](#表关系说明)
- [设计特点](#设计特点)
- [索引设计](#索引设计)
- [约束设计](#约束设计)

---

## 数据库基本信息

- **数据库名称**：cdiom_db
- **数据库版本**：MySQL 8.0.33+
- **字符集**：utf8mb4
- **排序规则**：utf8mb4_unicode_ci
- **存储引擎**：InnoDB
- **总表数**：21张（包含系统表、权限表、业务表、扩展表）

---

## 数据库表分类

### 系统表（6张）
1. `sys_role` - 系统角色表
2. `sys_user` - 系统用户表
3. `sys_config` - 系统参数配置表
4. `sys_notice` - 系统通知公告表
5. `operation_log` - 操作日志表
6. `login_log` - 登录日志表

### 权限表（4张）
7. `sys_user_role` - 用户角色关联表
8. `sys_permission` - 权限表
9. `sys_role_permission` - 角色权限关联表
10. `sys_user_permission` - 用户权限关联表（支持用户直接拥有权限，v1.5.0新增）

### 业务表（10张）
11. `supplier` - 供应商表
12. `drug_info` - 药品信息表
13. `supplier_drug` - 供应商-药品关联表（支持多对多关系）
14. `inventory` - 库存表（按批次管理）
15. `purchase_order` - 采购订单表
16. `purchase_order_item` - 采购订单明细表
17. `inbound_record` - 入库记录表
18. `outbound_apply` - 出库申请表
19. `outbound_apply_item` - 出库申请明细表
20. `inventory_adjustment` - 库存调整记录表

### 扩展表（1张）
21. `favorite_drug` - 常用药品收藏表（功能待实现）

### 核心表说明

以下是对系统核心业务表的简要说明：

- **sys_user（用户表）**：存储用户基本信息，支持用户名/手机号登录，包含登录锁定机制
- **sys_role（角色表）**：存储角色信息，支持角色状态管理
- **drug_info（药品信息表）**：存储药品基本信息，支持扫码识别（国家本位码、追溯码、商品码）
- **inventory（库存表）**：按批次管理库存，支持近效期预警，禁止负库存
- **purchase_order（采购订单表）**：采购订单管理，支持订单状态流转和物流跟踪
- **inbound_record（入库记录表）**：入库记录管理，支持从订单入库和临时入库，包含效期校验和特殊药品双人操作
- **outbound_apply（出库申请表）**：出库申请管理，支持审批流程和特殊药品双人审批

---

## 系统表字段说明

### sys_role（角色表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| role_name | VARCHAR(50) | 角色名称 | NOT NULL |
| role_code | VARCHAR(50) | 角色代码（唯一，如：SUPER_ADMIN） | NOT NULL, UNIQUE |
| description | VARCHAR(255) | 角色描述 | |
| status | TINYINT | 状态（0-禁用/1-正常） | DEFAULT 1 |
| create_by | BIGINT | 创建人ID | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除标记（0-未删除/1-已删除） | DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_role_code (role_code)
- KEY idx_status (status)
- KEY idx_deleted (deleted)

### sys_user（用户表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| username | VARCHAR(50) | 用户名（唯一，用于登录） | NOT NULL, UNIQUE |
| phone | VARCHAR(20) | 手机号（唯一，用于登录） | UNIQUE |
| email | VARCHAR(100) | 邮箱地址（用于超级管理员验证） | |
| password | VARCHAR(255) | 密码（BCrypt加密） | NOT NULL |
| role_id | BIGINT | 角色ID（外键） | FOREIGN KEY |
| status | TINYINT | 状态（0-禁用/1-正常） | DEFAULT 1 |
| lock_time | DATETIME | 锁定时间（登录失败5次后锁定1小时） | |
| login_fail_count | INT | 登录失败次数 | DEFAULT 0 |
| create_by | BIGINT | 创建人ID | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除标记（0-未删除/1-已删除） | DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_username (username)
- UNIQUE KEY uk_phone (phone)
- KEY idx_role_id (role_id)
- KEY idx_status (status)
- KEY idx_deleted (deleted)

**外键约束**：
- CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE SET NULL

### sys_config（系统参数配置表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| config_name | VARCHAR(100) | 配置名称 | NOT NULL |
| config_key | VARCHAR(100) | 配置键名（唯一） | NOT NULL, UNIQUE |
| config_value | TEXT | 配置值 | |
| config_type | TINYINT | 配置类型 | DEFAULT 1 |
| remark | VARCHAR(500) | 备注 | |
| create_by | BIGINT | 创建人ID | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除标记（0-未删除/1-已删除） | DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_config_key (config_key)
- KEY idx_config_type (config_type)
- KEY idx_deleted (deleted)

### sys_notice（系统通知公告表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| notice_title | VARCHAR(200) | 公告标题 | NOT NULL |
| notice_type | TINYINT | 公告类型 | DEFAULT 1 |
| notice_content | TEXT | 公告内容 | |
| status | TINYINT | 状态（0-关闭/1-开启） | DEFAULT 1 |
| create_by | BIGINT | 创建人ID | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除标记（0-未删除/1-已删除） | DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_notice_type (notice_type)
- KEY idx_status (status)
- KEY idx_deleted (deleted)

### operation_log（操作日志表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| user_id | BIGINT | 操作人ID | |
| username | VARCHAR(50) | 操作人用户名 | |
| module | VARCHAR(50) | 操作模块 | |
| operation_type | VARCHAR(20) | 操作类型（INSERT/UPDATE/DELETE/SELECT） | |
| operation_content | VARCHAR(500) | 操作内容 | |
| request_method | VARCHAR(10) | 请求方法（GET/POST/PUT/DELETE） | |
| request_url | VARCHAR(500) | 请求URL | |
| request_params | TEXT | 请求参数 | |
| ip | VARCHAR(50) | IP地址 | |
| status | TINYINT | 操作状态（0-失败/1-成功） | DEFAULT 1 |
| error_msg | TEXT | 错误信息 | |
| operation_time | DATETIME | 操作时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_module (module)
- KEY idx_operation_type (operation_type)
- KEY idx_status (status)
- KEY idx_operation_time (operation_time)

**说明**：操作日志不可删除，符合合规要求（保留5年）

### login_log（登录日志表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | |
| username | VARCHAR(50) | 用户名 | |
| ip | VARCHAR(50) | 登录IP | |
| location | VARCHAR(100) | 登录地点（IP定位） | |
| browser | VARCHAR(50) | 浏览器类型 | |
| os | VARCHAR(50) | 操作系统 | |
| status | TINYINT | 登录状态（0-失败/1-成功） | DEFAULT 1 |
| msg | VARCHAR(500) | 登录消息 | |
| login_time | DATETIME | 登录时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_status (status)
- KEY idx_login_time (login_time)

**说明**：登录日志不可删除，符合合规要求（保留5年）

---

## 权限表字段说明

### sys_user_role（用户角色关联表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | NOT NULL, FOREIGN KEY |
| role_id | BIGINT | 角色ID | NOT NULL, FOREIGN KEY |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_role (user_id, role_id)
- KEY idx_user_id (user_id)
- KEY idx_role_id (role_id)

**外键约束**：
- CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
- CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE

### sys_permission（权限表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| permission_name | VARCHAR(100) | 权限名称 | NOT NULL |
| permission_code | VARCHAR(100) | 权限代码（唯一） | NOT NULL, UNIQUE |
| permission_type | TINYINT | 权限类型（1-菜单/2-按钮/3-接口） | NOT NULL |
| parent_id | BIGINT | 父权限ID | DEFAULT 0 |
| sort_order | INT | 排序顺序 | DEFAULT 0 |
| is_required | TINYINT | 是否必需权限（0-否/1-是） | DEFAULT 0 |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_permission_code (permission_code)
- KEY idx_parent_id (parent_id)
- KEY idx_permission_type (permission_type)

### sys_role_permission（角色权限关联表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| role_id | BIGINT | 角色ID | NOT NULL, FOREIGN KEY |
| permission_id | BIGINT | 权限ID | NOT NULL, FOREIGN KEY |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_role_permission (role_id, permission_id)
- KEY idx_role_id (role_id)
- KEY idx_permission_id (permission_id)

**外键约束**：
- CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
- CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE

### sys_user_permission（用户权限关联表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | NOT NULL, FOREIGN KEY |
| permission_id | BIGINT | 权限ID | NOT NULL, FOREIGN KEY |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_permission (user_id, permission_id)
- KEY idx_user_id (user_id)
- KEY idx_permission_id (permission_id)

**外键约束**：
- CONSTRAINT fk_user_permission_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
- CONSTRAINT fk_user_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE

**说明**：v1.5.0新增，支持用户直接拥有权限，实现更细粒度的权限控制

---

## 业务表字段说明

### supplier（供应商表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| name | VARCHAR(200) | 供应商名称 | NOT NULL |
| contact_person | VARCHAR(50) | 联系人 | |
| phone | VARCHAR(20) | 联系电话 | |
| address | VARCHAR(500) | 地址 | |
| credit_code | VARCHAR(100) | 统一社会信用代码 | |
| license_image | VARCHAR(500) | 许可证图片路径 | |
| license_expiry_date | DATE | 许可证到期日期 | |
| status | TINYINT | 状态（0-禁用/1-启用/2-待审核） | DEFAULT 1 |
| audit_status | TINYINT | 审核状态（0-待审核/1-已通过/2-已驳回） | DEFAULT 0 |
| audit_reason | VARCHAR(500) | 审核理由 | |
| audit_by | BIGINT | 审核人ID | FOREIGN KEY |
| audit_time | DATETIME | 审核时间 | |
| remark | VARCHAR(2000) | 备注 | |
| create_by | BIGINT | 创建人ID | FOREIGN KEY |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除标记（0-未删除/1-已删除） | DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_name (name)
- KEY idx_status (status)
- KEY idx_credit_code (credit_code)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_supplier_audit_by FOREIGN KEY (audit_by) REFERENCES sys_user(id)
- CONSTRAINT fk_supplier_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id)

### drug_info（药品信息表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| national_code | VARCHAR(50) | 国家本位码（唯一，扫码识别） | NOT NULL, UNIQUE |
| trace_code | VARCHAR(100) | 药品追溯码（唯一） | UNIQUE |
| product_code | VARCHAR(100) | 商品码 | |
| drug_name | VARCHAR(200) | 通用名称 | NOT NULL |
| dosage_form | VARCHAR(50) | 剂型 | |
| specification | VARCHAR(100) | 规格 | |
| approval_number | VARCHAR(100) | 批准文号 | |
| manufacturer | VARCHAR(200) | 生产厂家 | |
| expiry_date | DATE | 有效期 | |
| is_special | TINYINT | 是否特殊药品（0-普通/1-特殊） | DEFAULT 0 |
| storage_requirement | VARCHAR(100) | 存储要求 | |
| storage_location | VARCHAR(200) | 存储位置 | |
| unit | VARCHAR(20) | 单位 | DEFAULT '盒' |
| description | TEXT | 描述 | |
| create_by | BIGINT | 创建人ID | FOREIGN KEY |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除标记（0-未删除/1-已删除） | DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_national_code (national_code)
- UNIQUE KEY uk_trace_code (trace_code)
- KEY idx_product_code (product_code)
- KEY idx_drug_name (drug_name)
- KEY idx_is_special (is_special)
- KEY idx_expiry_date (expiry_date)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_drug_info_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id)

### supplier_drug（供应商-药品关联表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| supplier_id | BIGINT | 供应商ID | NOT NULL, FOREIGN KEY |
| drug_id | BIGINT | 药品ID | NOT NULL, FOREIGN KEY |
| unit_price | DECIMAL(10, 2) | 单价 | DEFAULT 0.00 |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_supplier_drug (supplier_id, drug_id)
- KEY idx_supplier_id (supplier_id)
- KEY idx_drug_id (drug_id)

**外键约束**：
- CONSTRAINT fk_supplier_drug_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE
- CONSTRAINT fk_supplier_drug_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE

**说明**：支持多对多关系，一个药品可以有多个供应商，每个供应商可以设置不同的单价

### inventory（库存表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| drug_id | BIGINT | 药品ID（外键） | NOT NULL, FOREIGN KEY |
| batch_number | VARCHAR(100) | 批次号 | NOT NULL |
| quantity | INT | 库存数量（非负约束） | NOT NULL, DEFAULT 0, CHECK (quantity >= 0) |
| expiry_date | DATE | 有效期至 | NOT NULL |
| storage_location | VARCHAR(200) | 存储位置 | |
| production_date | DATE | 生产日期 | |
| manufacturer | VARCHAR(200) | 生产厂家 | |
| remark | VARCHAR(500) | 备注 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_drug_batch (drug_id, batch_number)
- KEY idx_drug_id (drug_id)
- KEY idx_batch_number (batch_number)
- KEY idx_expiry_date (expiry_date)
- KEY idx_quantity (quantity)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_inventory_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)

**唯一约束**：(drug_id, batch_number) - 同一药品同一批次唯一

**检查约束**：quantity >= 0（禁止负库存）

### purchase_order（采购订单表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| order_number | VARCHAR(50) | 订单编号（唯一） | NOT NULL, UNIQUE |
| supplier_id | BIGINT | 供应商ID（外键） | NOT NULL, FOREIGN KEY |
| purchaser_id | BIGINT | 采购员ID（外键） | NOT NULL, FOREIGN KEY |
| status | VARCHAR(20) | 订单状态（PENDING/CONFIRMED/SHIPPED/RECEIVED/CANCELLED等） | DEFAULT 'PENDING' |
| expected_delivery_date | DATE | 预计交货日期 | |
| logistics_number | VARCHAR(100) | 物流单号 | |
| ship_date | DATETIME | 发货日期 | |
| total_amount | DECIMAL(10, 2) | 订单总金额 | DEFAULT 0.00 |
| remark | VARCHAR(500) | 备注 | |
| reject_reason | VARCHAR(500) | 拒绝理由 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_order_number (order_number)
- KEY idx_supplier_id (supplier_id)
- KEY idx_purchaser_id (purchaser_id)
- KEY idx_status (status)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
- CONSTRAINT fk_purchase_order_purchaser FOREIGN KEY (purchaser_id) REFERENCES sys_user(id)

**订单状态流转**：PENDING → CONFIRMED → SHIPPED → RECEIVED

### purchase_order_item（采购订单明细表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| order_id | BIGINT | 订单ID（外键） | NOT NULL, FOREIGN KEY |
| drug_id | BIGINT | 药品ID（外键） | NOT NULL, FOREIGN KEY |
| quantity | INT | 采购数量 | NOT NULL |
| unit_price | DECIMAL(10, 2) | 单价 | DEFAULT 0.00 |
| total_price | DECIMAL(10, 2) | 小计金额 | DEFAULT 0.00 |
| remark | VARCHAR(200) | 备注 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- KEY idx_order_id (order_id)
- KEY idx_drug_id (drug_id)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_purchase_order_item_order FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE CASCADE
- CONSTRAINT fk_purchase_order_item_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)

### inbound_record（入库记录表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| record_number | VARCHAR(50) | 入库单号（唯一） | NOT NULL, UNIQUE |
| order_id | BIGINT | 关联采购订单ID（可为空，支持临时入库） | FOREIGN KEY |
| drug_id | BIGINT | 药品ID（外键） | NOT NULL, FOREIGN KEY |
| batch_number | VARCHAR(100) | 批次号 | NOT NULL |
| quantity | INT | 入库数量 | NOT NULL |
| expiry_date | DATE | 有效期至 | NOT NULL |
| arrival_date | DATE | 到货日期 | |
| production_date | DATE | 生产日期 | |
| manufacturer | VARCHAR(200) | 生产厂家 | |
| delivery_note_number | VARCHAR(100) | 随货同行单编号 | |
| delivery_note_image | VARCHAR(500) | 随货同行单图片路径 | |
| operator_id | BIGINT | 操作人ID（外键） | NOT NULL, FOREIGN KEY |
| second_operator_id | BIGINT | 第二操作人ID（特殊药品双人操作） | FOREIGN KEY |
| status | VARCHAR(20) | 验收状态（QUALIFIED/UNQUALIFIED） | DEFAULT 'QUALIFIED' |
| expiry_check_status | VARCHAR(20) | 效期校验状态（PASS/WARNING/FORCE） | DEFAULT 'PASS' |
| expiry_check_reason | VARCHAR(500) | 效期校验说明 | |
| remark | VARCHAR(500) | 备注 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_record_number (record_number)
- KEY idx_order_id (order_id)
- KEY idx_drug_id (drug_id)
- KEY idx_batch_number (batch_number)
- KEY idx_operator_id (operator_id)
- KEY idx_status (status)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_inbound_record_order FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE SET NULL
- CONSTRAINT fk_inbound_record_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)
- CONSTRAINT fk_inbound_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
- CONSTRAINT fk_inbound_record_second_operator FOREIGN KEY (second_operator_id) REFERENCES sys_user(id)

**说明**：支持从采购订单入库和临时入库两种方式

### outbound_apply（出库申请表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| apply_number | VARCHAR(50) | 申领单号（唯一） | NOT NULL, UNIQUE |
| applicant_id | BIGINT | 申请人ID（外键，医护人员） | NOT NULL, FOREIGN KEY |
| department | VARCHAR(100) | 所属科室 | |
| purpose | VARCHAR(200) | 用途 | |
| status | VARCHAR(20) | 申请状态（PENDING/APPROVED/REJECTED/OUTBOUND/CANCELLED） | DEFAULT 'PENDING' |
| approver_id | BIGINT | 审批人ID（外键，仓库管理员） | FOREIGN KEY |
| second_approver_id | BIGINT | 第二审批人ID（特殊药品双人审批） | FOREIGN KEY |
| approve_time | DATETIME | 审批时间 | |
| reject_reason | VARCHAR(500) | 驳回理由 | |
| outbound_time | DATETIME | 出库时间 | |
| remark | VARCHAR(500) | 备注 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_apply_number (apply_number)
- KEY idx_applicant_id (applicant_id)
- KEY idx_approver_id (approver_id)
- KEY idx_status (status)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_outbound_apply_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id)
- CONSTRAINT fk_outbound_apply_approver FOREIGN KEY (approver_id) REFERENCES sys_user(id)
- CONSTRAINT fk_outbound_apply_second_approver FOREIGN KEY (second_approver_id) REFERENCES sys_user(id)

**申请状态流转**：PENDING → APPROVED → OUTBOUND 或 PENDING → REJECTED

### outbound_apply_item（出库申请明细表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| apply_id | BIGINT | 出库申请ID（外键） | NOT NULL, FOREIGN KEY |
| drug_id | BIGINT | 药品ID（外键） | NOT NULL, FOREIGN KEY |
| batch_number | VARCHAR(100) | 批次号 | |
| quantity | INT | 申领数量 | NOT NULL |
| actual_quantity | INT | 实际出库数量 | |
| remark | VARCHAR(200) | 备注 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- KEY idx_apply_id (apply_id)
- KEY idx_drug_id (drug_id)
- KEY idx_batch_number (batch_number)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_outbound_apply_item_apply FOREIGN KEY (apply_id) REFERENCES outbound_apply(id) ON DELETE CASCADE
- CONSTRAINT fk_outbound_apply_item_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)

### inventory_adjustment（库存调整记录表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| adjustment_number | VARCHAR(50) | 调整单号（唯一） | NOT NULL, UNIQUE |
| drug_id | BIGINT | 药品ID（外键） | NOT NULL, FOREIGN KEY |
| batch_number | VARCHAR(100) | 批次号 | NOT NULL |
| adjustment_type | VARCHAR(20) | 调整类型（PROFIT-盘盈/LOSS-盘亏） | NOT NULL |
| quantity_before | INT | 调整前数量 | NOT NULL |
| quantity_after | INT | 调整后数量 | NOT NULL |
| adjustment_quantity | INT | 调整数量 | NOT NULL |
| adjustment_reason | VARCHAR(500) | 调整原因 | |
| operator_id | BIGINT | 操作人ID（外键） | NOT NULL, FOREIGN KEY |
| second_operator_id | BIGINT | 第二操作人ID（特殊药品） | FOREIGN KEY |
| adjustment_image | VARCHAR(500) | 盘点记录照片路径 | |
| remark | VARCHAR(500) | 备注 | |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_adjustment_number (adjustment_number)
- KEY idx_drug_id (drug_id)
- KEY idx_batch_number (batch_number)
- KEY idx_adjustment_type (adjustment_type)
- KEY idx_operator_id (operator_id)
- KEY idx_create_time (create_time)

**外键约束**：
- CONSTRAINT fk_inventory_adjustment_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id)
- CONSTRAINT fk_inventory_adjustment_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
- CONSTRAINT fk_inventory_adjustment_second_operator FOREIGN KEY (second_operator_id) REFERENCES sys_user(id)

**检查约束**：quantity_after >= 0（禁止负库存）

**说明**：调整数量自动计算：adjustmentQuantity = quantityAfter - quantityBefore

---

## 扩展表字段说明

### favorite_drug（常用药品收藏表）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID（外键） | NOT NULL, FOREIGN KEY |
| drug_id | BIGINT | 药品ID（外键） | NOT NULL, FOREIGN KEY |
| sort_order | INT | 排序顺序 | DEFAULT 0 |
| create_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_drug (user_id, drug_id)
- KEY idx_user_id (user_id)
- KEY idx_drug_id (drug_id)

**外键约束**：
- CONSTRAINT fk_favorite_drug_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
- CONSTRAINT fk_favorite_drug_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE

**说明**：功能待实现，数据库表已创建

---

## 表关系说明

### 用户与角色关系
- **多对多关系**：通过 `sys_user_role` 表关联
- 一个用户可以有多个角色（通过关联表）
- 一个角色可以分配给多个用户

### 角色与权限关系
- **多对多关系**：通过 `sys_role_permission` 表关联
- 一个角色可以拥有多个权限
- 一个权限可以分配给多个角色

### 用户与权限关系
- **多对多关系**：通过 `sys_user_permission` 表关联（v1.5.0新增）
- 支持用户直接拥有权限，实现更细粒度的权限控制
- 用户权限 = 角色权限 + 直接权限

### 供应商与药品关系
- **多对多关系**：通过 `supplier_drug` 表关联
- 一个供应商可以提供多个药品
- 一个药品可以由多个供应商提供

### 采购订单与入库记录关系

#### 表结构关系

```
purchase_order (采购订单主表)
    ├── id (主键)
    ├── order_number (订单编号，唯一)
    ├── supplier_id (供应商ID，外键)
    ├── purchaser_id (采购员ID，外键)
    ├── status (订单状态)
    └── ...

purchase_order_item (采购订单明细表)
    ├── id (主键)
    ├── order_id (订单ID，外键 → purchase_order.id)
    ├── drug_id (药品ID，外键 → drug_info.id)
    ├── quantity (采购数量)
    └── ...

inbound_record (入库记录表)
    ├── id (主键)
    ├── record_number (入库单号，唯一)
    ├── order_id (关联采购订单ID，外键 → purchase_order.id，可为NULL)
    ├── drug_id (药品ID，外键 → drug_info.id)
    ├── batch_number (批次号)
    ├── quantity (入库数量)
    └── ...
```

#### 关系说明

**一对多关系**：
- 一个采购订单（`purchase_order`）可以对应多个入库记录（`inbound_record`）
- 一个采购订单（`purchase_order`）可以包含多个订单明细（`purchase_order_item`）
- 一个入库记录（`inbound_record`）只能关联一个采购订单（`purchase_order`），也可以为空（临时入库）

**业务逻辑**：
1. **采购订单入库**：`inbound_record.order_id` 不为空，关联到具体的采购订单
2. **临时入库**：`inbound_record.order_id` 为空，不关联任何订单

#### 订单状态流转

```
PENDING (待确认) 
  → REJECTED (已拒绝) 或 
  → CONFIRMED (待发货) 
    → SHIPPED (已发货) 
      → RECEIVED (已入库) 或 
      → CANCELLED (已取消)
```

**状态说明**：
- **PENDING**：订单已创建，等待供应商确认
- **REJECTED**：供应商拒绝订单
- **CONFIRMED**：供应商确认订单，准备发货
- **SHIPPED**：供应商已发货，可以开始入库验收
- **RECEIVED**：订单全部入库完成
- **CANCELLED**：订单已取消

#### 入库与订单的关系

**部分入库**：
- 一个订单可以分多次入库（支持分批到货）
- 每次入库创建一条 `inbound_record` 记录
- 所有入库记录的 `quantity` 总和不能超过订单明细的 `quantity`

**订单状态更新规则**：
- 当订单首次入库时，订单状态保持 `SHIPPED`
- 当订单全部入库完成时（所有明细的已入库数量 = 采购数量），订单状态更新为 `RECEIVED`

#### 数据流转关系

**订单创建 → 入库验收 → 库存更新**：

```
1. 创建采购订单
   purchase_order (订单主表)
   purchase_order_item (订单明细表)

2. 供应商发货，订单状态更新为 SHIPPED

3. 到货验收，创建入库记录
   inbound_record (入库记录表)
   - order_id: 关联订单ID
   - drug_id: 药品ID
   - quantity: 入库数量
   - batch_number: 批次号
   - ...

4. 更新库存
   inventory (库存表)
   - 如果批次已存在：quantity += 入库数量
   - 如果批次不存在：新增库存记录

5. 更新订单状态
   - 计算已入库数量
   - 如果全部入库：status = RECEIVED
   - 如果部分入库：status = SHIPPED（保持不变）
```

**已入库数量计算逻辑**：
```sql
-- 查询订单某个药品明细的已入库数量
SELECT COALESCE(SUM(quantity), 0) as inbound_quantity
FROM inbound_record
WHERE order_id = ? AND drug_id = ?
  AND status = 'QUALIFIED'  -- 只统计合格入库的数量
```

**待入库数量**：
```
待入库数量 = 采购数量 - 已入库数量
```

#### 业务规则

**订单入库规则**：
1. **订单状态限制**：只有状态为 `SHIPPED` 的订单才能进行入库操作
2. **入库数量限制**：单次入库数量不能超过订单明细的待入库数量
3. **批次管理**：同一药品同一批次在库存表中唯一
4. **部分入库**：支持订单分多次入库，每次入库创建独立的入库记录

**订单状态更新规则**：
1. **首次入库**：订单状态保持 `SHIPPED`
2. **全部入库**：当所有明细的已入库数量 = 采购数量时，订单状态更新为 `RECEIVED`
3. **订单取消**：如果订单被取消，不能再进行入库操作

**数据完整性**：
1. **外键约束**：`inbound_record.order_id` 外键关联 `purchase_order.id`，删除订单时设置为NULL
2. **唯一约束**：`inbound_record.record_number` 唯一，`purchase_order.order_number` 唯一
3. **非负约束**：入库数量必须 > 0
- 每个供应商-药品关联可以设置不同的单价

### 采购订单与药品关系
- **一对多关系**：通过 `purchase_order_item` 表关联
- 一个订单可以包含多个药品
- 每个订单明细包含药品ID、数量、单价等信息

### 入库记录与采购订单关系
- **多对一关系**：`inbound_record.order_id` 关联 `purchase_order.id`
- 一个订单可以有多条入库记录（分批入库）
- 支持临时入库（order_id为空）

### 出库申请与药品关系
- **一对多关系**：通过 `outbound_apply_item` 表关联
- 一个出库申请可以包含多个药品
- 每个申请明细包含药品ID、批次号、数量等信息

### 库存与药品关系
- **多对一关系**：`inventory.drug_id` 关联 `drug_info.id`
- 一个药品可以有多个批次库存
- 每个批次库存包含批次号、数量、有效期等信息

---

## 设计特点

### 1. 规范化设计
- 遵循第三范式，减少数据冗余
- 合理使用外键约束，保证数据完整性
- 使用关联表实现多对多关系

### 2. 逻辑删除
- 所有业务表都支持逻辑删除（deleted字段）
- 数据不会真正删除，便于数据恢复和审计
- 查询时自动过滤已删除数据

### 3. 审计字段
- 所有表都包含 `create_time` 和 `update_time` 字段
- 业务表包含 `create_by` 字段，记录创建人
- 操作日志和登录日志不可删除，符合合规要求（保留5年）

### 4. 批次管理
- 库存按批次管理，支持先进先出（FIFO）
- 每个批次包含有效期信息，支持近效期预警
- 入库、出库、库存调整都关联批次号

### 5. 特殊药品管理
- 特殊药品的入库、出库、库存调整需双人操作
- 通过 `second_operator_id` 和 `second_approver_id` 字段记录第二操作人
- 出库申请需要双人审批（特殊药品）

### 6. 状态管理
- 订单状态：PENDING → CONFIRMED → SHIPPED → RECEIVED
- 出库申请状态：PENDING → APPROVED → OUTBOUND 或 PENDING → REJECTED
- 供应商状态：0-禁用/1-启用/2-待审核
- 审核状态：0-待审核/1-已通过/2-已驳回

### 7. 数据完整性
- 库存数量必须≥0，禁止负库存（CHECK约束）
- 唯一约束保证数据唯一性（订单编号、入库单号、申领单号等）
- 外键约束保证数据关联完整性

---

## 索引设计

### 主键索引
- 所有表都使用 `id` 作为主键，类型为 `BIGINT AUTO_INCREMENT`

### 唯一索引
- `sys_role.role_code` - 角色代码唯一
- `sys_user.username` - 用户名唯一
- `sys_user.phone` - 手机号唯一
- `sys_config.config_key` - 配置键名唯一
- `drug_info.national_code` - 国家本位码唯一
- `drug_info.trace_code` - 药品追溯码唯一
- `purchase_order.order_number` - 订单编号唯一
- `inbound_record.record_number` - 入库单号唯一
- `outbound_apply.apply_number` - 申领单号唯一
- `inventory_adjustment.adjustment_number` - 调整单号唯一
- `inventory(drug_id, batch_number)` - 同一药品同一批次唯一

### 普通索引
- 为常用查询字段创建索引，提升查询性能
- 状态字段、外键字段、时间字段都创建了索引
- 支持多条件查询和排序

---

## 约束设计

### 外键约束
- 所有外键都设置了约束，保证数据完整性
- 删除策略：
  - `ON DELETE CASCADE`：级联删除（关联表）
  - `ON DELETE SET NULL`：设置为NULL（可选关联）
  - 默认策略：不允许删除（有外键关联的数据）

### 检查约束
- `inventory.quantity >= 0` - 库存数量非负
- `inventory_adjustment.quantity_after >= 0` - 调整后数量非负

### 唯一约束
- 通过UNIQUE KEY实现唯一性约束
- 保证关键字段的唯一性（用户名、手机号、订单编号等）

---

## 注意事项

### 数据库相关
1. **数据库密码**：需要修改 `application.yml` 中的数据库密码为实际密码
2. **初始化脚本**：推荐使用 `init_simple.sql`，已包含所有21张表的创建语句
3. **字符集**：数据库和表都使用 utf8mb4，支持中文和特殊字符
4. **外键约束**：所有外键都设置了约束，删除时注意级联关系

### 业务相关
1. **逻辑删除**：所有删除操作均为逻辑删除，数据不会真正删除（deleted字段）
2. **操作日志**：操作日志和登录日志不可删除，符合合规要求（保留5年）
3. **库存约束**：库存数量必须≥0，禁止负库存
4. **特殊药品**：特殊药品的入库、出库、库存调整需双人操作

### 性能优化
1. **索引优化**：为常用查询字段创建索引，提升查询性能
2. **分页查询**：列表查询都支持分页，避免大数据量查询
3. **批量操作**：支持批量入库、批量出库等操作

---

**文档版本**: v1.0.0  
**最后更新**: 2026年1月20日

