package com.voyageai.voyageaibackend.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for DTO classes.
 * Tests getters, setters, and builders.
 */
class DtoTest {

  @Test
  void registerRequest_gettersAndSetters() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setDisplayName("Test User");

    assertEquals("test@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
    assertEquals("Test User", request.getDisplayName());
  }

  @Test
  void loginRequest_gettersAndSetters() {
    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");

    assertEquals("test@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
  }

  @Test
  void authResponse_builder() {
    AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
        .id(1L)
        .email("test@example.com")
        .displayName("Test User")
        .avatarUrl("https://example.com/avatar.jpg")
        .authProvider("LOCAL")
        .build();

    AuthResponse response = AuthResponse.builder()
        .token("test.jwt.token")
        .tokenType("Bearer")
        .user(userInfo)
        .build();

    assertNotNull(response);
    assertEquals("test.jwt.token", response.getToken());
    assertEquals("Bearer", response.getTokenType());
    assertNotNull(response.getUser());
    assertEquals(1L, response.getUser().getId());
    assertEquals("test@example.com", response.getUser().getEmail());
    assertEquals("Test User", response.getUser().getDisplayName());
    assertEquals("https://example.com/avatar.jpg", response.getUser().getAvatarUrl());
    assertEquals("LOCAL", response.getUser().getAuthProvider());
  }

  @Test
  void authResponse_defaultTokenType() {
    AuthResponse response = new AuthResponse();
    assertEquals("Bearer", response.getTokenType());
  }

  @Test
  void userInfo_noArgsConstructor() {
    AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
    assertNotNull(userInfo);
  }

  @Test
  void userInfo_allArgsConstructor() {
    AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
        1L,
        "test@example.com",
        "Test User",
        "https://example.com/avatar.jpg",
        "LOCAL"
    );

    assertEquals(1L, userInfo.getId());
    assertEquals("test@example.com", userInfo.getEmail());
    assertEquals("Test User", userInfo.getDisplayName());
    assertEquals("https://example.com/avatar.jpg", userInfo.getAvatarUrl());
    assertEquals("LOCAL", userInfo.getAuthProvider());
  }
}

