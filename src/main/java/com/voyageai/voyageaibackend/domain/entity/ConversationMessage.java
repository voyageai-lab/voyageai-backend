package com.voyageai.voyageaibackend.domain.entity;

import com.voyageai.voyageaibackend.domain.model.ConversationMessage.MessageType;
import com.voyageai.voyageaibackend.domain.model.ConversationMessage.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for conversation messages (persistent storage in MySQL).
 * 
 * <p>This entity works in tandem with Redis for conversation history management:
 * <ul>
 *   <li>Redis: Stores recent messages (last 50) for fast access during active conversations</li>
 *   <li>MySQL: Stores all messages for long-term history and analytics</li>
 * </ul>
 * 
 * <p>Dual-write strategy ensures:
 * - Fast reads for active conversations (Redis)
 * - Complete history preservation (MySQL)
 * - Ability to restore conversation context if Redis cache expires
 */
@Entity
@Table(name = "conversation_messages", indexes = {
    @Index(name = "idx_messages_project_created", columnList = "project_id,created_at"),
    @Index(name = "idx_messages_project_id", columnList = "project_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

  /**
   * Auto-generated primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Unique message identifier (UUID).
   */
  @Column(name = "message_id", nullable = false, unique = true, length = 100)
  private String messageId;

  /**
   * Project ID this message belongs to.
   */
  @Column(name = "project_id", nullable = false, length = 100)
  private String projectId;

  /**
   * Message sender role (USER, ASSISTANT, SYSTEM).
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  /**
   * Message type for frontend rendering (TEXT, ITINERARY, TOOL_RESULT, PROGRESS_UPDATE).
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false, length = 30)
  private MessageType messageType;

  /**
   * Message text content.
   */
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  /**
   * Structured data in JSON format (optional).
   * Used for ITINERARY and TOOL_RESULT message types.
   */
  @Column(name = "structured_data", columnDefinition = "JSON")
  private String structuredData;

  /**
   * Message creation timestamp.
   */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /**
   * Converts this entity to a domain model.
   *
   * @return ConversationMessage domain model
   */
  public com.voyageai.voyageaibackend.domain.model.ConversationMessage toDomainModel() {
    return com.voyageai.voyageaibackend.domain.model.ConversationMessage.builder()
        .messageId(this.messageId)
        .projectId(this.projectId)
        .role(this.role)
        .messageType(this.messageType)
        .content(this.content)
        .structuredData(this.structuredData)
        .timestamp(this.createdAt)
        .build();
  }

  /**
   * Creates an entity from a domain model.
   *
   * @param domainModel Domain model to convert
   * @return ConversationMessage entity
   */
  public static ConversationMessage fromDomainModel(
      com.voyageai.voyageaibackend.domain.model.ConversationMessage domainModel) {
    return ConversationMessage.builder()
        .messageId(domainModel.getMessageId())
        .projectId(domainModel.getProjectId())
        .role(domainModel.getRole())
        .messageType(domainModel.getMessageType())
        .content(domainModel.getContent())
        .structuredData(domainModel.getStructuredData())
        .createdAt(domainModel.getTimestamp())
        .build();
  }
}

