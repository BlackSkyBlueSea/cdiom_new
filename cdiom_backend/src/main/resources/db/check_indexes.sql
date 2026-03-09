-- ============================================
-- 检查现有索引脚本
-- 用于查看哪些索引已经存在，哪些需要创建
-- ============================================

USE cdiom_db;

-- 查看所有表的索引情况
SELECT 
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS '索引字段',
    CASE 
        WHEN INDEX_NAME = 'PRIMARY' THEN '主键'
        WHEN NON_UNIQUE = 0 THEN '唯一索引'
        ELSE '普通索引'
    END AS '索引类型'
FROM information_schema.statistics
WHERE table_schema = 'cdiom_db'
  AND TABLE_NAME IN ('inventory', 'drug_info', 'purchase_order', 'inbound_record', 
                     'outbound_apply', 'operation_log', 'login_log')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- 检查本次优化需要创建的索引是否已存在
SELECT 
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    CASE 
        WHEN COUNT(*) > 0 THEN '已存在'
        ELSE '不存在'
    END AS '状态'
FROM (
    SELECT 'inventory' AS TABLE_NAME, 'idx_drug_expiry' AS INDEX_NAME
    UNION ALL SELECT 'inventory', 'idx_location_expiry'
    UNION ALL SELECT 'drug_info', 'idx_name_special'
    UNION ALL SELECT 'drug_info', 'idx_approval_manufacturer'
    UNION ALL SELECT 'purchase_order', 'idx_supplier_status'
    UNION ALL SELECT 'purchase_order', 'idx_purchaser_status'
    UNION ALL SELECT 'purchase_order', 'idx_create_time_status'
    UNION ALL SELECT 'inbound_record', 'idx_order_status'
    UNION ALL SELECT 'inbound_record', 'idx_drug_batch'
    UNION ALL SELECT 'outbound_apply', 'idx_applicant_status'
    UNION ALL SELECT 'outbound_apply', 'idx_department_status'
    UNION ALL SELECT 'operation_log', 'idx_user_operation_time'
    UNION ALL SELECT 'operation_log', 'idx_module_operation_type'
    UNION ALL SELECT 'login_log', 'idx_user_login_time'
    UNION ALL SELECT 'login_log', 'idx_status_login_time'
) AS required_indexes
LEFT JOIN information_schema.statistics s
    ON s.table_schema = 'cdiom_db'
    AND s.TABLE_NAME = required_indexes.TABLE_NAME
    AND s.INDEX_NAME = required_indexes.INDEX_NAME
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

