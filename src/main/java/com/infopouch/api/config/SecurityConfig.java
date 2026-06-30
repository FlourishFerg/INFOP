package com.infopouch.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final CorsConfigurationSource corsConfigurationSource;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Security: Enable CORS with custom configuration
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        // Security: Add security headers for browser protection
        .headers(
            headers ->
                headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'self'; script-src 'self'")))
        // Security: Disable CSRF for stateless API (JWT-based)
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/**",
                        "/api/v1/email/**", // Allow email test endpoints
                        "/api/v1/research/public/**", // Public research catalog
                        // (search/view/citation)
                        "/api/v1/research/shared/**", // Public share-link resolution
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/v3/api-docs",
                        "/api-docs/**",
                        "/api-docs",
                        "/scalar/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/health", // Health check endpoint
                        "/actuator/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
