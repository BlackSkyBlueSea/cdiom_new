USE cdiom_db;

-- 修复角色表中文数据
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

-- 修复系统配置表中文数据
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

-- 修复通知公告表中文数据
UPDATE sys_notice SET 
    notice_title = '欢迎使用CDIOM系统',
    notice_content = '欢迎使用临床药品出入库管理系统！'
WHERE id = 1;

UPDATE sys_notice SET 
    notice_title = '系统使用说明',
    notice_content = '请各用户按照系统规范操作，确保数据准确性。'
WHERE id = 2;


