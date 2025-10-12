package com.voyageai.voyageaibackend.config;

import com.voyageai.voyageaibackend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the application.
 * Configures JWT-based stateless authentication and CORS.
 * OAuth2 Google Login is disabled by default (can be enabled in application.properties).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CorsConfigurationSource corsConfigurationSource;

  /**
   * Constructor with dependencies.
   *
   * @param jwtAuthenticationFilter the JWT authentication filter
   * @param corsConfigurationSource the CORS configuration source
   */
  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        CorsConfigurationSource corsConfigurationSource) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.corsConfigurationSource = corsConfigurationSource;
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
        // Enable CORS with custom configuration
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        
        // Disable CSRF for stateless JWT authentication
        .csrf(AbstractHttpConfigurer::disable)
        
        // Configure session management to be stateless for JWT
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        
        // Configure authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints (no authentication required)
            .requestMatchers(
                "/api/auth/register",     // Registration
                "/api/auth/login",        // Login
                "/api/health",            // Health check
                "/swagger-ui/**",         // Swagger UI
                "/swagger-ui.html",       // Swagger UI HTML
                "/v3/api-docs/**",        // OpenAPI docs
                "/actuator/**",           // Actuator endpoints
                "/error"                  // Error page
            ).permitAll()
            
            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        
        // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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

