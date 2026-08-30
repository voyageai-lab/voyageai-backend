package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Comment entity.
 */
@Repository
@SuppressWarnings("checkstyle:MethodName")
public interface CommentRepository extends JpaRepository<Comment, Long> {

  /** Find all top-level comments for a post (no parent). */
  List<Comment> findByPost_IdAndParentCommentIsNullOrderByCreatedAtAsc(Long postId);

  /** Find all replies to a specific parent comment. */
  List<Comment> findByParentComment_IdOrderByCreatedAtAsc(Long parentCommentId);

  /** Find all comments for a post (flat list, for counting etc.). */
  List<Comment> findByPost_IdOrderByCreatedAtAsc(Long postId);

  /** Count comments for a post. */
  long countByPost_Id(Long postId);
}
