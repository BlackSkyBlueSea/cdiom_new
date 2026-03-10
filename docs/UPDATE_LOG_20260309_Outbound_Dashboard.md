# 近期开发记录：出库、审批、仪表盘与权限（2026-03-08 ~ 2026-03-09）

> 本文档整理近期多轮会话中涉及的代码改动、业务流说明及与现有 md 文档的对应关系，便于追溯与测试。  
> 关联会话 requestID（供内部追溯）：af82a1e3-5ed7-4212-abfe-2ace75ccf91f、a2460ce9-3f5c-4aee-95d7-bd4a712223fe、0a3f98e5-4304-4497-815a-f1c9c28ebeeb、4eeb6f8e-dd99-4dac-b155-3f51608167ab、410ee6e9-43aa-4f52-b9c9-fdf052b8af7e。

---

## 目录

- [1. 业务流概览](#1-业务流概览)
- [2. 代码与功能改动清单](#2-代码与功能改动清单)
- [3. 与现有文档的对应关系](#3-与现有文档的对应关系)

---

## 1. 业务流概览

### 1.1 出库申请与审批流程

| 步骤 | 角色 | 动作 | 说明 |
|------|------|------|------|
| 1 | 医护人员 | 新建出库申请 | 填写科室（下拉+可输）、用途、药品与数量；申请人由后端按当前登录用户写入。 |
| 2 | 医护人员 | 查看详情 / 撤回 | 申请通过前，本人可「查看详情」和「撤回」；撤回后状态为已取消。 |
| 3 | 仓库管理员 | 审批（通过/驳回） | 打开审批弹窗时拉取**库存校验**；若任一项库存不足，界面提示且**禁止通过**；通过前后端再次校验库存，不足则返回友好错误。 |
| 4 | 仓库管理员 | 执行出库 | 仅「已通过」的申请可执行；执行时按 FIFO 扣减库存，申请状态变为已出库。 |

**重要约定：**

- **审批通过**：只将申请状态改为「已通过」，**不扣减库存**。
- **执行出库**：才真正扣减库存；库存管理、仪表盘中的「今日出库」等数据在执行出库后更新。
- **仪表盘 / 库存页**：切回该标签页时会自动重新请求数据，审批或执行出库后切回即可看到最新待办、今日出库、库存等。

### 1.2 身份与 Token 优先级

- 请求中**优先使用 Header 中的 `Authorization: Bearer <token>`**，其次才使用 Cookie `cdiom_token`。
- 避免同一浏览器中旧 Cookie（如其他用户）覆盖当前用户的身份，导致申请人/审批人显示错误。
- **同设备多标签多用户**：若在未使用「多用户登录」时在同一浏览器多标签分别登录不同用户，Cookie 会被后登录用户覆盖，旧标签页若仅从 Cookie 取 token 会误用他人身份，导致审批人记录为错误用户。修复：登录时**始终写入 sessionStorage**（再按模式决定是否写 Cookie），且 `getToken`/`getUser` 优先读 sessionStorage，保证每个标签页使用本标签登录身份，审批人显示与记录正确。

### 1.3 权限与前端按钮

- **医护人员**（角色 4）：具备 `outbound:view`、`outbound:apply`，可新建申请、查看详情、撤回本人待审批申请；列表操作列显示「查看详情」「撤回」（本人且 PENDING 时）。
- **仓库管理员**（角色 2）：具备出库审批、驳回、执行等权限；审批弹窗中展示库存校验结果，库存不足时禁止通过。

---

## 2. 代码与功能改动清单

### 2.1 后端

| 模块/文件 | 改动摘要 |
|-----------|----------|
| **仓库/出库仪表盘 SQL** | `InboundRecordMapper`、`OutboundApplyMapper` 中 `@Select` 内比较运算符由误写的 `&gt;=`/`&lt;=` 改为 `>=`/`<=`，修复 `SQLSyntaxErrorException`，仓库管理员仪表盘数据正常。 |
| **出库申请详情与明细权限** | `GET /api/v1/outbound/{id}`、`GET /api/v1/outbound/{id}/items` 增加 `outbound:view`、`outbound:apply`，医护人员可查看详情及明细。 |
| **科室列表** | `InboundRecordMapper` 或 `OutboundApplyMapper` 新增 `listDistinctDepartments()`；`OutboundApplyController` 新增 `GET /api/v1/outbound/departments`，供新建申请时科室下拉与手动输入。 |
| **申请人/审批人展示** | `OutboundApply` 增加非持久字段 `applicantRoleName`、`approverRoleName`；`OutboundApplyServiceImpl` 在列表与详情中填充申请人/审批人姓名及角色名，避免仅显示用户名造成混淆。 |
| **申请人撤回** | `OutboundApplyService` 新增 `withdrawOutboundApply(id, applicantUserId)`；仅申请人本人且状态为 PENDING 时可撤回；Controller 新增 `POST /api/v1/outbound/{id}/withdraw`，权限 `outbound:apply`。 |
| **审批前库存校验** | `InventoryMapper` 新增 `getTotalAvailableQuantityByDrugId(drugId, today)`；`InventoryService` 新增 `getTotalAvailableQuantity(drugId)`；`OutboundApplyServiceImpl.approveOutboundApply` 在改状态前按明细校验可用库存，不足则抛出友好错误；新增 `checkStockForApply(applyId)` 及 `GET /api/v1/outbound/{id}/stock-check` 供前端展示。 |
| **JWT Token 优先级** | `JwtAuthenticationFilter`、`OutboundApplyController.getTokenFromRequest` 取 Token 时**先读 Authorization 头，再读 Cookie**，保证多用户/切换用户后当前请求身份正确。 |
| **多标签审批人错乱修复** | 前端 `auth.js`：`setToken`/`setUser` 登录时**始终写入 sessionStorage**；`getToken`/`getUser` **优先读 sessionStorage 再 Cookie**，与 request 拦截器一致，避免同浏览器多标签登录不同用户时 Cookie 被覆盖导致审批人记录成他人。 |

### 2.2 前端

| 模块/文件 | 改动摘要 |
|-----------|----------|
| **供应商仪表盘** | 动态数据拉取与展示完善：数值/数组归一化、失败时默认空状态、刷新按钮、待处理订单/已确认金额等展示；图表与统计使用 `?? 0` 等安全访问。 |
| **出库管理页** | 新建申请：所属科室改为科室下拉（来自 `/outbound/departments`）+ 可手动输入；药品选择后展示名称与规格；审批时第二审批人下拉排除当前用户；列表与审批弹窗中申请人展示为「姓名（角色名）」；操作列对具备权限用户显示「查看详情」，对本人且 PENDING 显示「撤回」；新增只读「出库申请详情」弹窗，申请人可在详情弹窗内撤回。 |
| **审批弹窗** | 打开时请求 `GET /outbound/{id}/stock-check`；库存充足时绿色提示可审批通过；库存不足时红色提示并列出「药品名：需要 X，可用 Y」，并**禁用确定按钮**，无法通过。 |
| **权限映射** | `permission.js`：医护人员 `ROLE_PERMISSIONS[4]` 补充 `OUTBOUND_VIEW`、`OUTBOUND_APPLY`；仓库管理员 `ROLE_PERMISSIONS[2]` 补充出库/入库/库存相关权限，避免刷新后按钮消失。 |
| **仪表盘与库存页** | 仓库管理员仪表盘：标签页从隐藏变为可见时自动再请求一次仪表盘数据；库存管理页：标签页可见时自动再请求一次列表，便于执行出库后看到最新数据。 |

### 2.3 数据库与配置

- 科室列表来自 `outbound_apply` 表已有 `department` 去重，无需改表结构。
- 权限仍沿用现有 `sys_role_permission` / `sys_user_permission` 及 `add_user_permission_system.sql` 等脚本；前端 `ROLE_PERMISSIONS` 与后端注解保持一致即可。

---

## 3. 与现有文档的对应关系

| 文档 | 本次建议更新内容 |
|------|------------------|
| **CHANGELOG.md** | 在 v1.7.0 或新版本条下增加：仓库/出库仪表盘 SQL 修复、出库详情/明细/科室/撤回/库存校验接口与逻辑、JWT 优先级、前端权限与按钮、审批前库存校验与禁止通过、仪表盘/库存页可见性刷新。 |
| **API_Documentation.md** | 出库管理接口：更新 `GET /outbound/{id}`、`GET /outbound/{id}/items` 的权限说明；新增 `GET /outbound/departments`、`GET /outbound/{id}/stock-check`、`POST /outbound/{id}/withdraw`；修正取消接口权限为 `drug:manage`（取消与申请人撤回为不同接口）。 |
| **System_Test_Report.md** | 出库用例：增加库存校验接口、申请人撤回、审批前库存不足禁止通过、详情/撤回按钮与权限；业务逻辑节补充「审批不扣库存、执行出库才扣库存」及「切回页签刷新」说明。 |
| **Function_Modules.md** | 若有出库/审批/仪表盘模块说明，可补充：科室下拉、申请人撤回、审批前库存校验、身份与 Token 优先级。 |
| **项目进度.md** | 若有「近期更新」或「已完成功能」列表，可加入上述功能点。 |

---

## 4. 接口速查（本次新增/变更）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/outbound/departments` | outbound:view / outbound:apply | 获取科室下拉列表（去重、非空） |
| GET | `/api/v1/outbound/{id}` | drug:view / drug:manage / **outbound:view** / **outbound:apply** | 获取出库申请详情（含申请人/审批人姓名与角色名） |
| GET | `/api/v1/outbound/{id}/items` | drug:view / drug:manage / **outbound:view** / **outbound:apply** | 获取出库申请明细 |
| GET | `/api/v1/outbound/{id}/stock-check` | outbound:view / outbound:apply / outbound:approve / outbound:approve:special | 审批前库存校验，返回 sufficient、message、details |
| POST | `/api/v1/outbound/{id}/withdraw` | outbound:apply | 申请人撤回（仅本人且 PENDING） |

---

*文档版本：v1.0 | 整理日期：2026-03-09*
