package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.service.AuthService;
import com.voyageai.voyageaibackend.web.dto.AuthResponse;
import com.voyageai.voyageaibackend.web.dto.LoginRequest;
import com.voyageai.voyageaibackend.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 * Handles user registration and login operations.
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
}

