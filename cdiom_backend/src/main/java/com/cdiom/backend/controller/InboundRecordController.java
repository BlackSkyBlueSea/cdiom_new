package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.InboundRecord;
import com.cdiom.backend.service.InboundRecordService;
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
 * 入库记录管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/inbound")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class InboundRecordController {

    private final InboundRecordService inboundRecordService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询入库记录列表
     */
    @GetMapping
    public Result<Page<InboundRecord>> getInboundRecordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String expiryCheckStatus) {
        Page<InboundRecord> recordPage = inboundRecordService.getInboundRecordList(
                page, size, keyword, orderId, drugId, batchNumber, operatorId,
                startDate, endDate, status, expiryCheckStatus);
        return Result.success(recordPage);
    }

    /**
     * 根据ID查询入库记录
     */
    @GetMapping("/{id}")
    public Result<InboundRecord> getInboundRecordById(@PathVariable Long id) {
        InboundRecord record = inboundRecordService.getInboundRecordById(id);
        return Result.success(record);
    }

    /**
     * 根据入库单号查询入库记录
     */
    @GetMapping("/record-number/{recordNumber}")
    public Result<InboundRecord> getInboundRecordByRecordNumber(@PathVariable String recordNumber) {
        InboundRecord record = inboundRecordService.getInboundRecordByRecordNumber(recordNumber);
        return Result.success(record);
    }

    /**
     * 创建入库记录（采购订单入库）
     */
    @PostMapping("/from-order")
    public Result<InboundRecord> createInboundRecordFromOrder(
            @RequestBody InboundRecordRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long operatorId = getCurrentUserId(httpRequest);
            InboundRecord record = new InboundRecord();
            record.setOrderId(request.getOrderId());
            record.setDrugId(request.getDrugId());
            record.setBatchNumber(request.getBatchNumber());
            record.setQuantity(request.getQuantity());
            record.setExpiryDate(request.getExpiryDate());
            record.setArrivalDate(request.getArrivalDate() != null ? request.getArrivalDate() : LocalDate.now());
            record.setProductionDate(request.getProductionDate());
            record.setManufacturer(request.getManufacturer());
            record.setDeliveryNoteNumber(request.getDeliveryNoteNumber());
            record.setDeliveryNoteImage(request.getDeliveryNoteImage());
            record.setOperatorId(operatorId);
            record.setSecondOperatorId(request.getSecondOperatorId());
            record.setStatus(request.getStatus());
            record.setExpiryCheckStatus(request.getExpiryCheckStatus());
            record.setExpiryCheckReason(request.getExpiryCheckReason());
            record.setRemark(request.getRemark());

            InboundRecord created = inboundRecordService.createInboundRecordFromOrder(
                    record, request.getOrderId(), request.getDrugId());
            return Result.success("入库成功", created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建入库记录（临时入库）
     */
    @PostMapping("/temporary")
    public Result<InboundRecord> createInboundRecordTemporary(
            @RequestBody InboundRecordRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long operatorId = getCurrentUserId(httpRequest);
            InboundRecord record = new InboundRecord();
            record.setDrugId(request.getDrugId());
            record.setBatchNumber(request.getBatchNumber());
            record.setQuantity(request.getQuantity());
            record.setExpiryDate(request.getExpiryDate());
            record.setArrivalDate(request.getArrivalDate() != null ? request.getArrivalDate() : LocalDate.now());
            record.setProductionDate(request.getProductionDate());
            record.setManufacturer(request.getManufacturer());
            record.setDeliveryNoteNumber(request.getDeliveryNoteNumber());
            record.setDeliveryNoteImage(request.getDeliveryNoteImage());
            record.setOperatorId(operatorId);
            record.setSecondOperatorId(request.getSecondOperatorId());
            record.setStatus(request.getStatus());
            record.setExpiryCheckStatus(request.getExpiryCheckStatus());
            record.setExpiryCheckReason(request.getExpiryCheckReason());
            record.setRemark(request.getRemark());

            InboundRecord created = inboundRecordService.createInboundRecordTemporary(
                    record, request.getDrugId());
            return Result.success("临时入库成功", created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 效期校验
     */
    @PostMapping("/check-expiry")
    public Result<ExpiryCheckResponse> checkExpiryDate(@RequestBody ExpiryCheckRequest request) {
        String status = inboundRecordService.checkExpiryDate(request.getExpiryDate());
        ExpiryCheckResponse response = new ExpiryCheckResponse();
        response.setStatus(status);
        if ("WARNING".equals(status)) {
            response.setMessage("有效期不足180天，需确认");
        } else if ("FORCE".equals(status)) {
            response.setMessage("有效期不足90天，需填写强制入库原因");
        } else {
            response.setMessage("有效期校验通过");
        }
        return Result.success(response);
    }

    /**
     * 查询订单某个药品的已入库数量
     */
    @GetMapping("/order/{orderId}/drug/{drugId}/quantity")
    public Result<Integer> getInboundQuantityByOrderAndDrug(
            @PathVariable Long orderId,
            @PathVariable Long drugId) {
        Integer quantity = inboundRecordService.getInboundQuantityByOrderAndDrug(orderId, drugId);
        return Result.success(quantity);
    }

    /**
     * 查询今日入库数量
     */
    @GetMapping("/today-count")
    public Result<Long> getTodayInboundCount() {
        Long count = inboundRecordService.getTodayInboundCount();
        return Result.success(count);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        // 如果SecurityContext中没有，尝试从Token获取
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
        // 优先从Cookie获取
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("cdiom_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // 其次从Header获取
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 入库记录请求DTO
     */
    @Data
    public static class InboundRecordRequest {
        private Long orderId;
        private Long drugId;
        private String batchNumber;
        private Integer quantity;
        private LocalDate expiryDate;
        private LocalDate arrivalDate;
        private LocalDate productionDate;
        private String manufacturer;
        private String deliveryNoteNumber;
        private String deliveryNoteImage;
        private Long secondOperatorId;
        private String status; // QUALIFIED/UNQUALIFIED
        private String expiryCheckStatus; // PASS/WARNING/FORCE
        private String expiryCheckReason;
        private String remark;
    }

    /**
     * 效期校验请求DTO
     */
    @Data
    public static class ExpiryCheckRequest {
        private LocalDate expiryDate;
    }

    /**
     * 效期校验响应DTO
     */
    @Data
    public static class ExpiryCheckResponse {
        private String status; // PASS/WARNING/FORCE
        private String message;
    }
}

