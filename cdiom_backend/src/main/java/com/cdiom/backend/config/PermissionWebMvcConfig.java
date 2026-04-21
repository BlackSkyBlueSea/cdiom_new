package com.cdiom.backend.config;

import com.cdiom.backend.config.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册接口级权限拦截器，使 {@link com.cdiom.backend.annotation.RequiresPermission} 生效。
 */
@Configuration
@RequiredArgsConstructor
public class PermissionWebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
