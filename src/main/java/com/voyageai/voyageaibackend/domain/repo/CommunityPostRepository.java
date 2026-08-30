package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for CommunityPost entity.
 */
@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

  /** Paginated feed ordered by creation time (newest first). */
  List<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

  /** Cursor-based pagination: posts older than the given ID. */
  @Query("SELECT p FROM CommunityPost p WHERE p.id < :cursor ORDER BY p.id DESC")
  List<CommunityPost> findByCursorBefore(@Param("cursor") Long cursor, Pageable pageable);

  /** Posts by a specific user. */
  List<CommunityPost> findByUser_IdOrderByCreatedAtDesc(Long userId);

  /** Increment likes count atomically. */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE CommunityPost p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
  void incrementLikesCount(@Param("postId") Long postId);

  /** Decrement likes count atomically (floor at 0). */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE CommunityPost p SET p.likesCount = GREATEST(p.likesCount - 1, 0) WHERE p.id = :postId")
  void decrementLikesCount(@Param("postId") Long postId);

  /** Increment comments count atomically. */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE CommunityPost p SET p.commentsCount = p.commentsCount + 1 WHERE p.id = :postId")
  void incrementCommentsCount(@Param("postId") Long postId);

  /** Decrement comments count atomically (floor at 0). */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE CommunityPost p SET p.commentsCount = GREATEST(p.commentsCount - 1, 0) WHERE p.id = :postId")
  void decrementCommentsCount(@Param("postId") Long postId);
}
