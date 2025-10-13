package com.voyageai.voyageaibackend.config;

import com.voyageai.voyageaibackend.security.JwtAuthenticationFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the application.
 * Configures JWT-based stateless authentication, OAuth2 login, and CORS.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CorsConfigurationSource corsConfigurationSource;
  private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

  /**
   * Constructor with dependencies.
   *
   * @param jwtAuthenticationFilter the JWT authentication filter
   * @param corsConfigurationSource the CORS configuration source
   * @param oauth2LoginSuccessHandler the OAuth2 success handler
   */
  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        CorsConfigurationSource corsConfigurationSource,
                        OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.corsConfigurationSource = corsConfigurationSource;
    this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
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
        
        // Configure session management
        // Use IF_REQUIRED to support OAuth2 flow (needs session for state parameter)
        // but still use JWT for API authentication
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        )
        
        // Configure authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints (no authentication required)
            .requestMatchers(
                "/api/auth/register",     // Registration
                "/api/auth/login",        // Login
                "/api/health",            // Health check
                "/oauth2/**",             // OAuth2 endpoints
                "/login/oauth2/**",       // OAuth2 callback
                "/swagger-ui/**",         // Swagger UI
                "/swagger-ui.html",       // Swagger UI HTML
                "/v3/api-docs/**",        // OpenAPI docs
                "/actuator/**",           // Actuator endpoints
                "/error"                  // Error page
            ).permitAll()
            
            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        
        // Configure OAuth2 login
        .oauth2Login(oauth2 -> oauth2
            .successHandler(oauth2LoginSuccessHandler)
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

