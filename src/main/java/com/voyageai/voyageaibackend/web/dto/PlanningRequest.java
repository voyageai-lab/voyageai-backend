package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningRequest {

  @NotBlank(message = "Requirements cannot be empty")
  @Size(min = 2, max = 2000, message = "Requirements must be between 2 and 2000 characters")
  private String requirements;

  private String projectId;

  /** When true, worker fetches existing itinerary from DB and runs surgical edit pipeline. */
  private boolean editMode;
}
