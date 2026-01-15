# CDIOM - 临床药品出入库管理系统

## 项目概述

基于 Spring Boot + React + MySQL 的医疗药品仓库管理系统，实现药品信息管理、库存监控、出入库审批、批次追溯及多角色权限管控等核心功能。

> 📋 **核心业务需求分析**：详细的需求分析文档请参考 [Core_Business_Requirements_Analysis.md](./docs/Core_Business_Requirements_Analysis.md)，包含库存管理、入库管理、出库管理的完整业务流程和处理流程。

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.8
- Spring Security
- MyBatis-Plus 3.5.15
- MySQL 8.0.33
- JWT (jjwt 0.12.3)
- Hutool 5.8.28
- Lombok 1.18.30

### 前端
- React 18.2.0
- React Router 6.23.1
- Ant Design 5.20.6
- Axios 1.7.2
- Vite 7.3.1
- Day.js 1.11.11

## 项目结构

```
cdiom_new/
├── cdiom_backend/                    # 后端项目
│   ├── src/main/java/
│   │   └── com/cdiom/backend/
│   │       ├── CdiomApplication.java # 启动类
│   │       ├── controller/           # 控制器层
│   │       │   ├── AuthController.java
│   │       │   ├── SysUserController.java
│   │       │   ├── SysRoleController.java
│   │       │   ├── SysConfigController.java
│   │       │   ├── SysNoticeController.java
│   │       │   ├── OperationLogController.java
│   │       │   ├── LoginLogController.java
│   │       │   ├── DrugInfoController.java
│   │       │   ├── InventoryController.java
│   │       │   ├── InventoryAdjustmentController.java
│   │       │   ├── InboundRecordController.java
│   │       │   ├── OutboundApplyController.java
│   │       │   ├── PurchaseOrderController.java
│   │       │   ├── SupplierController.java
│   │       │   ├── SupplierDrugController.java
│   │       │   ├── DashboardController.java
│   │       │   ├── FileUploadController.java
│   │       │   └── SuperAdminController.java
│   │       ├── service/              # 服务层
│   │       │   ├── impl/            # 服务实现
│   │       │   └── *.java          # 服务接口
│   │       ├── mapper/               # 数据访问层
│   │       │   └── *.java          # Mapper接口
│   │       ├── model/                # 实体类
│   │       │   ├── SysUser.java
│   │       │   ├── SysRole.java
│   │       │   ├── SysConfig.java
│   │       │   ├── SysNotice.java
│   │       │   ├── SysPermission.java
│   │       │   ├── SysRolePermission.java
│   │       │   ├── SysUserPermission.java
│   │       │   ├── OperationLog.java
│   │       │   ├── LoginLog.java
│   │       │   ├── DrugInfo.java
│   │       │   ├── Inventory.java
│   │       │   ├── InventoryAdjustment.java
│   │       │   ├── InboundRecord.java
│   │       │   ├── OutboundApply.java
│   │       │   ├── OutboundApplyItem.java
│   │       │   ├── PurchaseOrder.java
│   │       │   ├── PurchaseOrderItem.java
│   │       │   ├── Supplier.java
│   │       │   └── SupplierDrug.java
│   │       ├── config/               # 配置类
│   │       │   ├── SecurityConfig.java
│   │       │   ├── MyBatisPlusConfig.java
│   │       │   ├── MyMetaObjectHandler.java
│   │       │   ├── WebMvcConfig.java
│   │       │   ├── WebConfig.java
│   │       │   ├── AsyncConfig.java
│   │       │   ├── ConcurrencyConfig.java
│   │       │   ├── RestTemplateConfig.java
│   │       │   ├── filter/
│   │       │   │   └── JwtAuthenticationFilter.java
│   │       │   └── interceptor/
│   │       │       └── PermissionInterceptor.java
│   │       ├── util/                 # 工具类
│   │       │   └── JwtUtil.java
│   │       └── annotation/          # 注解类
│   │           └── RequiresPermission.java
│   │       └── common/              # 公共类
│   │           ├── Result.java
│   │           └── exception/
│   │               ├── GlobalExceptionHandler.java
│   │               └── ServiceException.java
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── db/                       # 数据库脚本目录
│           ├── README.md            # 数据库脚本说明文档
│           ├── init_simple.sql      # 简化初始化脚本（推荐）⭐
│           ├── init.sql             # 完整初始化脚本（带注释）
│           ├── cdiom_db_complete.sql # 完整数据库脚本
│           ├── init_business_tables.sql # 业务表脚本
│           ├── init_permissions.sql # 权限数据初始化脚本
│           ├── fix_chinese_data.sql # 修复中文数据脚本
│           ├── fix_chinese_utf8mb4.sql # 修复中文数据脚本（简化版）
│           ├── fix_database_charset.sql # 修复数据库字符集脚本
│           ├── fix_operation_log_permission.sql # 修复操作日志权限脚本
│           ├── drug_info_insert.sql # 药品信息数据导入脚本
│           ├── update_admin_password.sql # 更新管理员密码脚本
│           ├── add_supplier_remark_field.sql # 为供应商表添加备注字段
│           ├── create_supplier_drug_relation.sql # 创建供应商-药品关联表
│           └── update_supplier_permissions.sql # 更新供应商管理权限
├── cdiom_frontend/                   # 前端项目
│   ├── src/
│   │   ├── pages/                    # 页面组件
│   │   │   ├── Login.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   ├── UserManagement.jsx
│   │   │   ├── RoleManagement.jsx
│   │   │   ├── ConfigManagement.jsx
│   │   │   ├── NoticeManagement.jsx
│   │   │   ├── OperationLog.jsx
│   │   │   ├── LoginLog.jsx
│   │   │   ├── DrugManagement.jsx
│   │   │   ├── InventoryManagement.jsx
│   │   │   ├── InboundManagement.jsx
│   │   │   ├── OutboundManagement.jsx
│   │   │   ├── PurchaseOrderManagement.jsx
│   │   │   ├── SupplierManagement.jsx
│   │   │   ├── SupplierDashboard.jsx
│   │   │   └── SupplierOrderManagement.jsx
│   │   ├── components/               # 公共组件
│   │   │   ├── Layout.jsx
│   │   │   ├── PrivateRoute.jsx
│   │   │   ├── SuperAdminModal.jsx
│   │   │   └── IndexRedirect.jsx
│   │   ├── utils/                    # 工具函数
│   │   │   ├── request.js           # Axios封装
│   │   │   └── auth.js              # 认证工具
│   │   ├── App.jsx                   # 根组件
│   │   └── main.jsx                  # 入口文件
│   ├── index.html                    # HTML模板
│   ├── vite.config.js                # Vite配置
│   └── package.json                  # 依赖配置
├── docs/                              # 项目文档目录
│   ├── README.md                     # 文档目录说明
│   ├── Core_Business_Requirements_Analysis.md           # 核心业务需求分析
│   ├── Order_Inbound_Record_Relationship_Guide.md     # 订单与入库关系说明
│   ├── Code_Completeness_Report.md         # 代码完整性检查报告
│   ├── Code_Logic_Vulnerability_Report.md       # 代码逻辑漏洞检查报告（2026-01-14）
│   ├── Permission_Issue_Fix_Guide.md           # 权限问题修复说明
│   ├── Concurrency_Configuration_Guide.md           # 并发访问配置说明
│   └── Drug_Data_Import_Guide.md           # 药品数据导入说明
└── README.md                          # 项目说明文档
```

## 已实现功能模块

### 后端模块
✅ 用户管理（SysUserController）
- 用户列表查询（分页、关键字搜索、角色筛选、状态筛选）
- 用户新增、编辑、删除（逻辑删除）
- 用户状态管理（启用/禁用）
- 用户解锁功能

✅ 角色管理（SysRoleController）
- 角色列表查询（分页、关键字搜索、状态筛选）
- 角色新增、编辑、删除（逻辑删除）
- 角色状态管理（启用/禁用）

✅ 参数配置（SysConfigController）
- 参数配置列表查询（分页、关键字搜索、类型筛选）
- 参数配置新增、编辑、删除（逻辑删除）
- 根据键名查询参数配置

✅ 通知公告（SysNoticeController）
- 通知公告列表查询（分页、关键字搜索、类型筛选、状态筛选）
- 通知公告新增、编辑、删除（逻辑删除）
- 通知公告状态管理（开启/关闭）

✅ 操作日志（OperationLogController）
- 操作日志列表查询（分页、多条件筛选）
- 操作日志详情查询

✅ 登录日志（LoginLogController）
- 登录日志列表查询（分页、多条件筛选）
- 登录日志详情查询

✅ 药品信息管理（DrugInfoController）
- 药品信息列表查询（分页、关键字搜索、特殊药品筛选）
- 药品信息新增、编辑、删除（逻辑删除）
- 根据商品码/本位码/追溯码查询（先查本地数据库，未找到则调用极速数据API）
- 根据药品名称查询（调用万维易源API，并自动调用极速数据API补充信息）
- 根据批准文号查询（调用万维易源API，并自动调用极速数据API补充信息）
- 根据供应商ID查询药品列表（新增）
- 多API数据自动合并（万维易源API + 极速数据API）

✅ 供应商-药品关联管理（SupplierDrugController）
- 添加供应商-药品关联（支持设置单价）
- 删除供应商-药品关联
- 更新供应商-药品关联的单价

✅ 文件上传管理（FileUploadController）
- 图片文件上传（支持jpg, jpeg, png, gif, bmp, webp格式）
- 文件大小验证（最大10MB）
- 文件删除功能
- 按日期分类存储

✅ 仪表盘（DashboardController）
- 基础统计数据（所有用户可访问）
- 登录趋势统计（仅系统管理员）
- 操作日志统计（仅系统管理员）
- 仓库管理员专用仪表盘（近效期预警、待办任务、出入库统计）

✅ 认证模块（AuthController）
- 用户登录（支持用户名/手机号登录）
- 获取当前登录用户信息
- 用户登出

✅ 权限控制系统
- 基于角色的权限控制（RBAC）
- 用户直接权限关联（支持用户直接拥有权限，实现更细粒度的权限控制）
- 权限拦截器（PermissionInterceptor）
- 权限注解（@RequiresPermission）
- 系统管理员自动拥有所有权限（*）
- 前端权限检查（菜单权限、按钮权限）
- 细粒度权限支持（出库、入库、库存管理等业务权限）

### 前端模块
✅ 登录页面
- 用户名/手机号登录
- 密码加密传输
- Token自动存储到Cookie

✅ 主布局
- 侧边栏导航菜单
- 顶部用户信息
- 响应式布局

✅ 用户管理页面
- 用户列表展示（分页）
- 用户新增、编辑、删除
- 用户状态管理
- 用户解锁功能

✅ 角色管理页面
- 角色列表展示（分页）
- 角色新增、编辑、删除
- 角色状态管理

✅ 参数配置页面
- 参数配置列表展示（分页）
- 参数配置新增、编辑、删除

✅ 通知公告页面
- 通知公告列表展示（分页）
- 通知公告新增、编辑、删除
- 通知公告状态管理

✅ 操作日志页面
- 操作日志列表展示（分页、多条件筛选）

✅ 登录日志页面
- 登录日志列表展示（分页、多条件筛选）

✅ 仪表盘页面
- 系统管理员仪表盘（基础统计、登录趋势、操作日志统计）
- 仓库管理员仪表盘（近效期预警、待办任务、出入库统计）
- 采购专员仪表盘（订单统计、供应商统计、订单趋势）
- 医护人员仪表盘（申领统计、状态分布、申领趋势）
- 供应商仪表盘（订单统计、状态分布、金额统计、订单趋势）
- 响应式布局（支持不同屏幕尺寸）

✅ 药品管理页面
- 药品信息列表展示（分页、关键字搜索、特殊药品筛选）
- 药品信息新增、编辑、删除
- 四种新增药品方式：
  1. 全部手动输入
  2. 扫描商品码/本位码/追溯码（先查本地，未找到则调用极速数据API）
  3. 输入药品名称搜索（调用万维易源API + 极速数据API）
  4. 输入批准文号搜索（调用万维易源API + 极速数据API）
- 表单自动填充（API返回数据自动填充，用户可手动核对修改）
- 表单布局优化（分组显示，更紧凑美观）
- 特殊药品友好提示（说明如何判断特殊药品）

✅ 供应商管理页面增强
- 供应商列表展示、新增、编辑、删除
- 供应商-药品关联管理（添加、删除、更新单价）
- 根据供应商动态获取关联药品列表
- 采购订单管理优化（根据供应商动态加载药品）

✅ 供应商仪表盘页面（新增）
- 订单统计（总订单数、状态分布）
- 金额统计（总金额、待确认金额、已确认金额）
- 订单趋势图表（最近7天订单数量趋势）
- 响应式布局

✅ 供应商订单管理页面（新增）
- 供应商订单列表展示（分页、搜索、状态筛选）
- 订单详情查看
- 订单状态管理
- 响应式表格布局

## 数据库设计

### 数据库基本信息
- **数据库名称：** cdiom_db
- **数据库版本：** MySQL 8.0.33
- **字符集：** utf8mb4
- **排序规则：** utf8mb4_unicode_ci
- **存储引擎：** InnoDB
- **总表数：** 21张（新增sys_user_permission表）

### 数据库表分类

#### 系统表（6张）
1. `sys_role` - 系统角色表
2. `sys_user` - 系统用户表
3. `sys_config` - 系统参数配置表
4. `sys_notice` - 系统通知公告表
5. `operation_log` - 操作日志表
6. `login_log` - 登录日志表

#### 权限表（4张）
7. `sys_user_role` - 用户角色关联表
8. `sys_permission` - 权限表
9. `sys_role_permission` - 角色权限关联表
10. `sys_user_permission` - 用户权限关联表（支持用户直接拥有权限）

#### 业务表（10张）
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

#### 扩展表（1张）
21. `favorite_drug` - 常用药品收藏表

### 核心表结构说明

#### 系统表
- **sys_role**: 存储5种固定角色（系统管理员、仓库管理员、采购专员、医护人员、供应商）
- **sys_user**: 存储用户信息，支持用户名/手机号登录，BCrypt密码加密
- **sys_config**: 存储系统配置参数（效期预警阈值、日志保留期等）
- **sys_notice**: 存储通知公告信息
- **operation_log**: 记录所有核心操作，不可删除，保留5年
- **login_log**: 记录登录日志，包含IP、浏览器、操作系统等信息

#### 权限表
- **sys_user_role**: 用户与角色的多对多关联（当前实现为1对1，保留扩展性）
- **sys_permission**: 权限表，支持树形结构（菜单-按钮-接口三级）
- **sys_role_permission**: 角色与权限的多对多关联

#### 业务表
- **supplier**: 供应商信息，包含资质审核、合作状态、备注等
- **drug_info**: 药品基础信息，支持扫码识别（国家本位码、追溯码、商品码）
- **supplier_drug**: 供应商-药品关联表，支持多对多关系，每个供应商可以为同一药品设置不同单价
- **inventory**: 库存表，按批次管理，支持近效期预警
- **purchase_order**: 采购订单主表，包含订单状态流转
- **purchase_order_item**: 采购订单明细表
- **inbound_record**: 入库记录表，包含合规校验（效期校验、特殊药品双人操作）
- **outbound_apply**: 出库申请表，包含审批流程
- **outbound_apply_item**: 出库申请明细表
- **inventory_adjustment**: 库存调整记录表（盘盈/盘亏）

### 数据库表关系

```
sys_role (1) ──< (N) sys_user
sys_user (1) ──< (N) sys_user_role (N) >── (1) sys_role
sys_role (1) ──< (N) sys_role_permission (N) >── (1) sys_permission

supplier (1) ──< (N) supplier_drug (N) >── (1) drug_info
supplier (1) ──< (N) purchase_order

drug_info (1) ──< (N) inventory
drug_info (1) ──< (N) purchase_order_item
drug_info (1) ──< (N) inbound_record
drug_info (1) ──< (N) outbound_apply_item
drug_info (1) ──< (N) inventory_adjustment
drug_info (1) ──< (N) favorite_drug

purchase_order (1) ──< (N) purchase_order_item
purchase_order (1) ──< (N) inbound_record

outbound_apply (1) ──< (N) outbound_apply_item

sys_user (1) ──< (N) supplier (创建人、审核人)
sys_user (1) ──< (N) drug_info (创建人)
sys_user (1) ──< (N) purchase_order (采购员)
sys_user (1) ──< (N) inbound_record (操作人、第二操作人)
sys_user (1) ──< (N) outbound_apply (申请人、审批人、第二审批人)
sys_user (1) ──< (N) inventory_adjustment (操作人、第二操作人)
```

### 数据库设计特点

1. **外键约束**: 所有外键都设置外键约束，保证数据完整性
2. **索引优化**: 为常用查询字段创建索引，提升查询性能
3. **约束检查**: 库存数量非负约束（CHECK约束）
4. **逻辑删除**: 业务表支持逻辑删除（deleted字段），保留历史数据
5. **时间戳**: 自动记录创建时间和更新时间
6. **字符集**: 统一使用utf8mb4，支持中文和特殊字符
7. **合规要求**: 操作日志不可删除，保留5年（符合GSP规范）

### 初始化数据
- 5个系统角色（系统管理员、仓库管理员、采购专员、医护人员、供应商）
- 1个默认管理员用户（admin/admin123，密码已BCrypt加密）
- 4个系统参数配置（效期预警阈值、日志保留期等）
- 2个示例通知公告

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Node.js 18+
- Maven 3.6+

### 后端启动

1. 创建数据库并执行初始化脚本

**方式一：使用完整初始化脚本（推荐）**
```bash
# 1. 执行基础初始化脚本（包含19张表：6张系统表 + 4张权限表 + 9张业务表）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_simple.sql

# 2. 创建供应商-药品关联表（第20张表，v1.3.0新增）
mysql -u root -p < cdiom_backend/src/main/resources/db/create_supplier_drug_relation.sql

# 3. 创建用户权限关联表和细粒度权限（v1.5.0新增）
mysql -u root -p < cdiom_backend/src/main/resources/db/add_user_permission_system.sql

# 4. 初始化权限数据（如果尚未初始化）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_permissions.sql

# 5. 初始化超级管理员（如果尚未初始化）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_super_admin.sql
```

**方式二：分步执行**
```bash
# 1. 先执行基础表结构（19张表）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_simple.sql

# 2. 创建供应商-药品关联表（第20张表）
mysql -u root -p < cdiom_backend/src/main/resources/db/create_supplier_drug_relation.sql

# 3. 创建用户权限关联表和细粒度权限
mysql -u root -p < cdiom_backend/src/main/resources/db/add_user_permission_system.sql

# 4. 如果需要单独创建业务表（已包含在init_simple.sql中）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_business_tables.sql
```

**注意**: 
- `init_simple.sql` 包含19张表的创建语句（6张系统表 + 4张权限表 + 9张业务表）
- `supplier_drug` 表（第13张业务表，总第20张表）需要单独执行 `create_supplier_drug_relation.sql`
- `sys_user_permission` 表（第10张权限表，总第21张表）需要单独执行 `add_user_permission_system.sql`
- `favorite_drug` 表（第21张表，扩展表）已包含在 `init_simple.sql` 中
- 执行顺序：先执行 `init_simple.sql`，再执行 `create_supplier_drug_relation.sql`，最后执行 `add_user_permission_system.sql`

2. 修改数据库配置
```yaml
# cdiom_backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cdiom_db?...
    username: root
    password: 12345  # 修改为你的数据库密码
```

3. 启动后端服务
```bash
cd cdiom_backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 前端启动

1. 安装依赖
```bash
cd cdiom_frontend
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

前端服务将在 `http://localhost:5173` 启动

### 默认登录信息
- 用户名：`admin`
- 密码：`admin123`

**注意**：首次登录前，请确保数据库已初始化，并且默认管理员用户的密码已正确加密。如果登录失败，请检查数据库中的密码是否为BCrypt加密后的值。

## API接口文档

### 统一响应格式

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

### 认证接口

#### 用户登录
- **接口**: `POST /api/v1/auth/login`
- **请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- **响应**: 返回Token和用户信息，Token自动存储到Cookie（key: cdiom_token）

#### 获取当前用户信息
- **接口**: `GET /api/v1/auth/current`
- **说明**: 需要Token认证

#### 用户登出
- **接口**: `POST /api/v1/auth/logout`
- **说明**: 清除Token

### 超级管理员管理接口

#### 查询超级管理员状态
- **接口**: `GET /api/v1/super-admin/status`
- **权限**: 需要 `user:manage` 权限
- **响应**: 返回超级管理员账户的状态信息（用户名、状态、邮箱、创建时间）

#### 发送验证码
- **接口**: `POST /api/v1/super-admin/send-verification-code`
- **权限**: 需要 `user:manage` 权限
- **请求体**:
```json
{
  "email": "user@example.com"
}
```
- **说明**: 验证码将发送到指定邮箱，验证码有效期为5分钟。必须使用当前登录用户绑定的邮箱。

#### 启用超级管理员
- **接口**: `POST /api/v1/super-admin/enable`
- **权限**: 需要 `user:manage` 权限
- **请求体**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```
- **说明**: 启用超级管理员账户，需要邮箱验证码验证。

#### 停用超级管理员
- **接口**: `POST /api/v1/super-admin/disable`
- **权限**: 需要 `user:manage` 权限
- **请求体**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```
- **说明**: 停用超级管理员账户，需要邮箱验证码验证。如果停用的是当前登录用户，系统会自动退出登录。

### 库存管理接口

#### 获取库存列表
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

#### 获取库存详情
- **接口**: `GET /api/v1/inventory/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 用户管理接口

#### 获取用户列表
- **接口**: `GET /api/v1/users`
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（用户名/手机号）
  - `roleId`: 角色ID（可选）
  - `status`: 状态（0-禁用/1-正常，可选）

#### 获取用户详情
- **接口**: `GET /api/v1/users/{id}`

#### 创建用户
- **接口**: `POST /api/v1/users`
- **请求体**: 用户信息（密码会自动BCrypt加密）

#### 更新用户
- **接口**: `PUT /api/v1/users/{id}`
- **说明**: 如果提供密码，会自动加密

#### 删除用户
- **接口**: `DELETE /api/v1/users/{id}`
- **说明**: 逻辑删除

#### 更新用户状态
- **接口**: `PUT /api/v1/users/{id}/status`
- **请求体**: `{"status": 0}` 或 `{"status": 1}`

#### 解锁用户
- **接口**: `PUT /api/v1/users/{id}/unlock`
- **说明**: 清除锁定时间和失败次数

### 角色管理接口

#### 获取角色列表
- **接口**: `GET /api/v1/roles`
- **参数**: `page`, `size`, `keyword`, `status`

#### 获取角色详情
- **接口**: `GET /api/v1/roles/{id}`

#### 创建角色
- **接口**: `POST /api/v1/roles`
- **请求体**: 角色信息（role_code必须唯一）

#### 更新角色
- **接口**: `PUT /api/v1/roles/{id}`

#### 删除角色
- **接口**: `DELETE /api/v1/roles/{id}`
- **说明**: 逻辑删除

#### 更新角色状态
- **接口**: `PUT /api/v1/roles/{id}/status`

### 参数配置接口

#### 获取配置列表
- **接口**: `GET /api/v1/configs`
- **参数**: `page`, `size`, `keyword`, `configType`

#### 获取配置详情
- **接口**: `GET /api/v1/configs/{id}`

#### 根据键名获取配置
- **接口**: `GET /api/v1/configs/key/{configKey}`
- **说明**: 用于获取特定配置值

#### 创建配置
- **接口**: `POST /api/v1/configs`
- **说明**: config_key必须唯一

#### 更新配置
- **接口**: `PUT /api/v1/configs/{id}`

#### 删除配置
- **接口**: `DELETE /api/v1/configs/{id}`

### 通知公告接口

#### 获取通知公告列表
- **接口**: `GET /api/v1/notices`
- **参数**: `page`, `size`, `keyword`, `noticeType`, `status`

#### 获取通知公告详情
- **接口**: `GET /api/v1/notices/{id}`

#### 创建通知公告
- **接口**: `POST /api/v1/notices`

#### 更新通知公告
- **接口**: `PUT /api/v1/notices/{id}`

#### 删除通知公告
- **接口**: `DELETE /api/v1/notices/{id}`

#### 更新通知公告状态
- **接口**: `PUT /api/v1/notices/{id}/status`

### 操作日志接口

#### 获取操作日志列表
- **接口**: `GET /api/v1/operation-logs`
- **参数**: `page`, `size`, `keyword`, `userId`, `module`, `operationType`, `status`

#### 获取操作日志详情
- **接口**: `GET /api/v1/operation-logs/{id}`

### 登录日志接口

#### 获取登录日志列表
- **接口**: `GET /api/v1/login-logs`
- **参数**: `page`, `size`, `keyword`, `userId`, `status`

#### 获取登录日志详情
- **接口**: `GET /api/v1/login-logs/{id}`

### 药品信息管理接口

#### 获取药品信息列表
- **接口**: `GET /api/v1/drugs`
- **参数**: `page`, `size`, `keyword`, `isSpecial`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 获取药品信息详情
- **接口**: `GET /api/v1/drugs/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 创建药品信息
- **接口**: `POST /api/v1/drugs`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 更新药品信息
- **接口**: `PUT /api/v1/drugs/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 删除药品信息
- **接口**: `DELETE /api/v1/drugs/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 逻辑删除

#### 根据商品码/本位码/追溯码查询
- **接口**: `GET /api/v1/drugs/search?code={code}`
- **说明**: 先查询本地数据库，如果未找到则调用极速数据API
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 根据药品名称查询
- **接口**: `GET /api/v1/drugs/search/name?drugName={drugName}`
- **说明**: 调用万维易源API获取基本信息，然后自动调用极速数据API补充信息
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 根据批准文号查询
- **接口**: `GET /api/v1/drugs/search/approval?approvalNumber={approvalNumber}`
- **说明**: 调用万维易源API获取基本信息，然后自动调用极速数据API补充信息
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 根据供应商ID查询药品列表
- **接口**: `GET /api/v1/drugs/supplier/{supplierId}`
- **说明**: 查询指定供应商关联的所有药品
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 仪表盘接口

#### 获取基础统计数据
- **接口**: `GET /api/v1/dashboard/statistics`
- **说明**: 所有登录用户都可以访问

#### 获取登录趋势
- **接口**: `GET /api/v1/dashboard/login-trend`
- **权限**: 需要 `log:login:view` 权限（仅系统管理员）

#### 获取操作日志统计
- **接口**: `GET /api/v1/dashboard/operation-statistics`
- **权限**: 需要 `log:operation:view` 权限（仅系统管理员）

#### 获取仓库管理员仪表盘数据
- **接口**: `GET /api/v1/dashboard/warehouse`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

### 供应商-药品关联管理接口

#### 添加供应商-药品关联
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

#### 删除供应商-药品关联
- **接口**: `DELETE /api/v1/supplier-drugs?supplierId={supplierId}&drugId={drugId}`
- **权限**: 需要 `drug:manage` 权限

#### 更新供应商-药品关联单价
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

### 文件上传接口

#### 上传文件
- **接口**: `POST /api/v1/upload`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **请求**: multipart/form-data
  - `file`: 文件（仅支持图片格式：jpg, jpeg, png, gif, bmp, webp）
  - 文件大小限制：最大10MB
- **响应**: 返回文件访问URL

#### 删除文件
- **接口**: `DELETE /api/v1/upload?url={fileUrl}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 根据文件URL删除已上传的文件

### 入库管理接口

#### 获取入库记录列表
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

#### 获取入库记录详情
- **接口**: `GET /api/v1/inbound/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 根据入库单号查询入库记录
- **接口**: `GET /api/v1/inbound/record-number/{recordNumber}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 创建入库记录（采购订单入库）
- **接口**: `POST /api/v1/inbound`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 支持采购订单入库和临时入库，包含效期校验和特殊药品双人操作

#### 创建临时入库记录
- **接口**: `POST /api/v1/inbound/temporary`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 不关联采购订单的临时入库

### 出库管理接口

#### 获取出库申请列表
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

#### 获取出库申请详情
- **接口**: `GET /api/v1/outbound/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 根据申领单号查询出库申请
- **接口**: `GET /api/v1/outbound/apply-number/{applyNumber}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 创建出库申请（医护人员申领）
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

#### 审批出库申请（通过）
- **接口**: `POST /api/v1/outbound/{id}/approve`
- **权限**: 需要 `outbound:approve` 或 `outbound:approve:special` 权限
- **请求体**:
```json
{
  "secondApproverId": 2
}
```
- **说明**: 特殊药品需要提供 `secondApproverId`（第二审批人ID）

#### 审批出库申请（驳回）
- **接口**: `POST /api/v1/outbound/{id}/reject`
- **权限**: 需要 `drug:manage` 权限
- **请求体**:
```json
{
  "rejectReason": "驳回理由"
}
```

#### 执行出库
- **接口**: `POST /api/v1/outbound/{id}/execute`
- **权限**: 需要 `outbound:execute` 权限
- **说明**: 执行出库操作，自动扣减库存（先进先出FIFO）

### 采购订单管理接口

#### 获取采购订单列表
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

#### 获取采购订单详情
- **接口**: `GET /api/v1/purchase-orders/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 创建采购订单
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

#### 更新采购订单
- **接口**: `PUT /api/v1/purchase-orders/{id}`
- **权限**: 需要 `drug:manage` 权限

#### 删除采购订单
- **接口**: `DELETE /api/v1/purchase-orders/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 逻辑删除

#### 更新订单状态
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

#### 生成订单条形码
- **接口**: `GET /api/v1/purchase-orders/{id}/barcode`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **说明**: 返回订单条形码图片（Base64或URL）

### 供应商管理接口

#### 获取供应商列表
- **接口**: `GET /api/v1/suppliers`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限
- **参数**: 
  - `page`: 页码（默认1）
  - `size`: 每页数量（默认10）
  - `keyword`: 关键字（供应商名称、联系人、电话）
  - `status`: 状态（0-禁用/1-启用/2-待审核，可选）
  - `auditStatus`: 审核状态（0-待审核/1-已通过/2-已驳回，可选）

#### 获取供应商详情
- **接口**: `GET /api/v1/suppliers/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 创建供应商
- **接口**: `POST /api/v1/suppliers`
- **权限**: 需要 `drug:manage` 权限

#### 更新供应商
- **接口**: `PUT /api/v1/suppliers/{id}`
- **权限**: 需要 `drug:manage` 权限

#### 删除供应商
- **接口**: `DELETE /api/v1/suppliers/{id}`
- **权限**: 需要 `drug:manage` 权限
- **说明**: 逻辑删除

#### 审核供应商
- **接口**: `POST /api/v1/suppliers/{id}/audit`
- **权限**: 需要 `supplier:audit` 权限
- **请求体**:
```json
{
  "auditStatus": 1,
  "auditReason": "审核通过"
}
```
- **说明**: `auditStatus`: 1-已通过/2-已驳回

### 库存调整接口

#### 获取库存调整记录列表
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

#### 获取库存调整记录详情
- **接口**: `GET /api/v1/inventory-adjustments/{id}`
- **权限**: 需要 `drug:view` 或 `drug:manage` 权限

#### 创建库存调整记录
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

## 安全特性

- ✅ JWT Token认证（8小时有效期）
- ✅ BCrypt密码加密
- ✅ 登录失败锁定机制（连续5次失败锁定1小时）
- ✅ 基于角色的权限控制（RBAC）
- ✅ 权限拦截器（接口级权限控制）
- ✅ 权限注解（@RequiresPermission）
- ✅ 前端权限检查（菜单权限、按钮权限）
- ✅ 操作日志记录
- ✅ 登录日志记录

## 项目进度

### 📊 项目完成度统计

**总体完成度：约 85%**

#### 核心业务模块完成度
- ✅ 系统管理模块：100%（用户、角色、配置、通知、日志）
- ✅ 权限管理模块：100%（RBAC、细粒度权限、用户权限关联）
- ✅ 药品管理模块：100%（CRUD、扫码识别、第三方API集成）
- ✅ 库存管理模块：100%（查询、筛选、预警、调整）
- ✅ 供应商管理模块：100%（CRUD、审核、关联管理）
- ✅ 入库管理模块：100%（验收、效期校验、特殊药品双人操作）
- ✅ 出库管理模块：100%（申请、审批、执行、库存扣减）
- ✅ 采购订单管理模块：100%（创建、流转、物流跟踪、条形码）
- ✅ 仪表盘模块：100%（5种角色专用仪表盘）

#### 功能增强完成度
- ✅ 数据可视化：100%（5种角色专用图表）
- ✅ 权限系统：100%（RBAC + 细粒度权限）
- ✅ 文件上传：100%（图片上传、删除、验证）
- ✅ 条形码服务：100%（订单条形码生成）
- ✅ IP定位服务：100%（登录地点识别）
- ⏳ 数据导出：0%（Excel导出待实现）
- ⏳ 扫码枪适配：0%（HID键盘模式待实现）
- ⏳ 响应式优化：70%（PC端完成，Pad端待优化）
- ⏳ 常用药品收藏：0%（数据库表已创建，功能待实现）

#### 技术架构完成度
- ✅ 后端架构：100%（Spring Boot + MyBatis-Plus + Spring Security）
- ✅ 前端架构：100%（React + Vite + Ant Design）
- ✅ 数据库设计：100%（21张表全部创建）
- ✅ 安全机制：100%（JWT、BCrypt、权限控制、日志记录）
- ✅ API集成：100%（万维易源API、极速数据API）

### ✅ 已完成（2026年1月）

#### 后端开发
- ✅ 项目基础架构搭建（Spring Boot 3.2.8 + MyBatis-Plus）
- ✅ 认证授权模块（JWT Token、BCrypt密码加密、登录锁定机制）
- ✅ 用户管理模块（CRUD、状态管理、解锁功能、权限管理、邮箱验证）
- ✅ 角色管理模块（CRUD、状态管理）
- ✅ 参数配置模块（CRUD、键名查询）
- ✅ 通知公告模块（CRUD、状态管理）
- ✅ 操作日志模块（查询、筛选）
- ✅ 登录日志模块（查询、筛选、IP定位）
- ✅ 药品信息管理模块（CRUD、多方式查询、第三方API集成、按供应商查询）
- ✅ 权限控制系统（RBAC、细粒度权限、用户直接权限关联、权限拦截器、权限注解）
- ✅ 仪表盘模块（系统管理员、仓库管理员、采购专员、医护人员、供应商专用仪表盘）
- ✅ 第三方API集成（万维易源API、极速数据API）
- ✅ 超级管理员管理模块（启用/停用、邮箱验证码验证）
- ✅ 邮箱验证码服务（EmailVerificationService）
- ✅ 库存管理模块（查询、筛选、近效期预警、库存统计、并发安全）
- ✅ 入库管理模块（采购订单入库、临时入库、验收、效期校验、特殊药品双人操作）
- ✅ 出库管理模块（出库申请、审批、执行、库存扣减、特殊药品双审）
- ✅ 采购订单管理模块（创建、编辑、状态流转、物流跟踪、条形码生成）
- ✅ 库存调整模块（盘盈/盘亏、特殊药品双人操作、审核）
- ✅ 供应商管理模块（CRUD、审核功能、多对多关系）
- ✅ 供应商-药品关联管理模块（添加、删除、更新单价）
- ✅ 文件上传模块（图片上传、删除、文件类型验证、大小限制）
- ✅ 条形码服务（BarcodeService、订单条形码生成）
- ✅ IP定位服务（IpLocationService、登录地点识别）
- ✅ 数据库表结构设计（21张表全部创建完成，包含sys_user_permission表）
- ✅ 统一响应格式（Result<T>）
- ✅ 全局异常处理（GlobalExceptionHandler）
- ✅ 自动填充处理器（创建时间、更新时间）

#### 前端开发
- ✅ 项目基础架构搭建（React 18 + Vite + Ant Design）
- ✅ 登录页面（用户名/手机号登录、Token管理）
- ✅ 主布局（侧边栏导航、顶部用户信息、响应式布局、权限控制、IndexRedirect）
- ✅ 用户管理页面（列表、新增、编辑、删除、状态管理、解锁、超级管理员管理、权限管理）
- ✅ 角色管理页面（列表、新增、编辑、删除、状态管理、超级管理员管理）
- ✅ 参数配置页面（列表、新增、编辑、删除）
- ✅ 通知公告页面（列表、新增、编辑、删除、状态管理）
- ✅ 操作日志页面（列表、多条件筛选、权限控制）
- ✅ 登录日志页面（列表、多条件筛选、IP定位显示）
- ✅ 仪表盘页面（系统管理员、仓库管理员、采购专员、医护人员、供应商专用仪表盘、响应式布局）
- ✅ 药品管理页面（列表、新增、编辑、删除、多方式查询、表单自动填充）
- ✅ 库存管理页面（列表、多条件筛选、近效期预警显示、库存调整功能）
- ✅ 入库管理页面（列表、多条件筛选、入库创建、验收、效期校验）
- ✅ 出库管理页面（列表、多条件筛选、出库申请、审批、执行）
- ✅ 采购订单管理页面（列表、新增、编辑、删除、状态流转、条形码生成、供应商筛选）
- ✅ 供应商管理页面（列表、新增、编辑、删除、审核功能、关联管理）
- ✅ 供应商仪表盘页面（订单统计、状态分布、金额统计、订单趋势）
- ✅ 供应商订单管理页面（订单列表、搜索、状态筛选、订单详情）
- ✅ 超级管理员管理组件（SuperAdminModal：状态显示、验证码发送、启用/停用）
- ✅ 路由守卫（PrivateRoute）
- ✅ HTTP请求封装（Axios拦截器）
- ✅ 权限工具（前端权限检查、细粒度权限支持）

#### 数据库设计
- ✅ 系统表设计（6张：sys_role, sys_user, sys_config, sys_notice, operation_log, login_log）
- ✅ 权限表设计（4张：sys_user_role, sys_permission, sys_role_permission, sys_user_permission）
- ✅ 业务表设计（10张：supplier, drug_info, supplier_drug, inventory, purchase_order, purchase_order_item, inbound_record, outbound_apply, outbound_apply_item, inventory_adjustment）
- ✅ 扩展表设计（1张：favorite_drug）
- ✅ 外键约束设计（所有外键都设置约束，保证数据完整性）
- ✅ 索引优化设计（为常用查询字段创建索引，提升查询性能）
- ✅ 初始化数据脚本（init_simple.sql、init_permissions.sql、init_super_admin.sql等）
- ✅ 数据库升级脚本（add_user_email_field.sql、add_supplier_remark_field.sql、create_supplier_drug_relation.sql、add_user_permission_system.sql等）

### 🚧 待实现功能

#### 业务模块（开发状态）
- ✅ 药品信息管理（CRUD、扫码识别、第三方API集成）**已完成**
  - 后端：DrugInfoController（CRUD、扫码识别、万维易源API、极速数据API集成、按供应商查询）
  - 前端：DrugManagement.jsx（列表、新增、编辑、删除、扫码、药品名称搜索、批准文号搜索）
- ✅ 库存管理（查询、筛选、近效期预警、库存调整）**已完成**
  - 后端：InventoryController（查询、筛选、近效期预警）、InventoryAdjustmentController（盘盈/盘亏、特殊药品双人操作、审核）
  - 前端：InventoryManagement.jsx（列表、多条件筛选、近效期预警、库存调整功能）
- ✅ 供应商管理（数据库结构优化、多对多关系、权限配置、关联管理）**已完成**
  - 后端：SupplierController（CRUD、审核功能）、SupplierDrugController（供应商-药品关联管理：添加、删除、更新单价）
  - 前端：SupplierManagement.jsx（列表、新增、编辑、删除、审核功能、关联管理）
  - 数据库：supplier_drug表（多对多关系）、supplier表（备注字段、审核状态）
- ✅ 入库管理（验收、效期校验、特殊药品双人操作）**已完成**
  - 后端：InboundRecordController（采购订单入库、临时入库、验收、效期校验、特殊药品双人操作）
  - 前端：InboundManagement.jsx（列表、多条件筛选、入库创建、验收、效期校验）
  - 功能：入库记录查询、采购订单入库、临时入库、效期校验（PASS/WARNING/FORCE）、特殊药品双人操作、入库后自动更新库存、订单状态自动更新
- ✅ 出库管理（审批、库存扣减、特殊药品双人审批）**已完成**
  - 后端：OutboundApplyController（出库申请、审批、执行、库存扣减、特殊药品双审）
  - 前端：OutboundManagement.jsx（列表、多条件筛选、出库申请、审批、执行）
  - 功能：出库申请（医护人员申领）、出库审批（普通药品单审、特殊药品双审）、出库执行（先进先出FIFO、库存扣减）、申请状态流转（PENDING → APPROVED/REJECTED → OUTBOUND）
- ✅ 采购订单管理（创建、流转、物流跟踪）**已完成**
  - 后端：PurchaseOrderController（创建、编辑、状态流转、物流跟踪、条形码生成）
  - 前端：PurchaseOrderManagement.jsx（列表、新增、编辑、删除、状态流转、条形码生成、供应商筛选）
  - 功能：采购订单创建、编辑、状态流转（PENDING → CONFIRMED → SHIPPED → RECEIVED）、物流跟踪、条形码生成、供应商筛选、订单明细管理
- ✅ 药品申领管理（申请、审批、出库）**已完成**
  - 已集成在出库管理模块中，医护人员可通过出库申请功能进行药品申领

#### 功能增强（开发状态）
- ✅ 数据可视化图表（仪表盘统计、趋势分析）**已完成**
  - 系统管理员仪表盘：基础统计、登录趋势、操作日志统计
  - 仓库管理员仪表盘：近效期预警、待办任务、出入库统计、出入库趋势
  - 采购专员仪表盘：订单统计、供应商统计、订单趋势
  - 医护人员仪表盘：申领统计、状态分布、申领趋势
  - 供应商仪表盘：订单统计、状态分布、金额统计、订单趋势
- ✅ 权限管理模块（权限分配、菜单权限、接口权限、细粒度权限）**已完成**
  - 基于角色的权限控制（RBAC）
  - 用户直接权限关联（细粒度权限控制）
  - 权限拦截器（接口级权限控制）
  - 权限注解（@RequiresPermission）
  - 前端权限检查（菜单权限、按钮权限）
- ✅ 文件上传功能（图片上传、文件存储）**已完成**
  - 后端：FileUploadController（图片上传、删除、文件类型验证、大小限制）
  - 支持格式：jpg, jpeg, png, gif, bmp, webp
  - 文件大小限制：最大10MB
  - 按日期分类存储（yyyy/MM/dd目录结构）
- ✅ 条形码服务（订单条形码生成）**已完成**
  - 后端：BarcodeService、BarcodeServiceImpl
  - 支持订单条形码生成和下载
- ✅ IP定位服务（登录地点识别）**已完成**
  - 后端：IpLocationService、IpLocationServiceImpl
  - 支持根据IP地址识别登录地点
- ⏳ 数据导出功能（Excel导出）
- ⏳ 扫码枪适配（HID键盘模式）
- ⏳ 跨终端响应式优化（Pad端适配）
- ⏳ 常用药品收藏功能（数据库表已创建，功能待实现）

## 开发规范

### 后端规范
- Controller层：负责接收请求、参数校验、返回响应
- Service层：负责业务逻辑处理
- Mapper层：负责数据库操作
- 统一使用Result<T>作为响应格式
- 统一异常处理（GlobalExceptionHandler）

### 前端规范
- 使用函数式组件和Hooks
- 统一使用Ant Design组件库
- API请求统一使用request工具
- 路由使用React Router
- Token存储在Cookie中

## 数据库表详细说明

### 系统表字段说明

#### sys_role（角色表）
- `id`: 主键ID
- `role_name`: 角色名称
- `role_code`: 角色代码（唯一，如：SUPER_ADMIN）
- `description`: 角色描述
- `status`: 状态（0-禁用/1-正常）
- `deleted`: 逻辑删除标记

#### sys_user（用户表）
- `id`: 主键ID
- `username`: 用户名（唯一，用于登录）
- `phone`: 手机号（唯一，用于登录）
- `password`: 密码（BCrypt加密）
- `role_id`: 角色ID（外键）
- `status`: 状态（0-禁用/1-正常）
- `lock_time`: 锁定时间（登录失败5次后锁定1小时）
- `login_fail_count`: 登录失败次数

#### operation_log（操作日志表）
- `id`: 主键ID
- `user_id`: 操作人ID
- `username`: 操作人用户名
- `module`: 操作模块
- `operation_type`: 操作类型（INSERT/UPDATE/DELETE/SELECT）
- `operation_content`: 操作内容
- `ip`: IP地址
- `status`: 操作状态（0-失败/1-成功）
- `operation_time`: 操作时间

#### login_log（登录日志表）
- `id`: 主键ID
- `user_id`: 用户ID
- `username`: 用户名
- `ip`: 登录IP
- `location`: 登录地点
- `browser`: 浏览器类型
- `os`: 操作系统
- `status`: 登录状态（0-失败/1-成功）
- `msg`: 登录消息
- `login_time`: 登录时间

### 业务表字段说明

#### drug_info（药品信息表）
- `national_code`: 国家本位码（唯一，扫码识别）
- `trace_code`: 药品追溯码（唯一）
- `product_code`: 商品码
- `drug_name`: 通用名称
- `dosage_form`: 剂型
- `specification`: 规格
- `is_special`: 是否特殊药品（0-普通/1-特殊）
- `storage_requirement`: 存储要求

#### inventory（库存表）
- `drug_id`: 药品ID（外键）
- `batch_number`: 批次号
- `quantity`: 库存数量（非负约束）
- `expiry_date`: 有效期至
- `storage_location`: 存储位置
- **唯一约束**: (drug_id, batch_number) - 同一药品同一批次唯一

#### purchase_order（采购订单表）
- `order_number`: 订单编号（唯一）
- `supplier_id`: 供应商ID（外键）
- `purchaser_id`: 采购员ID（外键）
- `status`: 订单状态（PENDING/CONFIRMED/SHIPPED/RECEIVED等）
- `logistics_number`: 物流单号
- `total_amount`: 订单总金额

#### inbound_record（入库记录表）
- `record_number`: 入库单号（唯一）
- `order_id`: 关联采购订单ID（可为空，支持临时入库）
- `drug_id`: 药品ID（外键）
- `batch_number`: 批次号
- `quantity`: 入库数量
- `expiry_date`: 有效期至
- `operator_id`: 操作人ID（外键）
- `second_operator_id`: 第二操作人ID（特殊药品双人操作）
- `status`: 验收状态（QUALIFIED/UNQUALIFIED）
- `expiry_check_status`: 效期校验状态（PASS/WARNING/FORCE）

#### outbound_apply（出库申请表）
- `apply_number`: 申领单号（唯一）
- `applicant_id`: 申请人ID（外键，医护人员）
- `department`: 所属科室
- `purpose`: 用途
- `status`: 申请状态（PENDING/APPROVED/REJECTED/OUTBOUND）
- `approver_id`: 审批人ID（外键，仓库管理员）
- `second_approver_id`: 第二审批人ID（特殊药品双人审批）

## 注意事项

### 数据库相关
1. **数据库密码**: 需要修改 `application.yml` 中的数据库密码为实际密码
2. **初始化脚本**: 推荐使用 `init_simple.sql`，已包含所有20张表的创建语句（注意：supplier_drug表需要单独执行create_supplier_drug_relation.sql创建）
3. **字符集**: 数据库和表都使用 utf8mb4，支持中文和特殊字符
4. **外键约束**: 所有外键都设置了约束，删除时注意级联关系

### 安全相关
1. **JWT密钥**: 建议在生产环境中修改 `application.yml` 中的JWT密钥
2. **默认密码**: 默认管理员密码为`admin123`，首次登录后建议修改
3. **密码加密**: 所有密码都使用BCrypt加密存储，强度10轮
4. **登录锁定**: 连续5次登录失败会锁定1小时，需要管理员解锁

### 业务相关
1. **逻辑删除**: 所有删除操作均为逻辑删除，数据不会真正删除（deleted字段）
2. **操作日志**: 操作日志和登录日志不可删除，符合合规要求（保留5年）
3. **库存约束**: 库存数量必须≥0，禁止负库存
4. **特殊药品**: 特殊药品的入库、出库、库存调整需双人操作

### 开发相关
1. **Token存储**: Token存储在Cookie中（key: cdiom_token），有效期8小时
2. **API前缀**: 所有API接口前缀为 `/api/v1`
3. **响应格式**: 统一使用 `Result<T>` 格式
4. **异常处理**: 统一使用 `GlobalExceptionHandler` 处理异常

## 常见问题

### Q1: 登录失败怎么办？
**A**: 检查以下几点：
1. 确认数据库已初始化，admin用户已创建
2. 确认密码是否为BCrypt加密后的值（不是明文）
3. 检查用户是否被锁定（连续5次失败会锁定1小时）
4. 检查用户状态是否为正常（status=1）

### Q2: 如何修改管理员密码？
**A**: 有两种方式：
1. **通过系统界面**: 登录后进入用户管理页面修改
2. **通过数据库**: 执行SQL更新密码（需要先BCrypt加密）
```sql
-- 需要将 'NEW_BCRYPT_PASSWORD' 替换为BCrypt加密后的新密码
UPDATE sys_user 
SET password = 'NEW_BCRYPT_PASSWORD' 
WHERE username = 'admin';
```

### Q3: 数据库初始化失败？
**A**: 检查以下几点：
1. MySQL版本是否为8.0+
2. 字符集是否为utf8mb4
3. 是否有足够的权限创建数据库和表
4. 检查SQL脚本中的中文注释是否导致编码问题（可使用init_simple.sql）

### Q4: 前端无法连接后端？
**A**: 检查以下几点：
1. 确认后端服务已启动（http://localhost:8080）
2. 检查 `vite.config.js` 中的代理配置
3. 检查后端CORS配置（如果需要）
4. 检查浏览器控制台的错误信息

### Q5: Token过期怎么办？
**A**: Token有效期为8小时，过期后需要重新登录。前端会自动检测Token过期并跳转到登录页。

## 开发指南

### 添加新的业务模块

1. **创建实体类**（Model）
```java
@Data
@TableName("your_table")
public class YourModel {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 其他字段...
}
```

2. **创建Mapper接口**
```java
@Mapper
public interface YourMapper extends BaseMapper<YourModel> {
}
```

3. **创建Service接口和实现**
```java
public interface YourService {
    // 方法定义
}

@Service
@RequiredArgsConstructor
public class YourServiceImpl implements YourService {
    private final YourMapper yourMapper;
    // 方法实现
}
```

4. **创建Controller**
```java
@RestController
@RequestMapping("/api/v1/your-module")
@RequiredArgsConstructor
public class YourController {
    private final YourService yourService;
    // 接口实现
}
```

### 前端添加新页面

1. **创建页面组件**
```jsx
// src/pages/YourPage.jsx
const YourPage = () => {
    // 页面逻辑
    return <div>Your Page</div>;
};
export default YourPage;
```

2. **添加路由**
```jsx
// src/App.jsx
<Route path="your-page" element={<YourPage />} />
```

3. **添加菜单项**
```jsx
// src/components/Layout.jsx
{
    key: '/your-page',
    icon: <YourIcon />,
    label: '你的页面',
}
```

## 版本历史

### v1.5.0 (2026-01-14)
- ✅ 实现细粒度权限系统
  - 创建用户权限关联表（sys_user_permission），支持用户直接拥有权限
  - 实现用户权限管理功能，支持更细粒度的权限控制
  - 添加细粒度出库、入库、库存管理权限
  - 新增数据库脚本：`add_user_permission_system.sql`
  - 新增权限配置脚本：`update_role_permissions_complete.sql`
- ✅ 新增供应商仪表盘功能
  - 前端：SupplierDashboard.jsx（供应商专用仪表盘）
  - 显示供应商订单统计、状态分布、金额统计等
  - 支持订单趋势图表展示
- ✅ 新增供应商订单管理功能
  - 前端：SupplierOrderManagement.jsx（供应商订单管理页面）
  - 供应商可查看和管理自己的订单
  - 支持订单状态筛选、搜索、详情查看
- ✅ 用户管理功能增强
  - 增强用户权限管理功能
  - 支持邮箱验证和权限处理
  - 改进用户管理界面和交互体验
- ✅ 仪表盘功能增强
  - Dashboard.jsx功能扩展，支持更多角色专用仪表盘
  - 采购专员专用仪表盘（订单统计、供应商统计）
  - 医护人员专用仪表盘（申领统计、状态分布）
  - 供应商专用仪表盘（订单统计、金额统计）
- ✅ 采购订单管理功能增强
  - PurchaseOrderController功能扩展
  - PurchaseOrderService业务逻辑优化
  - 前端PurchaseOrderManagement组件功能完善
  - 支持订单创建、编辑、状态流转、条形码生成
- ✅ 库存管理功能增强
  - InventoryService业务逻辑优化
  - 前端InventoryManagement组件功能增强
  - 支持库存查询、筛选、近效期预警
- ✅ 入库管理功能增强
  - InboundRecordService业务逻辑优化
  - 支持入库验收、效期校验、特殊药品双人操作
- ✅ 出库管理功能增强
  - OutboundApplyService业务逻辑优化
  - 前端OutboundManagement组件功能增强
  - 支持出库申请、审批、执行
- ✅ 通知公告功能增强
  - SysNoticeService业务逻辑优化
  - 前端NoticeManagement组件功能增强
- ✅ 登录日志功能增强
  - LoginLogService业务逻辑优化
  - 前端LoginLog组件功能增强
  - 支持IP定位服务优化
- ✅ 文件上传功能增强
  - FileUploadController功能扩展
  - 改进文件上传错误处理
- ✅ 条形码服务
  - 新增BarcodeService和BarcodeServiceImpl
  - 支持订单条形码生成
- ✅ 配置优化
  - application.yml配置更新，提高清晰度和可用性
  - WebMvcConfig配置增强
  - 异步配置优化
- ✅ 文档完善
  - 新增Code_Completeness_Report.md（代码完整性检查报告）
  - 新增Code_Logic_Vulnerability_Report.md（代码逻辑漏洞检查报告）
  - 新增Fine_Grained_Permission_System_Guide.md（细粒度权限系统指南）
  - 新增Form_Parameter_Mapping_Report.md（表单参数映射报告）
  - 更新Core_Business_Requirements_Analysis.md
  - 更新Order_Inbound_Record_Relationship_Guide.md
  - 更新Drug_Data_Import_Guide.md
  - 更新Permission_Issue_Fix_Guide.md
  - 更新Concurrency_Configuration_Guide.md
  - 更新Drug_Data_Retrieval_Troubleshooting_Guide.md
- ✅ 代码优化
  - 大量代码重构和优化
  - 改进异常处理
  - 优化数据库查询
  - 提升代码质量和可维护性

### v1.4.0 (2026-01-14)
- ✅ 实现供应商-药品关联管理功能
  - 创建SupplierDrug实体类，支持供应商与药品的多对多关系
  - 实现SupplierDrugController，提供关联添加、删除、单价更新接口
  - 更新DrugInfoController，支持根据供应商ID查询药品列表
  - 前端SupplierManagement组件增强，支持动态获取供应商关联的药品
  - 前端PurchaseOrderManagement组件优化，根据选择的供应商动态加载药品
- ✅ 新增文件上传功能
  - 创建FileUploadController，支持图片文件上传和删除
  - 支持图片格式：jpg, jpeg, png, gif, bmp, webp
  - 文件大小限制：最大10MB
  - 按日期分类存储（yyyy/MM/dd目录结构）
  - 生成唯一文件名（UUID），防止文件名冲突
  - 配置化上传路径和URL前缀（application.yml）
- ✅ 用户管理功能增强
  - SysUserController功能扩展，增强用户管理能力
  - SysUser实体添加验证注解，提升数据验证
  - 前端UserManagement组件功能增强
- ✅ 供应商管理功能增强
  - Supplier实体添加验证注解
  - SupplierController功能扩展
  - 前端SupplierManagement组件功能完善
- ✅ 全局异常处理增强
  - GlobalExceptionHandler功能扩展，提升错误处理能力
- ✅ 文档完善
  - 新增表单验证和安全检查报告（Form_Validation_Security_Report.md）
  - 更新数据库脚本说明文档

### v1.3.0 (2026-01-14)
- ✅ 完善供应商管理功能
  - 为供应商表添加备注字段（remark），支持供应商信息备注
  - 创建供应商-药品关联表（supplier_drug），支持供应商与药品的多对多关系
  - 支持一个药品可以有多个供应商，每个供应商可以设置不同的单价
  - 优化数据库结构，移除drug_info表中的supplier_id和supplier_name字段
  - 添加供应商审核权限（supplier:audit）
  - 更新角色权限配置：采购专员可创建供应商，仓库管理员可审核供应商
- ✅ 新增数据库脚本
  - `add_supplier_remark_field.sql` - 为供应商表添加备注字段
  - `create_supplier_drug_relation.sql` - 创建供应商-药品关联表并迁移数据
  - `update_supplier_permissions.sql` - 更新供应商管理权限配置

### v1.2.0 (2026-01-13)
- ✅ 完成超级管理员管理功能（启用/停用、邮箱验证码验证）
- ✅ 完成库存管理页面（列表查询、多条件筛选、近效期预警）
- ✅ 添加用户邮箱字段支持（用于超级管理员验证）
- ✅ 实现邮箱验证码服务（发送、验证、自动清理）

### v1.1.0 (2026-01)
- ✅ 完成药品信息管理模块（CRUD、多方式查询、第三方API集成）
- ✅ 完成权限控制系统（RBAC、权限拦截器、权限注解）
- ✅ 完成仪表盘功能（系统管理员仪表盘、仓库管理员仪表盘）
- ✅ 集成万维易源API（根据药品名称和批准文号查询）
- ✅ 集成极速数据API（根据商品码和批准文号查询）
- ✅ 实现多API数据自动合并逻辑
- ✅ 优化前端表单布局和用户体验
- ✅ 添加特殊药品友好提示

### v1.0.0 (2026-01)
- ✅ 完成基础架构搭建
- ✅ 完成用户管理、角色管理、参数配置、通知公告、操作日志、登录日志模块
- ✅ 完成数据库表结构设计（20张表）
- ✅ 完成前端基础页面开发

## 技术亮点

### 后端技术亮点
1. **前后端分离架构**: Spring Boot提供RESTful API，React负责UI展示
2. **JWT无状态认证**: 使用JWT Token实现无状态认证，支持分布式部署
3. **MyBatis-Plus增强**: 简化CRUD操作，支持逻辑删除、自动填充
4. **统一异常处理**: 全局异常处理器，统一错误响应格式
5. **安全机制完善**: BCrypt密码加密、登录失败锁定、操作日志记录

### 前端技术亮点
1. **现代化技术栈**: React 18 + Vite，开发体验优秀
2. **组件化开发**: 使用Ant Design组件库，快速构建UI
3. **路由守卫**: 实现基于Token的路由权限控制
4. **HTTP拦截器**: 统一处理请求和响应，自动携带Token
5. **响应式设计**: 支持PC和Pad端访问

### 数据库设计亮点
1. **规范化设计**: 遵循第三范式，减少数据冗余
2. **索引优化**: 为常用查询字段创建索引，提升查询性能
3. **外键约束**: 保证数据完整性和一致性
4. **合规设计**: 操作日志不可删除，保留5年（符合GSP规范）
5. **批次管理**: 库存按批次管理，支持近效期预警

## 许可证

ISC

## 作者

CDIOM开发团队

## 部署说明

### 开发环境部署

#### 后端部署
1. 确保JDK 17已安装
2. 确保MySQL 8.0已安装并运行
3. 执行数据库初始化脚本
4. 修改 `application.yml` 中的数据库配置
5. 运行 `mvn spring-boot:run` 启动服务

#### 前端部署
1. 确保Node.js 18+已安装
2. 执行 `npm install` 安装依赖
3. 运行 `npm run dev` 启动开发服务器

### 生产环境部署（建议）

#### 后端部署
1. **打包应用**
```bash
cd cdiom_backend
mvn clean package
```
生成jar包：`target/cdiom_backend-1.0.0.jar`

2. **运行应用**
```bash
java -jar cdiom_backend-1.0.0.jar
```

3. **使用Nginx反向代理**（可选）
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### 前端部署
1. **构建生产版本**
```bash
cd cdiom_frontend
npm run build
```
生成 `dist` 目录

2. **部署到Nginx**
```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/cdiom_frontend/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
    }
}
```

## 更新日志

### v1.5.0 (2026-01-14)
- ✅ 实现细粒度权限系统
  - **数据库结构**：
    - 创建用户权限关联表（`sys_user_permission`），支持用户直接拥有权限
    - 新增数据库脚本：`add_user_permission_system.sql`（创建用户权限关联表和细粒度权限）
    - 新增权限配置脚本：`update_role_permissions_complete.sql`（完整角色权限配置）
  - **权限管理**：
    - 支持用户直接拥有权限，实现更细粒度的权限控制
    - 添加细粒度出库相关权限（outbound:view, outbound:apply, outbound:approve, outbound:approve:special, outbound:execute, outbound:reject）
    - 添加细粒度入库相关权限（inbound:view, inbound:create, inbound:approve, inbound:execute）
    - 添加细粒度库存管理权限（inventory:view, inventory:adjust, inventory:adjust:approve）
    - 为各角色分配相应的细粒度权限
  - **后端实现**：
    - 创建SysUserPermission实体类和Mapper
    - 实现PermissionService权限查询功能增强
    - 更新权限拦截器，支持用户直接权限检查
- ✅ 新增供应商仪表盘功能
  - 前端：SupplierDashboard.jsx（供应商专用仪表盘）
  - 显示供应商订单统计（总订单数、状态分布）
  - 金额统计（总金额、待确认金额、已确认金额）
  - 订单趋势图表（最近7天订单数量趋势）
  - 响应式布局，支持不同屏幕尺寸
- ✅ 新增供应商订单管理功能
  - 前端：SupplierOrderManagement.jsx（供应商订单管理页面）
  - 供应商可查看和管理自己的订单
  - 支持订单列表展示（分页、搜索、状态筛选）
  - 订单详情查看
  - 订单状态管理
  - 响应式表格布局
- ✅ 仪表盘功能增强
  - Dashboard.jsx功能扩展，支持更多角色专用仪表盘
  - 采购专员专用仪表盘（订单统计、供应商统计、订单趋势）
  - 医护人员专用仪表盘（申领统计、状态分布、申领趋势）
  - 供应商专用仪表盘（订单统计、金额统计、订单趋势）
  - DashboardController和DashboardService功能扩展
- ✅ 采购订单管理功能增强
  - PurchaseOrderController功能扩展
  - PurchaseOrderService业务逻辑优化
  - 前端PurchaseOrderManagement组件功能完善（482行新增代码）
  - 支持订单创建、编辑、状态流转、条形码生成
  - 优化订单查询和筛选功能
- ✅ 库存管理功能增强
  - InventoryService业务逻辑优化（474行代码重构）
  - 前端InventoryManagement组件功能增强（291行新增代码）
  - 支持库存查询、筛选、近效期预警
  - 优化库存统计和预警功能
- ✅ 入库管理功能增强
  - InboundRecordService业务逻辑优化（611行代码重构）
  - 支持入库验收、效期校验、特殊药品双人操作
  - 优化入库流程和验证逻辑
- ✅ 出库管理功能增强
  - OutboundApplyService业务逻辑优化
  - 前端OutboundManagement组件功能增强（255行新增代码）
  - 支持出库申请、审批、执行
  - 优化出库流程和权限控制
- ✅ 通知公告功能增强
  - SysNoticeService业务逻辑优化（338行代码重构）
  - 前端NoticeManagement组件功能增强（131行新增代码）
- ✅ 登录日志功能增强
  - LoginLogService业务逻辑优化（138行代码重构）
  - 前端LoginLog组件功能增强（364行代码重构）
  - 支持IP定位服务优化（IpLocationServiceImpl 216行代码重构）
- ✅ 文件上传功能增强
  - FileUploadController功能扩展（252行代码重构）
  - 改进文件上传错误处理
  - 优化文件验证和存储逻辑
- ✅ 条形码服务
  - 新增BarcodeService和BarcodeServiceImpl（44行+80行代码）
  - 支持订单条形码生成
- ✅ 配置优化
  - application.yml配置更新（110行变更），提高清晰度和可用性
  - application.yml.example配置更新（74行变更）
  - WebMvcConfig配置增强（64行变更）
  - 异步配置优化（AsyncConfig）
- ✅ 文档完善
  - 新增Code_Completeness_Report.md（520行）- 代码完整性检查报告
  - 新增Code_Logic_Vulnerability_Report.md（382行）- 代码逻辑漏洞检查报告 ⚠️ **重要**
  - 新增Fine_Grained_Permission_System_Guide.md（350行）- 细粒度权限系统指南
  - 新增Form_Parameter_Mapping_Report.md（211行）- 表单参数映射报告
  - 更新Core_Business_Requirements_Analysis.md（1224行重构）
  - 更新Order_Inbound_Record_Relationship_Guide.md
  - 更新Drug_Data_Import_Guide.md
  - 更新Permission_Issue_Fix_Guide.md
  - 更新Concurrency_Configuration_Guide.md
  - 更新Drug_Data_Retrieval_Troubleshooting_Guide.md（164行新增）
- ✅ 代码优化
  - 大量代码重构和优化（124个文件变更，10618行新增，4661行删除）
  - 改进异常处理
  - 优化数据库查询
  - 提升代码质量和可维护性
  - 统一代码风格和规范

### v1.4.0 (2026-01-14)
- ✅ 实现供应商-药品关联管理功能
  - **后端实现**：
    - 创建SupplierDrug实体类，支持供应商与药品的多对多关系
    - 实现SupplierDrugService和SupplierDrugServiceImpl，提供关联管理业务逻辑
    - 创建SupplierDrugController，提供RESTful API接口（添加、删除、更新单价）
    - 更新DrugInfoController，新增根据供应商ID查询药品列表接口
    - 更新DrugInfoService，支持按供应商ID查询药品
  - **前端实现**：
    - 增强SupplierManagement组件，支持供应商-药品关联管理
    - 优化PurchaseOrderManagement组件，根据选择的供应商动态加载关联药品
    - 改进UserManagement组件，增强用户管理功能
  - **数据库**：
    - 使用已有的supplier_drug表（在v1.3.0中创建）
    - 支持一个药品可以有多个供应商，每个供应商可以设置不同的单价
- ✅ 新增文件上传功能
  - **后端实现**：
    - 创建FileUploadController，提供文件上传和删除接口
    - 支持图片格式：jpg, jpeg, png, gif, bmp, webp
    - 文件大小限制：最大10MB
    - 按日期分类存储（yyyy/MM/dd目录结构）
    - 生成唯一文件名（UUID），防止文件名冲突
    - 配置化上传路径和URL前缀（application.yml中的file.upload.path和file.upload.url-prefix）
  - **安全特性**：
    - 文件类型验证（仅允许图片格式）
    - 文件大小验证（最大10MB）
    - 权限控制（需要drug:view或drug:manage权限）
- ✅ 用户管理功能增强
  - SysUserController功能扩展，增强用户管理能力
  - SysUser实体添加验证注解，提升数据验证
  - SysUserService和SysUserServiceImpl功能增强
  - 前端UserManagement组件功能增强
- ✅ 供应商管理功能增强
  - Supplier实体添加验证注解
  - SupplierController功能扩展
  - SupplierService功能增强
  - 前端SupplierManagement组件功能完善
- ✅ 全局异常处理增强
  - GlobalExceptionHandler功能扩展，提升错误处理能力
  - 添加文件上传异常处理
- ✅ 配置增强
  - WebMvcConfig配置更新，支持文件上传
  - application.yml添加文件上传配置项
- ✅ 文档完善
  - 新增表单验证和安全检查报告（Form_Validation_Security_Report.md）
  - 更新数据库脚本说明文档（db/README.md）
  - 更新API接口文档，添加新接口说明

### v1.3.0 (2026-01-14)
- ✅ 完善供应商管理功能
  - **数据库结构优化**：
    - 为供应商表（supplier）添加备注字段（remark），支持供应商信息备注
    - 创建供应商-药品关联表（supplier_drug），实现供应商与药品的多对多关系
    - 支持一个药品可以有多个供应商，每个供应商可以设置不同的单价（unit_price）
    - 优化数据库结构，从drug_info表中移除supplier_id和supplier_name字段，改为通过中间表关联
    - 自动迁移现有数据：将drug_info表中的supplier_id数据迁移到supplier_drug中间表
  - **权限管理优化**：
    - 新增供应商审核权限（supplier:audit）
    - 采购专员（角色ID=3）：可查看、创建、更新供应商，但不能删除和审核
    - 仓库管理员（角色ID=2）：可审核供应商
  - **新增数据库脚本**：
    - `add_supplier_remark_field.sql` - 为供应商表添加备注字段
    - `create_supplier_drug_relation.sql` - 创建供应商-药品关联表并迁移现有数据
    - `update_supplier_permissions.sql` - 更新供应商管理权限配置
  - **数据库表变更**：
    - 新增表：`supplier_drug`（供应商-药品关联表，第20张表）
    - 修改表：`supplier`（添加remark字段）
    - 修改表：`drug_info`（移除supplier_id和supplier_name字段）

### v1.2.0 (2026-01-13)
- ✅ 新增超级管理员管理功能
  - 实现超级管理员账户的启用/停用功能
  - 使用邮箱验证码进行安全验证（必须使用当前登录用户绑定的邮箱）
  - 后端：SuperAdminController（启用/停用、状态查询、验证码发送）
  - 前端：SuperAdminModal组件（状态显示、验证码发送、启用/停用操作）
  - 数据库：为用户表添加email字段，创建init_super_admin.sql初始化脚本
  - 服务：EmailVerificationService（邮箱验证码发送、验证、自动清理）
  - 安全特性：验证码5分钟有效期、自动清理过期验证码、操作日志记录
  - 用户体验：邮箱地址脱敏显示、倒计时提示、自动退出登录（停用当前用户时）
- ✅ 新增库存管理页面
  - 前端：InventoryManagement.jsx（库存列表查询、多条件筛选）
  - 支持按药品名称、批次号、存储位置、有效期范围、特殊药品筛选
  - 近效期预警显示（红色预警≤90天、黄色预警90-180天）
  - 显示药品信息、批次信息、库存数量、有效期等关键数据
  - 响应式表格布局，支持横向滚动

### v1.1.0 (2026-01)
- ✅ 新增药品信息管理模块
  - 支持药品信息的增删改查
  - 支持四种新增药品方式：手动输入、扫码识别、药品名称搜索、批准文号搜索
  - 集成万维易源API（1468-4接口）
  - 集成极速数据API（JisuAPI）
  - 实现多API数据自动合并（万维易源API + 极速数据API）
  - 优化表单布局，分组显示更紧凑美观
  - 添加特殊药品友好提示
- ✅ 完善权限控制系统
  - 实现基于角色的权限控制（RBAC）
  - 添加权限拦截器（PermissionInterceptor）
  - 添加权限注解（@RequiresPermission）
  - 系统管理员自动拥有所有权限
  - 前端权限检查（菜单权限、按钮权限）
  - 操作日志仅系统管理员可见
- ✅ 新增仪表盘功能
  - 系统管理员仪表盘（基础统计、登录趋势、操作日志统计）
  - 仓库管理员仪表盘（近效期预警、待办任务、出入库统计）
  - 响应式布局，支持不同屏幕尺寸
- ✅ 优化错误处理
  - 改进API错误处理，避免频繁显示"系统内部错误"
  - 添加友好的错误提示
  - 优化前端请求拦截器

### v1.0.0 (2026-01)
- ✅ 完成项目基础架构搭建
- ✅ 完成用户管理、角色管理、参数配置、通知公告、操作日志、登录日志模块
- ✅ 完成数据库表结构设计（20张表全部创建）
- ✅ 完成前端基础页面开发
- ✅ 完善README文档
- ✅ 完成数据库初始化脚本

## 贡献指南

欢迎贡献代码！请遵循以下规范：

1. **代码规范**: 遵循Java和JavaScript编码规范
2. **提交信息**: 使用清晰的提交信息，说明修改内容
3. **测试**: 确保代码通过测试，不影响现有功能
4. **文档**: 更新相关文档说明

## 联系方式

如有问题或建议，请通过以下方式联系：
- 项目Issues: [GitHub Issues]
- 邮箱: [your-email@example.com]

## 第三方API集成

### 万维易源API（1468-4）
- **用途**: 根据药品名称或批准文号查询药品详细信息
- **接口地址**: `https://route.showapi.com/1468-4`
- **配置**: 在 `application.yml` 中配置 `yuanyanyao.api.app-key`
- **返回字段**: 批准文号、生产厂家、剂型、规格、有效期、存储要求等
- **使用场景**: 
  - 通过药品名称搜索药品信息
  - 通过批准文号搜索药品信息

### 极速数据API（JisuAPI）
- **用途**: 根据商品码或批准文号查询药品详细信息
- **接口地址**: `https://api.jisuapi.com/medicine/detail`
- **配置**: 在 `application.yml` 中配置 `jisuapi.api.app-key`
- **返回字段**: 国家本位码、条形码、描述、包装规格等
- **使用场景**:
  - 通过商品码/本位码/追溯码搜索药品信息
  - 补充万维易源API未返回的信息（国家本位码、条形码、描述）
  - 用包装规格替换万维易源API返回的规格

### API数据合并逻辑
1. **通过药品名称/批准文号查询**:
   - 先调用万维易源API获取基本信息
   - 如果获取到批准文号，自动调用极速数据API补充信息
   - 合并两个API的数据，极速数据API的规格字段会替换万维易源API的规格

2. **通过商品码/本位码/追溯码查询**:
   - 先查询本地数据库
   - 如果未找到，调用极速数据API查询

---

## 文档信息

**文档创建时间**：2026年1月13日 10:31:17  
**最后修改时间**：2026年1月15日  
**当前更新时间**：2026年1月15日  
**文档版本**：v1.5.2

---

**最后更新**: 2026年1月15日

**更新内容（v1.5.2）**：
- ✅ 修正Vite版本号（5.4.8 → 7.3.1）
- ✅ 完善项目结构描述（补充所有Controller、Model、Config、前端页面）
- ✅ 修正数据库表编号（业务表从11开始，扩展表为21）
- ✅ 更新数据库初始化说明（详细说明21张表的创建顺序）
- ✅ 补充API接口文档（入库管理、出库管理、采购订单管理、供应商管理、库存调整接口）
- ✅ 完善数据库相关注意事项

