package com.cdiom.backend.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.cdiom.backend.controller.LogWebSocketHandler;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket日志Appender
 * 将日志实时推送到WebSocket客户端
 * 这是最简便的方法，直接捕获所有控制台输出
 * 
 * @author cdiom
 */
public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {

    private static LogWebSocketHandler logWebSocketHandler;
    private static org.springframework.context.ApplicationContext applicationContext;

    @Setter
    private String applicationName = "cdiom-backend";

    /**
     * 设置ApplicationContext（由Spring调用）
     */
    public static void setApplicationContextStatic(org.springframework.context.ApplicationContext context) {
        applicationContext = context;
        // 延迟获取LogWebSocketHandler
        try {
            if (context != null) {
                logWebSocketHandler = context.getBean(LogWebSocketHandler.class);
            }
        } catch (Exception e) {
            // Bean可能还未初始化
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            // 如果LogWebSocketHandler还未初始化，尝试获取
            if (logWebSocketHandler == null && applicationContext != null) {
                try {
                    logWebSocketHandler = applicationContext.getBean(LogWebSocketHandler.class);
                } catch (Exception e) {
                    // 仍然未初始化，跳过
                    return;
                }
            }

            // 如果没有WebSocket连接，不处理
            if (logWebSocketHandler == null) {
                return;
            }

            // 构建日志消息
            Map<String, Object> logEntry = new HashMap<>();
            
            // 时间戳 - 使用日志事件的时间，格式化为 [yyyy/MM/dd HH:mm:ss.SSS]
            String timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getTimeStamp()),
                ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"));
            
            logEntry.put("timestamp", timestamp);
            
            // 日志级别
            logEntry.put("level", event.getLevel().toString());
            
            // 线程名
            logEntry.put("thread", event.getThreadName());
            
            // Logger名称
            logEntry.put("logger", event.getLoggerName());
            
            // 日志消息
            String message = event.getFormattedMessage();
            
            // 如果有异常信息，添加到消息中
            if (event.getThrowableProxy() != null) {
                StringBuilder sb = new StringBuilder(message);
                sb.append("\n");
                sb.append(event.getThrowableProxy().getClassName());
                if (event.getThrowableProxy().getMessage() != null) {
                    sb.append(": ").append(event.getThrowableProxy().getMessage());
                }
                
                // 添加堆栈跟踪
                if (event.getThrowableProxy().getStackTraceElementProxyArray() != null) {
                    for (int i = 0; i < Math.min(50, event.getThrowableProxy().getStackTraceElementProxyArray().length); i++) {
                        sb.append("\n\tat ").append(event.getThrowableProxy().getStackTraceElementProxyArray()[i].toString());
                    }
                }
                
                message = sb.toString();
            }
            
            logEntry.put("message", message);
            
            // 通过WebSocket推送
            logWebSocketHandler.broadcastMessage(logEntry);
            
        } catch (Exception e) {
            // 避免日志Appender自身出错导致循环
            System.err.println("WebSocketLogAppender error: " + e.getMessage());
        }
    }
}

