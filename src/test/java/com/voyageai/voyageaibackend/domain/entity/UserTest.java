package com.voyageai.voyageaibackend.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link User} entity.
 */
class UserTest {

  @Test
  void user_gettersAndSetters() {
    User user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");
    user.setPasswordHash("hashedPassword");
    user.setDisplayName("Test User");
    user.setAvatarUrl("https://example.com/avatar.jpg");
    user.setAuthProvider(User.AuthProvider.LOCAL);
    user.setProviderUserId(null);
    
    Instant now = Instant.now();
    user.setCreatedAt(now);

    assertEquals(1L, user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("hashedPassword", user.getPasswordHash());
    assertEquals("Test User", user.getDisplayName());
    assertEquals("https://example.com/avatar.jpg", user.getAvatarUrl());
    assertEquals(User.AuthProvider.LOCAL, user.getAuthProvider());
    assertNull(user.getProviderUserId());
    assertEquals(now, user.getCreatedAt());
  }

  @Test
  void user_defaultValues() {
    User user = new User();
    
    // createdAt should be set by default
    assertNotNull(user.getCreatedAt());
    
    // authProvider should default to LOCAL
    assertEquals(User.AuthProvider.LOCAL, user.getAuthProvider());
  }

  @Test
  void user_oauthProvider() {
    User user = new User();
    user.setAuthProvider(User.AuthProvider.GOOGLE);
    user.setProviderUserId("google-id-123");
    user.setPasswordHash(null); // OAuth users don't have passwords

    assertEquals(User.AuthProvider.GOOGLE, user.getAuthProvider());
    assertEquals("google-id-123", user.getProviderUserId());
    assertNull(user.getPasswordHash());
  }

  @Test
  void authProvider_enumValues() {
    assertEquals(2, User.AuthProvider.values().length);
    assertEquals(User.AuthProvider.LOCAL, User.AuthProvider.valueOf("LOCAL"));
    assertEquals(User.AuthProvider.GOOGLE, User.AuthProvider.valueOf("GOOGLE"));
  }
}

