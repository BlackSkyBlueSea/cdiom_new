package com.cdiom.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket日志处理器
 * 用于实时推送日志到前端
 * 
 * @author cdiom
 */
@Slf4j
@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(@org.springframework.lang.NonNull WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket连接已建立: {}", session.getId());
        
        // 发送欢迎消息
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        welcome.put("level", "INFO");
        welcome.put("message", "日志流连接成功");
        sendMessage(session, welcome);
    }

    @Override
    public void afterConnectionClosed(@org.springframework.lang.NonNull WebSocketSession session, @org.springframework.lang.NonNull CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket连接已关闭: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@org.springframework.lang.NonNull WebSocketSession session, @org.springframework.lang.NonNull TextMessage message) throws Exception {
        // 处理客户端发送的消息（如果需要）
        String payload = message.getPayload();
        log.debug("收到客户端消息: {}", payload);
    }

    /**
     * 定期发送心跳（可选，用于保持连接）
     * 实际日志通过Logback Appender推送
     */
    @Scheduled(fixedRate = 30000) // 每30秒发送一次心跳
    public void sendHeartbeat() {
        if (sessions.isEmpty()) {
            return;
        }

        // 发送心跳消息
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        heartbeat.put("level", "DEBUG");
        heartbeat.put("message", "WebSocket连接正常");
        heartbeat.put("type", "heartbeat");

        // 发送给所有连接的客户端
        broadcastMessage(heartbeat);
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            if (json != null) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 广播消息给所有连接的客户端
     */
    public void broadcastMessage(Map<String, Object> message) {
        List<WebSocketSession> closedSessions = new ArrayList<>();
        
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendMessage(session, message);
            } else {
                closedSessions.add(session);
            }
        }
        
        // 移除已关闭的会话
        sessions.removeAll(closedSessions);
    }
}

