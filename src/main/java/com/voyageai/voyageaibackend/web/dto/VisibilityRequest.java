package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for updating project visibility.
 */
@Data
public class VisibilityRequest {

  /** Target visibility: PRIVATE, LINK_SHARED, or PUBLIC. */
  @NotNull
  private String visibility;
}
