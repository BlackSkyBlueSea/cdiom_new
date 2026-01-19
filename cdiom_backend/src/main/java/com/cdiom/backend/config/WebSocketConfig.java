package com.cdiom.backend.config;

import com.cdiom.backend.controller.LogWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * 用于实时日志流
 * 
 * @author cdiom
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器，允许跨域
        registry.addHandler(logWebSocketHandler(), "/api/v1/logs/stream")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    @NonNull
    public LogWebSocketHandler logWebSocketHandler() {
        return new LogWebSocketHandler();
    }
}

