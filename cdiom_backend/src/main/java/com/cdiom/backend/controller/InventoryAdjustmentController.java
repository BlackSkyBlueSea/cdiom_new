package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.InventoryAdjustment;
import com.cdiom.backend.service.InventoryAdjustmentService;
import com.cdiom.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 库存调整管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/inventory-adjustments")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService inventoryAdjustmentService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询库存调整记录列表
     */
    @GetMapping
    public Result<Page<InventoryAdjustment>> getInventoryAdjustmentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String adjustmentType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Page<InventoryAdjustment> adjustmentPage = inventoryAdjustmentService.getInventoryAdjustmentList(
                page, size, keyword, drugId, batchNumber, adjustmentType, operatorId, startDate, endDate);
        return Result.success(adjustmentPage);
    }

    /**
     * 根据ID查询库存调整记录
     */
    @GetMapping("/{id}")
    public Result<InventoryAdjustment> getInventoryAdjustmentById(@PathVariable Long id) {
        InventoryAdjustment adjustment = inventoryAdjustmentService.getInventoryAdjustmentById(id);
        return Result.success(adjustment);
    }

    /**
     * 创建库存调整记录（盘盈/盘亏）
     */
    @PostMapping
    @RequiresPermission({"drug:manage"})
    public Result<InventoryAdjustment> createInventoryAdjustment(
            @RequestBody InventoryAdjustmentRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long operatorId = getCurrentUserId(httpRequest);
            InventoryAdjustment adjustment = new InventoryAdjustment();
            adjustment.setDrugId(request.getDrugId());
            adjustment.setBatchNumber(request.getBatchNumber());
            adjustment.setAdjustmentType(request.getAdjustmentType()); // PROFIT/LOSS
            adjustment.setQuantityBefore(request.getQuantityBefore());
            adjustment.setQuantityAfter(request.getQuantityAfter());
            adjustment.setAdjustmentQuantity(request.getQuantityAfter() - request.getQuantityBefore());
            adjustment.setAdjustmentReason(request.getAdjustmentReason());
            adjustment.setOperatorId(operatorId);
            adjustment.setSecondOperatorId(request.getSecondOperatorId()); // 特殊药品需第二操作人
            adjustment.setAdjustmentImage(request.getAdjustmentImage());
            adjustment.setRemark(request.getRemark());

            InventoryAdjustment created = inventoryAdjustmentService.createInventoryAdjustment(
                    adjustment, request.getSecondOperatorId());
            return Result.success("库存调整成功", created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        String token = getTokenFromRequest(request);
        if (token != null && jwtUtil.validateToken(token)) {
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("无法获取当前用户信息");
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("cdiom_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 库存调整请求DTO
     */
    @Data
    public static class InventoryAdjustmentRequest {
        private Long drugId;
        private String batchNumber;
        private String adjustmentType; // PROFIT-盘盈/LOSS-盘亏
        private Integer quantityBefore;
        private Integer quantityAfter;
        private String adjustmentReason;
        private Long secondOperatorId; // 特殊药品需第二操作人
        private String adjustmentImage;
        private String remark;
    }
}




