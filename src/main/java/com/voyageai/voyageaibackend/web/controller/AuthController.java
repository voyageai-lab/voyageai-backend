package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.service.AuthService;
import com.voyageai.voyageaibackend.web.dto.AuthResponse;
import com.voyageai.voyageaibackend.web.dto.LoginRequest;
import com.voyageai.voyageaibackend.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 
 * <p><b>Note:</b> These endpoints are public (no authentication required).
 * Use these to obtain JWT tokens for accessing protected endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", 
     description = "User registration and login (get JWT token here)")
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
  @Operation(
      summary = "Register new user",
      description = "Create a new account and receive a JWT token. "
          + "Copy the token from the response and use it in the 'Authorize' button above."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "User registered successfully. JWT token returned.",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  value = "{"
                      + "\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", "
                      + "\"user\": {\"id\": 1, \"email\": \"user@example.com\", "
                      + "\"displayName\": \"John Doe\"}"
                      + "}"
              )
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
  })
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
  @Operation(
      summary = "Login with email & password",
      description = "Authenticate with your credentials and receive a JWT token. "
          + "Copy the token from the response and paste it in the 'Authorize' button above "
          + "(🔓 icon at top right) to access protected endpoints."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Login successful. JWT token returned.",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  value = "{"
                      + "\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", "
                      + "\"user\": {\"id\": 1, \"email\": \"user@example.com\", "
                      + "\"displayName\": \"John Doe\"}"
                      + "}"
              )
          )
      ),
      @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
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
  @Operation(
      summary = "Get current user info",
      description = "Retrieve information about the currently authenticated user"
  )
  @SecurityRequirement(name = "bearer-jwt")
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

    /**
     * Constructs a logout response with the given message.
     *
     * @param message the logout message
     */
    public LogoutResponse(String message) {
      this.message = message;
    }

  }
}

