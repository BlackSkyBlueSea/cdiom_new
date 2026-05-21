-- 答辩演示数据清理（按编号前缀 DF26）
-- 执行前请备份数据库。

USE cdiom_db;
SET NAMES utf8mb4;

-- 兼容 MySQL Workbench Safe Updates（Error 1175）
SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM inventory_adjustment WHERE adjustment_number LIKE 'DF26-ADJ-%';

DELETE oai FROM outbound_apply_item oai
JOIN outbound_apply oa ON oa.id = oai.apply_id
WHERE oa.apply_number LIKE 'DF26-OUT-%';

DELETE FROM outbound_apply WHERE apply_number LIKE 'DF26-OUT-%';

DELETE FROM inbound_record WHERE record_number LIKE 'DF26-IN-%';

DELETE poi FROM purchase_order_item poi
JOIN purchase_order po ON po.id = poi.order_id
WHERE po.order_number LIKE 'DF26-PO-%';

DELETE FROM purchase_order WHERE order_number LIKE 'DF26-PO-%';

DELETE i FROM inventory i
JOIN drug_info d ON d.id = i.drug_id
WHERE d.national_code LIKE 'DF26DRUG%';

DELETE sd FROM supplier_drug sd
JOIN supplier s ON s.id = sd.supplier_id
WHERE s.credit_code LIKE 'DF26SUP%';

DELETE FROM drug_info WHERE national_code LIKE 'DF26DRUG%';

DELETE FROM supplier WHERE credit_code LIKE 'DF26SUP%';

DELETE FROM sys_user WHERE username IN ('wh_admin_df', 'buyer_df', 'nurse_df', 'vendor_df');

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
