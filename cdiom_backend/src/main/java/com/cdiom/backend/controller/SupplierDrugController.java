package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SupplierDrug;
import com.cdiom.backend.service.SupplierDrugService;
import com.cdiom.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 供应商-药品关联管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/supplier-drugs")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class SupplierDrugController {

    private final SupplierDrugService supplierDrugService;
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
     * 更新供应商-药品关联的单价
     */
    @PutMapping("/price")
    @RequiresPermission({"drug:manage"})
    public Result<SupplierDrug> updateSupplierDrugPrice(
            @RequestBody SupplierDrugPriceRequest request) {
        try {
            SupplierDrug supplierDrug = supplierDrugService.updateSupplierDrugPrice(
                    request.getSupplierId(), 
                    request.getDrugId(), 
                    request.getUnitPrice());
            return Result.success("单价更新成功", supplierDrug);
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
    }
}

