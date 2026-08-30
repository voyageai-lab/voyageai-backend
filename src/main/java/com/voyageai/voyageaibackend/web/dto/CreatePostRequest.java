package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for creating a community post.
 */
@Data
public class CreatePostRequest {

  @NotBlank
  private String projectId;

  @NotBlank
  private String title;

  private String description;
}
