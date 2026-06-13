package com.awm.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 可在此注册拦截器、消息转换器等

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // V1 不做认证，后续可在此添加认证拦截器
    }
}
