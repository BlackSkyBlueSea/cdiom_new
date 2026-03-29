package com.cdiom.backend.constant;

/**
 * 系统参数表 sys_config 中与业务映射相关的键名（与前端参数配置表一致）
 */
public final class SysConfigKeys {

    public static final String EXPIRY_WARNING_DAYS = "expiry_warning_days";
    public static final String EXPIRY_CRITICAL_DAYS = "expiry_critical_days";
    public static final String LOG_RETENTION_YEARS = "log_retention_years";
    public static final String JWT_EXPIRATION = "jwt_expiration";

    private SysConfigKeys() {
    }
}
