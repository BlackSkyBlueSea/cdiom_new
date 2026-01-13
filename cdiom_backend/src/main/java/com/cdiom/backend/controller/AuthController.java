package com.cdiom.backend.controller;

import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request, 
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse response,
                                       @RequestHeader(value = "User-Agent", defaultValue = "") String userAgent) {
        try {
            // 解析User-Agent获取浏览器和操作系统信息
            String browser = parseBrowser(userAgent);
            String os = parseOS(userAgent);
            
            // 获取客户端真实IP地址
            String ip = getClientIp(httpRequest);
            
            Object[] loginResult = authService.login(request.getUsername(), request.getPassword(), ip, browser, os);
            String token = (String) loginResult[0];
            SysUser user = (SysUser) loginResult[1];
            
            // 将Token存储到Cookie
            Cookie cookie = new Cookie("cdiom_token", token);
            cookie.setPath("/");
            cookie.setMaxAge(8 * 60 * 60); // 8小时
            cookie.setHttpOnly(false); // 允许前端访问
            response.addCookie(cookie);
            
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(token);
            loginResponse.setUser(user);
            
            return Result.success("登录成功", loginResponse);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> getCurrentUser() {
        SysUser user = authService.getCurrentUser();
        if (user == null) {
            return Result.error(401, "未登录");
        }
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        authService.logout();
        
        // 清除Cookie
        Cookie cookie = new Cookie("cdiom_token", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        return Result.success();
    }

    /**
     * 解析浏览器类型
     */
    private String parseBrowser(String userAgent) {
        if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari")) {
            return "Safari";
        } else if (userAgent.contains("Edge")) {
            return "Edge";
        }
        return "Unknown";
    }

    /**
     * 解析操作系统
     */
    private String parseOS(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac")) {
            return "Mac OS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iOS")) {
            return "iOS";
        }
        return "Unknown";
    }

    /**
     * 获取客户端真实IP地址
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
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private SysUser user;
    }
}

