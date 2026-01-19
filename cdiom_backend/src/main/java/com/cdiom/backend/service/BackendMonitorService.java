package com.cdiom.backend.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 后端监控服务
 * 
 * @author cdiom
 */
@Slf4j
@Service
public class BackendMonitorService {

    @Value("${spring.application.name:cdiom-backend}")
    private String applicationName;

    @Value("${server.port:8080}")
    private Integer serverPort;

    /**
     * 获取系统信息
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 应用信息
        info.put("applicationName", applicationName);
        info.put("serverPort", serverPort);
        info.put("backendUrl", "http://localhost:" + serverPort);
        info.put("apiBaseUrl", "/api/v1");
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // JVM信息
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("javaVersion", System.getProperty("java.version"));
        jvmInfo.put("javaVendor", System.getProperty("java.vendor"));
        jvmInfo.put("javaHome", System.getProperty("java.home"));
        jvmInfo.put("osName", System.getProperty("os.name"));
        jvmInfo.put("osVersion", System.getProperty("os.version"));
        jvmInfo.put("osArch", System.getProperty("os.arch"));
        jvmInfo.put("uptime", runtimeBean.getUptime() / 1000); // 秒
        jvmInfo.put("startTime", new Date(runtimeBean.getStartTime()));
        
        // 内存信息
        Map<String, Object> memoryInfo = new HashMap<>();
        long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long freeMemory = totalMemory - usedMemory;
        memoryInfo.put("total", totalMemory);
        memoryInfo.put("used", usedMemory);
        memoryInfo.put("free", freeMemory);
        memoryInfo.put("usagePercent", (usedMemory * 100.0 / totalMemory));
        
        jvmInfo.put("memory", memoryInfo);
        info.put("jvm", jvmInfo);
        
        return info;
    }

    /**
     * 获取健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // 检查内存使用率
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        double usagePercent = (usedMemory * 100.0 / totalMemory);
        
        Map<String, Object> details = new HashMap<>();
        details.put("memory", Map.of(
            "usagePercent", String.format("%.2f%%", usagePercent),
            "status", usagePercent > 90 ? "WARNING" : "OK"
        ));
        health.put("details", details);
        
        return health;
    }

    /**
     * 获取最近的日志
     * 
     * @param limit 返回的日志条数
     * @param level 日志级别过滤（可选）
     */
    public List<Map<String, Object>> getRecentLogs(Integer limit, String level) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try {
            // 尝试从日志文件读取（如果存在）
            String logPath = System.getProperty("user.dir") + "/logs";
            File logDir = new File(logPath);
            
            if (logDir.exists() && logDir.isDirectory()) {
                // 查找最新的日志文件
                File[] logFiles = logDir.listFiles((dir, name) -> 
                    name.endsWith(".log") || name.endsWith(".txt")
                );
                
                if (logFiles != null && logFiles.length > 0) {
                    // 按修改时间排序，取最新的
                    Arrays.sort(logFiles, (f1, f2) -> 
                        Long.compare(f2.lastModified(), f1.lastModified())
                    );
                    
                    File latestLogFile = logFiles[0];
                    logs = readLogsFromFile(latestLogFile, limit, level);
                }
            }
            
            // 如果从文件读取失败或没有日志文件，返回内存中的日志
            if (logs.isEmpty()) {
                logs = getMemoryLogs(limit, level);
            }
            
        } catch (Exception e) {
            log.error("读取日志文件失败", e);
            // 如果读取文件失败，返回内存日志
            logs = getMemoryLogs(limit, level);
        }
        
        return logs;
    }

    /**
     * 检查是否是堆栈跟踪行
     */
    private boolean isStackTraceLine(String line) {
        if (StrUtil.isBlank(line)) return false;
        String trimmed = line.trim();
        // 检查是否以 "at " 开头
        if (trimmed.startsWith("at ") || trimmed.startsWith("Caused by:")) {
            return true;
        }
        // 检查是否以制表符或大量空格开头
        if (line.startsWith("\t") || (line.length() > 4 && line.substring(0, 4).matches("^\\s+$"))) {
            return true;
        }
        return false;
    }

    /**
     * 从日志文件读取日志
     */
    private List<Map<String, Object>> readLogsFromFile(File logFile, Integer limit, String level) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try {
            // 读取文件的最后N行
            List<String> lines = FileUtil.readLines(logFile, StandardCharsets.UTF_8);
            
            // 从后往前读取，最多读取 limit * 2 行（因为可能被过滤）
            int startIndex = Math.max(0, lines.size() - limit * 2);
            List<String> recentLines = lines.subList(startIndex, lines.size());
            
            // 解析日志行 - 支持多种格式
            // 格式1: 2026-01-19 09:19:48.496 [thread] LEVEL logger - message
            // 格式2: [2026/01/19 09:19:48.496] [LEVEL] message
            // 格式3: 2026-01-19 09:19:48.496 [LEVEL] message
            Pattern logPattern1 = Pattern.compile(
                "(\\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[(.*?)\\]\\s+(\\w+)\\s+(.*?)\\s+-\\s+(.*)"
            );
            Pattern logPattern2 = Pattern.compile(
                "\\[(\\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\]\\s+\\[(\\w+)\\]\\s+(.*)"
            );
            Pattern logPattern3 = Pattern.compile(
                "(\\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[(\\w+)\\]\\s+(.*)"
            );
            for (String line : recentLines) {
                if (StrUtil.isBlank(line)) continue;
                
                // 检查是否是堆栈跟踪行
                if (isStackTraceLine(line)) {
                    // 堆栈跟踪行，附加到上一个日志条目
                    if (!logs.isEmpty()) {
                        Map<String, Object> lastLog = logs.get(logs.size() - 1);
                        String lastMessage = (String) lastLog.get("message");
                        if (lastMessage == null) {
                            lastMessage = "";
                        }
                        lastLog.put("message", lastMessage + "\n" + line);
                    } else {
                        // 如果没有上一个日志，创建新的
                        Map<String, Object> logEntry = new HashMap<>();
                        logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        logEntry.put("level", "DEBUG");
                        logEntry.put("message", line);
                        logs.add(logEntry);
                    }
                    continue;
                }
                
                java.util.regex.Matcher matcher1 = logPattern1.matcher(line);
                java.util.regex.Matcher matcher2 = logPattern2.matcher(line);
                java.util.regex.Matcher matcher3 = logPattern3.matcher(line);
                
                Map<String, Object> logEntry = new HashMap<>();
                boolean matched = false;
                
                if (matcher1.matches()) {
                    // 格式1: 2026-01-19 09:19:48.496 [thread] LEVEL logger - message
                    String timestamp = matcher1.group(1).replace('/', '-');
                    String thread = matcher1.group(2);
                    String logLevel = matcher1.group(3);
                    String logger = matcher1.group(4);
                    String message = matcher1.group(5);
                    
                    logEntry.put("timestamp", timestamp);
                    logEntry.put("level", logLevel);
                    logEntry.put("thread", thread);
                    logEntry.put("logger", logger);
                    logEntry.put("message", message);
                    matched = true;
                } else if (matcher2.matches()) {
                    // 格式2: [2026/01/19 09:19:48.496] [LEVEL] message
                    String timestamp = matcher2.group(1).replace('/', '-');
                    String logLevel = matcher2.group(2);
                    String message = matcher2.group(3);
                    
                    logEntry.put("timestamp", timestamp);
                    logEntry.put("level", logLevel);
                    logEntry.put("message", message);
                    matched = true;
                } else if (matcher3.matches()) {
                    // 格式3: 2026-01-19 09:19:48.496 [LEVEL] message
                    String timestamp = matcher3.group(1).replace('/', '-');
                    String logLevel = matcher3.group(2);
                    String message = matcher3.group(3);
                    
                    logEntry.put("timestamp", timestamp);
                    logEntry.put("level", logLevel);
                    logEntry.put("message", message);
                    matched = true;
                }
                
                if (!matched) {
                    // 如果格式不匹配，尝试简单解析
                    // 查找常见的日志级别
                    String upperLine = line.toUpperCase();
                    String detectedLevel = "INFO";
                    if (upperLine.contains("[ERROR]") || upperLine.contains("ERROR")) {
                        detectedLevel = "ERROR";
                    } else if (upperLine.contains("[WARN]") || upperLine.contains("WARNING")) {
                        detectedLevel = "WARN";
                    } else if (upperLine.contains("[DEBUG]")) {
                        detectedLevel = "DEBUG";
                    }
                    
                    logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    logEntry.put("level", detectedLevel);
                    logEntry.put("message", line);
                }
                
                // 级别过滤
                String logLevel = (String) logEntry.get("level");
                if (StrUtil.isNotBlank(level) && !logLevel.equalsIgnoreCase(level)) {
                    continue;
                }
                
                logs.add(logEntry);
                
                if (logs.size() >= limit) {
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("解析日志文件失败", e);
        }
        
        return logs;
    }

    /**
     * 获取内存中的日志（模拟日志）
     * 如果无法从文件读取，返回一些示例日志
     */
    private List<Map<String, Object>> getMemoryLogs(Integer limit, String level) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        // 生成一些示例日志
        String[] levels = {"INFO", "DEBUG", "WARN", "ERROR"};
        String[] messages = {
            "系统启动成功",
            "数据库连接池初始化完成",
            "JWT Token验证通过",
            "用户登录成功",
            "API请求处理完成",
            "定时任务执行中",
            "缓存刷新完成"
        };
        
        for (int i = 0; i < Math.min(limit, 20); i++) {
            String logLevel = levels[i % levels.length];
            
            // 级别过滤
            if (StrUtil.isNotBlank(level) && !logLevel.equalsIgnoreCase(level)) {
                continue;
            }
            
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().minusSeconds(i).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            logEntry.put("level", logLevel);
            logEntry.put("thread", "main");
            logEntry.put("logger", "com.cdiom.backend");
            logEntry.put("message", messages[i % messages.length] + " (示例日志)");
            logs.add(logEntry);
        }
        
        return logs;
    }
}

