# CDIOM 系统测试报告

## 文档信息

| 项目 | 说明 |
|------|------|
| **文档版本** | v1.0 |
| **创建/更新日期** | 2026年3月9日 |
| **适用版本** | CDIOM v1.7.0 |
| **测试目的** | 支撑功能测试、性能测试、安全测试及业务逻辑验证 |
| **参考文档** | API_Documentation.md、Function_Modules.md、Code_Logic_Vulnerability_Report.md、Code_Completeness_Report.md |

---

## 目录

- [1. 测试范围与前置条件](#1-测试范围与前置条件)
- [2. 功能测试用例](#2-功能测试用例)
- [3. 业务逻辑与数据一致性测试](#3-业务逻辑与数据一致性测试)
- [4. 性能测试要点](#4-性能测试要点)
- [5. 安全测试要点](#5-安全测试要点)
- [6. 兼容性与响应式测试](#6-兼容性与响应式测试)
- [7. 测试检查清单与记录表](#7-测试检查清单与记录表)

---

## 1. 测试范围与前置条件

### 1.1 测试范围

- **后端**：Spring Boot 3.2.8，Java 17，MyBatis-Plus，MySQL 8.x  
- **前端**：Vite + React，主要业务页面与 API 调用  
- **业务域**：认证与权限、用户/角色/配置/公告、药品与库存、入库/出库、采购订单、供应商、仪表盘、文件上传、后端监控  

### 1.2 测试环境与前置条件

| 项目 | 要求 |
|------|------|
| 后端服务 | 已启动，默认端口 8080，数据库已初始化（21 张表） |
| 前端服务 | 已启动，默认端口 5173，API 基地址指向后端 |
| 测试账号 | 至少具备：系统管理员、仓库管理员、采购专员、医护人员、供应商等角色账号 |
| 基础数据 | 药品信息、供应商、采购订单、库存等具备可测试数据 |
| 配置 | `application-local.yml` 或环境变量已配置（JWT、数据库、邮件、第三方 API 等），见 SECURITY_CONFIG.md |

### 1.3 统一响应与认证约定

- 接口统一格式：`{ "code": 200, "msg": "...", "data": ... }`，200 成功，401 未授权，500 失败。  
- 认证：JWT Token，8 小时有效，Cookie key：`cdiom_token`。  
- 权限：基于 RBAC + 细粒度权限（如 `outbound:apply`、`inbound:create` 等），接口需按角色/权限设计用例。

---

## 2. 功能测试用例

以下用例可直接用于功能测试执行，建议按模块分批执行并记录结果（通过/失败/阻塞）。

### 2.1 认证模块

| 用例编号 | 测试项 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| AUTH-01 | 用户名密码正确登录 | 用户存在且未锁定 | POST /api/v1/auth/login，正确用户名与密码 | 200，返回 token 与用户信息，Cookie 写入 cdiom_token | |
| AUTH-02 | 手机号登录 | 用户已绑定手机号 | 使用手机号作为 username 登录 | 200，登录成功 | |
| AUTH-03 | 错误密码 | 用户存在 | 正确用户名、错误密码 | 非 200，提示密码错误，登录失败次数+1 | |
| AUTH-04 | 连续 5 次失败锁定 | 同上 | 连续 5 次错误密码登录 | 第 5 次后提示账户锁定，剩余解锁时间（如 1 小时） | |
| AUTH-05 | 锁定期内拒绝登录 | 账户已锁定 | 正确密码登录 | 拒绝登录，提示锁定剩余时间 | |
| AUTH-06 | 获取当前用户 | 已登录 | GET /api/v1/auth/current，带 Token | 200，返回当前用户信息 | |
| AUTH-07 | 获取权限列表 | 已登录 | GET /api/v1/auth/permissions | 200，返回权限代码集合 | |
| AUTH-08 | 登出 | 已登录 | POST /api/v1/auth/logout | 200，Token/Cookie 清除，再请求需认证接口返回 401 | |

### 2.2 用户与角色管理

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| USER-01 | 用户列表分页与筛选 | user:manage | GET /api/v1/users?page=1&size=10&keyword=xxx&roleId=&status= | 200，分页数据与筛选正确 | |
| USER-02 | 创建用户 | user:manage | POST /api/v1/users，必填字段完整，密码明文 | 200，用户创建成功，密码为 BCrypt 加密存储 | |
| USER-03 | 编辑用户 | user:manage | PUT /api/v1/users/{id}，修改部分字段 | 200，数据更新正确 | |
| USER-04 | 禁用/启用用户 | user:manage | PUT /api/v1/users/{id}/status，status=0/1 | 200，状态变更后该用户无法/可以登录 | |
| USER-05 | 解锁用户 | user:manage | PUT /api/v1/users/{id}/unlock | 200，锁定时间与失败次数清零 | |
| USER-06 | 逻辑删除用户 | user:manage | DELETE /api/v1/users/{id} | 200，用户标记删除，列表不显示，回收站可查 | |
| USER-07 | 无权限访问用户接口 | 无 user:manage | 调用上述任一接口 | 403 或 401 | |
| ROLE-01 | 角色列表与筛选 | user:manage | GET /api/v1/roles?page=1&size=10&keyword= | 200，分页与筛选正确 | |
| ROLE-02 | 创建角色（role_code 唯一） | user:manage | POST /api/v1/roles，roleCode 唯一 | 200，角色创建成功 | |
| ROLE-03 | 重复 role_code 创建 | user:manage | POST 与已有 roleCode 相同的角色 | 失败，提示唯一性冲突 | |

### 2.3 药品与库存

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| DRUG-01 | 药品列表分页与筛选 | drug:view 或 drug:manage | GET /api/v1/drugs?page=1&size=10&keyword=&isSpecial= | 200，列表与筛选正确 | |
| DRUG-02 | 新增/编辑/删除药品 | drug:manage | POST/PUT/DELETE /api/v1/drugs | 200，逻辑删除可恢复或符合设计 | |
| DRUG-03 | 根据商品码/本位码/追溯码查询 | drug:view 或 drug:manage | GET /api/v1/drugs/search?code=xxx | 先查本地，未找到则调第三方 API，返回合并结果 | |
| INV-01 | 库存列表与多条件筛选 | drug:view 或 drug:manage | GET /api/v1/inventory，带 keyword、drugId、batchNumber、有效期区间等 | 200，筛选与分页正确 | |
| INV-02 | 近效期预警 | drug:view 或 drug:manage | GET /api/v1/inventory/near-expiry-warning | 200，返回 90/180 天等预警统计与列表 | |
| INV-03 | 库存导出 Excel | drug:view 或 drug:manage | GET /api/v1/inventory/export（同列表参数） | 200，下载 Excel，含导出人、导出时间水印 | |

### 2.4 入库管理

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| IN-01 | 入库记录列表与筛选 | drug:view 或 drug:manage | GET /api/v1/inbound?orderId=&drugId=&status= 等 | 200，分页与筛选正确 | |
| IN-02 | 从采购订单创建入库 | drug:manage | POST /api/v1/inbound/from-order，orderId、药品、批次、数量、效期等 | 200，入库记录生成；库存增加；订单已入库数量增加 | |
| IN-03 | 入库数量不超过订单采购量 | drug:manage | 对同一订单同一药品，多次入库使累计数量超过订单采购数量 | 失败，提示“入库数量超过订单采购数量” | |
| IN-04 | 全部入库后订单状态 | drug:manage | 某订单所有明细全部入库完成 | 订单状态自动变为 RECEIVED | |
| IN-05 | 效期校验 | drug:manage | POST /api/v1/inbound/check-expiry，body 含 expiryDate | 200，返回 PASS/WARNING/FORCE 及说明 | |
| IN-06 | 临时入库（无 orderId） | drug:manage | POST /api/v1/inbound/temporary，不传 orderId | 200，入库成功，库存增加 | |
| IN-07 | 可入库数量查询 | drug:view 或 drug:manage | GET /api/v1/inbound/order/{orderId}/drug/{drugId}/quantity | 200，返回该订单该药品可再入库数量 | |

### 2.5 出库管理

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| OUT-01 | 出库申请列表与筛选 | outbound:view 等 | GET /api/v1/outbound?status=&applicantId= 等 | 200，分页与状态筛选正确 | |
| OUT-02 | 创建出库申请 | outbound:apply | POST /api/v1/outbound，department、purpose、items（药品、批次、数量） | 200，申请单生成，状态 PENDING | |
| OUT-03 | 审批通过（普通） | outbound:approve | POST /api/v1/outbound/{id}/approve | 200，状态变为 APPROVED | |
| OUT-04 | 审批通过（特殊药品双人） | outbound:approve:special | POST /api/v1/outbound/{id}/approve，body 含 secondApproverId | 200，双人审批通过 | |
| OUT-05 | 驳回申请 | drug:manage | POST /api/v1/outbound/{id}/reject，rejectReason | 200，状态 REJECTED | |
| OUT-06 | 执行出库（FIFO） | outbound:execute | POST /api/v1/outbound/{id}/execute，outboundItems 与申请一致 | 200，库存按先进先出扣减，申请状态 OUTBOUND | |
| OUT-07 | 执行出库数量超过可用库存 | outbound:execute | 执行数量大于当前可用库存 | 失败，提示库存不足，不部分扣减 | |
| OUT-08 | 取消申请 | outbound:apply | POST /api/v1/outbound/{id}/cancel（仅 PENDING） | 200，状态 CANCELLED | |
| OUT-09 | 待审批数量 | outbound:approve | GET /api/v1/outbound/pending-count | 200，返回待审批数量 | |

### 2.6 采购订单

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| PO-01 | 订单列表与筛选 | drug:view 或 drug:manage | GET /api/v1/purchase-orders?supplierId=&status=&startDate= 等 | 200，供应商角色仅见己方订单 | |
| PO-02 | 创建订单 | drug:manage | POST /api/v1/purchase-orders，supplierId、expectedDeliveryDate、items | 200，订单状态 PENDING | |
| PO-03 | 编辑/删除（仅 PENDING） | drug:manage | PUT/DELETE 仅对 PENDING 订单 | 200；非 PENDING 应拒绝或提示 | |
| PO-04 | 状态流转 | drug:manage | 确认→CONFIRMED，发货→SHIPPED，入库完成→RECEIVED | 状态与业务规则一致，RECEIVED 由全部入库触发 | |
| PO-05 | 订单条形码 | drug:view 或 drug:manage | GET /api/v1/purchase-orders/{id}/barcode 与 /barcode/download | 200，返回 Base64 或文件下载 | |
| PO-06 | 可入库检查 | drug:view 或 drug:manage | GET /api/v1/purchase-orders/{id}/can-inbound、is-fully-inbound、inbound-quantities | 200，与订单明细及已入库数量一致 | |
| PO-07 | 导出订单 Excel | drug:view 或 drug:manage | GET /api/v1/purchase-orders/export | 200，Excel 含导出人、导出时间 | |

### 2.7 供应商与审核

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| SUP-01 | 供应商列表与筛选 | drug:view 或 drug:manage | GET /api/v1/suppliers?status=&auditStatus= | 200，分页与筛选正确 | |
| SUP-02 | 创建供应商（待审核） | drug:manage | POST /api/v1/suppliers，status=2 | 200，审核状态待审核 | |
| SUP-03 | 审核通过/驳回 | supplier:audit | POST /api/v1/suppliers/{id}/audit，auditStatus=1 或 2 | 200，状态更新，驳回含原因 | |
| SUP-04 | 供应商-药品关联与单价 | drug:manage | POST/PUT/DELETE 供应商药品、单价 | 200，关联与价格历史符合设计 | |

### 2.8 库存调整

| 用例编号 | 测试项 | 权限要求 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|----------|
| ADJ-01 | 调整记录列表 | drug:view 或 drug:manage | GET /api/v1/inventory-adjustments?adjustmentType= 等 | 200，分页与类型筛选正确 | |
| ADJ-02 | 盘盈/盘亏 | drug:manage | POST /api/v1/inventory-adjustments，PROFIT/LOSS，数量前后一致 | 200，库存数量变更，调整记录正确 | |
| ADJ-03 | 特殊药品双人操作 | drug:manage | 盘盈/盘亏时传 secondOperatorId | 200，符合双人操作规则 | |

### 2.9 参数配置、通知公告、日志

| 用例编号 | 测试项 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|
| CFG-01 | 配置 CRUD 与 key 唯一 | GET/POST/PUT/DELETE /api/v1/configs，按 key 查询 | 200，config_key 唯一 | |
| NOTICE-01 | 公告 CRUD 与状态 | GET/POST/PUT/DELETE /api/v1/notices，状态开启/关闭 | 200，状态生效 | |
| LOG-01 | 操作日志列表 | GET /api/v1/operation-logs（需 log:operation:view） | 200，仅列表，不可删 | |
| LOG-02 | 登录日志与地理位置 | GET /api/v1/login-logs，检查是否有 IP、地理位置（内网显示“内网IP”） | 200 | |

### 2.10 仪表盘与监控

| 用例编号 | 测试项 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|
| DASH-01 | 基础统计 | GET /api/v1/dashboard/statistics（登录后） | 200，用户数、药品数、订单数等 | |
| DASH-02 | 角色化仪表盘 | GET /api/v1/dashboard/warehouse、purchaser、medical-staff、supplier | 200，按角色返回对应统计与图表数据 | |
| MON-01 | 系统信息与健康检查 | GET /api/v1/system/info、GET /api/v1/health（可免登录） | 200，JVM/内存/健康状态 | |
| MON-02 | 最近日志与 WebSocket 日志流 | GET /api/v1/logs/recent，WS /api/v1/logs/stream | 200 / 连接成功并收到日志推送 | |

### 2.11 文件上传与超级管理员

| 用例编号 | 测试项 | 操作步骤 | 预期结果 | 实际结果 |
|----------|--------|----------|----------|----------|
| FILE-01 | 上传图片（类型+大小） | POST /api/v1/upload，multipart，jpg/png 等 ≤10MB | 200，返回 URL，按日期目录存储 | |
| FILE-02 | 非法类型/超大文件 | 上传非图片或 >10MB | 400/413 或明确错误提示 | |
| SA-01 | 超级管理员状态与启用/停用 | GET status；POST send-verification-code、enable、disable（邮箱验证码） | 仅绑定邮箱可收验证码，启用/停用需验证码 | |

---

## 3. 业务逻辑与数据一致性测试

结合 Code_Logic_Vulnerability_Report 与 Code_Completeness_Report，以下为必须验证的业务逻辑与数据一致性要点。

### 3.1 已修复逻辑的回归验证

| 检查项 | 说明 | 验证方法 |
|--------|------|----------|
| 登录锁定 | 连续 5 次失败锁定 1 小时，锁定期内拒绝登录，成功登录清零 | 见 AUTH-03～AUTH-06 |
| 库存并发 | 增删库存使用悲观锁与原子更新，无超卖与负数 | 并发执行出库/入库，检查库存与流水一致、无超卖 |
| 单号唯一 | 入库单号、出库申请单号、调整单号唯一，重复时重试 | 高并发创建单据，检查单号无重复、无脏数据 |
| 入库数量 | 单订单单药品累计入库 ≤ 订单采购数量 | 见 IN-03 |
| 订单状态 | 全部入库后订单自动 RECEIVED | 见 IN-04 |
| 出库 FIFO | 执行出库按先进先出扣减，且不部分扣减（数量不足则整体失败） | 见 OUT-06、OUT-07；可查库存批次与出库明细是否按生产/入库日期先后 |

### 3.2 事务与回滚

- 入库、出库、库存调整、订单状态变更等应在同一事务中，任一步失败应整体回滚。  
- 测试方法：构造会失败的场景（如库存不足、重复单号、违反唯一约束），确认数据库无部分提交。

### 3.3 数据一致性

- 库存表 `inventory` 与入库/出库/调整记录一致：任意时刻各批次数量 = 初始 + 入库 − 出库 + 调整。  
- 订单明细的“已入库数量”与入库记录汇总一致。  
- 建议：针对关键链路（如“创建订单→部分入库→执行出库→调整”）做端到端脚本或手工核对。

---

## 4. 性能测试要点

| 测试类型 | 目标 | 建议方法 |
|----------|------|----------|
| 接口响应时间 | 列表接口 P95 &lt; 2s，简单 CRUD &lt; 500ms（可依实际环境调整） | 使用 JMeter/Postman Runner/k6 等压测列表、详情、创建接口 |
| 并发用户 | 登录、列表查询、出库/入库提交在预期并发下无 5xx、无超时 | 模拟 10～50 并发（可按业务量调整），持续 1～5 分钟 |
| 数据库 | 无慢 SQL、无死锁 | 开启 MySQL 慢查询日志，压测期间观察锁等待与慢查询 |
| 分页与大数据量 | 大页码或大 size 不分页不拖垮服务 | 测试 size=100、500 等，必要时限制 max size |
| 导出 | Excel 导出 1 万行内可接受完成时间 | 记录导出耗时与内存占用 |
| 前端首屏与列表 | 首屏可交互时间、列表渲染时间在可接受范围 | 浏览器 Performance/Lighthouse 或手工体感 |

---

## 5. 安全测试要点

| 测试项 | 说明 | 验证方法 |
|--------|------|----------|
| 认证 | 未带 Token 或非法 Token 访问需认证接口返回 401 | 去掉 Cookie/Header 或篡改 Token 调用接口 |
| 授权 | 无权限角色访问受限接口返回 403 | 使用不同角色账号访问仅其他角色可用的接口 |
| 登录暴力破解 | 锁定机制生效（见 AUTH-04、AUTH-05） | 连续错误密码 |
| SQL 注入 | 关键字、筛选参数中注入 SQL 片段 | 在 keyword、id 等参数中注入 `' OR 1=1--` 等，应无异常数据返回 |
| 文件上传 | 仅允许规定类型（如 jpg/png 等）与大小（如 10MB），魔数校验 | 上传 .exe 改后缀、超大文件、畸形 MIME | 见 FILE-01、FILE-02 |
| 敏感信息 | 密码不明文存储、日志与错误信息不暴露内部实现 | 查库与日志、触发错误看响应内容 |
| 配置与密钥 | JWT、数据库、API Key 等不提交到仓库，见 SECURITY_CONFIG.md | 检查 .gitignore 与部署配置 |

---

## 6. 兼容性与响应式测试

| 测试项 | 说明 |
|--------|------|
| 浏览器 | Chrome、Edge、Firefox、Safari 最新版，登录与主要业务流程可用 |
| 分辨率/响应式 | 文档要求支持 PC 与 Pad；检查列表、表单、仪表盘在不同宽度下布局与操作正常 |
| 移动端 | 若有移动访问需求，需单独做基础流程与触摸操作验证 |

---

## 7. 测试检查清单与记录表

### 7.1 功能测试执行记录（示例）

| 模块 | 用例数 | 通过 | 失败 | 阻塞 | 备注 |
|------|--------|------|------|------|------|
| 认证 | 8 | | | | |
| 用户/角色 | 10 | | | | |
| 药品/库存 | 6 | | | | |
| 入库 | 7 | | | | |
| 出库 | 9 | | | | |
| 采购订单 | 7 | | | | |
| 供应商 | 4 | | | | |
| 库存调整 | 3 | | | | |
| 配置/公告/日志 | 4 | | | | |
| 仪表盘/监控 | 4 | | | | |
| 文件/超级管理员 | 3 | | | | |

### 7.2 业务逻辑与性能/安全检查

| 类别 | 检查项 | 通过 | 失败 | 说明 |
|------|--------|------|------|------|
| 业务逻辑 | 登录锁定、库存并发、单号唯一、入库数量、订单状态、出库 FIFO | | | |
| 业务逻辑 | 事务回滚、库存与流水一致 | | | |
| 性能 | 列表/CRUD 响应时间、并发无 5xx、导出耗时 | | | |
| 安全 | 认证/授权、文件上传、敏感信息与配置 | | | |

### 7.3 测试环境与版本记录

| 项目 | 内容 |
|------|------|
| 后端版本/分支 | |
| 前端版本/分支 | |
| 数据库版本 | |
| 测试执行人 | |
| 测试日期 | |
| 备注（环境差异、已知问题等） | |

---

**文档说明**：本报告基于当前项目进度与既有代码逻辑/业务文档整理，用于指导功能测试、性能测试与部分安全测试。执行时请以实际 API 与界面为准，若有接口变更请同步更新用例与预期结果。详细接口定义见 `API_Documentation.md`，功能模块见 `Function_Modules.md`，逻辑与漏洞修复情况见 `Code_Logic_Vulnerability_Report.md`。
