package ru.mzd.geoanalytics.dashboard.common.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private final ApplicationProperties applicationProperties;

    public CorsConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = applicationProperties.getCors().getAllowedOrigins();
        registry.addMapping("/api/**")
            .allowedOriginPatterns(origins.toArray(String[]::new))
            .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Request-Id")
            .allowCredentials(true);
    }
}
