package com.cdiom.backend.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 初始化WebSocketLogAppender的ApplicationContext
 * 
 * @author cdiom
 */
@Component
public class LogAppenderInitializer implements ApplicationContextAware {

    @Override
    public void setApplicationContext(@org.springframework.lang.NonNull ApplicationContext applicationContext) throws BeansException {
        // 将ApplicationContext传递给WebSocketLogAppender
        WebSocketLogAppender.setApplicationContextStatic(applicationContext);
    }
}

