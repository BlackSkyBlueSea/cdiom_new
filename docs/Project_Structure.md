# CDIOM - 项目结构详细说明

本文档详细描述了CDIOM系统的完整项目结构，包含后端、前端、数据库脚本、文档等所有目录和文件的详细说明。

## 目录

- [项目根目录结构](#项目根目录结构)
- [后端项目结构](#后端项目结构)
  - [Java源码结构](#java源码结构)
  - [资源文件结构](#资源文件结构)
- [前端项目结构](#前端项目结构)
- [数据库脚本结构](#数据库脚本结构)
- [文档目录结构](#文档目录结构)
- [目录说明](#目录说明)

---

## 项目根目录结构

```
cdiom_new/
├── cdiom_backend/              # 后端项目（Spring Boot）
├── cdiom_frontend/             # 前端项目（React + Vite）
├── docs/                       # 项目文档目录
├── py/                         # Python工具脚本目录
├── README.md                   # 项目主文档
├── 项目进度.md                 # 项目进度文档
└── 插图位置检查报告.md         # 插图位置检查报告
```

---

## 后端项目结构

### Java源码结构

```
cdiom_backend/
└── src/main/java/com/cdiom/backend/
    ├── CdiomApplication.java                    # Spring Boot 启动类
    │
    ├── controller/                              # 控制器层（RESTful API接口）
    │   ├── AuthController.java                  # 认证控制器：用户登录、登出、获取当前用户信息
    │   ├── SysUserController.java               # 用户管理控制器：用户CRUD、状态管理、权限管理
    │   ├── SysRoleController.java               # 角色管理控制器：角色CRUD、状态管理
    │   ├── SysConfigController.java             # 参数配置控制器：系统参数配置管理
    │   ├── SysNoticeController.java             # 通知公告控制器：通知公告CRUD、状态管理
    │   ├── OperationLogController.java          # 操作日志控制器：操作日志查询、详情查看
    │   ├── LoginLogController.java              # 登录日志控制器：登录日志查询、详情查看
    │   ├── DrugInfoController.java              # 药品信息控制器：药品CRUD、扫码识别、第三方API集成
    │   ├── InventoryController.java             # 库存管理控制器：库存查询、预警、统计、导出
    │   ├── InventoryAdjustmentController.java   # 库存调整控制器：库存盘盈/盘亏调整记录管理
    │   ├── InboundRecordController.java         # 入库管理控制器：入库记录、验收、效期校验
    │   ├── OutboundApplyController.java        # 出库管理控制器：出库申请、审批、执行
    │   ├── PurchaseOrderController.java         # 采购订单控制器：订单CRUD、状态流转、条形码生成
    │   ├── SupplierController.java              # 供应商管理控制器：供应商CRUD、审核功能
    │   ├── SupplierDrugController.java         # 供应商-药品关联控制器：关联关系管理、单价设置
    │   ├── SupplierApprovalController.java      # 供应商审核控制器：审核申请、质量检查、价格审核
    │   ├── SupplierDrugAgreementController.java # 供应商药品协议控制器：价格协议管理
    │   ├── SupplierDrugPriceHistoryController.java # 供应商药品价格历史控制器：价格历史记录查询
    │   ├── DashboardController.java             # 仪表盘控制器：各角色专用仪表盘数据统计
    │   ├── FileUploadController.java            # 文件上传控制器：图片上传、删除、验证
    │   ├── SuperAdminController.java            # 超级管理员控制器：超级管理员启用/停用、邮箱验证
    │   ├── BackendMonitorController.java        # 后端监控控制器：系统信息、日志监控、健康检查
    │   └── LogWebSocketHandler.java            # WebSocket日志处理器：实时日志推送
    │
    ├── service/                                 # 服务层（业务逻辑处理）
    │   ├── impl/                                # 服务实现类目录
    │   │   ├── AuthServiceImpl.java             # 认证服务实现：登录、登出、Token生成
    │   │   ├── SysUserServiceImpl.java          # 用户管理服务实现：用户CRUD、权限管理
    │   │   ├── SysRoleServiceImpl.java          # 角色管理服务实现：角色CRUD、权限分配
    │   │   ├── SysConfigServiceImpl.java        # 参数配置服务实现：系统参数管理
    │   │   ├── SysNoticeServiceImpl.java        # 通知公告服务实现：通知公告管理
    │   │   ├── OperationLogServiceImpl.java     # 操作日志服务实现：日志查询、记录
    │   │   ├── LoginLogServiceImpl.java         # 登录日志服务实现：登录日志查询、记录
    │   │   ├── DrugInfoServiceImpl.java         # 药品信息服务实现：药品CRUD、API集成
    │   │   ├── InventoryServiceImpl.java        # 库存管理服务实现：库存查询、预警、统计
    │   │   ├── InventoryAdjustmentServiceImpl.java # 库存调整服务实现：库存调整记录管理
    │   │   ├── InboundRecordServiceImpl.java    # 入库管理服务实现：入库记录、验收流程
    │   │   ├── OutboundApplyServiceImpl.java    # 出库管理服务实现：出库申请、审批流程
    │   │   ├── PurchaseOrderServiceImpl.java    # 采购订单服务实现：订单管理、状态流转
    │   │   ├── SupplierServiceImpl.java         # 供应商管理服务实现：供应商CRUD、审核
    │   │   ├── SupplierDrugServiceImpl.java     # 供应商-药品关联服务实现：关联关系管理
    │   │   ├── SupplierApprovalServiceImpl.java # 供应商审核服务实现：审核流程管理
    │   │   ├── SupplierDrugAgreementServiceImpl.java # 供应商药品协议服务实现：价格协议管理
    │   │   ├── SupplierDrugPriceHistoryServiceImpl.java # 供应商药品价格历史服务实现：价格历史记录
    │   │   ├── DashboardServiceImpl.java        # 仪表盘服务实现：数据统计、图表数据
    │   │   ├── PermissionServiceImpl.java        # 权限服务实现：权限查询、权限验证
    │   │   ├── BarcodeServiceImpl.java          # 条形码服务实现：订单条形码生成
    │   │   ├── EmailVerificationServiceImpl.java # 邮箱验证服务实现：验证码发送、验证
    │   │   ├── ExcelExportService.java          # Excel导出服务：数据导出功能
    │   │   ├── IpLocationServiceImpl.java       # IP定位服务实现：登录地点识别
    │   │   ├── YuanyanyaoServiceImpl.java       # 万维易源API服务实现：药品信息查询
    │   │   ├── JisuApiServiceImpl.java          # 极速数据API服务实现：药品信息查询
    │   │   └── PriceWarningServiceImpl.java     # 价格预警服务实现：价格预警检查
    │   │
    │   ├── AuthService.java                     # 认证服务接口
    │   ├── SysUserService.java                  # 用户管理服务接口
    │   ├── SysRoleService.java                  # 角色管理服务接口
    │   ├── SysConfigService.java                # 参数配置服务接口
    │   ├── SysNoticeService.java                # 通知公告服务接口
    │   ├── OperationLogService.java             # 操作日志服务接口
    │   ├── LoginLogService.java                  # 登录日志服务接口
    │   ├── DrugInfoService.java                 # 药品信息服务接口
    │   ├── InventoryService.java                # 库存管理服务接口
    │   ├── InventoryAdjustmentService.java      # 库存调整服务接口
    │   ├── InboundRecordService.java             # 入库管理服务接口
    │   ├── OutboundApplyService.java             # 出库管理服务接口
    │   ├── PurchaseOrderService.java             # 采购订单服务接口
    │   ├── SupplierService.java                 # 供应商管理服务接口
    │   ├── SupplierDrugService.java             # 供应商-药品关联服务接口
    │   ├── SupplierApprovalService.java         # 供应商审核服务接口
    │   ├── SupplierDrugAgreementService.java    # 供应商药品协议服务接口
    │   ├── SupplierDrugPriceHistoryService.java # 供应商药品价格历史服务接口
    │   ├── DashboardService.java                # 仪表盘服务接口
    │   ├── PermissionService.java                # 权限服务接口
    │   ├── BarcodeService.java                  # 条形码服务接口
    │   ├── EmailVerificationService.java        # 邮箱验证服务接口
    │   ├── IpLocationService.java               # IP定位服务接口
    │   ├── YuanyanyaoService.java               # 万维易源API服务接口
    │   ├── JisuApiService.java                  # 极速数据API服务接口
    │   ├── PriceWarningService.java             # 价格预警服务接口
    │   └── BackendMonitorService.java            # 后端监控服务接口
    │
    ├── mapper/                                  # 数据访问层（MyBatis Mapper接口）
    │   ├── SysUserMapper.java                   # 用户Mapper：用户数据操作
    │   ├── SysRoleMapper.java                   # 角色Mapper：角色数据操作
    │   ├── SysConfigMapper.java                 # 参数配置Mapper：配置数据操作
    │   ├── SysNoticeMapper.java                 # 通知公告Mapper：通知数据操作
    │   ├── SysPermissionMapper.java             # 权限Mapper：权限数据操作
    │   ├── SysRolePermissionMapper.java         # 角色权限关联Mapper：角色权限数据操作
    │   ├── SysUserPermissionMapper.java         # 用户权限关联Mapper：用户权限数据操作
    │   ├── OperationLogMapper.java              # 操作日志Mapper：操作日志数据操作
    │   ├── LoginLogMapper.java                  # 登录日志Mapper：登录日志数据操作
    │   ├── DrugInfoMapper.java                  # 药品信息Mapper：药品数据操作
    │   ├── InventoryMapper.java                 # 库存Mapper：库存数据操作
    │   ├── InventoryAdjustmentMapper.java       # 库存调整Mapper：库存调整数据操作
    │   ├── InboundRecordMapper.java             # 入库记录Mapper：入库记录数据操作
    │   ├── OutboundApplyMapper.java             # 出库申请Mapper：出库申请数据操作
    │   ├── OutboundApplyItemMapper.java         # 出库申请明细Mapper：出库明细数据操作
    │   ├── PurchaseOrderMapper.java             # 采购订单Mapper：采购订单数据操作
    │   ├── PurchaseOrderItemMapper.java         # 采购订单明细Mapper：订单明细数据操作
    │   ├── SupplierMapper.java                  # 供应商Mapper：供应商数据操作
    │   ├── SupplierDrugMapper.java              # 供应商-药品关联Mapper：关联数据操作
    │   ├── SupplierApprovalApplicationMapper.java # 供应商审核申请Mapper：审核申请数据操作
    │   ├── SupplierApprovalItemMapper.java      # 供应商审核项Mapper：审核项数据操作
    │   ├── SupplierApprovalLogMapper.java       # 供应商审核日志Mapper：审核日志数据操作
    │   ├── SupplierBlacklistMapper.java         # 供应商黑名单Mapper：黑名单数据操作
    │   ├── SupplierDrugAgreementMapper.java     # 供应商药品协议Mapper：协议数据操作
    │   ├── SupplierDrugPriceHistoryMapper.java  # 供应商药品价格历史Mapper：价格历史数据操作
    │   └── PriceWarningConfigMapper.java        # 价格预警配置Mapper：预警配置数据操作
    │
    ├── model/                                   # 实体类（数据库表映射）
    │   ├── SysUser.java                         # 系统用户实体：对应sys_user表
    │   ├── SysRole.java                         # 系统角色实体：对应sys_role表
    │   ├── SysConfig.java                       # 系统配置实体：对应sys_config表
    │   ├── SysNotice.java                       # 系统通知实体：对应sys_notice表
    │   ├── SysPermission.java                   # 权限实体：对应sys_permission表
    │   ├── SysRolePermission.java               # 角色权限关联实体：对应sys_role_permission表
    │   ├── SysUserPermission.java               # 用户权限关联实体：对应sys_user_permission表
    │   ├── OperationLog.java                    # 操作日志实体：对应operation_log表
    │   ├── LoginLog.java                        # 登录日志实体：对应login_log表
    │   ├── DrugInfo.java                        # 药品信息实体：对应drug_info表
    │   ├── Inventory.java                       # 库存实体：对应inventory表
    │   ├── InventoryAdjustment.java             # 库存调整实体：对应inventory_adjustment表
    │   ├── InboundRecord.java                   # 入库记录实体：对应inbound_record表
    │   ├── OutboundApply.java                   # 出库申请实体：对应outbound_apply表
    │   ├── OutboundApplyItem.java               # 出库申请明细实体：对应outbound_apply_item表
    │   ├── PurchaseOrder.java                   # 采购订单实体：对应purchase_order表
    │   ├── PurchaseOrderItem.java               # 采购订单明细实体：对应purchase_order_item表
    │   ├── Supplier.java                        # 供应商实体：对应supplier表
    │   ├── SupplierDrug.java                    # 供应商-药品关联实体：对应supplier_drug表
    │   ├── SupplierApprovalApplication.java     # 供应商审核申请实体：对应supplier_approval_application表
    │   ├── SupplierApprovalItem.java            # 供应商审核项实体：对应supplier_approval_item表
    │   ├── SupplierApprovalLog.java             # 供应商审核日志实体：对应supplier_approval_log表
    │   ├── SupplierBlacklist.java               # 供应商黑名单实体：对应supplier_blacklist表
    │   ├── SupplierDrugAgreement.java           # 供应商药品协议实体：对应supplier_drug_agreement表
    │   ├── SupplierDrugPriceHistory.java        # 供应商药品价格历史实体：对应supplier_drug_price_history表
    │   ├── PriceWarningConfig.java              # 价格预警配置实体：价格预警规则配置
    │   └── PriceWarningResult.java               # 价格预警结果实体：价格预警检查结果
    │
    ├── config/                                  # 配置类（Spring配置）
    │   ├── SecurityConfig.java                  # Spring Security配置：安全策略、认证授权配置
    │   ├── MyBatisPlusConfig.java               # MyBatis-Plus配置：分页插件、逻辑删除配置
    │   ├── MyMetaObjectHandler.java             # 自动填充处理器：创建时间、更新时间自动填充
    │   ├── WebMvcConfig.java                    # Web MVC配置：跨域、文件上传、拦截器配置
    │   ├── WebConfig.java                       # Web配置：静态资源、视图解析器配置
    │   ├── AsyncConfig.java                     # 异步配置：异步任务线程池配置
    │   ├── ConcurrencyConfig.java               # 并发配置：并发控制、锁机制配置
    │   ├── RestTemplateConfig.java              # RestTemplate配置：HTTP客户端配置
    │   ├── WebSocketConfig.java                 # WebSocket配置：WebSocket连接配置
    │   ├── LogAppenderInitializer.java          # 日志追加器初始化：日志系统初始化
    │   ├── WebSocketLogAppender.java            # WebSocket日志追加器：实时日志推送
    │   ├── filter/                              # 过滤器目录
    │   │   └── JwtAuthenticationFilter.java     # JWT认证过滤器：Token验证、用户信息注入
    │   └── interceptor/                         # 拦截器目录
    │       └── PermissionInterceptor.java       # 权限拦截器：接口级权限验证
    │
    ├── common/                                  # 公共类目录
    │   ├── Result.java                          # 统一响应结果类：API响应格式封装
    │   └── exception/                           # 异常处理目录
    │       ├── GlobalExceptionHandler.java      # 全局异常处理器：统一异常处理、错误响应
    │       └── ServiceException.java           # 业务异常类：自定义业务异常
    │
    ├── util/                                    # 工具类目录
    │   ├── JwtUtil.java                         # JWT工具类：Token生成、解析、验证
    │   ├── LoginConfigUtil.java                 # 登录配置工具类：登录配置管理
    │   ├── RetryUtil.java                       # 重试工具类：操作重试机制
    │   └── SystemConfigUtil.java                # 系统配置工具类：系统配置管理
    │
    ├── annotation/                              # 自定义注解目录
    │   └── RequiresPermission.java              # 权限注解：标记需要权限验证的接口
    │
    └── constant/                                # 常量类目录
        └── LoginConfigConstant.java              # 登录配置常量：登录相关常量定义
```

### 资源文件结构

```
cdiom_backend/
└── src/main/resources/
    ├── application.yml                          # 应用配置文件：数据库、JWT、第三方API等配置
    ├── application.yml.example                  # 配置文件示例：敏感配置模板
    ├── application-local.yml                    # 本地配置文件：本地开发环境配置（不提交到Git）
    ├── logback-spring.xml                       # 日志配置文件：日志级别、输出格式、文件路径
    │
    └── db/                                      # 数据库脚本目录
        ├── README.md                            # 数据库脚本说明文档：脚本使用指南
        │
        ├── init_simple.sql                      # 简化初始化脚本（推荐）⭐：基础表结构创建（19张表）
        ├── init.sql                             # 完整初始化脚本（带注释）：完整数据库初始化
        ├── cdiom_db_complete.sql                # 完整数据库脚本：数据库完整结构
        │
        ├── init_business_tables.sql              # 业务表脚本：业务表创建脚本
        ├── init_permissions.sql                 # 权限数据初始化脚本：权限数据初始化
        ├── init_super_admin.sql                 # 超级管理员初始化脚本：超级管理员账户创建
        │
        ├── add_user_email_field.sql             # 添加用户邮箱字段：用户表添加email字段
        ├── add_user_permission_system.sql       # 添加用户权限系统：用户权限关联表创建（v1.5.0）
        ├── add_supplier_remark_field.sql        # 为供应商表添加备注字段：supplier表添加remark字段
        ├── create_supplier_drug_relation.sql    # 创建供应商-药品关联表：supplier_drug表创建（v1.3.0）
        ├── create_supplier_approval_tables.sql  # 创建供应商审核相关表：供应商审核表创建
        ├── create_price_agreement_tables.sql    # 创建价格协议相关表：价格协议表创建
        │
        ├── add_supplier_approval_permissions.sql # 添加供应商审核权限：供应商审核权限初始化
        ├── update_supplier_permissions.sql      # 更新供应商管理权限：供应商权限更新
        ├── update_role_permissions_complete.sql # 完整角色权限配置：角色权限完整配置
        │
        ├── add_login_lock_mechanism.sql         # 添加登录锁定机制：登录失败锁定功能
        ├── check_login_lock_fields.sql          # 检查登录锁定字段：登录锁定字段检查脚本
        │
        ├── fix_chinese_data.sql                 # 修复中文数据脚本：中文数据编码修复
        ├── fix_chinese_utf8mb4.sql              # 修复中文数据脚本（简化版）：UTF8MB4编码修复
        ├── fix_database_charset.sql             # 修复数据库字符集脚本：数据库字符集修复
        ├── fix_operation_log_permission.sql     # 修复操作日志权限脚本：操作日志权限修复
        │
        ├── drug_info_insert.sql                 # 药品信息数据导入脚本：药品数据批量导入
        └── update_admin_password.sql            # 更新管理员密码脚本：管理员密码更新
```

---

## 前端项目结构

```
cdiom_frontend/
├── src/
│   ├── pages/                                  # 页面组件目录
│   │   ├── Login.jsx                           # 登录页面：用户登录界面
│   │   ├── Home.jsx                            # 首页：系统首页、导航入口
│   │   ├── Dashboard.jsx                       # 仪表盘页面：各角色专用数据统计展示
│   │   ├── BackendMonitor.jsx                  # 后端监控页面：系统监控、日志查看
│   │   │
│   │   ├── UserManagement.jsx                  # 用户管理页面：用户CRUD、权限管理
│   │   ├── RoleManagement.jsx                  # 角色管理页面：角色CRUD、状态管理
│   │   ├── ConfigManagement.jsx                # 参数配置页面：系统参数配置管理
│   │   ├── NoticeManagement.jsx                # 通知公告页面：通知公告CRUD、状态管理
│   │   ├── OperationLog.jsx                    # 操作日志页面：操作日志查询、筛选
│   │   ├── LoginLog.jsx                        # 登录日志页面：登录日志查询、IP定位显示
│   │   │
│   │   ├── DrugManagement.jsx                  # 药品管理页面：药品CRUD、扫码识别、API集成
│   │   ├── InventoryManagement.jsx             # 库存管理页面：库存查询、预警、调整
│   │   ├── InboundManagement.jsx                # 入库管理页面：入库记录、验收、效期校验
│   │   ├── OutboundManagement.jsx               # 出库管理页面：出库申请、审批、执行
│   │   ├── PurchaseOrderManagement.jsx          # 采购订单管理页面：订单CRUD、状态流转、条形码
│   │   ├── SupplierManagement.jsx              # 供应商管理页面：供应商CRUD、审核、关联管理
│   │   ├── SupplierDashboard.jsx               # 供应商仪表盘页面：供应商专用数据统计
│   │   └── SupplierOrderManagement.jsx         # 供应商订单管理页面：供应商订单查看、管理
│   │
│   ├── components/                             # 公共组件目录
│   │   ├── Layout.jsx                          # 主布局组件：侧边栏、顶部导航、路由容器
│   │   ├── Layout.css                          # 布局样式：主布局样式文件
│   │   ├── PrivateRoute.jsx                    # 路由守卫组件：权限验证、登录状态检查
│   │   ├── SuperAdminModal.jsx                 # 超级管理员模态框：超级管理员管理组件
│   │   ├── IndexRedirect.jsx                   # 首页重定向组件：路由重定向处理
│   │   └── common/                             # 通用组件目录
│   │       └── LoginModal.jsx                  # 登录模态框组件：登录弹窗组件
│   │
│   ├── utils/                                  # 工具函数目录
│   │   ├── request.js                          # Axios封装：HTTP请求封装、拦截器配置
│   │   ├── auth.js                             # 认证工具：Token管理、权限检查工具函数
│   │   ├── permission.js                       # 权限工具：权限检查工具函数
│   │   └── logger.js                           # 日志工具：日志记录工具函数
│   │
│   ├── styles/                                 # 样式文件目录
│   │   └── pad-responsive.css                  # Pad端响应式样式：Pad设备适配样式
│   │
│   ├── App.jsx                                 # 根组件：路由配置、全局状态管理
│   ├── App.css                                 # 根组件样式：全局样式文件
│   ├── main.jsx                                # 入口文件：React应用入口、根组件挂载
│   └── index.css                               # 全局样式：全局CSS样式文件
│
├── index.html                                  # HTML模板：应用HTML模板文件
├── vite.config.js                              # Vite配置文件：构建工具配置、代理配置
└── package.json                                # 依赖配置文件：项目依赖、脚本命令配置
```

---

## 数据库脚本结构

### 初始化脚本

| 脚本文件 | 说明 | 优先级 |
|---------|------|--------|
| `init_simple.sql` | 简化初始化脚本（推荐）⭐ | 必须执行 |
| `init.sql` | 完整初始化脚本（带注释） | 可选 |
| `cdiom_db_complete.sql` | 完整数据库脚本 | 可选 |

### 扩展脚本

| 脚本文件 | 说明 | 版本 |
|---------|------|------|
| `create_supplier_drug_relation.sql` | 创建供应商-药品关联表 | v1.3.0 |
| `add_user_permission_system.sql` | 添加用户权限系统 | v1.5.0 |
| `create_supplier_approval_tables.sql` | 创建供应商审核相关表 | - |
| `create_price_agreement_tables.sql` | 创建价格协议相关表 | - |

### 数据初始化脚本

| 脚本文件 | 说明 |
|---------|------|
| `init_permissions.sql` | 权限数据初始化 |
| `init_super_admin.sql` | 超级管理员初始化 |
| `drug_info_insert.sql` | 药品信息数据导入 |

### 修复脚本

| 脚本文件 | 说明 |
|---------|------|
| `fix_chinese_data.sql` | 修复中文数据编码 |
| `fix_database_charset.sql` | 修复数据库字符集 |
| `add_login_lock_mechanism.sql` | 添加登录锁定机制 |

> 📖 **详细说明**：更多数据库脚本说明请参考 [cdiom_backend/src/main/resources/db/README.md](../cdiom_backend/src/main/resources/db/README.md)

---

## 文档目录结构

```
docs/
├── README.md                                   # 文档目录说明：文档索引和使用说明
│
├── 📋 需求分析阶段/
│   └── Core_Business_Requirements_Analysis.md  # 核心业务需求分析：业务流程和处理流程详细说明
│
├── 🎨 设计阶段/
│   ├── Database_Design.md                      # 数据库设计文档：数据库表分类、表结构说明、表关系等
│   ├── Function_Modules.md                     # 功能模块详细说明：所有后端和前端模块的完整功能特性
│   └── API_Documentation.md                    # API接口文档：所有API接口的详细说明，包括请求参数、响应格式等
│
├── 🧪 测试阶段/
│   └── System_Test_Report.md                   # 系统测试报告：功能测试、性能测试、安全测试等完整的测试用例和结果
│
├── 📋 维护和版本管理/
│   └── CHANGELOG.md                            # 版本历史文档：所有版本的完整更新内容，包括功能更新、bug修复等
│
├── 🚀 部署和运维/
│   └── Deployment_Guide.md                     # 部署指南：开发环境部署、生产环境部署、Nginx配置等
│
├── 💻 开发指南/
│   ├── Development_Guide.md                    # 开发指南：如何添加新业务模块、前端页面开发、开发规范等
│   └── Project_Structure.md                    # 项目结构说明：本文档
│
└── 🔧 开发指南和问题修复/
    ├── Code_Completeness_Report.md             # 代码完整性检查报告
    ├── Code_Logic_Vulnerability_Report.md      # 代码逻辑漏洞检查报告 ⚠️ 重要
    ├── Permission_Issue_Fix_Guide.md           # 权限问题修复说明
    ├── Concurrency_Configuration_Guide.md     # 并发访问配置说明
    ├── Drug_Data_Import_Guide.md               # 药品数据导入说明
    ├── Fine_Grained_Permission_System_Guide.md # 细粒度权限系统指南
    ├── Form_Validation_Security_Report.md      # 表单验证和安全检查报告
    └── Drug_Data_Retrieval_Troubleshooting_Guide.md # 药品数据获取问题排查指南
```

> 📖 **详细说明**：更多文档说明请参考 [docs/README.md](./README.md)

---

## 目录说明

### 后端目录说明

#### controller/ - 控制器层
- **职责**：接收HTTP请求、参数校验、调用Service、返回响应
- **规范**：使用`@RestController`、`@RequestMapping`、`@RequiresPermission`
- **命名**：以`Controller`结尾，如`SysUserController`

#### service/ - 服务层
- **职责**：业务逻辑处理、事务管理、数据校验
- **规范**：接口定义在`service/`，实现在`service/impl/`
- **命名**：接口以`Service`结尾，实现以`ServiceImpl`结尾

#### mapper/ - 数据访问层
- **职责**：数据库操作、SQL映射
- **规范**：继承`BaseMapper<T>`，使用MyBatis-Plus
- **命名**：以`Mapper`结尾，如`SysUserMapper`

#### model/ - 实体类
- **职责**：数据库表映射、数据模型定义
- **规范**：使用`@TableName`、`@TableId`、`@TableLogic`等注解
- **命名**：与数据库表名对应，如`SysUser`对应`sys_user`表

#### config/ - 配置类
- **职责**：Spring配置、组件配置、拦截器配置
- **规范**：使用`@Configuration`、`@Bean`等注解
- **命名**：以`Config`结尾，如`SecurityConfig`

#### common/ - 公共类
- **职责**：统一响应格式、异常处理、公共工具
- **包含**：`Result.java`、`GlobalExceptionHandler.java`、`ServiceException.java`

#### util/ - 工具类
- **职责**：通用工具方法、辅助功能
- **包含**：`JwtUtil.java`、`LoginConfigUtil.java`、`RetryUtil.java`等

### 前端目录说明

#### pages/ - 页面组件
- **职责**：业务页面组件、页面逻辑处理
- **规范**：使用函数式组件、React Hooks
- **命名**：使用PascalCase，如`UserManagement.jsx`

#### components/ - 公共组件
- **职责**：可复用组件、布局组件、路由组件
- **规范**：组件职责单一、可复用
- **命名**：使用PascalCase，如`Layout.jsx`

#### utils/ - 工具函数
- **职责**：工具函数、辅助方法
- **包含**：`request.js`、`auth.js`、`permission.js`、`logger.js`

#### styles/ - 样式文件
- **职责**：全局样式、响应式样式
- **包含**：`pad-responsive.css`（Pad端响应式样式）

---

## 文件统计

### 后端文件统计

| 类型 | 数量 | 说明 |
|------|------|------|
| Controller | 23 | RESTful API控制器 |
| Service接口 | 26 | 业务服务接口 |
| Service实现 | 26 | 业务服务实现 |
| Mapper | 28 | 数据访问层接口 |
| Model | 28 | 实体类 |
| Config | 11 | 配置类 |
| Util | 4 | 工具类 |
| 数据库脚本 | 24 | SQL脚本文件 |

### 前端文件统计

| 类型 | 数量 | 说明 |
|------|------|------|
| 页面组件 | 18 | 业务页面 |
| 公共组件 | 5 | 可复用组件 |
| 工具函数 | 4 | 工具函数文件 |
| 样式文件 | 3 | CSS/Less样式文件 |

---

## 架构设计说明

### 分层架构

```
┌─────────────────────────────────────┐
│        Controller Layer             │  控制器层：接收请求、参数校验
├─────────────────────────────────────┤
│        Service Layer                │  服务层：业务逻辑处理
├─────────────────────────────────────┤
│        Mapper Layer                 │  数据访问层：数据库操作
├─────────────────────────────────────┤
│        Model Layer                  │  实体层：数据模型定义
└─────────────────────────────────────┘
```

### 数据流向

```
前端请求 → Controller → Service → Mapper → 数据库
                ↓           ↓        ↓
             响应返回 ← 业务处理 ← 数据查询
```

### 权限控制流程

```
请求 → JwtAuthenticationFilter → PermissionInterceptor → Controller
         (Token验证)              (权限验证)            (业务处理)
```

---

## 扩展说明

### 添加新业务模块

1. **创建实体类**：在`model/`目录下创建实体类
2. **创建Mapper**：在`mapper/`目录下创建Mapper接口
3. **创建Service**：在`service/`目录下创建Service接口和实现
4. **创建Controller**：在`controller/`目录下创建Controller
5. **创建数据库表**：在`db/`目录下创建SQL脚本

> 📖 **详细说明**：更多开发指南请参考 [Development_Guide.md](./Development_Guide.md)

---

**文档版本**: v1.0.0  
**最后更新**: 2026年1月20日

