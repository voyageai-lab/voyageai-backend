package com.voyageai.voyageaibackend.web.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a community post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostResponse {
  private Long id;
  private Long userId;
  private String planId;
  private String projectId;
  private String title;
  private String description;
  private String coverImageUrl;
  private int likesCount;
  private int commentsCount;
  private int viewsCount;
  private String authorDisplayName;
  private String authorAvatarUrl;
  private boolean likedByMe;
  private Instant createdAt;
  private Instant updatedAt;
}
