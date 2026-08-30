package com.voyageai.voyageaibackend.web.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a travel project.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

  /** Unique project identifier (UUID format). */
  private String projectId;

  /** Project title. */
  private String title;

  /** Project description (optional). */
  private String description;

  /** Project status (ACTIVE, ARCHIVED, DELETED). */
  private String status;

  /** Project visibility (PRIVATE, LINK_SHARED, PUBLIC). */
  private String visibility;

  /** Share token (null if not shared). */
  private String shareToken;

  /** Current user's role on this project (OWNER, EDITOR, VIEWER). */
  private String role;

  /** Display name of the project owner (useful for collaborated projects). */
  private String ownerDisplayName;

  /** Timestamp when project was created. */
  private Instant createdAt;

  /** Timestamp when project was last updated. */
  private Instant updatedAt;
}
