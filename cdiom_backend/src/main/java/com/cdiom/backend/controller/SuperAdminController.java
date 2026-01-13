package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.EmailVerificationService;
import com.cdiom.backend.service.OperationLogService;
import com.cdiom.backend.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthService authService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Value("${email.verification.super-admin-username:super_admin}")
    private String superAdminUsername;

    /**
     * 发送验证码到指定邮箱
     */
    @PostMapping("/send-verification-code")
    public Result<String> sendVerificationCode(@RequestBody SendCodeRequest request) {
        try {
            // 获取当前登录用户
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录或登录已过期，请重新登录");
            }

            // 验证邮箱必须是当前登录用户绑定的邮箱
            if (currentUser.getEmail() == null || currentUser.getEmail().trim().isEmpty()) {
                return Result.error("当前用户账户未绑定邮箱，请先在个人设置中绑定邮箱地址");
            }

            if (!currentUser.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
                return Result.error("邮箱地址不匹配，请输入当前登录用户账户绑定的个人完整邮箱");
            }

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
    public Result<Void> enableSuperAdmin(@RequestBody EnableRequest request, HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            // 获取当前登录用户
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录或登录已过期，请重新登录");
            }

            // 查找超级管理员用户
            SysUser superAdmin = sysUserService.getUserByUsername(superAdminUsername);
            if (superAdmin == null) {
                return Result.error("超级管理员用户不存在");
            }

            // 验证邮箱必须是当前登录用户绑定的邮箱
            if (currentUser.getEmail() == null || currentUser.getEmail().trim().isEmpty()) {
                return Result.error("当前用户账户未绑定邮箱，请先在个人设置中绑定邮箱地址");
            }

            if (!currentUser.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
                return Result.error("邮箱地址不匹配，请输入当前登录用户账户绑定的个人完整邮箱");
            }

            // 验证验证码
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getCode())) {
                return Result.error("验证码错误或已过期");
            }

            // 启用用户
            superAdmin.setStatus(1);
            sysUserService.updateUser(superAdmin);

            // 清除验证码
            emailVerificationService.clearCode(request.getEmail());

            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "用户管理",
                    "UPDATE",
                    "启用超级管理员账户",
                    httpRequest,
                    request,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);

            log.info("超级管理员已启用，操作人: {}, 操作邮箱: {}", currentUser.getUsername(), request.getEmail());
            return Result.success();
        } catch (Exception e) {
            log.error("启用超级管理员失败", e);
            // 记录失败的操作日志
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "用户管理",
                        "UPDATE",
                        "启用超级管理员账户",
                        httpRequest,
                        request,
                        false,
                        e.getMessage()
                );
            } else {
                operationLog.setStatus(0);
                operationLog.setErrorMsg(e.getMessage());
            }
            try {
                operationLogService.saveLog(operationLog);
            } catch (Exception logException) {
                log.error("保存操作日志失败", logException);
            }
            return Result.error("启用失败: " + e.getMessage());
        }
    }

    /**
     * 停用超级管理员
     */
    @PostMapping("/disable")
    public Result<DisableResponse> disableSuperAdmin(@RequestBody DisableRequest request, HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            // 获取当前登录用户
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录或登录已过期，请重新登录");
            }

            // 查找超级管理员用户
            SysUser superAdmin = sysUserService.getUserByUsername(superAdminUsername);
            if (superAdmin == null) {
                return Result.error("超级管理员用户不存在");
            }

            // 验证邮箱必须是当前登录用户绑定的邮箱
            if (currentUser.getEmail() == null || currentUser.getEmail().trim().isEmpty()) {
                return Result.error("当前用户账户未绑定邮箱，请先在个人设置中绑定邮箱地址");
            }

            if (!currentUser.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
                return Result.error("邮箱地址不匹配，请输入当前登录用户账户绑定的个人完整邮箱");
            }

            // 验证验证码
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getCode())) {
                return Result.error("验证码错误或已过期");
            }

            // 检查当前登录用户是否是超级管理员
            boolean isCurrentUser = superAdminUsername.equals(currentUser.getUsername());

            // 停用用户
            superAdmin.setStatus(0);
            sysUserService.updateUser(superAdmin);

            // 清除验证码
            emailVerificationService.clearCode(request.getEmail());

            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "用户管理",
                    "UPDATE",
                    "停用超级管理员账户" + (isCurrentUser ? "（当前登录用户）" : ""),
                    httpRequest,
                    request,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);

            log.info("超级管理员已停用，操作人: {}, 操作邮箱: {}, 是否当前用户: {}", 
                    currentUser.getUsername(), request.getEmail(), isCurrentUser);

            // 返回响应，包含是否当前用户被禁用的标识
            DisableResponse response = new DisableResponse();
            response.setCurrentUserDisabled(isCurrentUser);
            return Result.success("超级管理员已停用", response);
        } catch (Exception e) {
            log.error("停用超级管理员失败", e);
            // 记录失败的操作日志
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "用户管理",
                        "UPDATE",
                        "停用超级管理员账户",
                        httpRequest,
                        request,
                        false,
                        e.getMessage()
                );
            } else {
                operationLog.setStatus(0);
                operationLog.setErrorMsg(e.getMessage());
            }
            try {
                operationLogService.saveLog(operationLog);
            } catch (Exception logException) {
                log.error("保存操作日志失败", logException);
            }
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
            status.setEmail(superAdmin.getEmail());

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
        private String email;
    }

    @Data
    static class DisableResponse {
        /** 是否当前登录用户被禁用 */
        private Boolean currentUserDisabled;
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
            
            // 请求参数（隐藏敏感信息）
            try {
                if (requestBody != null) {
                    String paramsJson = objectMapper.writeValueAsString(requestBody);
                    // 隐藏验证码信息
                    if (paramsJson.contains("\"code\"")) {
                        paramsJson = paramsJson.replaceAll("\"code\"\\s*:\\s*\"[^\"]*\"", "\"code\":\"******\"");
                    }
                    log.setRequestParams(paramsJson);
                }
            } catch (Exception e) {
                SuperAdminController.log.warn("序列化请求参数失败", e);
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
        // 处理多个IP的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

