package com.cdiom.backend.config.security;

import com.cdiom.backend.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证访问受保护资源时返回与业务一致的 {@link Result} JSON（HTTP 401）。
 */
@Component
@RequiredArgsConstructor
public class JsonUnauthorizedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** 与 {@code GlobalExceptionHandler} 中认证失败文案保持一致 */
    private static final String MSG = "未登录或登录已过期";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> body = Result.error(401, MSG);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
