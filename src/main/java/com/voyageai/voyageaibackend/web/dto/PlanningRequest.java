package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for travel planning generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningRequest {

  /**
   * User's travel requirements and preferences.
   * 
   * <p>Example:
   * "Plan a 5-day trip to Tokyo for cherry blossom season.
   *  Budget: $2000. Interests: food, culture, temples.
   *  Prefer mid-range hotels."
   */
  @NotBlank(message = "Requirements cannot be empty")
  @Size(min = 10, max = 2000, message = "Requirements must be between 10 and 2000 characters")
  private String requirements;
}

