package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.model.SupplierDrug;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.OperationLogService;
import com.cdiom.backend.service.SupplierDrugService;
import com.cdiom.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 供应商-药品关联管理控制器
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/supplier-drugs")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class SupplierDrugController {

    private final SupplierDrugService supplierDrugService;
    private final AuthService authService;
    private final OperationLogService operationLogService;
    private final JwtUtil jwtUtil;

    /**
     * 添加供应商-药品关联
     */
    @PostMapping
    @RequiresPermission({"drug:manage"})
    public Result<SupplierDrug> addSupplierDrug(
            @RequestBody SupplierDrugRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long createBy = getCurrentUserId(httpRequest);
            SupplierDrug supplierDrug = supplierDrugService.addSupplierDrug(
                    request.getSupplierId(), 
                    request.getDrugId(), 
                    request.getUnitPrice(), 
                    createBy);
            return Result.success("关联添加成功", supplierDrug);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除供应商-药品关联
     */
    @DeleteMapping
    @RequiresPermission({"drug:manage"})
    public Result<Void> removeSupplierDrug(
            @RequestParam Long supplierId,
            @RequestParam Long drugId) {
        try {
            supplierDrugService.removeSupplierDrug(supplierId, drugId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新供应商-药品关联的单价（增强版：支持协议关联、历史记录和操作日志）
     */
    @PutMapping("/price")
    @RequiresPermission({"drug:manage"})
    public Result<SupplierDrug> updateSupplierDrugPrice(
            @RequestBody SupplierDrugPriceRequest request,
            HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            // 获取当前用户信息
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            
            // 获取客户端IP
            String ipAddress = getClientIp(httpRequest);
            
            // 更新价格（带协议关联和历史记录）
            SupplierDrug supplierDrug = supplierDrugService.updateSupplierDrugPrice(
                    request.getSupplierId(), 
                    request.getDrugId(), 
                    request.getUnitPrice(),
                    request.getAgreementId(),
                    request.getChangeReason(),
                    currentUser.getId(),
                    currentUser.getUsername(),
                    ipAddress);
            
            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "药品价格管理",
                    "UPDATE",
                    String.format("更新供应商-药品价格: 供应商ID=%d, 药品ID=%d, 价格=%s", 
                            request.getSupplierId(), request.getDrugId(), request.getUnitPrice()),
                    httpRequest,
                    request,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);
            
            return Result.success("单价更新成功", supplierDrug);
        } catch (Exception e) {
            log.error("更新价格失败", e);
            // 记录失败的操作日志
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "药品价格管理",
                        "UPDATE",
                        "更新供应商-药品价格失败",
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
     * 创建操作日志
     */
    private OperationLog createOperationLog(SysUser currentUser, String module, String operationType,
                                           String operationContent, HttpServletRequest request,
                                           Object requestBody, boolean success, String errorMsg) {
        OperationLog log = new OperationLog();
        
        // 操作人信息
        if (currentUser != null) {
            log.setUserId(currentUser.getId());
            log.setUsername(currentUser.getUsername());
        }
        
        // 操作信息
        log.setModule(module);
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        
        // 请求信息
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setIp(getClientIp(request));
            
            // 请求参数
            try {
                if (requestBody != null) {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String paramsJson = objectMapper.writeValueAsString(requestBody);
                    log.setRequestParams(paramsJson);
                }
            } catch (Exception e) {
                SupplierDrugController.log.warn("序列化请求参数失败", e);
            }
        }
        
        // 操作状态
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
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
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
     * 供应商-药品关联请求DTO
     */
    @Data
    public static class SupplierDrugRequest {
        private Long supplierId;
        private Long drugId;
        private BigDecimal unitPrice;
    }

    /**
     * 更新单价请求DTO
     */
    @Data
    public static class SupplierDrugPriceRequest {
        private Long supplierId;
        private Long drugId;
        private BigDecimal unitPrice;
        private Long agreementId;  // 关联的协议ID（可选）
        private String changeReason;  // 变更原因（可选）
    }
}

