package com.cdiom.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Web配置类
 * 配置HTTP响应编码为UTF-8
 * 
 * @author cdiom
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        // 设置StringHttpMessageConverter使用UTF-8编码
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(
            Objects.requireNonNull(StandardCharsets.UTF_8, "UTF-8 charset must not be null")
        );
        converters.add(0, stringConverter);
    }
}


