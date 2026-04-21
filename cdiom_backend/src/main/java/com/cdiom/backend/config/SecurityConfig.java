package com.cdiom.backend.config;

import com.cdiom.backend.config.filter.JwtAuthenticationFilter;
import com.cdiom.backend.config.filter.SecurityHeadersFilter;
import com.cdiom.backend.config.security.JsonUnauthorizedAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 
 * @author cdiom
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonUnauthorizedAuthenticationEntryPoint jsonUnauthorizedAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（JWT使用无状态认证）
                .csrf(csrf -> csrf.disable())
                // 禁用Session（使用JWT，无状态）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 未认证访问受保护资源：HTTP 401 + 与业务一致的 Result JSON
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonUnauthorizedAuthenticationEntryPoint))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 允许访问登录接口
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        // 允许访问监控接口（公开访问）
                        .requestMatchers("/api/v1/system/info", "/api/v1/logs/recent", "/api/v1/health").permitAll()
                        // 允许WebSocket连接
                        .requestMatchers("/api/v1/logs/stream/**").permitAll()
                        // 允许访问静态资源
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // 所有API接口都需要认证
                        .requestMatchers("/api/v1/**").authenticated()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                // 安全响应头（X-Content-Type-Options 等）
                .addFilterBefore(new SecurityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 全局注册安全头过滤器，确保所有响应（含静态资源、错误页）都带上 X-Content-Type-Options
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilterRegistration() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

