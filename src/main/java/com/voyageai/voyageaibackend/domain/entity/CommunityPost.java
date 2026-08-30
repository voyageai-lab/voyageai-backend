package com.voyageai.voyageaibackend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity representing a community post — a user-shared travel plan.
 *
 * <p>Maps to the existing {@code community_posts} table.
 * Denormalized counters ({@code likesCount}, {@code commentsCount}, {@code viewsCount})
 * are maintained for efficient feed queries without COUNT JOINs.
 */
@Entity
@Table(name = "community_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Reference to the MongoDB travel plan document. */
  @Column(name = "plan_id")
  private String planId;

  /** Reference to the associated travel project. */
  @Column(name = "project_id")
  private String projectId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "cover_image_url", length = 500)
  private String coverImageUrl;

  @Column(name = "likes_count", nullable = false)
  @Builder.Default
  private int likesCount = 0;

  @Column(name = "comments_count", nullable = false)
  @Builder.Default
  private int commentsCount = 0;

  @Column(name = "views_count", nullable = false)
  @Builder.Default
  private int viewsCount = 0;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private Instant updatedAt = Instant.now();
}
