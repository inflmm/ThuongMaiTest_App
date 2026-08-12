package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.example.demo.component.VisitorInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VisitorInterceptor visitorInterceptor;

    public WebConfig(VisitorInterceptor visitorInterceptor) {
        this.visitorInterceptor = visitorInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitorInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/admin/analytics/**", // API trang Admin
                    "/swagger-ui/**",          // Tài liệu Swagger
                    "/v3/api-docs/**",
                    "/assets/**"        // Tài nguyên tĩnh (CSS, JS, hình ảnh)
                );
    }
}
