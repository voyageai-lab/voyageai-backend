package com.voyageai.voyageaibackend.web.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a project collaborator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorResponse {
  private Long id;
  private String projectId;
  private Long userId;
  private String role;
  private String email;
  private String displayName;
  private String avatarUrl;
  private Instant acceptedAt;
  private Instant createdAt;
}
