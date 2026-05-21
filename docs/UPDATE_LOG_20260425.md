# 更新说明（2026-04-25）

本文档记录 **2026-04-25** 对 `docs/` 目录进行的**全量与代码对齐**修订摘要，便于与 [Project_Structure.md](./Project_Structure.md)、[Database_Design.md](./Database_Design.md) 交叉查阅。

## 修订要点（按主题）

| 主题 | 涉及文档 | 与代码一致后的要点 |
|------|-----------|-------------------|
| 项目结构 | [Project_Structure.md](./Project_Structure.md) | 根目录无 `插图位置检查报告.md`；`init_simple.sql` **20** 张基础表；`db/` 下 **35** 个 `.sql`；补充 `PermissionWebMvcConfig`、`JsonUnauthorizedAuthenticationEntryPoint`、`InboundSecondConfirmMailNotifier`、`model/vo`；前端 **`SupplierDrugManage.jsx`**、`config/appAccessPolicy.js`；统计表（Controller 23、页面 19、Config 16 等） |
| 数据库口径 | [Database_Design.md](./Database_Design.md) | 区分 `init_simple.sql` 内 20 表与扩展脚本；**不再**将 `supplier_drug`、`sys_user_permission` 误记为随 init 创建；注意事项中数据源密码指向 **`application-local.yml`** |
| 功能与完成度 | [Function_Modules.md](./Function_Modules.md)、[Code_Completeness_Report.md](./Code_Completeness_Report.md) | 登录 Token 与 `auth.js` 一致（sessionStorage + 条件 Cookie）；数据库完成度表述与 **20 + 扩展** 一致；前端页面 **19** 项含 `Home`、`SupplierDrugManage`；去除重复/错误编号；`init_simple` 表清单含 **`inbound_receipt_batch`**、不含 **`supplier_drug`** |
| 部署 | [Deployment_Guide.md](./Deployment_Guide.md) | 注释修正：`supplier_drug` 为扩展表，非「init 第 20 张」 |
| 版本历史 | [CHANGELOG.md](./CHANGELOG.md) | 历史条目中「21 张表」等表述加注**当前仓库**口径；`supplier_drug` 行与 Database_Design 互链 |
| 文档索引 | [README.md](./README.md) | 新增「八、专项方案…」表格，收录原索引未列出的专项/修复/任务类 Markdown |

## 回归建议

- 随机打开 [Project_Structure.md](./Project_Structure.md) 中列出的路径，在工作区中确认文件存在。
- 对新库仅执行 `init_simple.sql` 后 `SHOW TABLES` 应为 **20** 张；再执行 README 推荐扩展脚本后表数递增。

---

**最后更新**：2026-04-25
