package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.EmailVerificationService;
import com.cdiom.backend.service.SysUserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 超级管理员管理控制器
 * 用于启用/停用超级管理员账户，需要邮箱验证码验证
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@RequiresPermission("user:manage") // 需要用户管理权限
public class SuperAdminController {

    private final SysUserService sysUserService;
    private final EmailVerificationService emailVerificationService;

    @Value("${email.verification.super-admin-username:super_admin}")
    private String superAdminUsername;

    /**
     * 发送验证码到指定邮箱
     */
    @PostMapping("/send-verification-code")
    public Result<String> sendVerificationCode(@RequestBody SendCodeRequest request) {
        try {
            emailVerificationService.sendVerificationCode(request.getEmail());
            return Result.success("验证码已发送到邮箱，请查收");
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return Result.error("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 启用超级管理员
     */
    @PostMapping("/enable")
    public Result<Void> enableSuperAdmin(@RequestBody EnableRequest request) {
        try {
            // 验证验证码
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getCode())) {
                return Result.error("验证码错误或已过期");
            }

            // 查找超级管理员用户
            SysUser superAdmin = sysUserService.getUserByUsername(superAdminUsername);
            if (superAdmin == null) {
                return Result.error("超级管理员用户不存在");
            }

            // 启用用户
            superAdmin.setStatus(1);
            sysUserService.updateUser(superAdmin);

            // 清除验证码
            emailVerificationService.clearCode(request.getEmail());

            log.info("超级管理员已启用，操作邮箱: {}", request.getEmail());
            return Result.success();
        } catch (Exception e) {
            log.error("启用超级管理员失败", e);
            return Result.error("启用失败: " + e.getMessage());
        }
    }

    /**
     * 停用超级管理员
     */
    @PostMapping("/disable")
    public Result<Void> disableSuperAdmin(@RequestBody DisableRequest request) {
        try {
            // 验证验证码
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getCode())) {
                return Result.error("验证码错误或已过期");
            }

            // 查找超级管理员用户
            SysUser superAdmin = sysUserService.getUserByUsername(superAdminUsername);
            if (superAdmin == null) {
                return Result.error("超级管理员用户不存在");
            }

            // 停用用户
            superAdmin.setStatus(0);
            sysUserService.updateUser(superAdmin);

            // 清除验证码
            emailVerificationService.clearCode(request.getEmail());

            log.info("超级管理员已停用，操作邮箱: {}", request.getEmail());
            return Result.success();
        } catch (Exception e) {
            log.error("停用超级管理员失败", e);
            return Result.error("停用失败: " + e.getMessage());
        }
    }

    /**
     * 查询超级管理员状态
     */
    @GetMapping("/status")
    public Result<SuperAdminStatus> getSuperAdminStatus() {
        try {
            SysUser superAdmin = sysUserService.getUserByUsername(superAdminUsername);
            if (superAdmin == null) {
                return Result.error("超级管理员用户不存在");
            }

            SuperAdminStatus status = new SuperAdminStatus();
            status.setUsername(superAdmin.getUsername());
            status.setStatus(superAdmin.getStatus());
            status.setStatusText(superAdmin.getStatus() == 1 ? "已启用" : "已停用");
            status.setCreateTime(superAdmin.getCreateTime());

            return Result.success(status);
        } catch (Exception e) {
            log.error("查询超级管理员状态失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Data
    static class SendCodeRequest {
        @NotBlank(message = "邮箱地址不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
    }

    @Data
    static class EnableRequest {
        @NotBlank(message = "邮箱地址不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;
    }

    @Data
    static class DisableRequest {
        @NotBlank(message = "邮箱地址不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;
    }

    @Data
    static class SuperAdminStatus {
        private String username;
        private Integer status;
        private String statusText;
        private java.time.LocalDateTime createTime;
    }
}

