package com.cdiom.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 并发配置类
 * 配置Web MVC的异步支持
 * 
 * @author cdiom
 */
@Configuration
public class ConcurrencyConfig implements WebMvcConfigurer {

    /**
     * 配置异步支持
     * 用于支持异步请求处理
     */
    @Override
    public void configureAsyncSupport(@NonNull AsyncSupportConfigurer configurer) {
        // 设置默认超时时间（30秒）
        configurer.setDefaultTimeout(30000);
        
        // 设置任务执行器（使用默认的SimpleAsyncTaskExecutor）
        // 如果需要自定义，可以注入ThreadPoolTaskExecutor
    }
}

