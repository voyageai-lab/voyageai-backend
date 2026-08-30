package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for creating a comment.
 */
@Data
public class CreateCommentRequest {

  @NotBlank
  private String content;

  /** Null for top-level comments, set for replies. */
  private Long parentCommentId;
}
