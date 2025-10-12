package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for user login request.
 * Contains credentials for authentication.
 */
@Data
public class LoginRequest {

  /**
   * User's email address.
   * Must be a valid email format and is required.
   */
  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  /**
   * User's password.
   * Required for authentication.
   */
  @NotBlank(message = "Password is required")
  private String password;
}

