package com.voyageai.voyageaibackend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a collaborator on a travel project.
 *
 * <p>Supports RBAC with three roles: OWNER, EDITOR, VIEWER.
 * The unique constraint {@code (project_id, user_id)} prevents
 * duplicate invitations.
 */
@Entity
@Table(name = "project_collaborators", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_user", columnNames = {"project_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCollaborator {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false, length = 100)
  private String projectId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private CollaboratorRole role = CollaboratorRole.VIEWER;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "invited_by", nullable = false)
  private User invitedBy;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  /**
   * Collaborator role enum (resource-level RBAC).
   */
  public enum CollaboratorRole {
    /** Full control — the project creator. */
    OWNER,
    /** Can edit the itinerary and send messages. */
    EDITOR,
    /** Read-only access to the project. */
    VIEWER
  }
}
