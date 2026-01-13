package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.service.PurchaseOrderService;
import com.cdiom.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 采购订单管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询采购订单列表
     */
    @GetMapping
    public Result<Page<PurchaseOrder>> getPurchaseOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long purchaserId,
            @RequestParam(required = false) String status) {
        Page<PurchaseOrder> orderPage = purchaseOrderService.getPurchaseOrderList(
                page, size, keyword, supplierId, purchaserId, status);
        return Result.success(orderPage);
    }

    /**
     * 根据ID查询采购订单
     */
    @GetMapping("/{id}")
    public Result<PurchaseOrder> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(id);
        return Result.success(order);
    }

    /**
     * 根据订单编号查询采购订单（用于条形码扫描）
     */
    @GetMapping("/order-number/{orderNumber}")
    public Result<PurchaseOrder> getPurchaseOrderByOrderNumber(@PathVariable String orderNumber) {
        PurchaseOrder order = purchaseOrderService.getPurchaseOrderByOrderNumber(orderNumber);
        return Result.success(order);
    }

    /**
     * 创建采购订单
     */
    @PostMapping
    @RequiresPermission({"drug:manage"})
    public Result<PurchaseOrder> createPurchaseOrder(
            @RequestBody PurchaseOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long purchaserId = getCurrentUserId(httpRequest);
            PurchaseOrder order = new PurchaseOrder();
            order.setSupplierId(request.getSupplierId());
            order.setPurchaserId(purchaserId);
            order.setStatus("PENDING");
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
            order.setRemark(request.getRemark());

            PurchaseOrder created = purchaseOrderService.createPurchaseOrder(order, request.getItems());
            return Result.success("采购订单创建成功", created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新采购订单
     */
    @PutMapping("/{id}")
    @RequiresPermission({"drug:manage"})
    public Result<PurchaseOrder> updatePurchaseOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrderRequest request) {
        try {
            PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(id);
            if (order == null) {
                return Result.error("订单不存在");
            }
            order.setSupplierId(request.getSupplierId());
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
            order.setRemark(request.getRemark());

            PurchaseOrder updated = purchaseOrderService.updatePurchaseOrder(order);
            return Result.success("订单更新成功", updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除采购订单
     */
    @DeleteMapping("/{id}")
    @RequiresPermission({"drug:manage"})
    public Result<Void> deletePurchaseOrder(@PathVariable Long id) {
        try {
            purchaseOrderService.deletePurchaseOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/{id}/status")
    @RequiresPermission({"drug:manage"})
    public Result<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        try {
            purchaseOrderService.updateOrderStatus(id, request.getStatus(), request.getReason());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单明细列表
     */
    @GetMapping("/{id}/items")
    public Result<List<PurchaseOrderItem>> getOrderItems(@PathVariable Long id) {
        List<PurchaseOrderItem> items = purchaseOrderService.getOrderItems(id);
        return Result.success(items);
    }

    /**
     * 获取订单明细的已入库数量
     */
    @GetMapping("/{id}/inbound-quantities")
    public Result<Map<Long, Integer>> getInboundQuantitiesByOrder(@PathVariable Long id) {
        Map<Long, Integer> quantities = purchaseOrderService.getInboundQuantitiesByOrder(id);
        return Result.success(quantities);
    }

    /**
     * 检查订单是否可以入库
     */
    @GetMapping("/{id}/can-inbound")
    public Result<Boolean> canInbound(@PathVariable Long id) {
        boolean canInbound = purchaseOrderService.canInbound(id);
        return Result.success(canInbound);
    }

    /**
     * 检查订单是否全部入库
     */
    @GetMapping("/{id}/is-fully-inbound")
    public Result<Boolean> isOrderFullyInbound(@PathVariable Long id) {
        boolean isFullyInbound = purchaseOrderService.isOrderFullyInbound(id);
        return Result.success(isFullyInbound);
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
     * 采购订单请求DTO
     */
    @Data
    public static class PurchaseOrderRequest {
        private Long supplierId;
        private java.time.LocalDate expectedDeliveryDate;
        private String remark;
        private List<PurchaseOrderItem> items;
    }

    /**
     * 更新状态请求DTO
     */
    @Data
    public static class UpdateStatusRequest {
        private String status;
        private String reason;
    }
}

