package com.voyageai.voyageaibackend.config;

import com.voyageai.voyageaibackend.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 * Configures JWT-based stateless authentication and OAuth2 Google Login.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  /**
   * Constructor for SecurityConfig.
   *
   * @param oAuth2LoginSuccessHandler handler for successful OAuth2 login
   */
  public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
    this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
  }

  /**
   * Configures the security filter chain.
   * Sets up endpoint security rules and authentication mechanisms.
   *
   * @param http the HttpSecurity object to configure
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Disable CSRF for stateless JWT authentication
        .csrf(AbstractHttpConfigurer::disable)
        
        // Configure session management to be stateless for JWT
        // But we need a temporary session for OAuth2 flow
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        )
        
        // Configure authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints (no authentication required)
            .requestMatchers(
                "/api/auth/**",           // Authentication endpoints
                "/api/health",            // Health check
                "/swagger-ui/**",         // Swagger UI
                "/swagger-ui.html",       // Swagger UI HTML
                "/v3/api-docs/**",        // OpenAPI docs
                "/actuator/**",           // Actuator endpoints
                "/error",                 // Error page
                "/login/**",              // OAuth2 login endpoints
                "/oauth2/**"              // OAuth2 callback endpoints
            ).permitAll()
            
            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        
        // Configure OAuth2 login
        .oauth2Login(oauth2 -> oauth2
            .successHandler(oAuth2LoginSuccessHandler)
        );

    return http.build();
  }

  /**
   * Provides BCrypt password encoder bean.
   * BCrypt is a strong hashing algorithm suitable for password storage.
   *
   * @return the password encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

