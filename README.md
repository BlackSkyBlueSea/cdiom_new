# CDIOM - 临床药品出入库管理系统

## 项目概述

基于 Spring Boot + React + MySQL 的医疗药品仓库管理系统，实现药品信息管理、库存监控、出入库审批、批次追溯及多角色权限管控等核心功能。

> 📋 **核心业务需求分析**：详细的需求分析文档请参考 [核心业务需求分析.md](./核心业务需求分析.md)，包含库存管理、入库管理、出库管理的完整业务流程和处理流程。

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
- Vite 5.4.8
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
│   │       │   └── LoginLogController.java
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
│   │       │   ├── OperationLog.java
│   │       │   └── LoginLog.java
│   │       ├── config/               # 配置类
│   │       │   ├── SecurityConfig.java
│   │       │   ├── MyBatisPlusConfig.java
│   │       │   ├── MyMetaObjectHandler.java
│   │       │   └── filter/
│   │       │       └── JwtAuthenticationFilter.java
│   │       ├── util/                 # 工具类
│   │       │   └── JwtUtil.java
│   │       └── common/              # 公共类
│   │           ├── Result.java
│   │           └── exception/
│   │               ├── GlobalExceptionHandler.java
│   │               └── ServiceException.java
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── db/                       # 数据库脚本
│           ├── init.sql             # 完整初始化脚本（带注释）
│           ├── init_simple.sql      # 简化初始化脚本（推荐）
│           └── init_business_tables.sql # 业务表脚本
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
│   │   │   └── LoginLog.jsx
│   │   ├── components/               # 公共组件
│   │   │   ├── Layout.jsx
│   │   │   └── PrivateRoute.jsx
│   │   ├── utils/                    # 工具函数
│   │   │   ├── request.js           # Axios封装
│   │   │   └── auth.js              # 认证工具
│   │   ├── App.jsx                   # 根组件
│   │   └── main.jsx                  # 入口文件
│   ├── index.html                    # HTML模板
│   ├── vite.config.js                # Vite配置
│   └── package.json                  # 依赖配置
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
- 多API数据自动合并（万维易源API + 极速数据API）

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
- 权限拦截器（PermissionInterceptor）
- 权限注解（@RequiresPermission）
- 系统管理员自动拥有所有权限（*）
- 前端权限检查（菜单权限、按钮权限）

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

## 数据库设计

### 数据库基本信息
- **数据库名称：** cdiom_db
- **数据库版本：** MySQL 8.0.33
- **字符集：** utf8mb4
- **排序规则：** utf8mb4_unicode_ci
- **存储引擎：** InnoDB
- **总表数：** 19张

### 数据库表分类

#### 系统表（6张）
1. `sys_role` - 系统角色表
2. `sys_user` - 系统用户表
3. `sys_config` - 系统参数配置表
4. `sys_notice` - 系统通知公告表
5. `operation_log` - 操作日志表
6. `login_log` - 登录日志表

#### 权限表（3张）
7. `sys_user_role` - 用户角色关联表
8. `sys_permission` - 权限表
9. `sys_role_permission` - 角色权限关联表

#### 业务表（9张）
10. `supplier` - 供应商表
11. `drug_info` - 药品信息表
12. `inventory` - 库存表（按批次管理）
13. `purchase_order` - 采购订单表
14. `purchase_order_item` - 采购订单明细表
15. `inbound_record` - 入库记录表
16. `outbound_apply` - 出库申请表
17. `outbound_apply_item` - 出库申请明细表
18. `inventory_adjustment` - 库存调整记录表

#### 扩展表（1张）
19. `favorite_drug` - 常用药品收藏表

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
- **supplier**: 供应商信息，包含资质审核、合作状态等
- **drug_info**: 药品基础信息，支持扫码识别（国家本位码、追溯码、商品码）
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

supplier (1) ──< (N) drug_info
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
# 执行完整初始化脚本（包含所有19张表）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_simple.sql
```

**方式二：分步执行**
```bash
# 1. 先执行基础表结构
mysql -u root -p < cdiom_backend/src/main/resources/db/init_simple.sql

# 2. 如果需要单独创建业务表（已包含在init_simple.sql中）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_business_tables.sql
```

**注意**: `init_simple.sql` 已包含所有表的创建语句，推荐直接使用该文件。

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

### ✅ 已完成（2026年1月）

#### 后端开发
- ✅ 项目基础架构搭建（Spring Boot 3.2.8 + MyBatis-Plus）
- ✅ 认证授权模块（JWT Token、BCrypt密码加密、登录锁定机制）
- ✅ 用户管理模块（CRUD、状态管理、解锁功能）
- ✅ 角色管理模块（CRUD、状态管理）
- ✅ 参数配置模块（CRUD、键名查询）
- ✅ 通知公告模块（CRUD、状态管理）
- ✅ 操作日志模块（查询、筛选）
- ✅ 登录日志模块（查询、筛选）
- ✅ 药品信息管理模块（CRUD、多方式查询、第三方API集成）
- ✅ 权限控制系统（RBAC、权限拦截器、权限注解）
- ✅ 仪表盘模块（系统管理员仪表盘、仓库管理员仪表盘）
- ✅ 第三方API集成（万维易源API、极速数据API）
- ✅ 数据库表结构设计（19张表全部创建完成）
- ✅ 统一响应格式（Result<T>）
- ✅ 全局异常处理（GlobalExceptionHandler）
- ✅ 自动填充处理器（创建时间、更新时间）

#### 前端开发
- ✅ 项目基础架构搭建（React 18 + Vite + Ant Design）
- ✅ 登录页面（用户名/手机号登录、Token管理）
- ✅ 主布局（侧边栏导航、顶部用户信息、响应式布局、权限控制）
- ✅ 用户管理页面（列表、新增、编辑、删除、状态管理、解锁）
- ✅ 角色管理页面（列表、新增、编辑、删除、状态管理）
- ✅ 参数配置页面（列表、新增、编辑、删除）
- ✅ 通知公告页面（列表、新增、编辑、删除、状态管理）
- ✅ 操作日志页面（列表、多条件筛选、权限控制）
- ✅ 登录日志页面（列表、多条件筛选）
- ✅ 仪表盘页面（系统管理员仪表盘、仓库管理员仪表盘、响应式布局）
- ✅ 药品管理页面（列表、新增、编辑、删除、多方式查询、表单自动填充）
- ✅ 路由守卫（PrivateRoute）
- ✅ HTTP请求封装（Axios拦截器）
- ✅ 权限工具（前端权限检查）

#### 数据库设计
- ✅ 系统表设计（6张）
- ✅ 权限表设计（3张）
- ✅ 业务表设计（9张）
- ✅ 扩展表设计（1张）
- ✅ 外键约束设计
- ✅ 索引优化设计
- ✅ 初始化数据脚本

### 🚧 待实现功能

#### 业务模块（待开发）
- ✅ 药品信息管理（CRUD、扫码识别、第三方API集成）**已完成**
- ⏳ 库存管理（查询、调整、近效期预警）
- ⏳ 入库管理（验收、效期校验、特殊药品双人操作）
- ⏳ 出库管理（审批、库存扣减、特殊药品双人审批）
- ⏳ 采购订单管理（创建、流转、物流跟踪）
- ⏳ 供应商管理（CRUD、资质审核）
- ⏳ 药品申领管理（申请、审批、出库）

#### 功能增强（待开发）
- ✅ 数据可视化图表（仪表盘统计、趋势分析）**部分完成**
- ✅ 权限管理模块（权限分配、菜单权限、接口权限）**已完成**
- ⏳ 文件上传功能（图片上传、文件存储）
- ⏳ 数据导出功能（Excel导出）
- ⏳ 扫码枪适配（HID键盘模式）
- ⏳ 跨终端响应式优化（Pad端适配）
- ⏳ 常用药品收藏功能

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
2. **初始化脚本**: 推荐使用 `init_simple.sql`，已包含所有19张表的创建语句
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
- ✅ 完成数据库表结构设计（19张表）
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
- ✅ 完成数据库表结构设计（19张表全部创建）
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

**最后更新**: 2026年1月12日

