package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.OutboundApply;
import com.cdiom.backend.model.OutboundApplyItem;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.OutboundApplyService;
import com.cdiom.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
@Slf4j
public class OutboundApplyController {

    private final OutboundApplyService outboundApplyService;
    private final DrugInfoMapper drugInfoMapper;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询出库申请列表
     */
    @GetMapping
    @RequiresPermission({
            "outbound:view",
            "outbound:apply",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute"
    })
    public Result<Page<OutboundApply>> getOutboundApplyList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "applicantId", required = false) Long applicantId,
            @RequestParam(value = "approverId", required = false) Long approverId,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("[INT-FE-02][BE出库] GET /outbound list page={} size={} status={} keyword={}",
                page, size, status, keyword);
        Page<OutboundApply> applyPage = outboundApplyService.getOutboundApplyList(
                page, size, keyword, applicantId, approverId, department, status, startDate, endDate);
        log.info("[INT-FE-02][BE出库] GET /outbound list ok total={} records={}",
                applyPage.getTotal(), applyPage.getRecords() != null ? applyPage.getRecords().size() : 0);
        return Result.success(applyPage);
    }

    /**
     * 获取已有科室列表（供新建出库申请时下拉选择，无则仍可手动输入）
     */
    @GetMapping("/departments")
    @RequiresPermission({"outbound:view", "outbound:apply", "outbound:apply:on-behalf"})
    public Result<List<String>> listDepartments() {
        List<String> list = outboundApplyService.listDepartments();
        return Result.success(list);
    }

    /**
     * 新建申领时：按药品查询库存批次（医护人员通常无 drug:view，不能调 /inventory）
     */
    @GetMapping("/drug-batches")
    @RequiresPermission({"outbound:apply", "outbound:apply:on-behalf"})
    public Result<List<Map<String, Object>>> listDrugBatchesForApply(@RequestParam("drugId") Long drugId) {
        try {
            return Result.success(outboundApplyService.listDrugBatchesForApply(drugId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 代录出库：可选申领医护人员列表（启用、角色为医护人员）
     */
    @GetMapping("/medical-applicants")
    @RequiresPermission({"outbound:apply:on-behalf"})
    public Result<List<Map<String, Object>>> listMedicalApplicantsForProxy() {
        return Result.success(outboundApplyService.listMedicalApplicantsForProxy());
    }

    /**
     * 特殊药品第二审批人候选：具备出库审核权限（outbound:approve 或 outbound:approve:special）的用户，含超级管理员
     */
    @GetMapping("/second-approver-candidates")
    @RequiresPermission({"outbound:approve", "outbound:approve:special"})
    public Result<List<SysUser>> listSecondApproverCandidates() {
        return Result.success(outboundApplyService.listOutboundSecondApproverCandidates());
    }

    /**
     * 根据ID查询出库申请（列表查看、申请人查看详情等）
     */
    @GetMapping("/{id}")
    @RequiresPermission({
            "outbound:view",
            "outbound:apply",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute",
            "drug:manage"
    })
    public Result<OutboundApply> getOutboundApplyById(@PathVariable Long id) {
        log.info("[INT-FE-02][BE出库] GET /outbound/{}", id);
        OutboundApply apply = outboundApplyService.getOutboundApplyById(id);
        if (apply != null) {
            log.info("[INT-FE-02][BE出库] GET /outbound/{} ok applyNumber={} status={}",
                    id, apply.getApplyNumber(), apply.getStatus());
        }
        return Result.success(apply);
    }

    /**
     * 校验出库申请所需库存是否充足（审批前在界面展示，不足时禁止通过）
     */
    @GetMapping("/{id}/stock-check")
    @RequiresPermission({
            "outbound:view",
            "outbound:apply",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute"
    })
    public Result<Map<String, Object>> checkStockForApply(@PathVariable Long id) {
        log.info("[INT-FE-02][BE出库] GET /outbound/{}/stock-check", id);
        Map<String, Object> data = outboundApplyService.checkStockForApply(id);
        log.info("[INT-FE-02][BE出库] GET /outbound/{}/stock-check ok sufficient={}",
                id, data != null ? data.get("sufficient") : null);
        return Result.success(data);
    }

    /**
     * 根据申领单号查询出库申请
     */
    @GetMapping("/apply-number/{applyNumber}")
    @RequiresPermission({
            "outbound:view",
            "outbound:apply",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute",
            "drug:manage"
    })
    public Result<OutboundApply> getOutboundApplyByApplyNumber(@PathVariable String applyNumber) {
        OutboundApply apply = outboundApplyService.getOutboundApplyByApplyNumber(applyNumber);
        return Result.success(apply);
    }

    /**
     * 创建出库申请（医护人员申领）
     */
    @PostMapping
    @RequiresPermission({"outbound:apply"})
    public Result<OutboundApply> createOutboundApply(
            @Valid @RequestBody OutboundApplyRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long applicantId = getCurrentUserId(httpRequest);
            OutboundApply apply = new OutboundApply();
            apply.setApplicantId(applicantId);
            apply.setDepartment(request.getDepartment());
            apply.setPurpose(request.getPurpose());
            apply.setStatus("PENDING");
            apply.setRemark(request.getRemark());

            log.info("[INT-FE-02][BE出库] POST /outbound create applicantId={} items={}",
                    applicantId, request.getItems() != null ? request.getItems().size() : 0);
            OutboundApply created = outboundApplyService.createOutboundApply(apply, request.getItems());
            log.info("[INT-FE-02][BE出库] POST /outbound create ok id={} applyNumber={} status={}",
                    created.getId(), created.getApplyNumber(), created.getStatus());
            return Result.success("出库申请创建成功", created);
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound create fail: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 仓库管理员代医护人员创建出库申请（现场申领、系统留痕）
     */
    @PostMapping("/on-behalf")
    @RequiresPermission({"outbound:apply:on-behalf"})
    public Result<OutboundApply> createOutboundApplyOnBehalf(
            @Valid @RequestBody OutboundApplyOnBehalfRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long proxyId = getCurrentUserId(httpRequest);
            log.info("[INT-FE-02][BE出库] POST /outbound/on-behalf proxyId={} applicantId={} items={}",
                    proxyId, request.getApplicantId(), request.getItems() != null ? request.getItems().size() : 0);
            OutboundApply created = outboundApplyService.createOutboundApplyOnBehalf(
                    proxyId,
                    request.getApplicantId(),
                    request.getDepartment(),
                    request.getPurpose(),
                    request.getRemark(),
                    request.getItems());
            log.info("[INT-FE-02][BE出库] POST /outbound/on-behalf ok id={} applyNumber={} status={}",
                    created.getId(), created.getApplyNumber(), created.getStatus());
            return Result.success("代录出库申请已创建", created);
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/on-behalf fail: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批出库申请（通过）
     */
    @PostMapping("/{id}/approve")
    @RequiresPermission({"outbound:approve", "outbound:approve:special"})
    public Result<Void> approveOutboundApply(
            @PathVariable Long id,
            @RequestBody ApproveRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long approverId = getCurrentUserId(httpRequest);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/approve approverId={} secondApproverId={}",
                    id, approverId, request != null ? request.getSecondApproverId() : null);
            outboundApplyService.approveOutboundApply(id, approverId, request.getSecondApproverId());
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/approve ok", id);
            return Result.success();
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/{}/approve fail: {}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 特殊药品第二审批：第一审批已完成（PENDING_SECOND）时，由第二审批人本人确认通过。
     */
    @PostMapping("/{id}/second-approve")
    @RequiresPermission({"outbound:approve", "outbound:approve:special"})
    public Result<Void> secondApproveOutboundApply(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long uid = getCurrentUserId(httpRequest);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/second-approve operatorId={}", id, uid);
            outboundApplyService.secondApproveOutboundApply(id, uid);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/second-approve ok", id);
            return Result.success();
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/{}/second-approve fail: {}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批出库申请（驳回）
     */
    @PostMapping("/{id}/reject")
    @RequiresPermission({"outbound:approve", "outbound:approve:special"})
    public Result<Void> rejectOutboundApply(
            @PathVariable Long id,
            @RequestBody RejectRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long approverId = getCurrentUserId(httpRequest);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/reject approverId={}", id, approverId);
            outboundApplyService.rejectOutboundApply(id, approverId, request.getRejectReason());
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/reject ok", id);
            return Result.success();
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/{}/reject fail: {}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 执行出库
     */
    @PostMapping("/{id}/execute")
    @RequiresPermission({"outbound:execute"})
    public Result<Void> executeOutbound(
            @PathVariable Long id,
            @RequestBody ExecuteOutboundRequest request) {
        try {
            int n = request.getOutboundItems() != null ? request.getOutboundItems().size() : 0;
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/execute outboundLines={}", id, n);
            outboundApplyService.executeOutbound(id, request.getOutboundItems());
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/execute ok (状态应变为已出库，库存与仪表盘统计随后一致)", id);
            return Result.success();
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/{}/execute fail: {}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消出库申请（需 drug:manage 权限）
     */
    @PostMapping("/{id}/cancel")
    @RequiresPermission({"drug:manage"})
    public Result<Void> cancelOutboundApply(@PathVariable Long id) {
        try {
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/cancel", id);
            outboundApplyService.cancelOutboundApply(id);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/cancel ok", id);
            return Result.success();
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/{}/cancel fail: {}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 申请人撤回出库申请（仅待审批状态下本人可撤回）
     */
    @PostMapping("/{id}/withdraw")
    @RequiresPermission({"outbound:apply"})
    public Result<Void> withdrawOutboundApply(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long applicantId = getCurrentUserId(httpRequest);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/withdraw applicantId={}", id, applicantId);
            outboundApplyService.withdrawOutboundApply(id, applicantId);
            log.info("[INT-FE-02][BE出库] POST /outbound/{}/withdraw ok", id);
            return Result.success();
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] POST /outbound/{}/withdraw fail: {}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询待审批的出库申请数量
     */
    @GetMapping("/pending-count")
    @RequiresPermission({
            "outbound:view",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute",
            "outbound:apply:on-behalf",
            "drug:manage"
    })
    public Result<Long> getPendingOutboundCount() {
        Long count = outboundApplyService.getPendingOutboundCount();
        return Result.success(count);
    }

    /**
     * 查询今日出库数量
     */
    @GetMapping("/today-count")
    @RequiresPermission({
            "outbound:view",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute",
            "outbound:apply:on-behalf",
            "drug:manage"
    })
    public Result<Long> getTodayOutboundCount() {
        Long count = outboundApplyService.getTodayOutboundCount();
        return Result.success(count);
    }

    /**
     * 出库拣货汇总：待执行申领单按药品、批次、存储位置汇总，供仓库打印现场拣货单（与执行出库 FIFO 规则一致模拟）。
     *
     * @param date  scope=approve_day 时按审批通过日期筛选，默认当天；scope=all_pending 时忽略
     * @param scope approve_day：指定日审批通过且未出库；all_pending：全部待执行
     */
    @GetMapping("/pick-summary")
    @RequiresPermission({"outbound:execute", "outbound:view", "outbound:approve", "outbound:approve:special"})
    public Result<Map<String, Object>> getOutboundPickSummary(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "scope", defaultValue = "approve_day") String scope) {
        return Result.success(outboundApplyService.getOutboundPickSummary(date, scope));
    }

    /**
     * 获取出库申请明细列表（列表查看、申请人查看详情等）
     */
    @GetMapping("/{id}/items")
    @RequiresPermission({
            "outbound:view",
            "outbound:apply",
            "outbound:apply:on-behalf",
            "outbound:approve",
            "outbound:approve:special",
            "outbound:execute",
            "drug:manage"
    })
    public Result<List<Map<String, Object>>> getOutboundApplyItems(@PathVariable Long id) {
        try {
            log.info("[INT-FE-02][BE出库] GET /outbound/{}/items", id);
            List<OutboundApplyItem> items = outboundApplyService.getOutboundApplyItems(id);
            List<Map<String, Object>> result = new ArrayList<>();
            for (OutboundApplyItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("drugId", item.getDrugId());
                itemMap.put("batchNumber", item.getBatchNumber());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("actualQuantity", item.getActualQuantity());
                itemMap.put("remark", item.getRemark());
                DrugInfo drug = drugInfoMapper.selectById(item.getDrugId());
                if (drug != null) {
                    itemMap.put("drugName", drug.getDrugName());
                    itemMap.put("specification", drug.getSpecification());
                    itemMap.put("isSpecial", drug.getIsSpecial());
                } else {
                    itemMap.put("drugName", null);
                    itemMap.put("specification", null);
                    itemMap.put("isSpecial", null);
                }
                result.add(itemMap);
            }
            log.info("[INT-FE-02][BE出库] GET /outbound/{}/items ok rows={}", id, result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[INT-FE-02][BE出库] GET /outbound/{}/items fail: {}", id, e.getMessage());
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
     * 从请求中获取Token（与 JwtAuthenticationFilter 一致：优先 Header，其次 Cookie）
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("cdiom_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
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
     * 代录出库申请请求 DTO
     */
    @Data
    public static class OutboundApplyOnBehalfRequest {
        @NotNull(message = "请选择申领医护人员")
        private Long applicantId;
        @NotBlank(message = "请填写所属科室")
        private String department;
        @NotBlank(message = "请填写用途说明")
        private String purpose;
        private String remark;
        @NotEmpty(message = "申领明细不能为空")
        private List<Map<String, Object>> items;
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

