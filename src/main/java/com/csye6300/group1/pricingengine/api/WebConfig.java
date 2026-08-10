package com.csye6300.group1.pricingengine.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the REST API layer, so a separately-hosted frontend (a generated
 * dashboard, a local dev server on another port, etc.) can call this API
 * across origins. Allowed origins come from application.properties
 * (cors.allowed-origins) rather than being hardcoded, following the same
 * config-over-code convention used for db.url and the pricing.* properties.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
