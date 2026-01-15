package com.cdiom.backend.util;

import com.cdiom.backend.constant.LoginConfigConstant;
import com.cdiom.backend.model.SysConfig;
import com.cdiom.backend.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录防暴力破解配置读取工具
 * 
 * @author cdiom
 */
@Component
@RequiredArgsConstructor
public class LoginConfigUtil {

    private final SysConfigService sysConfigService;

    /**
     * 获取登录失败次数阈值
     */
    public int getLoginFailThreshold() {
        return getConfigValue(LoginConfigConstant.LOGIN_FAIL_THRESHOLD, LoginConfigConstant.DEFAULT_FAIL_THRESHOLD);
    }

    /**
     * 获取失败时间窗口（分钟）
     */
    public int getLoginFailTimeWindow() {
        return getConfigValue(LoginConfigConstant.LOGIN_FAIL_TIME_WINDOW, LoginConfigConstant.DEFAULT_TIME_WINDOW);
    }

    /**
     * 获取锁定时长（小时）
     */
    public int getLoginLockDuration() {
        return getConfigValue(LoginConfigConstant.LOGIN_LOCK_DURATION, LoginConfigConstant.DEFAULT_LOCK_DURATION);
    }

    /**
     * 通用配置读取方法：从数据库获取配置值，异常则返回默认值
     */
    private int getConfigValue(String configKey, int defaultValue) {
        try {
            SysConfig config = sysConfigService.getConfigByKey(configKey);
            if (config == null || config.getConfigValue() == null || config.getConfigValue().trim().isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception e) {
            // 配置值非法/查询失败，返回默认值
            return defaultValue;
        }
    }
}

