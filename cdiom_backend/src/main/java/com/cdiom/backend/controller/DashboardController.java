package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表盘控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取统计数据
     * 所有登录用户都可以访问
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = dashboardService.getStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取登录趋势
     * 仅系统管理员可以访问
     */
    @GetMapping("/login-trend")
    @RequiresPermission("log:login:view")
    public Result<Map<String, Object>> getLoginTrend() {
        Map<String, Object> trend = dashboardService.getLoginTrend();
        return Result.success(trend);
    }

    /**
     * 获取操作日志统计
     * 仅系统管理员可以访问
     */
    @GetMapping("/operation-statistics")
    @RequiresPermission("log:operation:view")
    public Result<Map<String, Object>> getOperationStatistics() {
        Map<String, Object> statistics = dashboardService.getOperationLogStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取仓库管理员仪表盘数据
     * 仓库管理员可以访问
     */
    @GetMapping("/warehouse")
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<Map<String, Object>> getWarehouseDashboard() {
        Map<String, Object> data = dashboardService.getWarehouseDashboard();
        return Result.success(data);
    }
}


