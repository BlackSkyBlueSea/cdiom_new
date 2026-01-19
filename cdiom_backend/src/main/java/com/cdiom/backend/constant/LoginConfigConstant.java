package com.cdiom.backend.constant;

/**
 * 登录配置常量
 * 
 * @author cdiom
 */
public class LoginConfigConstant {
    // 配置键名
    public static final String LOGIN_FAIL_THRESHOLD = "login.fail.threshold";
    public static final String LOGIN_FAIL_TIME_WINDOW = "login.fail.time.window";
    public static final String LOGIN_LOCK_DURATION = "login.lock.duration";

    // 默认值
    public static final int DEFAULT_FAIL_THRESHOLD = 5;
    public static final int DEFAULT_TIME_WINDOW = 10;
    public static final int DEFAULT_LOCK_DURATION = 1;
}


