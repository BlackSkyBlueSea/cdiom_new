package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.*;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.OperationLogService;
import com.cdiom.backend.service.SupplierApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商准入审批控制器
 * 实现分权制衡的审批流程
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/supplier-approvals")
@RequiredArgsConstructor
@RequiresPermission({"supplier:approval:view"})
public class SupplierApprovalController {

    private final SupplierApprovalService approvalService;
    private final AuthService authService;
    private final OperationLogService operationLogService;

    /**
     * 创建审批申请（需要 supplier:approval:apply 权限）
     */
    @PostMapping
    @RequiresPermission({"supplier:approval:apply"})
    public Result<SupplierApprovalApplication> createApplication(
            @RequestBody ApprovalApplicationRequest request,
            HttpServletRequest httpRequest) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            SupplierApprovalApplication application = new SupplierApprovalApplication();
            application.setSupplierId(request.getSupplierId());
            application.setSupplierName(request.getSupplierName());
            application.setContactPerson(request.getContactPerson());
            application.setPhone(request.getPhone());
            application.setAddress(request.getAddress());
            application.setCreditCode(request.getCreditCode());
            application.setLicenseImage(request.getLicenseImage());
            application.setLicenseExpiryDate(request.getLicenseExpiryDate());
            application.setApplicationType(request.getApplicationType() != null ? 
                    request.getApplicationType() : "NEW");
            application.setRemark(request.getRemark());

            SupplierApprovalApplication created = approvalService.createApplication(
                    application, request.getItems(), currentUser.getId(), currentUser.getUsername());

            // 记录操作日志
            recordOperationLog(currentUser, "供应商准入审批", "INSERT", 
                    "创建供应商准入申请", httpRequest, request, true, null);

            return Result.success("申请创建成功，等待审核", created);
        } catch (Exception e) {
            log.error("创建申请失败", e);
            SysUser currentUser = authService.getCurrentUser();
            recordOperationLog(currentUser, "供应商准入审批", "INSERT", 
                    "创建供应商准入申请失败", httpRequest, request, false, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 资质核验（需要 supplier:approval:quality 权限）
     */
    @PostMapping("/{id}/quality-check")
    @RequiresPermission({"supplier:approval:quality"})
    public Result<Void> qualityCheck(
            @PathVariable Long id,
            @RequestBody QualityCheckRequest request,
            HttpServletRequest httpRequest) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            approvalService.qualityCheck(id, request.getResult(), request.getOpinion(),
                    currentUser.getId(), currentUser.getUsername(), getClientIp(httpRequest));

            recordOperationLog(currentUser, "供应商准入审批", "UPDATE", 
                    "资质核验", httpRequest, request, true, null);

            return Result.success("资质核验完成", null);
        } catch (Exception e) {
            log.error("资质核验失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 价格审核（需要 supplier:approval:price 权限）
     */
    @PostMapping("/{id}/price-review")
    @RequiresPermission({"supplier:approval:price"})
    public Result<Void> priceReview(
            @PathVariable Long id,
            @RequestBody PriceReviewRequest request,
            HttpServletRequest httpRequest) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            approvalService.priceReview(id, request.getResult(), request.getOpinion(),
                    currentUser.getId(), currentUser.getUsername(), getClientIp(httpRequest));

            recordOperationLog(currentUser, "供应商准入审批", "UPDATE", 
                    "价格审核", httpRequest, request, true, null);

            return Result.success("价格审核完成", null);
        } catch (Exception e) {
            log.error("价格审核失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 最终审批（需要 supplier:approval:final 权限）
     */
    @PostMapping("/{id}/final-approve")
    @RequiresPermission({"supplier:approval:final"})
    public Result<Void> finalApprove(
            @PathVariable Long id,
            @RequestBody FinalApproveRequest request,
            HttpServletRequest httpRequest) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            approvalService.finalApprove(id, request.getResult(), request.getOpinion(),
                    currentUser.getId(), currentUser.getUsername(), getClientIp(httpRequest));

            recordOperationLog(currentUser, "供应商准入审批", "UPDATE", 
                    "最终审批", httpRequest, request, true, null);

            return Result.success("审批完成", null);
        } catch (Exception e) {
            log.error("最终审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询申请详情（需要 supplier:approval:view 权限）
     */
    @GetMapping("/{id}")
    @RequiresPermission({"supplier:approval:view"})
    public Result<SupplierApprovalApplication> getApplication(@PathVariable Long id) {
        try {
            SupplierApprovalApplication application = approvalService.getApplicationById(id);
            if (application == null) {
                return Result.error("申请不存在");
            }
            return Result.success(application);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询审批日志（需要 supplier:approval:view 权限）
     */
    @GetMapping("/{id}/logs")
    @RequiresPermission({"supplier:approval:view"})
    public Result<List<SupplierApprovalLog>> getApprovalLogs(@PathVariable Long id) {
        try {
            List<SupplierApprovalLog> logs = approvalService.getApprovalLogs(id);
            return Result.success(logs);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 记录操作日志
     */
    private void recordOperationLog(SysUser currentUser, String module, String operationType,
                                    String operationContent, HttpServletRequest request,
                                    Object requestBody, boolean success, String errorMsg) {
        if (currentUser == null) return;
        
        OperationLog log = new OperationLog();
        log.setUserId(currentUser.getId());
        log.setUsername(currentUser.getUsername());
        log.setModule(module);
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setIp(getClientIp(request));
        }
        
        log.setStatus(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        
        operationLogService.saveLog(log);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 审批申请请求DTO
     */
    @Data
    public static class ApprovalApplicationRequest {
        private Long supplierId;
        private String supplierName;
        private String contactPerson;
        private String phone;
        private String address;
        private String creditCode;
        private String licenseImage;
        private java.time.LocalDate licenseExpiryDate;
        private String applicationType;
        private String remark;
        private List<SupplierApprovalItem> items;
    }

    /**
     * 资质核验请求DTO
     */
    @Data
    public static class QualityCheckRequest {
        private String result;  // PASS/FAIL
        private String opinion;
    }

    /**
     * 价格审核请求DTO
     */
    @Data
    public static class PriceReviewRequest {
        private String result;  // PASS/FAIL
        private String opinion;
    }

    /**
     * 最终审批请求DTO
     */
    @Data
    public static class FinalApproveRequest {
        private String result;  // APPROVED/REJECTED
        private String opinion;
    }
}

