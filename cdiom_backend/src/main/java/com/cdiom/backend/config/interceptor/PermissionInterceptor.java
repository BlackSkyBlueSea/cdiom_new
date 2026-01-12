package com.cdiom.backend.config.interceptor;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.service.PermissionService;
import com.cdiom.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限拦截器
 * 用于检查用户是否有权限访问指定的接口
 * 
 * @author cdiom
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler) throws Exception {
        
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        
        // 检查方法上的权限注解
        RequiresPermission methodAnnotation = method.getAnnotation(RequiresPermission.class);
        
        // 检查类上的权限注解
        RequiresPermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        
        // 优先使用方法上的注解，如果没有则使用类上的注解
        RequiresPermission annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        // 如果没有权限注解，则允许访问
        if (annotation == null) {
            return true;
        }

        // 获取Token
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            writeErrorResponse(response, "未登录或Token已过期");
            return false;
        }

        // 获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            writeErrorResponse(response, "无法获取用户信息");
            return false;
        }

        // 检查权限
        String[] permissionCodes = annotation.value();
        if (permissionCodes == null || permissionCodes.length == 0) {
            // 如果没有指定权限代码，允许访问
            return true;
        }
        
        boolean requireAll = annotation.requireAll();

        try {
            if (requireAll) {
                // 要求所有权限都满足
                for (String permissionCode : permissionCodes) {
                    if (permissionCode == null || permissionCode.trim().isEmpty()) {
                        continue;
                    }
                    if (!permissionService.hasPermission(userId, permissionCode)) {
                        writeErrorResponse(response, "权限不足：" + permissionCode);
                        return false;
                    }
                }
            } else {
                // 任意一个权限满足即可
                if (!permissionService.hasAnyPermission(userId, permissionCodes)) {
                    writeErrorResponse(response, "权限不足，需要以下权限之一：" + String.join(", ", permissionCodes));
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("权限检查异常，userId: {}, permissionCodes: {}", userId, List.of(permissionCodes), e);
            writeErrorResponse(response, "权限检查失败，请联系管理员");
            return false;
        }

        return true;
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从Cookie获取
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("cdiom_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // 其次从Header获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}


