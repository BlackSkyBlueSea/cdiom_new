package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.InboundReceiptBatch;
import com.cdiom.backend.model.InboundRecord;
import com.cdiom.backend.model.vo.InboundSplitResult;
import com.cdiom.backend.model.vo.OrderInboundRemainingRow;
import com.cdiom.backend.service.InboundRecordService;
import com.cdiom.backend.util.JwtUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String expiryCheckStatus,
            @RequestParam(required = false) String secondConfirmStatus,
            @RequestParam(required = false) Long secondOperatorId) {
        Page<InboundRecord> recordPage = inboundRecordService.getInboundRecordList(
                page, size, keyword, orderId, drugId, batchNumber, operatorId,
                startDate, endDate, status, expiryCheckStatus, secondConfirmStatus, secondOperatorId);
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
     * 不合格入库处置意向选项（代码 -> 展示名），便于前端下拉与后续扩展统一口径
     */
    @GetMapping("/disposition-options")
    public Result<Map<String, String>> listDispositionOptions() {
        return Result.success(inboundRecordService.listDispositionOptions());
    }

    /**
     * 创建入库记录（采购订单入库）
     */
    @PostMapping("/from-order")
    public Result<InboundRecord> createInboundRecordFromOrder(
            @Valid @RequestBody InboundRecordRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long operatorId = getCurrentUserId(httpRequest);
            InboundRecord record = new InboundRecord();
            record.setOrderId(request.getOrderId());
            record.setDrugId(request.getDrugId());
            record.setBatchNumber(request.getBatchNumber());
            record.setQuantity(request.getQuantity());
            record.setExpiryDate(request.getExpiryDate());
            record.setArrivalDate(request.getArrivalDate());
            record.setProductionDate(request.getProductionDate());
            record.setManufacturer(request.getManufacturer());
            record.setStorageLocation(request.getStorageLocation());
            record.setDeliveryNoteNumber(request.getDeliveryNoteNumber());
            record.setDeliveryNoteImage(request.getDeliveryNoteImage());
            record.setOperatorId(operatorId);
            record.setSecondOperatorId(request.getSecondOperatorId());
            record.setStatus(request.getStatus());
            record.setExpiryCheckStatus(request.getExpiryCheckStatus());
            record.setExpiryCheckReason(request.getExpiryCheckReason());
            record.setRemark(request.getRemark());
            record.setDispositionCode(request.getDispositionCode());
            record.setDispositionRemark(request.getDispositionRemark());

            InboundRecord created = inboundRecordService.createInboundRecordFromOrder(
                    record, request.getOrderId(), request.getDrugId(),
                    request.getReceiptBatchId(), request.getArrivalAt());
            String msg = "PENDING_SECOND".equals(created.getSecondConfirmStatus())
                    ? "已提交，请第二操作人登录系统确认后方可入账（已邮件提醒）"
                    : "入库成功";
            return Result.success(msg, created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 仅登记采购到货批次头（可选：先登记再逐药品入库）
     */
    @PostMapping("/receipt-batch")
    public Result<InboundReceiptBatch> createReceiptBatch(
            @Valid @RequestBody ReceiptBatchCreateRequest body,
            HttpServletRequest httpRequest) {
        try {
            Long operatorId = getCurrentUserId(httpRequest);
            InboundReceiptBatch b = inboundRecordService.createReceiptBatchHeader(
                    body.getOrderId(),
                    body.getDeliveryNoteNumber(),
                    body.getArrivalAt() != null ? body.getArrivalAt() : LocalDateTime.now(),
                    operatorId,
                    body.getRemark(),
                    body.getDeliveryNoteImage());
            return Result.success("到货批次已登记", b);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建入库记录（临时入库）
     */
    @PostMapping("/temporary")
    public Result<InboundRecord> createInboundRecordTemporary(
            @Valid @RequestBody InboundRecordRequest request,
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
            record.setStorageLocation(request.getStorageLocation());
            record.setDeliveryNoteNumber(request.getDeliveryNoteNumber());
            record.setDeliveryNoteImage(request.getDeliveryNoteImage());
            record.setOperatorId(operatorId);
            record.setSecondOperatorId(request.getSecondOperatorId());
            record.setStatus(request.getStatus());
            record.setExpiryCheckStatus(request.getExpiryCheckStatus());
            record.setExpiryCheckReason(request.getExpiryCheckReason());
            record.setRemark(request.getRemark());
            record.setDispositionCode(request.getDispositionCode());
            record.setDispositionRemark(request.getDispositionRemark());

            InboundRecord created = inboundRecordService.createInboundRecordTemporary(
                    record, request.getDrugId());
            String msg = "PENDING_SECOND".equals(created.getSecondConfirmStatus())
                    ? "已提交，请第二操作人登录系统确认后方可入账（已邮件提醒）"
                    : "临时入库成功";
            return Result.success(msg, created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 同一批号同时登记合格与不合格两条明细（同一追溯码，事务）
     */
    @PostMapping("/from-order/split")
    public Result<InboundSplitResult> createInboundSplitFromOrder(
            @Valid @RequestBody SplitInboundFromOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long operatorId = getCurrentUserId(httpRequest);
            InboundRecord base = new InboundRecord();
            base.setBatchNumber(request.getBatchNumber());
            base.setExpiryDate(request.getExpiryDate());
            base.setArrivalDate(request.getArrivalDate());
            base.setProductionDate(request.getProductionDate());
            base.setManufacturer(request.getManufacturer());
            base.setStorageLocation(request.getStorageLocation());
            base.setDeliveryNoteNumber(request.getDeliveryNoteNumber());
            base.setDeliveryNoteImage(request.getDeliveryNoteImage());
            base.setOperatorId(operatorId);
            base.setSecondOperatorId(request.getSecondOperatorId());
            base.setExpiryCheckStatus(request.getExpiryCheckStatus());
            base.setExpiryCheckReason(request.getExpiryCheckReason());
            base.setRemark(request.getRemark());

            InboundSplitResult result = inboundRecordService.createInboundSplitFromOrder(
                    base,
                    request.getOrderId(),
                    request.getDrugId(),
                    request.getQualifiedQuantity(),
                    request.getUnqualifiedQuantity(),
                    request.getUnqualifiedReason(),
                    request.getUnqualifiedDispositionCode(),
                    request.getUnqualifiedDispositionRemark(),
                    request.getReceiptBatchId(),
                    request.getArrivalAt());
            String msg = result.getQualifiedRecord() != null
                    && "PENDING_SECOND".equals(result.getQualifiedRecord().getSecondConfirmStatus())
                    ? "拆行验收已提交，合格部分请第二操作人确认后方可入账（已邮件提醒）"
                    : "拆行验收已提交（合格与不合格两条已关联同一追溯码）";
            return Result.success(msg, result);
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
     * 订单行已占用到货数量（合格已入账或待第二人确认 + 不合格登记），用于入库时校验不超量
     */
    @GetMapping("/order/{orderId}/drug/{drugId}/committed-quantity")
    public Result<Integer> getInboundCommittedQuantityByOrderAndDrug(
            @PathVariable Long orderId,
            @PathVariable Long drugId) {
        Integer quantity = inboundRecordService.getInboundCommittedQuantityByOrderAndDrug(orderId, drugId);
        return Result.success(quantity);
    }

    /**
     * 订单各药品：订单量、已占用（含待第二人确认）、剩余可入库（用于入库界面动态展示）
     */
    @GetMapping("/order/{orderId}/inbound-remaining")
    public Result<List<OrderInboundRemainingRow>> listOrderInboundRemaining(@PathVariable Long orderId) {
        List<OrderInboundRemainingRow> rows = inboundRecordService.listOrderInboundRemaining(orderId);
        return Result.success(rows);
    }

    /**
     * 第二操作人确认入库（须登录，特殊药品待确认）
     */
    @PostMapping("/{id}/second-confirm")
    public Result<InboundRecord> secondConfirmInbound(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long uid = getCurrentUserId(httpRequest);
            InboundRecord updated = inboundRecordService.secondConfirmInbound(id, uid);
            return Result.success("确认成功，已入账", updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 第二操作人驳回
     */
    @PostMapping("/{id}/second-reject")
    public Result<InboundRecord> secondRejectInbound(
            @PathVariable Long id,
            @RequestBody SecondRejectRequest body,
            HttpServletRequest httpRequest) {
        try {
            Long uid = getCurrentUserId(httpRequest);
            InboundRecord updated = inboundRecordService.secondRejectInbound(id, uid, body != null ? body.getReason() : null);
            return Result.success("已驳回", updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 第一操作人撤回待第二人确认的入库单
     */
    @PostMapping("/{id}/withdraw-pending-second")
    public Result<InboundRecord> withdrawPendingSecond(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long uid = getCurrentUserId(httpRequest);
            InboundRecord updated = inboundRecordService.withdrawPendingSecondInbound(id, uid);
            return Result.success("已撤回", updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
        /** 沿用已登记的到货批次头；为空则按随货单等自动新建一批次 */
        private Long receiptBatchId;
        /** 新建批次时的到货时间（精确到秒）；沿用 receiptBatchId 时可不传 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime arrivalAt;
        private Long drugId;
        private String batchNumber;
        private Integer quantity;
        private LocalDate expiryDate;
        private LocalDate arrivalDate;
        private LocalDate productionDate;
        private String manufacturer;
        /** 合格入库必填：本批药品存放位置 */
        private String storageLocation;
        private String deliveryNoteNumber;
        private String deliveryNoteImage;
        private Long secondOperatorId;
        private String status; // QUALIFIED/UNQUALIFIED
        private String expiryCheckStatus; // PASS/WARNING/FORCE
        private String expiryCheckReason;
        private String remark;
        /** 仅不合格：处置意向代码，空则默认 PENDING */
        private String dispositionCode;
        private String dispositionRemark;
    }

    @Data
    public static class SplitInboundFromOrderRequest {
        @NotNull
        private Long orderId;
        private Long receiptBatchId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime arrivalAt;
        @NotNull
        private Long drugId;
        @NotBlank
        private String batchNumber;
        @NotNull
        @Min(1)
        private Integer qualifiedQuantity;
        @NotNull
        @Min(1)
        private Integer unqualifiedQuantity;
        @NotBlank
        private String unqualifiedReason;
        @NotNull
        private LocalDate expiryDate;
        private LocalDate arrivalDate;
        @NotNull
        private LocalDate productionDate;
        private String manufacturer;
        private String storageLocation;
        private String deliveryNoteNumber;
        private String deliveryNoteImage;
        private Long secondOperatorId;
        private String expiryCheckStatus;
        private String expiryCheckReason;
        private String remark;
        /** 不合格行处置意向，空则默认 PENDING */
        private String unqualifiedDispositionCode;
        private String unqualifiedDispositionRemark;
    }

    @Data
    public static class ReceiptBatchCreateRequest {
        @NotNull
        private Long orderId;
        @NotBlank
        private String deliveryNoteNumber;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime arrivalAt;
        private String remark;
        private String deliveryNoteImage;
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

    @Data
    public static class SecondRejectRequest {
        private String reason;
    }
}








