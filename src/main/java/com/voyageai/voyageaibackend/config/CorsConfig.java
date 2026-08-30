package com.voyageai.voyageaibackend.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * Allows frontend applications from specified origins to access the API.
 */
@Configuration
public class CorsConfig {

  @Value("${frontend.url}")
  private String frontendUrl;

  @Value("${frontend.extra-origins:}")
  private String extraOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    var origins = new java.util.ArrayList<>(Arrays.asList(
        frontendUrl,
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:4200"
    ));
    if (extraOrigins != null && !extraOrigins.isBlank()) {
      Arrays.stream(extraOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(origins::add);
    }
    configuration.setAllowedOrigins(origins);
    
    // Allowed HTTP methods
    configuration.setAllowedMethods(Arrays.asList(
        "GET",
        "POST",
        "PUT",
        "PATCH",
        "DELETE",
        "OPTIONS"
    ));
    
    // Allowed headers
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers"
    ));
    
    // Expose headers (让前端可以访问这些响应头)
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "Content-Disposition"
    ));
    
    // Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(true);
    
    // Max age for preflight requests (in seconds)
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
  }
}

