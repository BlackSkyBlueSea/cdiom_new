package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.OutboundApply;
import com.cdiom.backend.service.OutboundApplyService;
import com.cdiom.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 出库申请管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/outbound")
@RequiredArgsConstructor
public class OutboundApplyController {

    private final OutboundApplyService outboundApplyService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询出库申请列表
     */
    @GetMapping
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<Page<OutboundApply>> getOutboundApplyList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Page<OutboundApply> applyPage = outboundApplyService.getOutboundApplyList(
                page, size, keyword, applicantId, approverId, department, status, startDate, endDate);
        return Result.success(applyPage);
    }

    /**
     * 根据ID查询出库申请
     */
    @GetMapping("/{id}")
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<OutboundApply> getOutboundApplyById(@PathVariable Long id) {
        OutboundApply apply = outboundApplyService.getOutboundApplyById(id);
        return Result.success(apply);
    }

    /**
     * 根据申领单号查询出库申请
     */
    @GetMapping("/apply-number/{applyNumber}")
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<OutboundApply> getOutboundApplyByApplyNumber(@PathVariable String applyNumber) {
        OutboundApply apply = outboundApplyService.getOutboundApplyByApplyNumber(applyNumber);
        return Result.success(apply);
    }

    /**
     * 创建出库申请（医护人员申领）
     */
    @PostMapping
    @RequiresPermission({"drug:manage"})
    public Result<OutboundApply> createOutboundApply(
            @RequestBody OutboundApplyRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long applicantId = getCurrentUserId(httpRequest);
            OutboundApply apply = new OutboundApply();
            apply.setApplicantId(applicantId);
            apply.setDepartment(request.getDepartment());
            apply.setPurpose(request.getPurpose());
            apply.setStatus("PENDING");
            apply.setRemark(request.getRemark());

            OutboundApply created = outboundApplyService.createOutboundApply(apply, request.getItems());
            return Result.success("出库申请创建成功", created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批出库申请（通过）
     */
    @PostMapping("/{id}/approve")
    @RequiresPermission({"drug:manage"})
    public Result<Void> approveOutboundApply(
            @PathVariable Long id,
            @RequestBody ApproveRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long approverId = getCurrentUserId(httpRequest);
            outboundApplyService.approveOutboundApply(id, approverId, request.getSecondApproverId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批出库申请（驳回）
     */
    @PostMapping("/{id}/reject")
    @RequiresPermission({"drug:manage"})
    public Result<Void> rejectOutboundApply(
            @PathVariable Long id,
            @RequestBody RejectRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long approverId = getCurrentUserId(httpRequest);
            outboundApplyService.rejectOutboundApply(id, approverId, request.getRejectReason());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 执行出库
     */
    @PostMapping("/{id}/execute")
    @RequiresPermission({"drug:manage"})
    public Result<Void> executeOutbound(
            @PathVariable Long id,
            @RequestBody ExecuteOutboundRequest request) {
        try {
            outboundApplyService.executeOutbound(id, request.getOutboundItems());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消出库申请
     */
    @PostMapping("/{id}/cancel")
    @RequiresPermission({"drug:manage"})
    public Result<Void> cancelOutboundApply(@PathVariable Long id) {
        try {
            outboundApplyService.cancelOutboundApply(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询待审批的出库申请数量
     */
    @GetMapping("/pending-count")
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<Long> getPendingOutboundCount() {
        Long count = outboundApplyService.getPendingOutboundCount();
        return Result.success(count);
    }

    /**
     * 查询今日出库数量
     */
    @GetMapping("/today-count")
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<Long> getTodayOutboundCount() {
        Long count = outboundApplyService.getTodayOutboundCount();
        return Result.success(count);
    }

    /**
     * 获取出库申请明细列表
     */
    @GetMapping("/{id}/items")
    @RequiresPermission({"drug:view", "drug:manage"})
    public Result<List<Map<String, Object>>> getOutboundApplyItems(@PathVariable Long id) {
        try {
            List<com.cdiom.backend.model.OutboundApplyItem> items = outboundApplyService.getOutboundApplyItems(id);
            // 转换为前端需要的格式，包含药品信息
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (com.cdiom.backend.model.OutboundApplyItem item : items) {
                Map<String, Object> itemMap = new java.util.HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("drugId", item.getDrugId());
                itemMap.put("batchNumber", item.getBatchNumber());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("actualQuantity", item.getActualQuantity());
                itemMap.put("remark", item.getRemark());
                result.add(itemMap);
            }
            return Result.success(result);
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
     * 出库申请请求DTO
     */
    @Data
    public static class OutboundApplyRequest {
        private String department;
        private String purpose;
        private String remark;
        private List<Map<String, Object>> items; // [{drugId, batchNumber(可选), quantity}]
    }

    /**
     * 审批请求DTO
     */
    @Data
    public static class ApproveRequest {
        private Long secondApproverId; // 特殊药品需第二审批人
    }

    /**
     * 驳回请求DTO
     */
    @Data
    public static class RejectRequest {
        private String rejectReason;
    }

    /**
     * 执行出库请求DTO
     */
    @Data
    public static class ExecuteOutboundRequest {
        private List<Map<String, Object>> outboundItems; // [{drugId, batchNumber, actualQuantity}]
    }
}

