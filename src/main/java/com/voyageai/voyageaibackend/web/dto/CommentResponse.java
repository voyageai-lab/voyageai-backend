package com.voyageai.voyageaibackend.web.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a comment (supports nested replies).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
  private Long id;
  private Long postId;
  private Long userId;
  private Long parentCommentId;
  private String content;
  private String authorDisplayName;
  private String authorAvatarUrl;
  private List<CommentResponse> replies;
  private Instant createdAt;
  private Instant updatedAt;
}
