package com.cdiom.backend.controller;

import com.cdiom.backend.common.Result;
import com.cdiom.backend.service.BackendMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后端监控控制器
 * 提供系统信息、日志查询等监控功能
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BackendMonitorController {

    private final BackendMonitorService backendMonitorService;

    /**
     * 获取系统信息
     * 无需登录即可访问（公开接口）
     */
    @GetMapping("/system/info")
    public Result<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> systemInfo = backendMonitorService.getSystemInfo();
            return Result.success(systemInfo);
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return Result.error("获取系统信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近的日志
     * 无需登录即可访问（公开接口）
     * 
     * @param limit 返回的日志条数，默认100
     * @param level 日志级别过滤（可选）
     */
    @GetMapping("/logs/recent")
    public Result<List<Map<String, Object>>> getRecentLogs(
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String level) {
        try {
            List<Map<String, Object>> logs = backendMonitorService.getRecentLogs(limit, level);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("获取日志失败", e);
            return Result.error("获取日志失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查端点
     * 无需登录即可访问（公开接口）
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        try {
            Map<String, Object> health = backendMonitorService.getHealthStatus();
            return Result.success(health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return Result.error("健康检查失败: " + e.getMessage());
        }
    }
}

