package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.model.SupplierDrugAgreement;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.OperationLogService;
import com.cdiom.backend.service.SupplierDrugAgreementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

/**
 * 供应商-药品价格协议管理控制器
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/supplier-drug-agreements")
@RequiredArgsConstructor
@RequiresPermission({"price:agreement:view"})
public class SupplierDrugAgreementController {

    private final SupplierDrugAgreementService agreementService;
    private final AuthService authService;
    private final OperationLogService operationLogService;

    /**
     * 创建价格协议（需要 price:agreement:manage 权限）
     */
    @PostMapping
    @RequiresPermission({"price:agreement:manage"})
    public Result<SupplierDrugAgreement> createAgreement(
            @RequestBody AgreementRequest request,
            HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            SupplierDrugAgreement agreement = new SupplierDrugAgreement();
            agreement.setSupplierId(request.getSupplierId());
            agreement.setDrugId(request.getDrugId());
            agreement.setAgreementNumber(request.getAgreementNumber());
            agreement.setAgreementName(request.getAgreementName());
            agreement.setAgreementFileUrl(request.getAgreementFileUrl());
            agreement.setAgreementType(request.getAgreementType() != null ? request.getAgreementType() : "PRICE");
            agreement.setEffectiveDate(request.getEffectiveDate());
            agreement.setExpiryDate(request.getExpiryDate());
            agreement.setUnitPrice(request.getUnitPrice());
            agreement.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
            agreement.setRemark(request.getRemark());
            agreement.setCreateBy(currentUser.getId());

            SupplierDrugAgreement created = agreementService.createAgreement(agreement);

            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "价格协议管理",
                    "INSERT",
                    String.format("创建价格协议: 协议编号=%s, 供应商ID=%d, 药品ID=%d", 
                            request.getAgreementNumber(), request.getSupplierId(), request.getDrugId()),
                    httpRequest,
                    request,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);

            return Result.success("协议创建成功", created);
        } catch (Exception e) {
            log.error("创建协议失败", e);
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "价格协议管理",
                        "INSERT",
                        "创建价格协议失败",
                        httpRequest,
                        request,
                        false,
                        e.getMessage()
                );
                operationLogService.saveLog(operationLog);
            }
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询协议（需要 price:agreement:view 权限）
     */
    @GetMapping("/{id}")
    @RequiresPermission({"price:agreement:view"})
    public Result<SupplierDrugAgreement> getAgreementById(@PathVariable Long id) {
        try {
            SupplierDrugAgreement agreement = agreementService.getAgreementById(id);
            if (agreement == null) {
                return Result.error("协议不存在");
            }
            return Result.success(agreement);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据供应商ID和药品ID查询当前生效的协议（需要 price:agreement:view 权限）
     */
    @GetMapping("/current")
    @RequiresPermission({"price:agreement:view"})
    public Result<SupplierDrugAgreement> getCurrentAgreement(
            @RequestParam Long supplierId,
            @RequestParam Long drugId) {
        try {
            SupplierDrugAgreement agreement = agreementService.getCurrentAgreement(supplierId, drugId);
            return Result.success(agreement);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据供应商ID和药品ID查询所有协议（需要 price:agreement:view 权限）
     */
    @GetMapping("/list")
    @RequiresPermission({"price:agreement:view"})
    public Result<List<SupplierDrugAgreement>> getAgreements(
            @RequestParam Long supplierId,
            @RequestParam Long drugId) {
        try {
            List<SupplierDrugAgreement> agreements = agreementService.getAgreementsBySupplierAndDrug(supplierId, drugId);
            return Result.success(agreements);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新协议（需要 price:agreement:manage 权限）
     */
    @PutMapping("/{id}")
    @RequiresPermission({"price:agreement:manage"})
    public Result<SupplierDrugAgreement> updateAgreement(
            @PathVariable Long id,
            @RequestBody AgreementRequest request,
            HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            SupplierDrugAgreement agreement = agreementService.getAgreementById(id);
            if (agreement == null) {
                return Result.error("协议不存在");
            }

            // 更新字段
            if (request.getAgreementNumber() != null) {
                agreement.setAgreementNumber(request.getAgreementNumber());
            }
            if (request.getAgreementName() != null) {
                agreement.setAgreementName(request.getAgreementName());
            }
            if (request.getAgreementFileUrl() != null) {
                agreement.setAgreementFileUrl(request.getAgreementFileUrl());
            }
            if (request.getAgreementType() != null) {
                agreement.setAgreementType(request.getAgreementType());
            }
            if (request.getEffectiveDate() != null) {
                agreement.setEffectiveDate(request.getEffectiveDate());
            }
            if (request.getExpiryDate() != null) {
                agreement.setExpiryDate(request.getExpiryDate());
            }
            if (request.getUnitPrice() != null) {
                agreement.setUnitPrice(request.getUnitPrice());
            }
            if (request.getCurrency() != null) {
                agreement.setCurrency(request.getCurrency());
            }
            if (request.getRemark() != null) {
                agreement.setRemark(request.getRemark());
            }

            SupplierDrugAgreement updated = agreementService.updateAgreement(agreement);

            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "价格协议管理",
                    "UPDATE",
                    String.format("更新价格协议: ID=%d", id),
                    httpRequest,
                    request,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);

            return Result.success("协议更新成功", updated);
        } catch (Exception e) {
            log.error("更新协议失败", e);
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "价格协议管理",
                        "UPDATE",
                        "更新价格协议失败",
                        httpRequest,
                        request,
                        false,
                        e.getMessage()
                );
                operationLogService.saveLog(operationLog);
            }
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除协议（需要 price:agreement:manage 权限）
     */
    @DeleteMapping("/{id}")
    @RequiresPermission({"price:agreement:manage"})
    public Result<Void> deleteAgreement(@PathVariable Long id, HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }

            agreementService.deleteAgreement(id);

            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "价格协议管理",
                    "DELETE",
                    String.format("删除价格协议: ID=%d", id),
                    httpRequest,
                    null,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);

            return Result.success("协议删除成功", null);
        } catch (Exception e) {
            log.error("删除协议失败", e);
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "价格协议管理",
                        "DELETE",
                        "删除价格协议失败",
                        httpRequest,
                        null,
                        false,
                        e.getMessage()
                );
                operationLogService.saveLog(operationLog);
            }
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建操作日志
     */
    private OperationLog createOperationLog(SysUser currentUser, String module, String operationType,
                                           String operationContent, HttpServletRequest request,
                                           Object requestBody, boolean success, String errorMsg) {
        OperationLog log = new OperationLog();
        
        if (currentUser != null) {
            log.setUserId(currentUser.getId());
            log.setUsername(currentUser.getUsername());
        }
        
        log.setModule(module);
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setIp(getClientIp(request));
            
            try {
                if (requestBody != null) {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String paramsJson = objectMapper.writeValueAsString(requestBody);
                    log.setRequestParams(paramsJson);
                }
            } catch (Exception e) {
                SupplierDrugAgreementController.log.warn("序列化请求参数失败", e);
            }
        }
        
        log.setStatus(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        
        return log;
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
            ip = request.getHeader("WL-Proxy-Client-IP");
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
     * 协议请求DTO
     */
    @Data
    public static class AgreementRequest {
        private Long supplierId;
        private Long drugId;
        private String agreementNumber;
        private String agreementName;
        private String agreementFileUrl;
        private String agreementType;
        private LocalDate effectiveDate;
        private LocalDate expiryDate;
        private java.math.BigDecimal unitPrice;
        private String currency;
        private String remark;
    }
}

