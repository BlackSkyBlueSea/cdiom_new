# 更新说明（2026-04-06）

本文档汇总 **2026-03-29 之后** 至 **2026-04-06** 前后，与当前仓库代码对齐的文档修订与功能要点，便于与 [CHANGELOG.md](./CHANGELOG.md) 中的「工作区变更摘要（2026-04-06）」交叉查阅。

## 文档修订范围

| 文档 | 修订要点 |
|------|----------|
| [API_Documentation.md](./API_Documentation.md) | 用户列表 `permissionId`；药品列表排序与回收站/恢复；**供应商药品列表** 更正为 `GET /drugs?supplierId=`；仓库仪表盘权限说明与 **近效期明细** 接口；入库 **到货批次**、**disposition-options**、**from-order/split**、列表筛选 **secondConfirmStatus** 等 |
| [Function_Modules.md](./Function_Modules.md) | 用户/药品/采购订单/入库/库存/仪表盘等模块描述与上述行为一致 |
| [Project_Structure.md](./Project_Structure.md) | `SecurityHeadersFilter`；`InboundReceiptBatch` 与 Mapper；Mapper/Model 数量与过滤器说明 |
| [Development_Guide.md](./Development_Guide.md) | 前端依赖版本与 `pom.xml` 中 MyBatis-Plus 版本与 **package.json** 对齐 |
| [CHANGELOG.md](./CHANGELOG.md) | 新增「工作区变更摘要（2026-04-06）」 |

## 代码侧对应关系（速查）

- **药品**：`DrugInfoController` — 排序参数、`/deleted`、`/{id}/restore`。
- **入库**：`InboundRecordController` — `/receipt-batch`、`/disposition-options`、`/from-order/split`；服务层 `InboundReceiptBatch` 与批次编码逻辑。
- **仪表盘**：`DashboardController` — `/warehouse/near-expiry-details`。
- **用户**：`SysUserController` — 列表 `permissionId`。
- **安全**：`SecurityHeadersFilter` + `JwtAuthenticationFilter`（请求头 Token 优先）。

## 回归建议

- 药品：列表排序、回收站、按 `supplierId` 筛选、恢复。
- 入库：登记批次 → 从订单入库携带 `receiptBatchId`；不合格处置下拉与拆分入库。
- 仪表盘：仓库角色打开近效期黄/红明细与卡片数据一致。
- 用户管理：按权限筛选后结果与权限配置一致。

---

**文档版本**：与项目 v1.7.0 主线一致（增量说明见 CHANGELOG 工作区摘要）  
**最后更新**：2026-04-06
