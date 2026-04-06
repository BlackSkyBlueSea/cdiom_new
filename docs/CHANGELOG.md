# CDIOM - 版本历史

本文档详细记录了CDIOM系统的所有版本更新历史，包含每个版本的功能更新、bug修复、性能优化等完整的版本变更内容。

> 📦 **GitHub仓库**：[https://github.com/BlackSkyBlueSea/cdiom_new](https://github.com/BlackSkyBlueSea/cdiom_new)  
> 🔖 **版本回退**：每个版本下方都提供了GitHub版本链接，点击可查看该版本的代码提交记录，便于版本回退和代码审查。

## 目录

- [工作区变更摘要（2026-03-28）](#工作区变更摘要2026-03-28多轮会话汇总)
- [v1.6.0 (2026-01-20)](#v160-2026-01-20)
- [v1.5.2 (2026-01-20)](#v152-2026-01-20)
- [v1.5.0 (2026-01-14)](#v150-2026-01-14)
- [v1.4.0 (2026-01-14)](#v140-2026-01-14)
- [v1.3.0 (2026-01-14)](#v130-2026-01-14)
- [v1.2.0 (2026-01-13)](#v120-2026-01-13)
- [v1.1.0 (2026-01)](#v110-2026-01)
- [v1.0.0 (2026-01)](#v100-2026-01)

---

## 工作区变更摘要（2026-03-28，多轮会话汇总）

> 详细条目、文件清单与回归建议见 **docs/UPDATE_LOG_20260328.md**。  
> 会话 request id（供 Cursor 侧对照）：`b243a701-41db-4bdf-b51a-6071a5987244`、`e450cf9c-094b-4238-999b-0ccdf2d80d63`、`cb98d4dd-2b61-44c3-bc0c-2d7520ee1c2a`、`c7f21f07-04fc-47ee-b43b-13de389e6cb8`、`2502b7f6-3749-4ac6-9c80-bebda7250e9b`、`bc6f7e9d-9962-44b6-8718-901f743dc271`、`577342f1-22bf-45c3-8a80-3163a3355ca7`、`b8db9dc3-93f4-4295-adbf-69eec56fd630`、`169941b9-b372-4d62-8870-cbd67076583e`。

### 系统参数与运行时生效

- ✅ **SystemConfigUtil**：从 `sys_config` 读取近效期预警/红色阈值、日志保留年限、JWT 有效期（毫秒），带缓存与合法范围校验；配置变更后由 `SysConfigServiceImpl` 清理缓存。
- ✅ **JWT**：`JwtUtil` 签发 Token 使用运行时 `jwt_expiration`。
- ✅ **业务一致**：仪表盘近效期统计、库存预警、入库效期校验等统一使用上述运行时参数。
- ✅ **接口**：`GET /api/v1/configs/runtime-effective` 增加 `logRetentionYears`、`jwtExpirationMs` 等。

### 日志与账号安全

- ✅ **定时清理**：`LogRetentionCleanupScheduler` 每日 03:00 按 `log_retention_years` 物理删除过期登录日志与操作日志。
- ✅ **管理员联系信息**：`GET /api/v1/auth/admin-contact` + `AdminContactInfo`，供个人中心提示用户联系系统管理员。
- ✅ **用户管理**：禁止删除或变更**当前登录账号**的状态；解锁用户使用 `UpdateWrapper` 正确清空锁定时间字段。

### 前端

- ✅ **列表页布局**：`tablePageLayout.js` 与 `Layout` 样式配合，多业务列表页主内容区与表体滚动一致。

---

## v1.7.0 (2026-03-09)

> 🔗 **GitHub版本链接**：[查看v1.7.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 安全性和代码质量优化

#### 安全性增强
- ✅ 文件上传安全性增强（MIME类型验证 + 文件魔数验证）
- ✅ Controller参数验证完善（所有POST/PUT方法已添加@Valid注解）
- ✅ 前端console日志清理（所有console调用已替换为logger工具）

#### 性能优化
- ✅ N+1查询问题优化（使用JOIN查询优化）
- ✅ 数据库索引优化（所有联合索引已存在）
- ✅ 前端代码优化（使用useCallback和useMemo优化性能）

#### 错误处理完善
- ✅ 统一使用ServiceException替代RuntimeException
- ✅ 为关键方法添加异常捕获和转换
- ✅ 异常信息统一为用户友好的中文提示

#### 代码质量优化
- ✅ 全面检查前后端代码，未发现严重问题
- ✅ 代码风格统一，符合规范

### ✅ 功能完善

#### 数据导出功能
- ✅ Excel导出：药品列表、库存列表、采购订单列表
- ✅ 导出文件包含导出人和导出时间水印
- ✅ 已在前后端联调验证通过

#### IP定位服务
- ✅ 集成高德地图API实现IP地理位置查询
- ✅ 登录日志自动记录地理位置
- ✅ 内网IP识别（返回"内网IP"）

### ✅ 出库、审批、仪表盘与权限（2026-03-08～03-09）

#### 仓库/出库仪表盘与 SQL 修复
- ✅ 修复 `InboundRecordMapper`、`OutboundApplyMapper` 中 `@Select` 比较运算符误写（`&gt;=`/`&lt;=` → `>=`/`<=`），解决仓库管理员仪表盘无数据问题

#### 出库申请与审批业务
- ✅ **科室选择**：新建出库申请支持科室下拉（`GET /outbound/departments`），无则仍可手动输入
- ✅ **申请人/审批人展示**：列表与详情中展示「姓名（角色名）」，避免与用户名混淆；后端填充 `applicantRoleName`、`approverRoleName`
- ✅ **申请人撤回**：待审批状态下本人可撤回（`POST /outbound/{id}/withdraw`）；前端操作列与详情弹窗提供「撤回」按钮
- ✅ **查看详情**：具备 outbound:view/apply 的用户可见「查看详情」按钮及只读详情弹窗；详情接口与明细接口对医护人员开放权限

#### 审批前库存校验（避免只走流程无实际意义）
- ✅ **后端**：审批通过前按申请明细校验当前可用库存（未过期且数量>0）；任一项不足则拒绝通过并返回友好提示（药品名、需要量、可用量）
- ✅ **库存校验接口**：`GET /outbound/{id}/stock-check` 返回 sufficient、message、details，供审批弹窗展示
- ✅ **前端**：审批弹窗打开时拉取库存校验；库存充足时绿色提示，不足时红色提示并列出缺货项、**禁用确定按钮**禁止通过

#### 身份与 Token
- ✅ **JWT 取 Token 优先级**：先读请求头 `Authorization`，再读 Cookie `cdiom_token`，避免多用户/切换用户后误用旧 Cookie 身份（申请人显示错误）

#### 前端权限与刷新
- ✅ **医护人员**：`ROLE_PERMISSIONS[4]` 补充 outbound:view、outbound:apply，解决刷新后「新建出库申请」等按钮消失
- ✅ **仓库管理员**：`ROLE_PERMISSIONS[2]` 补充出库/入库/库存相关权限，审批、执行等按钮与后端一致
- ✅ **仪表盘与库存页**：仓库管理员仪表盘、库存管理页在标签页从隐藏变为可见时自动重新请求数据，审批或执行出库后切回即可看到最新数据

#### 供应商仪表盘
- ✅ 动态数据拉取与展示完善：数值/数组归一化、失败默认空状态、刷新按钮、待处理订单/已确认金额等

> 📄 详细改动与业务流说明见：`docs/UPDATE_LOG_20260309_Outbound_Dashboard.md`

---

## v1.6.0 (2026-01-20)

> 🔗 **GitHub版本链接**：[查看v1.6.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/4638187) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 文档更新和优化

#### 文档结构优化
- 更新项目完成度统计（85% → 92-96%）
- 更新响应式优化完成度（70% → 100%）
- 更新文档版本和时间信息
- 完善文档目录结构说明
- 补充待解决问题清单

#### 文档模块化
- 将详细功能模块描述移至 `Function_Modules.md`
- 将详细API接口文档移至 `API_Documentation.md`
- 将详细数据库设计文档移至 `Database_Design.md`
- 将详细版本历史文档移至 `CHANGELOG.md`
- README.md保留简要概述和链接，提高可读性

---

## v1.5.2 (2026-01-20)

> 🔗 **GitHub版本链接**：[查看v1.5.2版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/1b9cfe6) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 文档修正和完善

#### 文档修正
- 修正Vite版本号（5.4.8 → 7.3.1）
- 修正数据库表编号（业务表从11开始，扩展表为21）

#### 文档完善
- 完善项目结构描述（补充所有Controller、Model、Config、前端页面）
- 更新数据库初始化说明（详细说明21张表的创建顺序）
- 补充API接口文档（入库管理、出库管理、采购订单管理、供应商管理、库存调整接口）
- 完善数据库相关注意事项

---

## v1.5.0 (2026-01-14)

> 🔗 **GitHub版本链接**：[查看v1.5.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/0d1aa2f) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 实现细粒度权限系统

#### 数据库结构
- 创建用户权限关联表（`sys_user_permission`），支持用户直接拥有权限
- 新增数据库脚本：`add_user_permission_system.sql`（创建用户权限关联表和细粒度权限）
- 新增权限配置脚本：`update_role_permissions_complete.sql`（完整角色权限配置）

#### 权限管理
- 支持用户直接拥有权限，实现更细粒度的权限控制
- 添加细粒度出库相关权限（outbound:view, outbound:apply, outbound:approve, outbound:approve:special, outbound:execute, outbound:reject）
- 添加细粒度入库相关权限（inbound:view, inbound:create, inbound:approve, inbound:execute）
- 添加细粒度库存管理权限（inventory:view, inventory:adjust, inventory:adjust:approve）
- 为各角色分配相应的细粒度权限

#### 后端实现
- 创建SysUserPermission实体类和Mapper
- 实现PermissionService权限查询功能增强
- 更新权限拦截器，支持用户直接权限检查

### ✅ 新增供应商仪表盘功能
- 前端：SupplierDashboard.jsx（供应商专用仪表盘）
- 显示供应商订单统计（总订单数、状态分布）
- 金额统计（总金额、待确认金额、已确认金额）
- 订单趋势图表（最近7天订单数量趋势）
- 响应式布局，支持不同屏幕尺寸

### ✅ 新增供应商订单管理功能
- 前端：SupplierOrderManagement.jsx（供应商订单管理页面）
- 供应商可查看和管理自己的订单
- 支持订单列表展示（分页、搜索、状态筛选）
- 订单详情查看
- 订单状态管理
- 响应式表格布局

### ✅ 仪表盘功能增强
- Dashboard.jsx功能扩展，支持更多角色专用仪表盘
- 采购专员专用仪表盘（订单统计、供应商统计、订单趋势）
- 医护人员专用仪表盘（申领统计、状态分布、申领趋势）
- 供应商专用仪表盘（订单统计、金额统计、订单趋势）
- DashboardController和DashboardService功能扩展

### ✅ 采购订单管理功能增强
- PurchaseOrderController功能扩展
- PurchaseOrderService业务逻辑优化
- 前端PurchaseOrderManagement组件功能完善（482行新增代码）
- 支持订单创建、编辑、状态流转、条形码生成
- 优化订单查询和筛选功能

### ✅ 库存管理功能增强
- InventoryService业务逻辑优化（474行代码重构）
- 前端InventoryManagement组件功能增强（291行新增代码）
- 支持库存查询、筛选、近效期预警
- 优化库存统计和预警功能

### ✅ 入库管理功能增强
- InboundRecordService业务逻辑优化（611行代码重构）
- 支持入库验收、效期校验、特殊药品双人操作
- 优化入库流程和验证逻辑

### ✅ 出库管理功能增强
- OutboundApplyService业务逻辑优化
- 前端OutboundManagement组件功能增强（255行新增代码）
- 支持出库申请、审批、执行
- 优化出库流程和权限控制

### ✅ 通知公告功能增强
- SysNoticeService业务逻辑优化（338行代码重构）
- 前端NoticeManagement组件功能增强（131行新增代码）

### ✅ 登录日志功能增强
- LoginLogService业务逻辑优化（138行代码重构）
- 前端LoginLog组件功能增强（364行代码重构）
- 支持IP定位服务优化（IpLocationServiceImpl 216行代码重构）

### ✅ 文件上传功能增强
- FileUploadController功能扩展（252行代码重构）
- 改进文件上传错误处理
- 优化文件验证和存储逻辑

### ✅ 条形码服务
- 新增BarcodeService和BarcodeServiceImpl（44行+80行代码）
- 支持订单条形码生成

### ✅ 配置优化
- application.yml配置更新（110行变更），提高清晰度和可用性
- application.yml.example配置更新（74行变更）
- WebMvcConfig配置增强（64行变更）
- 异步配置优化（AsyncConfig）

### ✅ 文档完善
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

### ✅ 代码优化
- 大量代码重构和优化（124个文件变更，10618行新增，4661行删除）
- 改进异常处理
- 优化数据库查询
- 提升代码质量和可维护性
- 统一代码风格和规范

---

## v1.4.0 (2026-01-14)

> 🔗 **GitHub版本链接**：[查看v1.4.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/265456a) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 实现供应商-药品关联管理功能

#### 后端实现
- 创建SupplierDrug实体类，支持供应商与药品的多对多关系
- 实现SupplierDrugService和SupplierDrugServiceImpl，提供关联管理业务逻辑
- 创建SupplierDrugController，提供RESTful API接口（添加、删除、更新单价）
- 更新DrugInfoController，新增根据供应商ID查询药品列表接口
- 更新DrugInfoService，支持按供应商ID查询药品

#### 前端实现
- 增强SupplierManagement组件，支持供应商-药品关联管理
- 优化PurchaseOrderManagement组件，根据选择的供应商动态加载关联药品
- 改进UserManagement组件，增强用户管理功能

#### 数据库
- 使用已有的supplier_drug表（在v1.3.0中创建）
- 支持一个药品可以有多个供应商，每个供应商可以设置不同的单价

### ✅ 新增文件上传功能

#### 后端实现
- 创建FileUploadController，提供文件上传和删除接口
- 支持图片格式：jpg, jpeg, png, gif, bmp, webp
- 文件大小限制：最大10MB
- 按日期分类存储（yyyy/MM/dd目录结构）
- 生成唯一文件名（UUID），防止文件名冲突
- 配置化上传路径和URL前缀（application.yml中的file.upload.path和file.upload.url-prefix）

#### 安全特性
- 文件类型验证（仅允许图片格式）
- 文件大小验证（最大10MB）
- 权限控制（需要drug:view或drug:manage权限）

### ✅ 用户管理功能增强
- SysUserController功能扩展，增强用户管理能力
- SysUser实体添加验证注解，提升数据验证
- SysUserService和SysUserServiceImpl功能增强
- 前端UserManagement组件功能增强

### ✅ 供应商管理功能增强
- Supplier实体添加验证注解
- SupplierController功能扩展
- SupplierService功能增强
- 前端SupplierManagement组件功能完善

### ✅ 全局异常处理增强
- GlobalExceptionHandler功能扩展，提升错误处理能力
- 添加文件上传异常处理

### ✅ 配置增强
- WebMvcConfig配置更新，支持文件上传
- application.yml添加文件上传配置项

### ✅ 文档完善
- 新增表单验证和安全检查报告（Form_Validation_Security_Report.md）
- 更新数据库脚本说明文档（db/README.md）
- 更新API接口文档，添加新接口说明

---

## v1.3.0 (2026-01-14)

> 🔗 **GitHub版本链接**：[查看v1.3.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/0524b16) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 完善供应商管理功能

#### 数据库结构优化
- 为供应商表（supplier）添加备注字段（remark），支持供应商信息备注
- 创建供应商-药品关联表（supplier_drug），实现供应商与药品的多对多关系
- 支持一个药品可以有多个供应商，每个供应商可以设置不同的单价（unit_price）
- 优化数据库结构，从drug_info表中移除supplier_id和supplier_name字段，改为通过中间表关联
- 自动迁移现有数据：将drug_info表中的supplier_id数据迁移到supplier_drug中间表

#### 权限管理优化
- 新增供应商审核权限（supplier:audit）
- 采购专员（角色ID=3）：可查看、创建、更新供应商，但不能删除和审核
- 仓库管理员（角色ID=2）：可审核供应商

#### 新增数据库脚本
- `add_supplier_remark_field.sql` - 为供应商表添加备注字段
- `create_supplier_drug_relation.sql` - 创建供应商-药品关联表并迁移现有数据
- `update_supplier_permissions.sql` - 更新供应商管理权限配置

#### 数据库表变更
- 新增表：`supplier_drug`（供应商-药品关联表，第20张表）
- 修改表：`supplier`（添加remark字段）
- 修改表：`drug_info`（移除supplier_id和supplier_name字段）

---

## v1.2.0 (2026-01-13)

> 🔗 **GitHub版本链接**：[查看v1.2.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/abee6ae) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 新增超级管理员管理功能
- 实现超级管理员账户的启用/停用功能
- 使用邮箱验证码进行安全验证（必须使用当前登录用户绑定的邮箱）
- 后端：SuperAdminController（启用/停用、状态查询、验证码发送）
- 前端：SuperAdminModal组件（状态显示、验证码发送、启用/停用操作）
- 数据库：为用户表添加email字段，创建init_super_admin.sql初始化脚本
- 服务：EmailVerificationService（邮箱验证码发送、验证、自动清理）
- 安全特性：验证码5分钟有效期、自动清理过期验证码、操作日志记录
- 用户体验：邮箱地址脱敏显示、倒计时提示、自动退出登录（停用当前用户时）

### ✅ 新增库存管理页面
- 前端：InventoryManagement.jsx（库存列表查询、多条件筛选）
- 支持按药品名称、批次号、存储位置、有效期范围、特殊药品筛选
- 近效期预警显示（红色预警≤90天、黄色预警90-180天）
- 显示药品信息、批次信息、库存数量、有效期等关键数据
- 响应式表格布局，支持横向滚动

---

## v1.1.0 (2026-01)

> 🔗 **GitHub版本链接**：[查看v1.1.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/7fd7e3c) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 新增药品信息管理模块
- 支持药品信息的增删改查
- 支持四种新增药品方式：手动输入、扫码识别、药品名称搜索、批准文号搜索
- 集成万维易源API（1468-4接口）
- 集成极速数据API（JisuAPI）
- 实现多API数据自动合并（万维易源API + 极速数据API）
- 优化表单布局，分组显示更紧凑美观
- 添加特殊药品友好提示

### ✅ 完善权限控制系统
- 实现基于角色的权限控制（RBAC）
- 添加权限拦截器（PermissionInterceptor）
- 添加权限注解（@RequiresPermission）
- 系统管理员自动拥有所有权限
- 前端权限检查（菜单权限、按钮权限）
- 操作日志仅系统管理员可见

### ✅ 新增仪表盘功能
- 系统管理员仪表盘（基础统计、登录趋势、操作日志统计）
- 仓库管理员仪表盘（近效期预警、待办任务、出入库统计）
- 响应式布局，支持不同屏幕尺寸

### ✅ 优化错误处理
- 改进API错误处理，避免频繁显示"系统内部错误"
- 添加友好的错误提示
- 优化前端请求拦截器

---

## v1.0.0 (2026-01)

> 🔗 **GitHub版本链接**：[查看v1.0.0版本](https://github.com/BlackSkyBlueSea/cdiom_new/commit/4f7afd3) | [查看所有提交](https://github.com/BlackSkyBlueSea/cdiom_new/commits/main)

### ✅ 完成项目基础架构搭建
- Spring Boot 3.2.8 + MyBatis-Plus后端架构
- React 18 + Vite + Ant Design前端架构
- JWT Token认证机制
- BCrypt密码加密

### ✅ 完成用户管理、角色管理、参数配置、通知公告、操作日志、登录日志模块
- 完整的CRUD功能
- 状态管理
- 逻辑删除
- 操作日志记录

### ✅ 完成数据库表结构设计（20张表全部创建）
- 系统表（6张）
- 权限表（3张）
- 业务表（10张）
- 扩展表（1张）

### ✅ 完成前端基础页面开发
- 登录页面
- 主布局（侧边栏导航、顶部用户信息）
- 基础管理页面

### ✅ 完善README文档
- 项目概述
- 快速开始指南
- 技术栈说明

### ✅ 完成数据库初始化脚本
- init_simple.sql（简化版初始化脚本）
- init.sql（完整版初始化脚本）
- init_permissions.sql（权限数据初始化）

---

**文档版本**: v1.0.0  
**最后更新**: 2026年1月20日

