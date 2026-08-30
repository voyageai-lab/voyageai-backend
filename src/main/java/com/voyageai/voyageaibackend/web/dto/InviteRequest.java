package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for inviting a collaborator.
 */
@Data
public class InviteRequest {

  @NotBlank
  @Email
  private String email;

  /** Role to assign: EDITOR or VIEWER. */
  @NotBlank
  private String role;
}
