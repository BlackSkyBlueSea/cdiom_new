-- 答辩数据验收查询

USE cdiom_db;
SET NAMES utf8mb4;

SELECT '==== 演示用户 ====' AS section;
SELECT id, username, role_id, status FROM sys_user
WHERE username IN ('admin','super_admin','wh_admin_df','buyer_df','nurse_df','vendor_df')
ORDER BY FIELD(username,'admin','super_admin','wh_admin_df','buyer_df','nurse_df','vendor_df');

SELECT '==== 系统参数 ====' AS section;
SELECT config_key, config_value FROM sys_config
WHERE config_key IN ('expiry_warning_days','expiry_critical_days','jwt_expiration');

SELECT '==== 答辩供应商 ====' AS section;
SELECT id, name, phone, credit_code, status FROM supplier WHERE credit_code LIKE 'DF26SUP%';

SELECT '==== 供应商门户：vendor_df.phone 应与某条 supplier.phone 一致 ====' AS section;
SELECT u.username, u.phone AS user_phone, s.name AS supplier_name, s.phone AS supplier_phone
FROM sys_user u
LEFT JOIN supplier s ON s.phone = u.phone AND s.deleted = 0
WHERE u.username = 'vendor_df';

SELECT '==== 答辩药品 ====' AS section;
SELECT id, national_code, drug_name, is_special, expiry_date FROM drug_info WHERE national_code LIKE 'DF26DRUG%';

SELECT '==== 答辩库存 ====' AS section;
SELECT i.id, d.national_code, i.batch_number, i.quantity, i.expiry_date, i.storage_location
FROM inventory i JOIN drug_info d ON d.id = i.drug_id
WHERE d.national_code LIKE 'DF26DRUG%' ORDER BY d.id, i.expiry_date;

SELECT '==== 答辩采购单 ====' AS section;
SELECT id, order_number, status, total_amount FROM purchase_order WHERE order_number LIKE 'DF26-PO-%';

SELECT '==== 答辩出库申请 ====' AS section;
SELECT id, apply_number, status, department, purpose FROM outbound_apply WHERE apply_number LIKE 'DF26-OUT-%';
