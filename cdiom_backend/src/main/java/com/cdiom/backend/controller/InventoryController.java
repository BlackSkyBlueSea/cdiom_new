package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.InventoryMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.ExcelExportService;
import com.cdiom.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ExcelExportService excelExportService;
    private final AuthService authService;
    private final InventoryMapper inventoryMapper;
    private final DrugInfoMapper drugInfoMapper;

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

            // 构建查询条件（与列表查询保持一致）
            LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
            
            // 如果有关键字，需要关联drug_info表查询
            if (StringUtils.hasText(keyword)) {
                // 先查询药品ID
                LambdaQueryWrapper<DrugInfo> drugWrapper = new LambdaQueryWrapper<>();
                drugWrapper.and(w -> w.like(DrugInfo::getDrugName, keyword)
                        .or().like(DrugInfo::getNationalCode, keyword)
                        .or().like(DrugInfo::getApprovalNumber, keyword));
                List<DrugInfo> drugs = drugInfoMapper.selectList(drugWrapper);
                if (!drugs.isEmpty()) {
                    List<Long> drugIds = drugs.stream().map(DrugInfo::getId).collect(Collectors.toList());
                    wrapper.in(Inventory::getDrugId, drugIds);
                } else {
                    // 如果没有找到药品，返回空结果
                    wrapper.eq(Inventory::getId, -1);
                }
            }
            
            if (drugId != null) {
                wrapper.eq(Inventory::getDrugId, drugId);
            }
            
            if (StringUtils.hasText(batchNumber)) {
                wrapper.like(Inventory::getBatchNumber, batchNumber);
            }
            
            if (StringUtils.hasText(storageLocation)) {
                wrapper.like(Inventory::getStorageLocation, storageLocation);
            }
            
            if (expiryDateStart != null) {
                wrapper.ge(Inventory::getExpiryDate, expiryDateStart);
            }
            
            if (expiryDateEnd != null) {
                wrapper.le(Inventory::getExpiryDate, expiryDateEnd);
            }
            
            // 特殊药品筛选（需要关联drug_info表）
            if (isSpecial != null) {
                LambdaQueryWrapper<DrugInfo> drugWrapper = new LambdaQueryWrapper<>();
                drugWrapper.eq(DrugInfo::getIsSpecial, isSpecial);
                List<DrugInfo> drugs = drugInfoMapper.selectList(drugWrapper);
                if (!drugs.isEmpty()) {
                    List<Long> drugIds = drugs.stream().map(DrugInfo::getId).collect(Collectors.toList());
                    wrapper.in(Inventory::getDrugId, drugIds);
                } else {
                    wrapper.eq(Inventory::getId, -1);
                }
            }
            
            // 只查询数量大于0的库存
            wrapper.gt(Inventory::getQuantity, 0);
            
            // 按有效期排序（最早到期的在前，FIFO）
            wrapper.orderByAsc(Inventory::getExpiryDate);
            wrapper.orderByDesc(Inventory::getCreateTime);
            
            // 查询所有数据（不分页）
            List<Inventory> inventoryList = inventoryMapper.selectList(wrapper);

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
}



