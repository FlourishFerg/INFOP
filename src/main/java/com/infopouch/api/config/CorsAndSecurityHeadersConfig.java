package com.infopouch.api.config;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
@Slf4j
public class CorsAndSecurityHeadersConfig {

  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;

  /** Security: Proper CORS configuration - restrict to known origins only */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> allowedOrigins =
        new ArrayList<>(
            List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:4200",
                frontendUrl,
                baseUrl));

    configuration.setAllowedOrigins(allowedOrigins);

    configuration.setAllowedMethods(
        java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
    configuration.setExposedHeaders(java.util.Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L); // 1 hour

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /** Security: Request logging for audit trail (without sensitive data) */
  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
    loggingFilter.setIncludeClientInfo(true);
    loggingFilter.setIncludeQueryString(true);
    loggingFilter.setIncludePayload(false); // Don't log request body to avoid PII
    loggingFilter.setMaxPayloadLength(10000);
    loggingFilter.setIncludeHeaders(false); // Don't log headers to avoid tokens
    loggingFilter.setAfterMessagePrefix("REQUEST DATA : ");
    loggingFilter.setAfterMessageSuffix("");
    return loggingFilter;
  }
}
