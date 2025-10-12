package com.voyageai.voyageaibackend.security;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Success handler for OAuth2 login.
 * Handles user creation/update and JWT token generation after successful OAuth authentication.
 */
@Component
@Transactional
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  @Value("${frontend.url:http://localhost:3000}")
  private String frontendUrl;

  /**
   * Constructor for OAuth2LoginSuccessHandler.
   *
   * @param userRepository repository for user data access
   * @param jwtUtil utility for JWT operations
   */
  public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Handles successful OAuth2 authentication.
   * Creates or updates user record and redirects to frontend with JWT token.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param authentication the authentication object
   * @throws IOException if redirection fails
   */
  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
    OAuth2User oauthUser = oauthToken.getPrincipal();

    // Extract user information from OAuth2User
    String email = oauthUser.getAttribute("email");
    String name = oauthUser.getAttribute("name");
    String picture = oauthUser.getAttribute("picture");
    String providerId = oauthUser.getAttribute("sub"); // Google's unique user ID

    // Determine auth provider from registration ID
    String registrationId = oauthToken.getAuthorizedClientRegistrationId();
    User.AuthProvider authProvider = User.AuthProvider.valueOf(registrationId.toUpperCase());

    // Find or create user
    User user = processOauthUser(email, name, picture, providerId, authProvider);

    // Generate JWT token
    String token = jwtUtil.generateToken(user.getEmail(), user.getId());

    // Redirect to frontend with token
    String redirectUrl = String.format("%s/auth/callback?token=%s", frontendUrl, token);
    response.sendRedirect(redirectUrl);
  }

  /**
   * Processes OAuth user: creates new user or updates existing one.
   * Handles account merging logic.
   *
   * @param email user's email
   * @param name user's display name
   * @param picture user's profile picture URL
   * @param providerId OAuth provider's user ID
   * @param authProvider authentication provider type
   * @return the user entity
   */
  private User processOauthUser(
      String email,
      String name,
      String picture,
      String providerId,
      User.AuthProvider authProvider
  ) {
    // Check if user exists with this OAuth provider
    return userRepository.findByAuthProviderAndProviderUserId(authProvider, providerId)
        .orElseGet(() -> {
          // Check if user exists with same email (account merging scenario)
          return userRepository.findByEmail(email)
              .map(existingUser -> mergeOauthAccount(existingUser, providerId, authProvider))
              .orElseGet(() -> createNewOauthUser(
                  email, name, picture, providerId, authProvider
              ));
        });
  }

  /**
   * Creates a new user from OAuth data.
   *
   * @param email user's email
   * @param name user's display name
   * @param picture user's profile picture URL
   * @param providerId OAuth provider's user ID
   * @param authProvider authentication provider type
   * @return the created user
   */
  private User createNewOauthUser(
      String email,
      String name,
      String picture,
      String providerId,
      User.AuthProvider authProvider
  ) {
    User newUser = new User();
    newUser.setEmail(email);
    newUser.setDisplayName(name);
    newUser.setAvatarUrl(picture);
    newUser.setProviderUserId(providerId);
    newUser.setAuthProvider(authProvider);
    newUser.setPasswordHash(null); // OAuth users don't have passwords
    newUser.setCreatedAt(Instant.now());

    return userRepository.save(newUser);
  }

  /**
   * Merges OAuth account with existing local account.
   * Updates existing user to also support OAuth authentication.
   *
   * @param existingUser the existing user entity
   * @param providerId OAuth provider's user ID
   * @param authProvider authentication provider type
   * @return the updated user
   */
  private User mergeOauthAccount(
      User existingUser,
      String providerId,
      User.AuthProvider authProvider
  ) {
    // Update existing user to also support OAuth
    // Note: In a more complex scenario, you might want to store multiple auth methods
    // For now, we'll switch the auth provider if user originally registered locally
    existingUser.setAuthProvider(authProvider);
    existingUser.setProviderUserId(providerId);

    return userRepository.save(existingUser);
  }
}

