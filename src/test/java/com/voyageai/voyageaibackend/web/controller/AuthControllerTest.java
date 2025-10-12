package com.voyageai.voyageaibackend.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.exception.AuthenticationException;
import com.voyageai.voyageaibackend.exception.BusinessException;
import com.voyageai.voyageaibackend.service.AuthService;
import com.voyageai.voyageaibackend.web.dto.AuthResponse;
import com.voyageai.voyageaibackend.web.dto.LoginRequest;
import com.voyageai.voyageaibackend.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link AuthController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AuthService authService;

  @Test
  void register_success() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setDisplayName("Test User");

    AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
        .id(1L)
        .email("test@example.com")
        .displayName("Test User")
        .authProvider("LOCAL")
        .build();

    AuthResponse response = AuthResponse.builder()
        .token("test.jwt.token")
        .user(userInfo)
        .build();

    when(authService.register(any(RegisterRequest.class))).thenReturn(response);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("test.jwt.token"))
        .andExpect(jsonPath("$.user.email").value("test@example.com"))
        .andExpect(jsonPath("$.user.displayName").value("Test User"));
  }

  @Test
  void register_emailAlreadyExists() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("existing@example.com");
    request.setPassword("password123");
    request.setDisplayName("Test User");

    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(new BusinessException("Email already registered", "EMAIL_EXISTS", 409));

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("EMAIL_EXISTS"))
        .andExpect(jsonPath("$.message").value("Email already registered"));
  }

  @Test
  void register_invalidEmail() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("invalid-email"); // Invalid email format
    request.setPassword("password123");
    request.setDisplayName("Test User");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"));
  }

  @Test
  void register_passwordTooShort() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setPassword("short"); // Less than 8 characters
    request.setDisplayName("Test User");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors.password").value("Password must be at least 8 characters long"));
  }

  @Test
  void login_success() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");

    AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
        .id(1L)
        .email("test@example.com")
        .displayName("Test User")
        .authProvider("LOCAL")
        .build();

    AuthResponse response = AuthResponse.builder()
        .token("test.jwt.token")
        .user(userInfo)
        .build();

    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("test.jwt.token"))
        .andExpect(jsonPath("$.user.email").value("test@example.com"));
  }

  @Test
  void login_invalidCredentials() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("wrongpassword");

    when(authService.login(any(LoginRequest.class)))
        .thenThrow(new AuthenticationException("Invalid email or password"));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"))
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void login_missingEmail() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setPassword("password123");
    // Email is missing

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void getCurrentUser_authenticated_returnsUserInfo() throws Exception {
    // Create a mock user
    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setEmail("test@example.com");
    mockUser.setDisplayName("Test User");
    mockUser.setAvatarUrl("https://example.com/avatar.jpg");
    mockUser.setAuthProvider(User.AuthProvider.LOCAL);

    // Create authentication
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(mockUser, null, null);

    // Perform request with authenticated user
    mockMvc.perform(get("/api/auth/me")
            .with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.displayName").value("Test User"))
        .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
        .andExpect(jsonPath("$.authProvider").value("LOCAL"));
  }

  @Test
  void getCurrentUser_unauthenticated_returns403() throws Exception {
    // Perform request without authentication
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isForbidden());
  }
}

