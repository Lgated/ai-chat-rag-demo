package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 允许的源（前端地址）
        corsConfig.addAllowedOrigin("http://localhost:5174");
        
        // 允许的 HTTP 方法
        corsConfig.addAllowedMethod("*");  // 允许所有方法：GET, POST, PUT, DELETE 等
        
        // 允许的请求头
        corsConfig.addAllowedHeader("*");  // 允许所有请求头
        
        // 允许发送凭证（如 cookies）
        corsConfig.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        corsConfig.setMaxAge(3600L);
        
        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}

