-- 答辩演示数据种子
-- 依赖：已存在 sys_role（1~6）、sys_permission 及角色权限脚本已执行过的库。
-- 演示账号密码：admin / admin123；super_admin / super123；其余 *_df 账号均为 admin123（与 admin 相同 BCrypt）。

USE cdiom_db;
SET NAMES utf8mb4;

START TRANSACTION;

-- 与 init 脚本一致的 admin123（$2a$）
SET @pwd_admin123 = '$2a$10$miCOohBEJc0JYzL7OLFgQOTUQETRofEoR46sXfOmiG7MeBzyblhHm';
SET @pwd_super123 = '$2a$10$PYyQVNzrbGajEGa.7XPIzu83vpo6WXZfEDvakWbeaale7ExoBTEIm';

-- 系统参数（答辩可调阈值）
INSERT INTO sys_config(config_name, config_key, config_value, config_type, remark, deleted)
VALUES
('近效期预警天数', 'expiry_warning_days', '180', 1, '答辩演示', 0),
('严重近效期天数', 'expiry_critical_days', '90', 1, '答辩演示', 0),
('JWT过期时间(ms)', 'jwt_expiration', '28800000', 1, '8小时', 0)
ON DUPLICATE KEY UPDATE
config_value = VALUES(config_value),
remark = VALUES(remark),
deleted = 0;

UPDATE sys_user SET
  password = @pwd_admin123,
  status = 1, lock_time = NULL, login_fail_count = 0, last_login_fail_time = NULL, update_time = NOW()
WHERE username = 'admin';

UPDATE sys_user SET
  password = @pwd_super123,
  status = 1, lock_time = NULL, login_fail_count = 0, last_login_fail_time = NULL, update_time = NOW()
WHERE username = 'super_admin';

SET @adminId = (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1);

INSERT INTO sys_user
(username, phone, email, password, role_id, permission_customized, status, deleted)
VALUES
('wh_admin_df', '13900000021', 'wh_admin_df@example.com', @pwd_admin123, 2, 0, 1, 0),
('buyer_df',    '13900000031', 'buyer_df@example.com',    @pwd_admin123, 3, 0, 1, 0),
('nurse_df',    '13900000041', 'nurse_df@example.com',    @pwd_admin123, 4, 0, 1, 0),
('vendor_df',   '13900000051', 'vendor_df@example.com',   @pwd_admin123, 5, 0, 1, 0)
ON DUPLICATE KEY UPDATE
phone = VALUES(phone),
email = VALUES(email),
password = VALUES(password),
role_id = VALUES(role_id),
permission_customized = VALUES(permission_customized),
status = VALUES(status),
deleted = VALUES(deleted),
lock_time = NULL,
login_fail_count = 0,
last_login_fail_time = NULL,
update_time = NOW();

-- supplier.phone 须与供应商门户登录账号 sys_user.phone 完全一致（见 SupplierServiceImpl.findSupplierForUser）；答辩供应商A 对应 vendor_df。
INSERT INTO supplier
(name, contact_person, phone, address, credit_code, license_expiry_date, status, audit_status, create_by, deleted)
VALUES
('答辩供应商A', '李甲', '13900000051', '上海市浦东新区A路1号', 'DF26SUPA001', DATE_ADD(CURDATE(), INTERVAL 365 DAY), 1, 1, @adminId, 0),
('答辩供应商B', '王乙', '021-60000002', '上海市浦东新区B路2号', 'DF26SUPB001', DATE_ADD(CURDATE(), INTERVAL 365 DAY), 1, 1, @adminId, 0),
('答辩供应商C(禁用)', '赵丙', '021-60000003', '上海市浦东新区C路3号', 'DF26SUPC001', DATE_ADD(CURDATE(), INTERVAL 365 DAY), 0, 1, @adminId, 0)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
contact_person = VALUES(contact_person),
phone = VALUES(phone),
address = VALUES(address),
license_expiry_date = VALUES(license_expiry_date),
status = VALUES(status),
audit_status = VALUES(audit_status),
create_by = VALUES(create_by),
deleted = VALUES(deleted),
update_time = NOW();

INSERT INTO drug_info
(national_code, trace_code, product_code, drug_name, dosage_form, specification, approval_number, manufacturer,
 expiry_date, is_special, storage_requirement, storage_location, unit, description, create_by, deleted)
VALUES
('DF26DRUG0001', 'DF26TR0001', 'DF26PC0001', '答辩阿莫西林胶囊', '胶囊', '0.25g*24粒', '国药准字DF260001',
 '答辩药业A', DATE_ADD(CURDATE(), INTERVAL 240 DAY), 0, '阴凉干燥', 'A-01-01', '盒', '普通药-稳定库存', @adminId, 0),
('DF26DRUG0002', 'DF26TR0002', 'DF26PC0002', '答辩头孢克肟胶囊', '胶囊', '0.1g*12粒', '国药准字DF260002',
 '答辩药业B', DATE_ADD(CURDATE(), INTERVAL 120 DAY), 0, '阴凉干燥', 'A-01-02', '盒', '普通药-近效期', @adminId, 0),
('DF26DRUG0003', 'DF26TR0003', 'DF26PC0003', '答辩吗啡注射液', '注射液', '1ml:10mg*10支', '国药准字DF260003',
 '答辩药业C', DATE_ADD(CURDATE(), INTERVAL 300 DAY), 1, '双人管理', 'S-01-01', '支', '特殊药-双人规则', @adminId, 0)
ON DUPLICATE KEY UPDATE
trace_code = VALUES(trace_code),
product_code = VALUES(product_code),
drug_name = VALUES(drug_name),
dosage_form = VALUES(dosage_form),
specification = VALUES(specification),
approval_number = VALUES(approval_number),
manufacturer = VALUES(manufacturer),
expiry_date = VALUES(expiry_date),
is_special = VALUES(is_special),
storage_requirement = VALUES(storage_requirement),
storage_location = VALUES(storage_location),
unit = VALUES(unit),
description = VALUES(description),
create_by = VALUES(create_by),
deleted = VALUES(deleted),
update_time = NOW();

SET @supA = (SELECT id FROM supplier WHERE credit_code = 'DF26SUPA001' LIMIT 1);
SET @supB = (SELECT id FROM supplier WHERE credit_code = 'DF26SUPB001' LIMIT 1);
SET @drug1 = (SELECT id FROM drug_info WHERE national_code = 'DF26DRUG0001' LIMIT 1);
SET @drug2 = (SELECT id FROM drug_info WHERE national_code = 'DF26DRUG0002' LIMIT 1);
SET @drug3 = (SELECT id FROM drug_info WHERE national_code = 'DF26DRUG0003' LIMIT 1);

INSERT INTO supplier_drug(supplier_id, drug_id, unit_price, is_active, create_by, deleted)
VALUES
(@supA, @drug1, 12.50, 1, @adminId, 0),
(@supA, @drug2, 15.80, 1, @adminId, 0),
(@supB, @drug3, 88.00, 1, @adminId, 0)
ON DUPLICATE KEY UPDATE
unit_price = VALUES(unit_price),
is_active = VALUES(is_active),
create_by = VALUES(create_by),
deleted = VALUES(deleted),
update_time = NOW();

INSERT INTO inventory(drug_id, batch_number, quantity, expiry_date, storage_location, production_date, manufacturer, remark)
VALUES
(@drug1, 'DF26-BATCH-001', 100, DATE_ADD(CURDATE(), INTERVAL 220 DAY), 'A-01-01', DATE_SUB(CURDATE(), INTERVAL 100 DAY), '答辩药业A', '普通药库存A'),
(@drug1, 'DF26-BATCH-002', 30,  DATE_ADD(CURDATE(), INTERVAL 95 DAY),  'A-01-02', DATE_SUB(CURDATE(), INTERVAL 140 DAY), '答辩药业A', '近效期边界'),
(@drug2, 'DF26-BATCH-003', 20,  DATE_ADD(CURDATE(), INTERVAL 80 DAY),  'A-01-03', DATE_SUB(CURDATE(), INTERVAL 120 DAY), '答辩药业B', '近效期预警'),
(@drug3, 'DF26-BATCH-004', 10,  DATE_ADD(CURDATE(), INTERVAL 260 DAY), 'S-01-01', DATE_SUB(CURDATE(), INTERVAL 60 DAY),  '答辩药业C', '特殊药库存')
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
expiry_date = VALUES(expiry_date),
storage_location = VALUES(storage_location),
production_date = VALUES(production_date),
manufacturer = VALUES(manufacturer),
remark = VALUES(remark),
update_time = NOW();

SET @buyerId = (SELECT id FROM sys_user WHERE username = 'buyer_df' LIMIT 1);
SET @nurseId = (SELECT id FROM sys_user WHERE username = 'nurse_df' LIMIT 1);

INSERT INTO purchase_order
(order_number, supplier_id, purchaser_id, status, expected_delivery_date, logistics_number, ship_date, total_amount, remark)
VALUES
('DF26-PO-001', @supA, @buyerId, 'CONFIRMED', DATE_ADD(CURDATE(), INTERVAL 3 DAY), NULL, NULL, 565.00, '答辩订单1-待入库'),
('DF26-PO-002', @supB, @buyerId, 'SHIPPED',   DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'DF26-LOGI-002', NOW(), 880.00, '答辩订单2-已发货')
ON DUPLICATE KEY UPDATE
supplier_id = VALUES(supplier_id),
purchaser_id = VALUES(purchaser_id),
status = VALUES(status),
expected_delivery_date = VALUES(expected_delivery_date),
logistics_number = VALUES(logistics_number),
ship_date = VALUES(ship_date),
total_amount = VALUES(total_amount),
remark = VALUES(remark),
update_time = NOW();

SET @po1 = (SELECT id FROM purchase_order WHERE order_number = 'DF26-PO-001' LIMIT 1);
SET @po2 = (SELECT id FROM purchase_order WHERE order_number = 'DF26-PO-002' LIMIT 1);

DELETE FROM purchase_order_item WHERE order_id IN (@po1, @po2);

INSERT INTO purchase_order_item(order_id, drug_id, quantity, unit_price, total_price, remark)
VALUES
(@po1, @drug1, 20, 12.50, 250.00, '订单1-药品1'),
(@po1, @drug2, 20, 15.75, 315.00, '订单1-药品2'),
(@po2, @drug3, 10, 88.00, 880.00, '订单2-特殊药');

INSERT INTO outbound_apply(apply_number, applicant_id, department, purpose, status, remark)
VALUES
('DF26-OUT-001', @nurseId, '内科', '答辩演示-常规申领', 'PENDING', 'FIFO 演示'),
('DF26-OUT-002', @nurseId, 'ICU', '答辩演示-库存不足', 'PENDING', '审批失败演示')
ON DUPLICATE KEY UPDATE
applicant_id = VALUES(applicant_id),
department = VALUES(department),
purpose = VALUES(purpose),
status = VALUES(status),
remark = VALUES(remark),
update_time = NOW();

SET @out1 = (SELECT id FROM outbound_apply WHERE apply_number = 'DF26-OUT-001' LIMIT 1);
SET @out2 = (SELECT id FROM outbound_apply WHERE apply_number = 'DF26-OUT-002' LIMIT 1);

DELETE FROM outbound_apply_item WHERE apply_id IN (@out1, @out2);

INSERT INTO outbound_apply_item(apply_id, drug_id, batch_number, quantity, remark)
VALUES
(@out1, @drug1, NULL, 15, '不指定批次'),
(@out2, @drug2, NULL, 9999, '故意超库存');

COMMIT;
