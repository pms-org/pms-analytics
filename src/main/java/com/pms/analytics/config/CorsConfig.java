package com.pms.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(split(allowedOrigins))
                .allowedMethods(split(allowedMethods))
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private String[] split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }
}
