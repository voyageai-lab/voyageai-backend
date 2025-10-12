package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.AuthenticationException;
import com.voyageai.voyageaibackend.exception.BusinessException;
import com.voyageai.voyageaibackend.security.JwtUtil;
import com.voyageai.voyageaibackend.web.dto.AuthResponse;
import com.voyageai.voyageaibackend.web.dto.LoginRequest;
import com.voyageai.voyageaibackend.web.dto.RegisterRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for AuthService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtUtil jwtUtil;

  @InjectMocks
  private AuthService authService;

  private RegisterRequest registerRequest;
  private LoginRequest loginRequest;
  private User user;

  /**
   * Sets up test data before each test.
   */
  @BeforeEach
  void setUp() {
    registerRequest = new RegisterRequest();
    registerRequest.setEmail("test@example.com");
    registerRequest.setPassword("password123");
    registerRequest.setDisplayName("Test User");

    loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("password123");

    user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");
    user.setPasswordHash("$2a$10$encodedPassword");
    user.setDisplayName("Test User");
    user.setAuthProvider(User.AuthProvider.LOCAL);
  }

  /**
   * Tests successful user registration.
   */
  @Test
  void testRegister_Success() {
    // Arrange
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("mock-jwt-token");

    // Act
    AuthResponse response = authService.register(registerRequest);

    // Assert
    assertNotNull(response);
    assertEquals("Bearer", response.getTokenType());
    assertEquals("mock-jwt-token", response.getToken());
    assertNotNull(response.getUser());
    assertEquals("test@example.com", response.getUser().getEmail());
    
    verify(userRepository).existsByEmail("test@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userRepository).save(any(User.class));
    verify(jwtUtil).generateToken(anyString(), anyLong());
  }

  /**
   * Tests registration with existing email.
   */
  @Test
  void testRegister_EmailAlreadyExists() {
    // Arrange
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // Act & Assert
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> authService.register(registerRequest)
    );
    
    assertEquals("Email already registered", exception.getMessage());
    assertEquals("EMAIL_EXISTS", exception.getErrorCode());
    assertEquals(409, exception.getStatusCode());
    
    verify(userRepository).existsByEmail("test@example.com");
  }

  /**
   * Tests successful user login.
   */
  @Test
  void testLogin_Success() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("mock-jwt-token");

    // Act
    AuthResponse response = authService.login(loginRequest);

    // Assert
    assertNotNull(response);
    assertEquals("Bearer", response.getTokenType());
    assertEquals("mock-jwt-token", response.getToken());
    assertNotNull(response.getUser());
    assertEquals("test@example.com", response.getUser().getEmail());
    
    verify(userRepository).findByEmail("test@example.com");
    verify(passwordEncoder).matches("password123", "$2a$10$encodedPassword");
    verify(jwtUtil).generateToken(anyString(), anyLong());
  }

  /**
   * Tests login with non-existent email.
   */
  @Test
  void testLogin_UserNotFound() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    AuthenticationException exception = assertThrows(
        AuthenticationException.class,
        () -> authService.login(loginRequest)
    );
    
    assertEquals("Invalid email or password", exception.getMessage());
    
    verify(userRepository).findByEmail("test@example.com");
  }

  /**
   * Tests login with incorrect password.
   */
  @Test
  void testLogin_WrongPassword() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    // Act & Assert
    AuthenticationException exception = assertThrows(
        AuthenticationException.class,
        () -> authService.login(loginRequest)
    );
    
    assertEquals("Invalid email or password", exception.getMessage());
    
    verify(userRepository).findByEmail("test@example.com");
    verify(passwordEncoder).matches("password123", "$2a$10$encodedPassword");
  }

  /**
   * Tests login attempt for OAuth user (should fail).
   */
  @Test
  void testLogin_OAuthUser() {
    // Arrange
    user.setAuthProvider(User.AuthProvider.GOOGLE);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

    // Act & Assert
    AuthenticationException exception = assertThrows(
        AuthenticationException.class,
        () -> authService.login(loginRequest)
    );
    
    assertEquals(
        "This account uses GOOGLE authentication. Please login with GOOGLE",
        exception.getMessage()
    );
    
    verify(userRepository).findByEmail("test@example.com");
  }
}

