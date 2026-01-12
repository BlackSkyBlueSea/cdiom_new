package com.cdiom.backend.service;

import java.util.Map;

/**
 * 仪表盘服务接口
 * 
 * @author cdiom
 */
public interface DashboardService {

    /**
     * 获取统计数据
     */
    Map<String, Object> getStatistics();

    /**
     * 获取最近登录趋势（最近7天）
     */
    Map<String, Object> getLoginTrend();

    /**
     * 获取操作日志统计（最近7天）
     */
    Map<String, Object> getOperationLogStatistics();

    /**
     * 获取仓库管理员仪表盘数据
     * 包括：近效期预警、待办任务、今日出入库统计、库存总量
     */
    Map<String, Object> getWarehouseDashboard();
}


