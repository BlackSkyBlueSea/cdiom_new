package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.service.SupplierService;
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
 * 供应商管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class SupplierController {

    private final SupplierService supplierService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询供应商列表
     */
    @GetMapping
    public Result<Page<Supplier>> getSupplierList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus) {
        Page<Supplier> supplierPage = supplierService.getSupplierList(
                page, size, keyword, status, auditStatus);
        return Result.success(supplierPage);
    }

    /**
     * 根据ID查询供应商
     */
    @GetMapping("/{id}")
    public Result<Supplier> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return Result.success(supplier);
    }

    /**
     * 创建供应商
     */
    @PostMapping
    @RequiresPermission({"drug:manage"})
    public Result<Supplier> createSupplier(
            @RequestBody SupplierRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long createBy = getCurrentUserId(httpRequest);
            Supplier supplier = new Supplier();
            supplier.setName(request.getName());
            supplier.setContactPerson(request.getContactPerson());
            supplier.setPhone(request.getPhone());
            supplier.setAddress(request.getAddress());
            supplier.setCreditCode(request.getCreditCode());
            supplier.setLicenseImage(request.getLicenseImage());
            supplier.setLicenseExpiryDate(request.getLicenseExpiryDate());
            supplier.setStatus(request.getStatus() != null ? request.getStatus() : 2); // 默认待审核
            supplier.setAuditStatus(0); // 待审核
            supplier.setCreateBy(createBy);

            Supplier created = supplierService.createSupplier(supplier);
            return Result.success("供应商创建成功", created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新供应商
     */
    @PutMapping("/{id}")
    @RequiresPermission({"drug:manage"})
    public Result<Supplier> updateSupplier(
            @PathVariable Long id,
            @RequestBody SupplierRequest request) {
        try {
            Supplier supplier = supplierService.getSupplierById(id);
            if (supplier == null) {
                return Result.error("供应商不存在");
            }
            supplier.setName(request.getName());
            supplier.setContactPerson(request.getContactPerson());
            supplier.setPhone(request.getPhone());
            supplier.setAddress(request.getAddress());
            supplier.setCreditCode(request.getCreditCode());
            supplier.setLicenseImage(request.getLicenseImage());
            supplier.setLicenseExpiryDate(request.getLicenseExpiryDate());
            supplier.setStatus(request.getStatus());

            Supplier updated = supplierService.updateSupplier(supplier);
            return Result.success("供应商更新成功", updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除供应商（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @RequiresPermission({"drug:manage"})
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新供应商状态
     */
    @PostMapping("/{id}/status")
    @RequiresPermission({"drug:manage"})
    public Result<Void> updateSupplierStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        try {
            supplierService.updateSupplierStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核供应商
     */
    @PostMapping("/{id}/audit")
    @RequiresPermission({"drug:manage"})
    public Result<Void> auditSupplier(
            @PathVariable Long id,
            @RequestBody AuditRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long auditBy = getCurrentUserId(httpRequest);
            supplierService.auditSupplier(id, request.getAuditStatus(), request.getAuditReason(), auditBy);
            return Result.success();
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
     * 供应商请求DTO
     */
    @Data
    public static class SupplierRequest {
        private String name;
        private String contactPerson;
        private String phone;
        private String address;
        private String creditCode;
        private String licenseImage;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate licenseExpiryDate;
        private Integer status;
    }

    /**
     * 更新状态请求DTO
     */
    @Data
    public static class UpdateStatusRequest {
        private Integer status;
    }

    /**
     * 审核请求DTO
     */
    @Data
    public static class AuditRequest {
        private Integer auditStatus; // 1-已通过/2-已驳回
        private String auditReason;
    }
}

