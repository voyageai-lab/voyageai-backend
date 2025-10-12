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

  /**
   * Configures CORS settings for the application.
   * Allows requests from the configured frontend URL.
   *
   * @return the CORS configuration source
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Allowed origins (frontend URLs)
    configuration.setAllowedOrigins(Arrays.asList(
        frontendUrl,              // Production/configured frontend
        "http://localhost:3000",  // React default dev server
        "http://localhost:5173",  // Vite default dev server
        "http://localhost:4200"   // Angular default dev server
    ));
    
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

