package com.cdiom.backend.util;

import com.cdiom.backend.model.SysConfig;
import com.cdiom.backend.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 系统配置读取工具类
 * 优先从数据库读取配置，如果数据库中没有则使用配置文件中的默认值
 * 使用本地缓存提高性能，支持配置更新后立即生效
 * 
 * @author cdiom
 */
@Slf4j
@Component
public class SystemConfigUtil {

    // 使用 @Lazy 避免循环依赖：SystemConfigUtil 依赖 SysConfigService，SysConfigServiceImpl 依赖 SystemConfigUtil
    private final SysConfigService sysConfigService;

    /**
     * 构造器注入，使用 @Lazy 延迟加载 SysConfigService 以解决循环依赖
     */
    public SystemConfigUtil(@Lazy SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    // 配置文件中的默认值
    @Value("${system.config.expiry-warning-days:180}")
    private Integer defaultExpiryWarningDays;

    @Value("${system.config.expiry-critical-days:90}")
    private Integer defaultExpiryCriticalDays;

    // 本地缓存：key=configKey, value=配置值
    private final ConcurrentHashMap<String, Integer> configCache = new ConcurrentHashMap<>();
    
    // 读写锁，保证缓存更新的线程安全
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    /**
     * 获取药品过期预警天数
     * 优先从数据库读取，如果数据库中没有则使用配置文件中的默认值
     * 使用缓存提高性能，配置更新后需要调用 clearCache() 清除缓存
     */
    public Integer getExpiryWarningDays() {
        return getConfigValue("expiry_warning_days", defaultExpiryWarningDays);
    }

    /**
     * 获取药品过期严重预警天数
     * 优先从数据库读取，如果数据库中没有则使用配置文件中的默认值
     * 使用缓存提高性能，配置更新后需要调用 clearCache() 清除缓存
     */
    public Integer getExpiryCriticalDays() {
        return getConfigValue("expiry_critical_days", defaultExpiryCriticalDays);
    }

    /**
     * 通用配置读取方法：从数据库获取配置值，异常则返回默认值
     * 使用缓存机制，减少数据库查询次数
     */
    private Integer getConfigValue(String configKey, Integer defaultValue) {
        // 先尝试从缓存读取
        cacheLock.readLock().lock();
        try {
            Integer cachedValue = configCache.get(configKey);
            if (cachedValue != null) {
                return cachedValue;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // 缓存未命中，从数据库读取
        cacheLock.writeLock().lock();
        try {
            // 双重检查，避免并发时重复查询数据库
            Integer cachedValue = configCache.get(configKey);
            if (cachedValue != null) {
                return cachedValue;
            }

            // 从数据库查询
            Integer value = defaultValue;
            try {
                SysConfig config = sysConfigService.getConfigByKey(configKey);
                if (config != null && config.getConfigValue() != null && !config.getConfigValue().trim().isEmpty()) {
                    value = Integer.parseInt(config.getConfigValue().trim());
                }
            } catch (Exception e) {
                log.warn("读取系统配置失败，使用默认值: configKey={}, error={}", configKey, e.getMessage());
            }

            // 存入缓存
            configCache.put(configKey, value);
            return value;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 清除指定配置的缓存
     * 当系统配置被更新时，应该调用此方法清除缓存，使新配置立即生效
     * 
     * @param configKey 配置键名，如果为null则清除所有缓存
     */
    public void clearCache(String configKey) {
        cacheLock.writeLock().lock();
        try {
            if (configKey != null) {
                configCache.remove(configKey);
                log.info("已清除系统配置缓存: configKey={}", configKey);
            } else {
                configCache.clear();
                log.info("已清除所有系统配置缓存");
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 清除所有配置缓存
     */
    public void clearAllCache() {
        clearCache(null);
    }
}

