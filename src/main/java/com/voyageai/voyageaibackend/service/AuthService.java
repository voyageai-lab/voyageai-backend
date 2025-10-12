package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.AuthenticationException;
import com.voyageai.voyageaibackend.exception.BusinessException;
import com.voyageai.voyageaibackend.security.JwtUtil;
import com.voyageai.voyageaibackend.web.dto.AuthResponse;
import com.voyageai.voyageaibackend.web.dto.LoginRequest;
import com.voyageai.voyageaibackend.web.dto.RegisterRequest;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for authentication operations.
 * Handles user registration, login, and JWT token generation.
 */
@Service
@Transactional
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  /**
   * Constructor for AuthService.
   * Uses constructor injection for better testability.
   *
   * @param userRepository repository for user data access
   * @param passwordEncoder encoder for password hashing
   * @param jwtUtil utility for JWT operations
   */
  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Registers a new user with local authentication.
   *
   * @param request the registration request containing user details
   * @return authentication response with JWT token
   * @throws BusinessException if email already exists
   */
  public AuthResponse register(RegisterRequest request) {
    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BusinessException("Email already registered", "EMAIL_EXISTS", 409);
    }

    // Create new user entity
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setDisplayName(request.getDisplayName() != null 
        ? request.getDisplayName() 
        : extractNameFromEmail(request.getEmail()));
    user.setAuthProvider(User.AuthProvider.LOCAL);
    user.setCreatedAt(Instant.now());

    // Save user to database
    User savedUser = userRepository.save(user);

    // Generate JWT token
    String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId());

    // Build and return response
    return buildAuthResponse(savedUser, token);
  }

  /**
   * Authenticates a user with email and password.
   *
   * @param request the login request containing credentials
   * @return authentication response with JWT token
   * @throws AuthenticationException if credentials are invalid
   */
  public AuthResponse login(LoginRequest request) {
    // Find user by email
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

    // Check if user is using local authentication
    if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
      throw new AuthenticationException(
          "This account uses " + user.getAuthProvider() + " authentication. "
          + "Please login with " + user.getAuthProvider()
      );
    }

    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new AuthenticationException("Invalid email or password");
    }

    // Generate JWT token
    String token = jwtUtil.generateToken(user.getEmail(), user.getId());

    // Build and return response
    return buildAuthResponse(user, token);
  }

  /**
   * Builds an AuthResponse from a User entity and token.
   *
   * @param user the user entity
   * @param token the JWT token
   * @return the authentication response
   */
  private AuthResponse buildAuthResponse(User user, String token) {
    AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .displayName(user.getDisplayName())
        .avatarUrl(user.getAvatarUrl())
        .authProvider(user.getAuthProvider().name())
        .build();

    return AuthResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .user(userInfo)
        .build();
  }

  /**
   * Extracts a display name from an email address.
   * Used as default when user doesn't provide a display name.
   *
   * @param email the email address
   * @return extracted name (part before @)
   */
  private String extractNameFromEmail(String email) {
    return email.split("@")[0];
  }
}

