package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a project (e.g., rename).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

  /** New project title. */
  @NotBlank(message = "Title is required")
  @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
  private String title;
}
