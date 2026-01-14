package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.OperationLogService;
import com.cdiom.backend.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理控制器
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@RequiresPermission("user:manage")
public class SysUserController {

    private final SysUserService sysUserService;
    private final AuthService authService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result<Page<SysUser>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status) {
        Page<SysUser> userPage = sysUserService.getUserList(page, size, keyword, roleId, status);
        // 清除密码信息
        userPage.getRecords().forEach(user -> user.setPassword(null));
        return Result.success(userPage);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = sysUserService.getUserById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<SysUser> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            SysUser user = new SysUser();
            user.setUsername(request.getUsername());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword());
            user.setRoleId(request.getRoleId());
            user.setStatus(request.getStatus());
            
            SysUser createdUser = sysUserService.createUser(user);
            createdUser.setPassword(null);
            return Result.success("创建成功", createdUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<SysUser> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            SysUser user = new SysUser();
            user.setId(id);
            user.setUsername(request.getUsername());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword());
            user.setRoleId(request.getRoleId());
            user.setStatus(request.getStatus());
            
            SysUser updatedUser = sysUserService.updateUser(user);
            updatedUser.setPassword(null);
            return Result.success("更新成功", updatedUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            sysUserService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        try {
            sysUserService.updateUserStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 解锁用户
     */
    @PutMapping("/{id}/unlock")
    public Result<Void> unlockUser(@PathVariable Long id) {
        try {
            sysUserService.unlockUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取已删除用户列表（回收站）
     */
    @GetMapping("/deleted")
    public Result<Page<SysUser>> getDeletedUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        try {
            Page<SysUser> userPage = sysUserService.getDeletedUserList(page, size, keyword);
            // 清除密码信息
            userPage.getRecords().forEach(user -> user.setPassword(null));
            return Result.success(userPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 恢复用户
     */
    @PutMapping("/{id}/restore")
    public Result<Void> restoreUser(@PathVariable Long id, HttpServletRequest request) {
        OperationLog operationLog = null;
        try {
            SysUser currentUser = authService.getCurrentUser();
            
            // 先尝试获取用户信息（用于日志记录）
            // 由于@TableLogic可能过滤已删除的用户，我们尝试从已删除列表中获取
            String username = "ID:" + id;
            try {
                Page<SysUser> deletedPage = sysUserService.getDeletedUserList(1, 1, null);
                SysUser deletedUser = deletedPage.getRecords().stream()
                        .filter(u -> u.getId().equals(id))
                        .findFirst()
                        .orElse(null);
                if (deletedUser != null) {
                    username = deletedUser.getUsername();
                }
            } catch (Exception e) {
                // 如果获取失败，使用默认值
                log.warn("获取已删除用户信息失败", e);
            }
            
            sysUserService.restoreUser(id);
            
            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "用户管理",
                    "UPDATE",
                    "恢复用户：" + username,
                    request,
                    null,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);
            
            return Result.success();
        } catch (Exception e) {
            log.error("恢复用户失败", e);
            // 记录失败的操作日志
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "用户管理",
                        "UPDATE",
                        "恢复用户失败",
                        request,
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
     * 永久删除用户（物理删除）
     */
    @DeleteMapping("/{id}/permanent")
    public Result<Void> permanentlyDeleteUser(
            @PathVariable Long id,
            @RequestBody PermanentDeleteRequest request,
            HttpServletRequest httpRequest) {
        OperationLog operationLog = null;
        try {
            // 验证DELETE字符串
            if (!"DELETE".equals(request.getConfirmText())) {
                return Result.error("确认文本不正确，请输入 'DELETE' 进行确认");
            }
            
            SysUser currentUser = authService.getCurrentUser();
            
            // 先获取用户信息（用于日志记录）
            String username = "ID:" + id;
            try {
                Page<SysUser> deletedPage = sysUserService.getDeletedUserList(1, 100, null);
                SysUser deletedUser = deletedPage.getRecords().stream()
                        .filter(u -> u.getId().equals(id))
                        .findFirst()
                        .orElse(null);
                if (deletedUser != null) {
                    username = deletedUser.getUsername();
                }
            } catch (Exception e) {
                log.warn("获取已删除用户信息失败", e);
            }
            
            // 执行物理删除
            sysUserService.permanentlyDeleteUser(id);
            
            // 记录操作日志
            operationLog = createOperationLog(
                    currentUser,
                    "用户管理",
                    "DELETE",
                    "永久删除用户：" + username + "（物理删除）",
                    httpRequest,
                    request,
                    true,
                    null
            );
            operationLogService.saveLog(operationLog);
            
            log.warn("用户被永久删除，操作人：{}，被删除用户：{}", 
                    currentUser != null ? currentUser.getUsername() : "未知", username);
            
            return Result.success();
        } catch (Exception e) {
            log.error("永久删除用户失败", e);
            // 记录失败的操作日志
            if (operationLog == null) {
                SysUser currentUser = authService.getCurrentUser();
                operationLog = createOperationLog(
                        currentUser,
                        "用户管理",
                        "DELETE",
                        "永久删除用户失败",
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
            
            // 请求参数（隐藏敏感信息）
            try {
                if (requestBody != null) {
                    String paramsJson = objectMapper.writeValueAsString(requestBody);
                    log.setRequestParams(paramsJson);
                }
            } catch (Exception e) {
                SysUserController.log.warn("序列化请求参数失败", e);
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

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度必须在2-50个字符之间")
        private String username;
        
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号（11位数字，以1开头）")
        private String phone;
        
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
        private String password;
        
        @NotNull(message = "角色ID不能为空")
        private Long roleId;
        
        private Integer status;
    }
    
    @Data
    public static class UpdateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度必须在2-50个字符之间")
        private String username;
        
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号（11位数字，以1开头）")
        private String phone;
        
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
        private String password; // 更新时密码可选
        
        @NotNull(message = "角色ID不能为空")
        private Long roleId;
        
        private Integer status;
    }
    
    @Data
    public static class UpdateStatusRequest {
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值必须为0或1")
        @Max(value = 1, message = "状态值必须为0或1")
        private Integer status;
    }
    
    @Data
    public static class PermanentDeleteRequest {
        @NotBlank(message = "确认文本不能为空")
        private String confirmText;
    }
}

