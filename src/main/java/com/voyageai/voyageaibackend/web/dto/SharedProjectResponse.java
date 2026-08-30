package com.voyageai.voyageaibackend.web.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for viewing a shared project (public access).
 * Contains project metadata and owner info but no sensitive data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedProjectResponse {

  /** Project UUID. */
  private String projectId;

  /** Project title. */
  private String title;

  /** Project description. */
  private String description;

  /** Owner display name. */
  private String ownerDisplayName;

  /** Owner avatar URL. */
  private String ownerAvatarUrl;

  /** When the project was created. */
  private Instant createdAt;

  /** When the project was last updated. */
  private Instant updatedAt;
}
