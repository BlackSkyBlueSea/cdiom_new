package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.mapper.InventoryMapper;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.ExcelExportService;
import com.cdiom.backend.service.InventoryService;
import com.cdiom.backend.service.SysUserService;
import com.cdiom.backend.util.SystemConfigUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@RequiresPermission({"inventory:view", "drug:manage"})
public class InventoryController {

    private final InventoryService inventoryService;
    private final ExcelExportService excelExportService;
    private final AuthService authService;
    private final InventoryMapper inventoryMapper;
    private final SysUserService sysUserService;
    private final SystemConfigUtil systemConfigUtil;

    /**
     * 分页查询库存列表
     */
    @GetMapping
    public Result<Page<Inventory>> getInventoryList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String storageLocation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDateEnd,
            @RequestParam(required = false) Integer isSpecial) {
        Page<Inventory> inventoryPage = inventoryService.getInventoryList(
                page, size, keyword, drugId, batchNumber, storageLocation,
                expiryDateStart, expiryDateEnd, isSpecial);
        return Result.success(inventoryPage);
    }

    /**
     * 库存调整等环节选择第二操作人：启用用户列表（不含密码）。
     * 方法级权限：具备库存查看/调整/调整审批或药品维护之一即可，避免仅缺其中一项时 403。
     */
    @GetMapping("/second-operator-candidates")
    @RequiresPermission({"inventory:view", "inventory:adjust", "inventory:adjust:approve", "drug:manage"})
    public Result<List<SysUser>> listSecondOperatorCandidates() {
        return Result.success(sysUserService.listActiveUsersForSecondOperatorPick(1000));
    }

    /**
     * 根据ID查询库存信息
     */
    @GetMapping("/{id}")
    public Result<Inventory> getInventoryById(@PathVariable Long id) {
        Inventory inventory = inventoryService.getInventoryById(id);
        return Result.success(inventory);
    }

    /**
     * 修改库存批次的存储位置
     */
    @PatchMapping("/{id}/storage-location")
    @RequiresPermission({"drug:manage"})
    public Result<Inventory> updateStorageLocation(
            @PathVariable Long id,
            @Valid @RequestBody StorageLocationUpdateRequest body) {
        inventoryService.updateStorageLocation(id, body.getStorageLocation());
        return Result.success(inventoryService.getInventoryById(id));
    }

    /**
     * 获取近效期预警统计
     */
    @GetMapping("/near-expiry-warning")
    public Result<Map<String, Long>> getNearExpiryWarning() {
        Map<String, Long> warning = inventoryService.getNearExpiryWarning();
        return Result.success(warning);
    }

    /**
     * 近效期黄/红阈值（天），与 sys_config 运行时一致；供库存列表「效期状态」展示。
     * 权限与库存列表相同，无需 config:manage。
     */
    @GetMapping("/expiry-thresholds")
    public Result<Map<String, Integer>> getExpiryThresholdsForDisplay() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("expiryWarningDays", systemConfigUtil.getExpiryWarningDays());
        m.put("expiryCriticalDays", systemConfigUtil.getExpiryCriticalDays());
        return Result.success(m);
    }

    /**
     * 获取库存总量
     */
    @GetMapping("/total")
    public Result<Long> getTotalInventory() {
        Long total = inventoryService.getTotalInventory();
        return Result.success(total);
    }

    /**
     * 导出库存列表到Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInventoryList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String storageLocation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDateEnd,
            @RequestParam(required = false) Integer isSpecial) {
        try {
            // 获取当前用户
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("未登录或登录已过期");
            }

            // 与分页列表使用同一套 JOIN 条件，避免导出与界面筛选不一致
            List<Inventory> inventoryList = inventoryMapper.selectInventoryListWithJoin(
                    keyword, drugId, batchNumber, storageLocation,
                    expiryDateStart, expiryDateEnd, isSpecial);

            // 生成Excel
            byte[] excelBytes = excelExportService.exportInventoryList(inventoryList, currentUser);

            // 设置响应头
            String fileName = "库存列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Data
    public static class StorageLocationUpdateRequest {
        @NotBlank(message = "存储位置不能为空")
        @Size(max = 200, message = "存储位置长度不能超过200个字符")
        private String storageLocation;
    }
}



