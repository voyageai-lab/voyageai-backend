package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for User entity operations.
 * Provides methods for user data access and manipulation.
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by their email address.
   *
   * @param email the email address to search for
   * @return an Optional containing the user if found, empty otherwise
   */
  Optional<User> findByEmail(String email);

  /**
   * Checks if a user exists with the given email address.
   *
   * @param email the email address to check
   * @return true if a user exists with the email, false otherwise
   */
  boolean existsByEmail(String email);

  /**
   * Finds a user by their OAuth provider and provider user ID.
   * Used for OAuth authentication (e.g., Google Login).
   *
   * @param authProvider the authentication provider
   * @param providerUserId the user ID from the OAuth provider
   * @return an Optional containing the user if found, empty otherwise
   */
  Optional<User> findByAuthProviderAndProviderUserId(
      User.AuthProvider authProvider,
      String providerUserId
  );
}

