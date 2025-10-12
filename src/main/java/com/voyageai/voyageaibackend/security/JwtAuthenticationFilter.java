package com.voyageai.voyageaibackend.security;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter that validates JWT tokens from request headers.
 * Extracts the token from Authorization header, validates it, and sets Spring Security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  /**
   * Constructor with dependencies.
   *
   * @param jwtUtil the JWT utility for token operations
   * @param userRepository the user repository for user lookup
   */
  public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      @org.springframework.lang.NonNull HttpServletRequest request,
      @org.springframework.lang.NonNull HttpServletResponse response,
      @org.springframework.lang.NonNull FilterChain filterChain)
      throws ServletException, IOException {
    
    try {
      // Extract JWT from Authorization header
      String jwt = getJwtFromRequest(request);
      
      if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
        // Extract email from token
        String email = jwtUtil.extractEmail(jwt);
        
        // Load user from database
        User user = userRepository.findByEmail(email)
            .orElse(null);
        
        if (user != null) {
          // Create authentication token
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  user,
                  null,
                  null  // No authorities/roles for now
              );
          
          authentication.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request)
          );
          
          // Set authentication in Spring Security context
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (Exception ex) {
      // Log but don't block the request - let it proceed and potentially fail at endpoint level
      logger.error("Could not set user authentication in security context", ex);
    }
    
    filterChain.doFilter(request, response);
  }

  /**
   * Extracts JWT token from the Authorization header.
   *
   * @param request the HTTP request
   * @return the JWT token, or null if not present
   */
  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    
    return null;
  }
}

