package com.jkingai.diagramarchitect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record ApiSecurityProperties(
        String apiKey,
        List<String> allowedOrigins
) {
}
