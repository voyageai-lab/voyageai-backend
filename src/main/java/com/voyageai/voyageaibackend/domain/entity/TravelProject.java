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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a travel planning project/conversation.
 * 
 * <p>A project groups multiple planning tasks (iterations) together,
 * allowing users to have a multi-turn conversation to refine their travel plans.
 * 
 * <p>Example workflow:
 * <pre>
 * 1. User creates project: "Tokyo Trip 2024"
 * 2. First task: "Plan a 5-day trip to Tokyo"
 * 3. Second task: "Add more focus on food experiences"
 * 4. Third task: "Change budget to $3000"
 * </pre>
 */
@Entity
@Table(name = "travel_projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelProject {

  /**
   * Primary key (auto-generated).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Unique project identifier (UUID format).
   * Used in API endpoints and external references.
   */
  @Column(nullable = false, unique = true, length = 100)
  private String projectId;

  /**
   * User who owns this project.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Project title (e.g., "Tokyo Cherry Blossom Trip 2024").
   * Auto-generated from first task or user-provided.
   */
  @Column(nullable = false, length = 255)
  private String title;

  /**
   * Optional project description or notes.
   */
  @Column(columnDefinition = "TEXT")
  private String description;

  /**
   * Project status.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectStatus status;

  /**
   * Timestamp when project was created.
   */
  @Column(nullable = false)
  private Instant createdAt;

  /**
   * Timestamp when project was last updated.
   */
  @Column(nullable = false)
  private Instant updatedAt;

  /**
   * Project status enum.
   */
  public enum ProjectStatus {
    /**
     * Project is active and can accept new tasks.
     */
    ACTIVE,

    /**
     * Project is archived (read-only, no new tasks).
     */
    ARCHIVED,

    /**
     * Project is deleted (soft delete).
     */
    DELETED
  }
}

