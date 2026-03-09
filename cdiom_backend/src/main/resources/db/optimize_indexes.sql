-- ============================================
-- 数据库索引优化脚本（简化版）
-- 用于优化查询性能，特别是联合查询和JOIN查询
-- 创建时间：2026-03-08
-- 
-- 使用说明：
-- 1. 如果索引已存在会报错（Error 1061: Duplicate key name），这是正常的，可以忽略
-- 2. 建议先执行 check_indexes.sql 查看哪些索引已存在
-- 3. 或者手动执行每个语句，遇到已存在的索引就跳过
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 库存表（inventory）索引优化
-- ============================================

-- 添加联合索引：优化根据药品ID和有效期范围查询（常用于近效期预警）
-- 如果报错 "Duplicate key name 'idx_drug_expiry'"，说明索引已存在，可以忽略
ALTER TABLE `inventory` ADD INDEX `idx_drug_expiry` (`drug_id`, `expiry_date`);

-- 添加联合索引：优化根据存储位置和有效期查询
-- 如果报错 "Duplicate key name 'idx_location_expiry'"，说明索引已存在，可以忽略
ALTER TABLE `inventory` ADD INDEX `idx_location_expiry` (`storage_location`, `expiry_date`);

-- ============================================
-- 2. 药品信息表（drug_info）索引优化
-- ============================================

-- 添加联合索引：优化根据药品名称和特殊药品标识查询（常用于列表筛选）
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `drug_info` ADD INDEX `idx_name_special` (`drug_name`, `is_special`);

-- 添加联合索引：优化根据批准文号和生产厂家查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `drug_info` ADD INDEX `idx_approval_manufacturer` (`approval_number`, `manufacturer`);

-- ============================================
-- 3. 采购订单表（purchase_order）索引优化
-- ============================================

-- 添加联合索引：优化根据供应商ID和状态查询（常用于供应商查看自己的订单）
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `purchase_order` ADD INDEX `idx_supplier_status` (`supplier_id`, `status`);

-- 添加联合索引：优化根据采购员ID和状态查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `purchase_order` ADD INDEX `idx_purchaser_status` (`purchaser_id`, `status`);

-- 添加联合索引：优化根据创建时间和状态查询（常用于时间范围筛选）
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `purchase_order` ADD INDEX `idx_create_time_status` (`create_time`, `status`);

-- ============================================
-- 4. 入库记录表（inbound_record）索引优化
-- ============================================

-- 添加联合索引：优化根据订单ID和状态查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `inbound_record` ADD INDEX `idx_order_status` (`order_id`, `status`);

-- 添加联合索引：优化根据药品ID和批次号查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `inbound_record` ADD INDEX `idx_drug_batch` (`drug_id`, `batch_number`);

-- ============================================
-- 5. 出库申请表（outbound_apply）索引优化
-- ============================================

-- 添加联合索引：优化根据申请人ID和状态查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `outbound_apply` ADD INDEX `idx_applicant_status` (`applicant_id`, `status`);

-- 添加联合索引：优化根据部门和状态查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `outbound_apply` ADD INDEX `idx_department_status` (`department`, `status`);

-- ============================================
-- 6. 操作日志表（operation_log）索引优化
-- ============================================

-- 添加联合索引：优化根据用户ID和操作时间查询（常用于用户操作历史）
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `operation_log` ADD INDEX `idx_user_operation_time` (`user_id`, `operation_time`);

-- 添加联合索引：优化根据模块和操作类型查询
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `operation_log` ADD INDEX `idx_module_operation_type` (`module`, `operation_type`);

-- ============================================
-- 7. 登录日志表（login_log）索引优化
-- ============================================

-- 添加联合索引：优化根据用户ID和登录时间查询（常用于用户登录历史）
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `login_log` ADD INDEX `idx_user_login_time` (`user_id`, `login_time`);

-- 添加联合索引：优化根据状态和登录时间查询（常用于统计成功/失败登录）
-- 如果报错 "Duplicate key name"，说明索引已存在，可以忽略
ALTER TABLE `login_log` ADD INDEX `idx_status_login_time` (`status`, `login_time`);

-- ============================================
-- 8. 供应商-药品关联表（supplier_drug）索引优化
-- ============================================

-- 添加联合索引：优化根据供应商ID和药品ID查询（已存在uk_supplier_drug，但可以添加价格相关索引）
-- 注意：uk_supplier_drug 已经覆盖了 (supplier_id, drug_id) 的查询

-- ============================================
-- 索引优化说明
-- ============================================
-- 
-- 【重要提示】
-- 如果执行时遇到 "Error 1061: Duplicate key name 'xxx'" 错误，
-- 说明该索引已存在，这是正常的，可以安全忽略，继续执行后续语句即可。
-- 
-- 【推荐执行方式】
-- 方式1（推荐）：先执行 check_indexes.sql 查看哪些索引已存在，然后手动执行需要的索引创建语句
-- 方式2：直接执行本脚本，遇到已存在的索引错误就忽略，继续执行
-- 
-- 【索引说明】
-- 1. 联合索引的顺序很重要：将选择性高的字段放在前面
-- 2. 索引会占用存储空间，但能显著提升查询性能
-- 3. 建议在生产环境执行前，先在测试环境验证性能提升效果
-- 4. 定期分析慢查询日志，根据实际查询模式调整索引策略
-- 
-- 【检查索引是否存在的SQL】
-- 执行 check_indexes.sql 或运行以下查询：
-- SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX 
-- FROM information_schema.statistics 
-- WHERE table_schema = 'cdiom_db' 
-- AND table_name = 'inventory' 
-- ORDER BY INDEX_NAME, SEQ_IN_INDEX;

