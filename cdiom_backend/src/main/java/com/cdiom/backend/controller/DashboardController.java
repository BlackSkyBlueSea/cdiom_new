package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.DashboardService;
import com.cdiom.backend.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final SupplierService supplierService;

    /**
     * 全站汇总统计（用户/角色/配置/药品/登录/操作等），仅系统管理类权限可访问。
     * 仓库工作台所需药品与通知简报由 {@link #getWarehouseDashboard()} 返回，避免低权限账号调用本接口。
     */
    @GetMapping("/statistics")
    @RequiresPermission({"user:manage", "role:manage", "config:manage"})
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
     * 仓库工作台：需具备库存/入库/出库执行或审核/代录/药品维护等之一；不包含仅 outbound:view（医护）或仅 drug:view。
     */
    @GetMapping("/warehouse")
    @RequiresPermission({
            "inventory:view",
            "inbound:view",
            "outbound:execute",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "drug:manage"
    })
    public Result<Map<String, Object>> getWarehouseDashboard() {
        Map<String, Object> data = dashboardService.getWarehouseDashboard();
        return Result.success(data);
    }

    /**
     * 近效期预警明细（药品名称、批次、效期等），与仪表盘卡片区间一致
     *
     * @param level yellow — 介于严重预警天数与预警天数之间；red — 至多到严重预警天数
     */
    @GetMapping("/warehouse/near-expiry-details")
    @RequiresPermission({
            "inventory:view",
            "inbound:view",
            "outbound:execute",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "drug:manage"
    })
    public Result<List<Inventory>> getWarehouseNearExpiryDetails(@RequestParam String level) {
        List<Inventory> list = dashboardService.getWarehouseNearExpiryDetails(level);
        return Result.success(list);
    }

    /**
     * 采购专员仪表盘数据
     */
    @GetMapping("/purchaser")
    @RequiresPermission({"purchase:view", "drug:view", "drug:manage"})
    public Result<Map<String, Object>> getPurchaserDashboard() {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Map<String, Object> data = dashboardService.getPurchaserDashboard(currentUser.getId());
        return Result.success(data);
    }

    /**
     * 医护人员仪表盘数据（与出库功能路由权限 OR 对齐）
     */
    @GetMapping("/medical-staff")
    @RequiresPermission({
            "outbound:view",
            "outbound:apply",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute"
    })
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
     */
    @GetMapping("/supplier")
    @RequiresPermission({"purchase:view", "drug:view", "drug:manage"})
    public Result<Map<String, Object>> getSupplierDashboard() {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Supplier supplier = supplierService.findSupplierForUser(currentUser.getId(), currentUser.getPhone());
        if (supplier == null) {
            return Result.error("未找到关联的供应商信息");
        }
        
        Long supplierId = supplier.getId();
        Map<String, Object> data = dashboardService.getSupplierDashboard(supplierId);
        return Result.success(data);
    }
}


