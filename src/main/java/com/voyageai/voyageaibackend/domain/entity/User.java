package com.voyageai.voyageaibackend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * User entity representing a user in the system.
 * Supports both local authentication (email/password) and OAuth providers (Google).
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "uk_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  /**
   * Password hash for local authentication.
   * Nullable for OAuth users who don't have a password.
   */
  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  /**
   * Authentication provider type.
   * LOCAL: email/password authentication
   * GOOGLE: Google OAuth authentication
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "auth_provider", nullable = false)
  private AuthProvider authProvider = AuthProvider.LOCAL;

  /**
   * OAuth provider user ID (e.g., Google sub claim).
   * Only populated for OAuth users.
   */
  @Column(name = "provider_user_id")
  private String providerUserId;

  @Column(nullable = false, name = "created_at")
  private Instant createdAt = Instant.now();

  /**
   * Enum representing authentication provider types.
   */
  public enum AuthProvider {
    /**
     * Local email/password authentication.
     */
    LOCAL,

    /**
     * Google OAuth authentication.
     */
    GOOGLE
  }
}

