package com.memdiag.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        if (!corsProperties.getAllowedOrigins().isEmpty()) {
            config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        }

        if (!corsProperties.getAllowedMethods().isEmpty()) {
            config.setAllowedMethods(corsProperties.getAllowedMethods());
        }

        if (!corsProperties.getAllowedHeaders().isEmpty()) {
            config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        } else {
            config.addAllowedHeader("*");
        }

        config.setAllowCredentials(corsProperties.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
