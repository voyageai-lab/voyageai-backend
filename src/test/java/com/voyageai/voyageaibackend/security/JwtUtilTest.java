package com.voyageai.voyageaibackend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link JwtUtil}.
 */
class JwtUtilTest {

  private JwtUtil jwtUtil;
  private static final String TEST_SECRET = "mySecretKeyForJWT123456789012345678901234567890";
  private static final long TEST_EXPIRATION = 3600000L; // 1 hour
  private static final String TEST_EMAIL = "test@example.com";
  private static final Long TEST_USER_ID = 123L;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
  }

  @Test
  void generateToken_success() {
    String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertTrue(token.startsWith("eyJ")); // JWT tokens start with eyJ
  }

  @Test
  void extractEmail_success() {
    String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

    String extractedEmail = jwtUtil.extractEmail(token);

    assertEquals(TEST_EMAIL, extractedEmail);
  }

  @Test
  void validateToken_validToken() {
    String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

    boolean isValid = jwtUtil.validateToken(token, TEST_EMAIL);

    assertTrue(isValid);
  }

  @Test
  void validateToken_wrongEmail() {
    String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

    boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

    assertFalse(isValid);
  }

  @Test
  void validateToken_invalidToken() {
    String invalidToken = "invalid.token.here";

    assertThrows(JwtException.class, () -> {
      jwtUtil.validateToken(invalidToken, TEST_EMAIL);
    });
  }

  @Test
  void extractEmail_expiredToken() {
    // Create a token with very short expiration
    JwtUtil shortExpirationUtil = new JwtUtil();
    ReflectionTestUtils.setField(shortExpirationUtil, "secret", TEST_SECRET);
    ReflectionTestUtils.setField(shortExpirationUtil, "expiration", 1L); // 1ms

    String token = shortExpirationUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

    // Wait for token to expire
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    assertThrows(ExpiredJwtException.class, () -> {
      jwtUtil.extractEmail(token);
    });
  }

  @Test
  void extractClaim_userId() {
    String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

    Long extractedUserId = jwtUtil.extractClaim(token, claims -> claims.get("userId", Long.class));

    assertEquals(TEST_USER_ID, extractedUserId);
  }
}

