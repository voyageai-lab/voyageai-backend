package com.voyageai.voyageaibackend.web.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for share link operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {

  /** Project UUID. */
  private String projectId;

  /** Current visibility level. */
  private String visibility;

  /** The share token (null if not shared). */
  private String shareToken;

  /** Full share URL for convenience. */
  private String shareUrl;

  /** When the share token was created. */
  private Instant shareTokenCreatedAt;
}
