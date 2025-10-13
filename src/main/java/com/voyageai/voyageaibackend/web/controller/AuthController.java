package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.service.AuthService;
import com.voyageai.voyageaibackend.web.dto.AuthResponse;
import com.voyageai.voyageaibackend.web.dto.LoginRequest;
import com.voyageai.voyageaibackend.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 * Handles user registration, login, logout, and user info retrieval.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  /**
   * Constructor for AuthController.
   *
   * @param authService service for authentication operations
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Registers a new user with email and password.
   *
   * @param request the registration request
   * @return authentication response with JWT token
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Authenticates a user with email and password.
   *
   * @param request the login request
   * @return authentication response with JWT token
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets the current authenticated user's information.
   * Requires a valid JWT token in the Authorization header.
   *
   * @return user information (without password)
   */
  @GetMapping("/me")
  public ResponseEntity<AuthResponse.UserInfo> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    User user = (User) authentication.getPrincipal();
    
    // Build UserInfo from the authenticated User entity
    AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .displayName(user.getDisplayName())
        .avatarUrl(user.getAvatarUrl())
        .authProvider(user.getAuthProvider().name())
        .build();
    
    return ResponseEntity.ok(userInfo);
  }

  /**
   * Logs out the current user.
   * Note: Since we use stateless JWT authentication, the actual token invalidation
   * happens on the client side (removing the token from storage).
   * This endpoint is provided for:
   * - Logging logout events
   * - Future enhancements (token blacklisting, session cleanup, etc.)
   * - Consistent API design
   *
   * @return success message
   */
  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout() {
    // Get current user for logging purposes
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication != null && authentication.isAuthenticated()) {
      User user = (User) authentication.getPrincipal();
      // Log logout event (optional)
      // In the future, could add token blacklisting here
      System.out.println("User logged out: " + user.getEmail());
    }
    
    // Clear security context
    SecurityContextHolder.clearContext();
    
    return ResponseEntity.ok(new LogoutResponse("Logout successful"));
  }

  /**
   * Response DTO for logout endpoint.
   */
  @Getter
  public static class LogoutResponse {
    private final String message;

    public LogoutResponse(String message) {
      this.message = message;
    }

  }
}

