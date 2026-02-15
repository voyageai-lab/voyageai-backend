package com.voyageai.voyageaibackend.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for planning request submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningResponse {

  /**
   * Unique task ID for tracking the planning request.
   */
  private String taskId;

  /**
   * Project ID this task belongs to.
   */
  private String projectId;

  /**
   * Human-readable message.
   */
  private String message;

  /**
   * URL to poll for task status.
   */
  private String statusUrl;
}

