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

  /** Timestamp when project was created. */
  private Instant createdAt;

  /** Timestamp when project was last updated. */
  private Instant updatedAt;
}
