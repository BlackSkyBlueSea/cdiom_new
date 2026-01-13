# CDIOM 数据库脚本说明

本目录包含CDIOM系统的所有数据库脚本文件，用于数据库初始化、数据修复、权限配置等操作。

## 📋 脚本文件列表

### 🚀 数据库初始化脚本

#### 1. `init_simple.sql` ⭐ **推荐使用**
- **用途**：简化版数据库初始化脚本（推荐）
- **说明**：包含所有19张表的创建语句，无中文注释，避免编码问题
- **执行方式**：
  ```bash
  mysql -u root -p < init_simple.sql
  ```
- **包含内容**：
  - 创建数据库 `cdiom_db`
  - 创建所有系统表（6张）
  - 创建所有权限表（3张）
  - 创建所有业务表（9张）
  - 创建扩展表（1张）
  - 初始化基础数据（角色、用户、配置、通知）

#### 2. `init.sql`
- **用途**：完整初始化脚本（带详细注释）
- **说明**：包含所有表的创建语句和详细的中文注释
- **注意**：如果遇到编码问题，建议使用 `init_simple.sql`

#### 3. `cdiom_db_complete.sql`
- **用途**：完整数据库脚本（包含所有表结构和数据）
- **说明**：完整的数据库备份/恢复脚本

#### 4. `init_business_tables.sql`
- **用途**：业务表创建脚本
- **说明**：单独创建业务相关表的脚本（已包含在 `init_simple.sql` 中）
- **包含表**：
  - `supplier` - 供应商表
  - `drug_info` - 药品信息表
  - `inventory` - 库存表
  - `purchase_order` - 采购订单表
  - `purchase_order_item` - 采购订单明细表
  - `inbound_record` - 入库记录表
  - `outbound_apply` - 出库申请表
  - `outbound_apply_item` - 出库申请明细表
  - `inventory_adjustment` - 库存调整记录表

#### 5. `init_permissions.sql`
- **用途**：权限数据初始化脚本
- **说明**：初始化系统权限和角色权限关联数据
- **包含内容**：
  - 插入权限数据（用户管理、角色管理、药品管理、配置管理、通知管理、日志查看等）
  - 插入角色权限关联数据（为各角色分配权限）

#### 6. `init_super_admin.sql`
- **用途**：超级管理员角色和用户初始化脚本
- **说明**：创建超级管理员角色（roleId=6）和用户（username: super_admin）
- **包含内容**：
  - 插入超级管理员角色
  - 插入超级管理员用户（默认状态为停用，需要通过邮箱验证码启用）
  - 更新超级管理员密码

### 🔧 数据修复脚本

#### 7. `fix_chinese_data.sql`
- **用途**：修复中文数据脚本
- **说明**：修复数据库中的中文乱码问题

#### 8. `fix_chinese_utf8mb4.sql`
- **用途**：修复中文数据脚本（简化版）
- **说明**：简化版的中文数据修复脚本

#### 9. `fix_database_charset.sql`
- **用途**：修复数据库字符集脚本
- **说明**：将数据库和表的字符集修改为 utf8mb4

#### 10. `fix_operation_log_permission.sql`
- **用途**：修复操作日志权限脚本
- **说明**：修复操作日志相关的权限配置问题

### 📊 数据导入脚本

#### 11. `drug_info_insert.sql`
- **用途**：药品信息数据导入脚本
- **说明**：批量导入药品基础数据

### 🔐 用户管理脚本

#### 12. `update_admin_password.sql`
- **用途**：更新管理员密码脚本
- **说明**：更新默认管理员（admin）的密码
- **注意**：需要将密码替换为BCrypt加密后的值

#### 13. `add_user_email_field.sql`
- **用途**：为用户表添加邮箱字段
- **说明**：为 `sys_user` 表添加 `email` 字段，用于超级管理员启用/禁用操作的验证
- **字段特性**：
  - 可为空（普通用户可以不填写邮箱）
  - 非NULL值必须唯一（通过唯一索引保证）
  - 超级管理员账户必须绑定邮箱

## 📖 使用说明

### 首次安装数据库

**推荐方式**（使用简化脚本）：
```bash
mysql -u root -p < init_simple.sql
```

**完整方式**（使用完整脚本）：
```bash
mysql -u root -p < init.sql
```

### 初始化权限数据

如果使用 `init_simple.sql` 初始化，权限数据可能未完全初始化，需要执行：
```bash
mysql -u root -p < init_permissions.sql
```

### 初始化超级管理员

如果需要使用超级管理员功能，执行：
```bash
mysql -u root -p < init_super_admin.sql
```

**注意**：
- 超级管理员默认状态为停用（status=0）
- 需要通过系统界面使用邮箱验证码启用
- 需要先为超级管理员用户绑定邮箱地址

### 修复中文数据问题

如果遇到中文乱码问题，按顺序执行：
```bash
# 1. 修复数据库字符集
mysql -u root -p < fix_database_charset.sql

# 2. 修复中文数据
mysql -u root -p < fix_chinese_utf8mb4.sql
```

### 添加邮箱字段

如果现有数据库中没有 `email` 字段，执行：
```bash
mysql -u root -p < add_user_email_field.sql
```

## ⚠️ 注意事项

1. **字符集**：所有脚本都使用 utf8mb4 字符集，确保支持中文和特殊字符
2. **外键约束**：所有外键都设置了约束，删除数据时注意级联关系
3. **逻辑删除**：业务表支持逻辑删除（deleted字段），数据不会真正删除
4. **操作日志**：操作日志不可删除，符合合规要求（保留5年）
5. **密码加密**：所有密码都使用BCrypt加密，不要直接存储明文密码
6. **备份数据**：执行任何脚本前，建议先备份数据库

## 🔍 数据库表结构

### 系统表（6张）
1. `sys_role` - 系统角色表
2. `sys_user` - 系统用户表
3. `sys_config` - 系统参数配置表
4. `sys_notice` - 系统通知公告表
5. `operation_log` - 操作日志表
6. `login_log` - 登录日志表

### 权限表（3张）
7. `sys_user_role` - 用户角色关联表
8. `sys_permission` - 权限表
9. `sys_role_permission` - 角色权限关联表

### 业务表（9张）
10. `supplier` - 供应商表
11. `drug_info` - 药品信息表
12. `inventory` - 库存表（按批次管理）
13. `purchase_order` - 采购订单表
14. `purchase_order_item` - 采购订单明细表
15. `inbound_record` - 入库记录表
16. `outbound_apply` - 出库申请表
17. `outbound_apply_item` - 出库申请明细表
18. `inventory_adjustment` - 库存调整记录表

### 扩展表（1张）
19. `favorite_drug` - 常用药品收藏表

## 📝 默认数据

### 默认角色（5个）
1. 系统管理员（roleId=1）
2. 仓库管理员（roleId=2）
3. 采购专员（roleId=3）
4. 医护人员（roleId=4）
5. 供应商（roleId=5）

### 默认用户（1个）
- **用户名**：`admin`
- **密码**：`admin123`（BCrypt加密）
- **角色**：系统管理员

### 默认配置（4个）
- 效期预警阈值（默认90天）
- 日志保留期（默认5年）
- 系统名称
- 系统版本

## 🆘 常见问题

### Q1: 执行脚本时出现编码错误？
**A**: 使用 `init_simple.sql` 替代 `init.sql`，该脚本不包含中文注释。

### Q2: 如何修改管理员密码？
**A**: 执行 `update_admin_password.sql`，将密码替换为BCrypt加密后的值。

### Q3: 如何添加邮箱字段？
**A**: 执行 `add_user_email_field.sql`，该脚本会自动检查字段是否存在。

### Q4: 超级管理员无法启用？
**A**: 检查以下事项：
1. 超级管理员用户是否已创建（执行 `init_super_admin.sql`）
2. 超级管理员用户是否已绑定邮箱
3. 邮箱验证码是否正确

### Q5: 权限数据未初始化？
**A**: 执行 `init_permissions.sql` 初始化权限数据。

---

**最后更新**：2026年1月13日  
**维护人员**：CDIOM开发团队
