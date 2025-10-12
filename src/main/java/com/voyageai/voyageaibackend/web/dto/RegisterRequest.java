package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user registration request.
 * Contains user input for creating a new local account.
 */
@Data
public class RegisterRequest {

  /**
   * User's email address.
   * Must be a valid email format and is required.
   */
  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  /**
   * User's password.
   * Must be at least 8 characters long.
   */
  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters long")
  private String password;

  /**
   * User's display name.
   * Optional field for user profile.
   */
  @Size(max = 100, message = "Display name must not exceed 100 characters")
  private String displayName;
}

