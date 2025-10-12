package com.voyageai.voyageaibackend.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication response.
 * Contains JWT token and user information (excluding sensitive data).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  /**
   * JWT access token for authentication.
   * Should be included in the Authorization header for subsequent requests.
   */
  private String token;

  /**
   * Token type (typically "Bearer").
   */
  @Builder.Default
  private String tokenType = "Bearer";

  /**
   * User information embedded in the response.
   */
  private UserInfo user;

  /**
   * Nested DTO containing safe user information (no password).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    /**
     * User's unique identifier.
     */
    private Long id;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's display name.
     */
    private String displayName;

    /**
     * User's avatar URL.
     */
    private String avatarUrl;

    /**
     * Authentication provider (LOCAL or GOOGLE).
     */
    private String authProvider;
  }
}

