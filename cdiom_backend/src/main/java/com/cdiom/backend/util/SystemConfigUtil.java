package com.cdiom.backend.util;

import com.cdiom.backend.constant.SysConfigKeys;
import com.cdiom.backend.model.SysConfig;
import com.cdiom.backend.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 系统配置读取工具类（与参数配置表 sys_config 映射）
 * 优先从数据库读取，无记录或非法值时使用配置文件默认值
 */
@Slf4j
@Component
public class SystemConfigUtil {

    private static final long JWT_EXPIRATION_MIN_MS = 60_000L;
    private static final long JWT_EXPIRATION_MAX_MS = 365L * 24 * 60 * 60 * 1000;
    private static final int LOG_RETENTION_MIN_YEARS = 1;
    private static final int LOG_RETENTION_MAX_YEARS = 50;

    private final SysConfigService sysConfigService;

    public SystemConfigUtil(@Lazy SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Value("${system.config.expiry-warning-days:180}")
    private Integer defaultExpiryWarningDays;

    @Value("${system.config.expiry-critical-days:90}")
    private Integer defaultExpiryCriticalDays;

    @Value("${system.config.log-retention-years:5}")
    private Integer defaultLogRetentionYears;

    @Value("${jwt.expiration:28800000}")
    private Long defaultJwtExpirationMs;

    private final ConcurrentHashMap<String, Integer> intCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> longCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public Integer getExpiryWarningDays() {
        return getInt(SysConfigKeys.EXPIRY_WARNING_DAYS, defaultExpiryWarningDays);
    }

    public Integer getExpiryCriticalDays() {
        return getInt(SysConfigKeys.EXPIRY_CRITICAL_DAYS, defaultExpiryCriticalDays);
    }

    /**
     * 操作/登录日志保留年限（用于定时清理）
     */
    public int getLogRetentionYears() {
        int v = getInt(SysConfigKeys.LOG_RETENTION_YEARS, defaultLogRetentionYears);
        if (v < LOG_RETENTION_MIN_YEARS) {
            return LOG_RETENTION_MIN_YEARS;
        }
        if (v > LOG_RETENTION_MAX_YEARS) {
            return LOG_RETENTION_MAX_YEARS;
        }
        return v;
    }

    /**
     * JWT 有效期（毫秒），与参数表 jwt_expiration 一致；非法值回退为配置文件默认值
     */
    public long getJwtExpirationMillis() {
        long v = getLong(SysConfigKeys.JWT_EXPIRATION, defaultJwtExpirationMs);
        if (v < JWT_EXPIRATION_MIN_MS || v > JWT_EXPIRATION_MAX_MS) {
            log.warn("jwt_expiration 超出允许范围 [{}, {}] ms，使用默认 {}", JWT_EXPIRATION_MIN_MS, JWT_EXPIRATION_MAX_MS, defaultJwtExpirationMs);
            return defaultJwtExpirationMs;
        }
        return v;
    }

    private Integer getInt(String configKey, Integer defaultValue) {
        cacheLock.readLock().lock();
        try {
            Integer cached = intCache.get(configKey);
            if (cached != null) {
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            Integer cached = intCache.get(configKey);
            if (cached != null) {
                return cached;
            }
            int value = defaultValue;
            try {
                SysConfig config = sysConfigService.getConfigByKey(configKey);
                if (config != null && config.getConfigValue() != null && !config.getConfigValue().trim().isEmpty()) {
                    value = Integer.parseInt(config.getConfigValue().trim());
                }
            } catch (Exception e) {
                log.warn("读取系统配置失败，使用默认值: configKey={}, error={}", configKey, e.getMessage());
            }
            intCache.put(configKey, value);
            return value;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private long getLong(String configKey, Long defaultValue) {
        cacheLock.readLock().lock();
        try {
            Long cached = longCache.get(configKey);
            if (cached != null) {
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            Long cached = longCache.get(configKey);
            if (cached != null) {
                return cached;
            }
            long value = defaultValue;
            try {
                SysConfig config = sysConfigService.getConfigByKey(configKey);
                if (config != null && config.getConfigValue() != null && !config.getConfigValue().trim().isEmpty()) {
                    value = Long.parseLong(config.getConfigValue().trim());
                }
            } catch (Exception e) {
                log.warn("读取系统配置失败，使用默认值: configKey={}, error={}", configKey, e.getMessage());
            }
            longCache.put(configKey, value);
            return value;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 清除指定键的缓存；传入 null 则清空全部
     */
    public void clearCache(String configKey) {
        cacheLock.writeLock().lock();
        try {
            if (configKey != null) {
                intCache.remove(configKey);
                longCache.remove(configKey);
                log.info("已清除系统配置缓存: configKey={}", configKey);
            } else {
                intCache.clear();
                longCache.clear();
                log.info("已清除所有系统配置缓存");
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void clearAllCache() {
        clearCache(null);
    }
}
