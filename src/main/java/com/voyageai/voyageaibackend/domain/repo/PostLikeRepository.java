package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for PostLike entity.
 */
@Repository
@SuppressWarnings("checkstyle:MethodName")
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

  /** Find a like by post and user (to check if already liked). */
  Optional<PostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

  /** Check if a user has liked a post. */
  boolean existsByPost_IdAndUser_Id(Long postId, Long userId);
}
