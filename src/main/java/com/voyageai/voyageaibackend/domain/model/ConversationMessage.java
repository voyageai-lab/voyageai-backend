package com.voyageai.voyageaibackend.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single message in a conversation thread within a travel planning project.
 * 
 * <p>Conversation messages enable multi-turn dialogue between users and the AI assistant,
 * allowing context-aware responses and iterative refinement of travel plans.
 * 
 * <p>This model is used for both in-memory (Redis) and persistent (MySQL) storage.
 * Redis stores recent conversation history (last 50 messages) for fast access,
 * while MySQL provides long-term storage for all conversation history.
 * 
 * <p>Message flow examples:
 * <pre>
 * 1. User: "I want to visit Tokyo for 5 days" (USER, TEXT)
 * 2. Assistant: "What's your budget and accommodation preference?" (ASSISTANT, TEXT)
 * 3. User: "Around $2000, I prefer local guesthouses" (USER, TEXT)
 * 4. Assistant: [Generated itinerary] (ASSISTANT, ITINERARY)
 * 5. User: "Find me restaurants near Day 1 hotel" (USER, TEXT)
 * 6. Assistant: [Restaurant recommendations] (ASSISTANT, TOOL_RESULT)
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

  /**
   * Unique message identifier (UUID format).
   * Example: "msg-123e4567-e89b-12d3-a456-426614174000"
   */
  private String messageId;

  /**
   * Project ID this message belongs to.
   * Links message to a specific travel planning project.
   */
  private String projectId;

  /**
   * Role of the message sender.
   */
  private Role role;

  /**
   * Type of message content (determines how frontend renders it).
   */
  private MessageType messageType;

  /**
   * Text content of the message.
   * Always present, contains the main message text.
   */
  private String content;

  /**
   * Structured data in JSON format (optional).
   * 
   * <p>Used when messageType is:
   * <ul>
   *   <li>ITINERARY: Contains {@link StructuredItinerary} JSON</li>
   *   <li>TOOL_RESULT: Contains tool-specific result JSON (hotels, restaurants, etc.)</li>
   * </ul>
   * 
   * <p>Null for TEXT and PROGRESS_UPDATE message types.
   */
  private String structuredData;

  /**
   * Message creation timestamp.
   */
  private Instant timestamp;

  /**
   * Message sender role enum.
   */
  public enum Role {
    /**
     * Message from the user.
     * Example: "I want to visit Tokyo"
     */
    USER,

    /**
     * Message from the AI assistant.
     * Example: "Here's your personalized Tokyo itinerary..."
     */
    ASSISTANT,

    /**
     * System-generated message.
     * Example: "Task submitted successfully", "Processing your request..."
     */
    SYSTEM
  }

  /**
   * Message type enum (determines frontend rendering).
   */
  public enum MessageType {
    /**
     * Plain text conversation message.
     * Rendered as a simple chat bubble.
     */
    TEXT,

    /**
     * Complete itinerary result with structured data.
     * Rendered as an interactive itinerary view with map integration.
     * Contains {@link StructuredItinerary} JSON in structuredData field.
     */
    ITINERARY,

    /**
     * Tool call result (hotels, restaurants, flights, etc.).
     * Rendered as rich cards with images and details.
     * Contains tool-specific JSON in structuredData field.
     */
    TOOL_RESULT,

    /**
     * Progress update message during async task processing.
     * Rendered as a loading indicator with status text.
     * Example: "Analyzing your requirements...", "Generating itinerary..."
     */
    PROGRESS_UPDATE
  }
}

