package com.cdiom.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 用于配置静态资源访问
 * 
 * @author cdiom
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/api/v1/files}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 配置文件访问路径
        String fileUrl = urlPrefix.replace("/api/v1", "");
        registry.addResourceHandler(fileUrl + "/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}

