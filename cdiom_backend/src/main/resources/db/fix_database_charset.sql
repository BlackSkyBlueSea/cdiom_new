-- ============================================
-- CDIOM数据库中文乱码修复脚本
-- 功能：修复数据库、表和字段的字符集，解决中文显示为问号的问题
-- 使用方法：在MySQL Workbench中执行此脚本
-- ============================================

USE cdiom_db;

-- 设置当前会话的字符集
SET NAMES utf8mb4;

-- ============================================
-- 第一步：修改数据库字符集
-- ============================================
ALTER DATABASE cdiom_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================
-- 第二步：修改所有表的字符集和排序规则
-- ============================================

-- 修改系统角色表
ALTER TABLE sys_role CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_role MODIFY COLUMN role_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称';
ALTER TABLE sys_role MODIFY COLUMN role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色代码';
ALTER TABLE sys_role MODIFY COLUMN description VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述';

-- 修改系统用户表
ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_user MODIFY COLUMN username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名';
ALTER TABLE sys_user MODIFY COLUMN phone VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号';
ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）';

-- 修改系统参数配置表
ALTER TABLE sys_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_config MODIFY COLUMN config_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数名称';
ALTER TABLE sys_config MODIFY COLUMN config_key VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数键名';
ALTER TABLE sys_config MODIFY COLUMN config_value TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数值';
ALTER TABLE sys_config MODIFY COLUMN remark VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注';

-- 修改系统通知公告表
ALTER TABLE sys_notice CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_notice MODIFY COLUMN notice_title VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题';
ALTER TABLE sys_notice MODIFY COLUMN notice_content TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公告内容';

-- 修改操作日志表
ALTER TABLE operation_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE operation_log MODIFY COLUMN username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人用户名';
ALTER TABLE operation_log MODIFY COLUMN module VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作模块';
ALTER TABLE operation_log MODIFY COLUMN operation_type VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型：INSERT/UPDATE/DELETE/SELECT';
ALTER TABLE operation_log MODIFY COLUMN operation_content VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作内容';
ALTER TABLE operation_log MODIFY COLUMN request_url VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL';
ALTER TABLE operation_log MODIFY COLUMN request_params TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求参数';
ALTER TABLE operation_log MODIFY COLUMN ip VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作IP';
ALTER TABLE operation_log MODIFY COLUMN error_msg TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息';

-- 修改登录日志表
ALTER TABLE login_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE login_log MODIFY COLUMN username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名';
ALTER TABLE login_log MODIFY COLUMN ip VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录IP';
ALTER TABLE login_log MODIFY COLUMN location VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录地点';
ALTER TABLE login_log MODIFY COLUMN browser VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器类型';
ALTER TABLE login_log MODIFY COLUMN os VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统';
ALTER TABLE login_log MODIFY COLUMN msg VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录消息';

-- ============================================
-- 第三步：重新更新所有包含中文的数据
-- ============================================

-- 更新系统角色表的中文数据
UPDATE sys_role SET 
    role_name = '系统管理员',
    description = '系统管理员，拥有所有权限'
WHERE id = 1;

UPDATE sys_role SET 
    role_name = '仓库管理员',
    description = '仓库管理员，负责药品入库、出库、库存管理'
WHERE id = 2;

UPDATE sys_role SET 
    role_name = '采购专员',
    description = '采购专员，负责供应商管理和采购订单管理'
WHERE id = 3;

UPDATE sys_role SET 
    role_name = '医护人员',
    description = '医护人员，负责药品申领'
WHERE id = 4;

UPDATE sys_role SET 
    role_name = '供应商',
    description = '供应商，负责订单管理'
WHERE id = 5;

-- 更新系统参数配置表的中文数据
UPDATE sys_config SET 
    config_name = '近效期预警阈值（天）',
    remark = '药品有效期至当前日期的天数差，小于等于此值将预警'
WHERE config_key = 'expiry_warning_days';

UPDATE sys_config SET 
    config_name = '红色预警阈值（天）',
    remark = '药品有效期至当前日期的天数差，小于等于此值将红色预警'
WHERE config_key = 'expiry_critical_days';

UPDATE sys_config SET 
    config_name = '日志保留期（年）',
    remark = '操作日志和登录日志的保留年限'
WHERE config_key = 'log_retention_years';

UPDATE sys_config SET 
    config_name = 'JWT过期时间（毫秒）',
    remark = 'JWT Token的有效期，默认8小时'
WHERE config_key = 'jwt_expiration';

-- 更新系统通知公告表的中文数据
UPDATE sys_notice SET 
    notice_title = '欢迎使用CDIOM系统',
    notice_content = '欢迎使用临床药品出入库管理系统！'
WHERE id = 1;

UPDATE sys_notice SET 
    notice_title = '系统使用说明',
    notice_content = '请各用户按照系统规范操作，确保数据准确性。'
WHERE id = 2;

-- ============================================
-- 第四步：验证修复结果（可选执行）
-- ============================================

-- 查看数据库字符集
-- SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME 
-- FROM information_schema.SCHEMATA 
-- WHERE SCHEMA_NAME = 'cdiom_db';

-- 查看表的字符集
-- SELECT TABLE_NAME, TABLE_COLLATION 
-- FROM information_schema.TABLES 
-- WHERE TABLE_SCHEMA = 'cdiom_db';

-- 查看字段的字符集
-- SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME 
-- FROM information_schema.COLUMNS 
-- WHERE TABLE_SCHEMA = 'cdiom_db' 
-- AND CHARACTER_SET_NAME IS NOT NULL;

-- ============================================
-- 修复完成提示
-- ============================================
SELECT '数据库字符集修复完成！请刷新MySQL Workbench查看中文数据。' AS '修复状态';




