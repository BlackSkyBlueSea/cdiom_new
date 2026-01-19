package com.cdiom.backend.config;

import com.cdiom.backend.config.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（JWT使用无状态认证）
                .csrf(csrf -> csrf.disable())
                // 禁用Session（使用JWT，无状态）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

