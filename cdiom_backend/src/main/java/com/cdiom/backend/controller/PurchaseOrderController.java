package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.mapper.SupplierMapper;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.BarcodeService;
import com.cdiom.backend.service.ExcelExportService;
import com.cdiom.backend.service.PurchaseOrderService;
import com.cdiom.backend.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final BarcodeService barcodeService;
    private final AuthService authService;
    private final SupplierMapper supplierMapper;
    private final JwtUtil jwtUtil;
    private final ExcelExportService excelExportService;
    private final PurchaseOrderMapper purchaseOrderMapper;

    /**
     * 分页查询采购订单列表
     * 供应商只能查看自己的订单，采购专员可以查看所有订单
     */
    @GetMapping
    public Result<Page<PurchaseOrder>> getPurchaseOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long purchaserId,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        // 获取当前用户
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        
        // 如果是供应商角色，只能查看自己的订单
        if (currentUser.getRoleId() != null && currentUser.getRoleId() == 5L) {
            // 通过supplier表的createBy字段查询供应商ID
            LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Supplier::getCreateBy, currentUser.getId());
            wrapper.eq(Supplier::getDeleted, 0);
            List<Supplier> suppliers = supplierMapper.selectList(wrapper);
            
            if (suppliers != null && !suppliers.isEmpty()) {
                // 取第一个供应商（通常一个供应商用户对应一个供应商记录）
                Long mySupplierId = suppliers.get(0).getId();
                // 强制使用供应商自己的ID，忽略传入的supplierId参数
                supplierId = mySupplierId;
            } else {
                // 如果供应商用户没有关联的供应商记录，返回空列表
                return Result.success(new Page<>(page, size));
            }
        }
        
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
     * 更新订单状态（通用方法）
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
     * 确认订单（PENDING -> CONFIRMED）
     * 供应商可以确认自己的订单
     */
    @PostMapping("/{id}/confirm")
    @RequiresPermission({"drug:manage"})
    public Result<Void> confirmOrder(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 检查权限：供应商只能确认自己的订单
            checkSupplierOrderPermission(id, request);
            purchaseOrderService.confirmOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 拒绝订单（PENDING -> REJECTED）
     * 供应商可以拒绝自己的订单
     */
    @PostMapping("/{id}/reject")
    @RequiresPermission({"drug:manage"})
    public Result<Void> rejectOrder(
            @PathVariable Long id,
            @RequestBody RejectOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 检查权限：供应商只能拒绝自己的订单
            checkSupplierOrderPermission(id, httpRequest);
            purchaseOrderService.rejectOrder(id, request.getReason());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发货（CONFIRMED -> SHIPPED）
     * 供应商可以对自己的订单发货
     */
    @PostMapping("/{id}/ship")
    @RequiresPermission({"drug:manage"})
    public Result<Void> shipOrder(
            @PathVariable Long id,
            @RequestBody ShipOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 检查权限：供应商只能对自己的订单发货
            checkSupplierOrderPermission(id, httpRequest);
            purchaseOrderService.shipOrder(id, request.getLogisticsNumber());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    @RequiresPermission({"drug:manage"})
    public Result<Void> cancelOrder(
            @PathVariable Long id,
            @RequestBody CancelOrderRequest request) {
        try {
            purchaseOrderService.cancelOrder(id, request.getReason());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新物流单号
     * 供应商可以更新自己订单的物流单号
     */
    @PutMapping("/{id}/logistics")
    @RequiresPermission({"drug:manage"})
    public Result<Void> updateLogisticsNumber(
            @PathVariable Long id,
            @RequestBody UpdateLogisticsRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 检查权限：供应商只能更新自己订单的物流单号
            checkSupplierOrderPermission(id, httpRequest);
            purchaseOrderService.updateLogisticsNumber(id, request.getLogisticsNumber());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成订单条形码（Base64）
     */
    @GetMapping("/{id}/barcode")
    public Result<Map<String, String>> generateBarcode(@PathVariable Long id) {
        try {
            PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(id);
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
                return Result.error("订单编号为空");
            }
            
            // 生成条形码（默认尺寸：300x100）
            String barcodeBase64 = barcodeService.generateBarcodeBase64(order.getOrderNumber(), 300, 100);
            
            Map<String, String> result = new java.util.HashMap<>();
            result.put("orderNumber", order.getOrderNumber());
            result.put("barcodeBase64", barcodeBase64);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("生成条形码失败：" + e.getMessage());
        }
    }

    /**
     * 下载订单条形码图片
     */
    @GetMapping("/{id}/barcode/download")
    public void downloadBarcode(
            @PathVariable Long id,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "100") int height,
            HttpServletResponse response) {
        try {
            PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(id);
            if (order == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // 设置响应头
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=barcode_" + order.getOrderNumber() + ".png");
            
            // 生成条形码并写入响应流
            barcodeService.generateBarcodeToStream(order.getOrderNumber(), width, height, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
     * 获取已发货订单跟踪列表（供应商专用）
     * 返回已发货状态的订单列表，用于跟踪物流
     */
    @GetMapping("/shipped-orders")
    public Result<Page<PurchaseOrder>> getShippedOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        try {
            // 获取当前用户
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            
            // 如果是供应商角色，只能查看自己的订单
            Long supplierId = null;
            if (currentUser.getRoleId() != null && currentUser.getRoleId() == 5L) {
                LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Supplier::getCreateBy, currentUser.getId());
                wrapper.eq(Supplier::getDeleted, 0);
                List<Supplier> suppliers = supplierMapper.selectList(wrapper);
                
                if (suppliers != null && !suppliers.isEmpty()) {
                    supplierId = suppliers.get(0).getId();
                } else {
                    return Result.success(new Page<>(page, size));
                }
            }
            
            // 查询已发货订单
            Page<PurchaseOrder> orderPage = purchaseOrderService.getPurchaseOrderList(
                    page, size, null, supplierId, null, "SHIPPED");
            return Result.success(orderPage);
        } catch (Exception e) {
            return Result.error("获取已发货订单失败：" + e.getMessage());
        }
    }

    /**
     * 检查供应商订单权限
     * 供应商只能操作自己的订单
     */
    private void checkSupplierOrderPermission(Long orderId, HttpServletRequest request) {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未登录");
        }
        
        // 如果是供应商角色，检查订单是否属于自己
        if (currentUser.getRoleId() != null && currentUser.getRoleId() == 5L) {
            PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            
            // 通过supplier表的createBy字段查询供应商ID
            LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Supplier::getCreateBy, currentUser.getId());
            wrapper.eq(Supplier::getDeleted, 0);
            List<Supplier> suppliers = supplierMapper.selectList(wrapper);
            
            if (suppliers == null || suppliers.isEmpty()) {
                throw new RuntimeException("未找到关联的供应商信息");
            }
            
            Long mySupplierId = suppliers.get(0).getId();
            if (!mySupplierId.equals(order.getSupplierId())) {
                throw new RuntimeException("无权操作此订单");
            }
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

    /**
     * 拒绝订单请求DTO
     */
    @Data
    public static class RejectOrderRequest {
        private String reason;
    }

    /**
     * 发货请求DTO
     */
    @Data
    public static class ShipOrderRequest {
        private String logisticsNumber;
    }

    /**
     * 取消订单请求DTO
     */
    @Data
    public static class CancelOrderRequest {
        private String reason;
    }

    /**
     * 更新物流单号请求DTO
     */
    @Data
    public static class UpdateLogisticsRequest {
        private String logisticsNumber;
    }

    /**
     * 导出采购订单列表到Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPurchaseOrderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long purchaserId,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        try {
            // 获取当前用户
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("未登录或登录已过期");
            }
            
            // 如果是供应商角色，只能查看自己的订单
            if (currentUser.getRoleId() != null && currentUser.getRoleId() == 5L) {
                // 通过supplier表的createBy字段查询供应商ID
                LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Supplier::getCreateBy, currentUser.getId());
                wrapper.eq(Supplier::getDeleted, 0);
                List<Supplier> suppliers = supplierMapper.selectList(wrapper);
                
                if (suppliers != null && !suppliers.isEmpty()) {
                    // 取第一个供应商（通常一个供应商用户对应一个供应商记录）
                    Long mySupplierId = suppliers.get(0).getId();
                    // 强制使用供应商自己的ID，忽略传入的supplierId参数
                    supplierId = mySupplierId;
                } else {
                    // 如果供应商用户没有关联的供应商记录，返回空列表
                    supplierId = -1L; // 设置为不存在的ID，查询结果为空
                }
            }

            // 构建查询条件（与列表查询保持一致）
            LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
            
            if (StringUtils.hasText(keyword)) {
                wrapper.and(w -> w.like(PurchaseOrder::getOrderNumber, keyword)
                        .or().like(PurchaseOrder::getLogisticsNumber, keyword));
            }
            
            if (supplierId != null && supplierId != -1L) {
                wrapper.eq(PurchaseOrder::getSupplierId, supplierId);
            } else if (supplierId != null && supplierId == -1L) {
                // 供应商用户没有关联的供应商记录，返回空结果
                wrapper.eq(PurchaseOrder::getId, -1);
            }
            
            if (purchaserId != null) {
                wrapper.eq(PurchaseOrder::getPurchaserId, purchaserId);
            }
            
            if (StringUtils.hasText(status)) {
                wrapper.eq(PurchaseOrder::getStatus, status);
            }
            
            wrapper.orderByDesc(PurchaseOrder::getCreateTime);
            
            // 查询所有数据（不分页）
            List<PurchaseOrder> orderList = purchaseOrderMapper.selectList(wrapper);

            // 生成Excel
            byte[] excelBytes = excelExportService.exportPurchaseOrderList(orderList, currentUser);

            // 设置响应头
            String fileName = "采购订单列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
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

