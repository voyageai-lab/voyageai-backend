package com.voyageai.voyageaibackend.security;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtAuthenticationFilter.
 */
class JwtAuthenticationFilterTest {

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_validToken_setsAuthentication() throws Exception {
    // Arrange
    String token = "valid.jwt.token";
    String email = "test@example.com";
    User user = new User();
    user.setId(1L);
    user.setEmail(email);

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractEmail(token)).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertEquals(user, authentication.getPrincipal());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_noAuthorizationHeader_continuesFilterChain() throws Exception {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtUtil, never()).validateToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_invalidToken_continuesFilterChain() throws Exception {
    // Arrange
    String token = "invalid.jwt.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.validateToken(token)).thenReturn(false);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findByEmail(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_userNotFound_continuesFilterChain() throws Exception {
    // Arrange
    String token = "valid.jwt.token";
    String email = "nonexistent@example.com";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractEmail(token)).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_authorizationHeaderWithoutBearer_continuesFilterChain() throws Exception {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtUtil, never()).validateToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_emptyAuthorizationHeader_continuesFilterChain() throws Exception {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn("");

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtUtil, never()).validateToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_exceptionDuringValidation_continuesFilterChain() throws Exception {
    // Arrange
    String token = "valid.jwt.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("JWT parsing error"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}

