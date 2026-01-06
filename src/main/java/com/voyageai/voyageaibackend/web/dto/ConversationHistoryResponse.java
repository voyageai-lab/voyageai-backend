package com.voyageai.voyageaibackend.web.dto;

import com.voyageai.voyageaibackend.domain.model.ConversationMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for conversation history endpoint.
 * 
 * <p>Contains the complete conversation history for a project,
 * used by frontend to render chat interface and maintain context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryResponse {

  /**
   * Project ID for this conversation.
   */
  private String projectId;

  /**
   * List of conversation messages in chronological order (oldest first).
   * 
   * <p>Frontend can iterate through these to render:
   * - Chat bubbles (TEXT messages)
   * - Itinerary views (ITINERARY messages)
   * - Tool result cards (TOOL_RESULT messages)
   * - Progress indicators (PROGRESS_UPDATE messages)
   */
  private List<ConversationMessage> messages;

  /**
   * Total message count in this project.
   * 
   * <p>May be greater than messages.size() if pagination/limit is applied.
   */
  private Long totalCount;
}

