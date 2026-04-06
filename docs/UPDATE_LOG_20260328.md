# 开发记录：系统参数贯通、日志保留、布局与管理员联系信息（2026-03-28）

> **追溯说明**：下列 request id 为 2026-03-28 前后多轮对话标识，便于与 Cursor 侧记录对照：  
> `b243a701-41db-4bdf-b51a-6071a5987244`、`e450cf9c-094b-4238-999b-0ccdf2d80d63`、`cb98d4dd-2b61-44c3-bc0c-2d7520ee1c2a`、`c7f21f07-04fc-47ee-b43b-13de389e6cb8`、`2502b7f6-3749-4ac6-9c80-bebda7250e9b`、`bc6f7e9d-9962-44b6-8718-901f743dc271`、`577342f1-22bf-45c3-8a80-3163a3355ca7`、`b8db9dc3-93f4-4295-adbf-69eec56fd630`、`169941b9-b372-4d62-8870-cbd67076583e`。  
> **本地说明**：当前仓库的 `agent-transcripts` 目录按**会话 UUID** 存储 `.jsonl`，与上述 **request id** 不一一对应，无法在本地直接打开上述 id 的原文。本文档内容根据**截至 2026-03-29 工作区内的实际代码与文件**归纳，用于记录进度与结果。

---

## 1. 目标与结果概览

| 方向 | 目标 | 结果（代码侧） |
|------|------|----------------|
| 系统参数 | 近效期、JWT 时长、日志保留等与 `sys_config` 一致并可运行时查询 | `SystemConfigUtil` 统一读取并带缓存与合法范围校验；`SysConfigServiceImpl` 在增删改后清理对应缓存；`runtime-effective` 接口扩展 |
| 业务生效 | 仪表盘、库存预警、入库效期校验使用库内配置 | `DashboardServiceImpl`、`InventoryServiceImpl`、`InboundRecordServiceImpl` 使用 `SystemConfigUtil` |
| JWT | Token 有效期随参数表可调 | `JwtUtil` 生成 Token 时使用 `getJwtExpirationMillis()` |
| 日志治理 | 按保留年限清理历史登录/操作日志 | `LogRetentionCleanupScheduler` 每日凌晨 3 点执行物理删除；`CdiomApplication` 已 `@EnableScheduling` |
| 用户体验 | 普通用户知晓如何联系系统管理员 | `AdminContactInfo` + `AuthService.getAdminContactForUsers()` + `GET /api/v1/auth/admin-contact` |
| 安全与运维 | 防止管理员误伤自身账号 | `SysUserServiceImpl` 对删除、状态变更校验「非当前登录用户」；解锁用户用 `UpdateWrapper` 清空锁定字段 |
| 前端 | 列表页表格区域与主内容区协同滚动 | 新增 `tablePageLayout.js`，多业务列表页与 `Layout.css` / `Layout.jsx` 配合 |

**Git 状态说明**：上述改动在编写本文档时多为**工作区未提交变更**；远程 `main` 最近提交仍止于 2026-03-17 附近，与本次汇总无冲突，合并前请自行 `git commit`。

---

## 2. 后端改动清单

| 模块/文件 | 摘要 |
|-----------|------|
| `constant/SysConfigKeys.java` | 定义 `expiry_warning_days`、`expiry_critical_days`、`log_retention_years`、`jwt_expiration` 与表字段一致 |
| `util/SystemConfigUtil.java` | 从库读取上述键；整型/长整型缓存 + 读写锁；JWT 毫秒数与日志保留年数带上下限；提供 `clearCache` |
| `util/JwtUtil.java` | `@Lazy` 注入 `SystemConfigUtil`；签发时使用运行时 JWT 有效期 |
| `service/impl/SysConfigServiceImpl.java` | 创建/更新/删除配置后调用 `systemConfigUtil.clearCache`，保证立即生效 |
| `controller/SysConfigController.java` | `GET /runtime-effective` 增加 `logRetentionYears`、`jwtExpirationMs`（及既有登录相关参数） |
| `schedule/LogRetentionCleanupScheduler.java` | 按 `log_retention_years` 删除早于阈值的登录日志与操作日志 |
| `model/AdminContactInfo.java` | 管理员对外展示：用户名、手机、邮箱 |
| `service/AuthService.java` / `AuthServiceImpl.java` | `getAdminContactForUsers()`：取首个启用且角色为系统管理员的用户 |
| `controller/AuthController.java` | `GET /api/v1/auth/admin-contact`，需登录 |
| `service/impl/DashboardServiceImpl.java` | 近效期黄/红边界与文案使用 `SystemConfigUtil` |
| `service/impl/InventoryServiceImpl.java` | 库存预警展示使用运行时阈值 |
| `service/impl/InboundRecordServiceImpl.java` | 入库效期校验使用运行时阈值 |
| `mapper/InventoryMapper.java`（及 XML） | 近效期统计 SQL 与红黄边界参数化（与仪表盘逻辑一致） |
| `service/impl/SysUserServiceImpl.java` | `assertTargetIsNotCurrentUser`；删除/禁用等场景禁止操作当前登录账号；解锁显式清空 `lock_time` 等 |
| `resources/application.yml` | `system.config.log-retention-years` 等默认值与说明 |

---

## 3. 前端改动清单

| 模块/文件 | 摘要 |
|-----------|------|
| `utils/tablePageLayout.js` | 统一列表页 `calc(100vh - …)` 表体高度、工具栏堆叠/紧凑两种布局样式 |
| `components/Layout.css`、`Layout.jsx` | 主内容区与表格区域布局配合 |
| `components/SuperAdminModal.jsx` | 与超级管理员流程相关的交互调整（与后端验证码、邮箱校验一致） |
| 多页面如 `ConfigManagement`、`Dashboard`、`InboundManagement`、`InventoryManagement`、各类日志与供应商相关页 | 引入表格布局工具或样式，保证长列表在视口内滚动 |

---

## 4. 与测试文档的对应关系

- 新增/强化的接口与参数见 `System_Test_Report.md` 中 **AUTH（管理员联系）**、**CFG（runtime-effective 扩展项）** 等用例补充。  
- 版本级说明已写入 `CHANGELOG.md` 中 **2026-03-28** 相关小节。

---

## 5. 建议回归验证（手工）

1. **参数配置**：修改 `jwt_expiration`、`log_retention_years` 后调用 `GET /api/v1/configs/runtime-effective`，再重新登录确认 Token 时长预期；保存配置后仪表盘/库存预警与入库效期提示与配置一致。  
2. **日志清理**：在非生产环境可将系统时间或保留年数临时调小后观察调度日志（或手写集成测试），确认仅删除阈值之前记录。  
3. **管理员联系**：任意登录用户访问 `GET /api/v1/auth/admin-contact`，前端个人中心展示与接口一致。  
4. **用户管理**：当前登录管理员对用户列表中**自己**执行删除或禁用，应收到明确业务错误提示。  
5. **列表页布局**：各主要列表页在 1080p 高度下表头固定、表体滚动、分页可见。

---

## 6. 相关文档索引

| 文档 | 用途 |
|------|------|
| `UPDATE_LOG_20260309_Outbound_Dashboard.md` | 2026-03-08～09 出库、审批、仪表盘 |
| `System_Test_Report.md` | 系统测试用例与检查表 |
| `CHANGELOG.md` | 版本与变更历史 |
| `API_Documentation.md` | 接口明细（若新增路径需同步维护） |
