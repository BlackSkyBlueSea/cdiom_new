package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 库存管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class InventoryController {

    private final InventoryService inventoryService;

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
     * 根据ID查询库存信息
     */
    @GetMapping("/{id}")
    public Result<Inventory> getInventoryById(@PathVariable Long id) {
        Inventory inventory = inventoryService.getInventoryById(id);
        return Result.success(inventory);
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
     * 获取库存总量
     */
    @GetMapping("/total")
    public Result<Long> getTotalInventory() {
        Long total = inventoryService.getTotalInventory();
        return Result.success(total);
    }
}

