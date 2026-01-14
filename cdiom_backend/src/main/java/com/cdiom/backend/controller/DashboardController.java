package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.mapper.SupplierMapper;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    private final AuthService authService;
    private final SupplierMapper supplierMapper;

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

    /**
     * 获取采购专员仪表盘数据
     * 采购专员可以访问
     */
    @GetMapping("/purchaser")
    public Result<Map<String, Object>> getPurchaserDashboard() {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Map<String, Object> data = dashboardService.getPurchaserDashboard(currentUser.getId());
        return Result.success(data);
    }

    /**
     * 获取医护人员仪表盘数据
     * 医护人员可以访问
     */
    @GetMapping("/medical-staff")
    public Result<Map<String, Object>> getMedicalStaffDashboard() {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Map<String, Object> data = dashboardService.getMedicalStaffDashboard(currentUser.getId());
        return Result.success(data);
    }

    /**
     * 获取供应商仪表盘数据
     * 供应商可以访问
     */
    @GetMapping("/supplier")
    public Result<Map<String, Object>> getSupplierDashboard() {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        // 通过supplier表的createBy字段查询供应商ID
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getCreateBy, currentUser.getId());
        wrapper.eq(Supplier::getDeleted, 0);
        List<Supplier> suppliers = supplierMapper.selectList(wrapper);
        
        if (suppliers == null || suppliers.isEmpty()) {
            return Result.error("未找到关联的供应商信息");
        }
        
        // 取第一个供应商（通常一个供应商用户对应一个供应商记录）
        Long supplierId = suppliers.get(0).getId();
        Map<String, Object> data = dashboardService.getSupplierDashboard(supplierId);
        return Result.success(data);
    }
}


