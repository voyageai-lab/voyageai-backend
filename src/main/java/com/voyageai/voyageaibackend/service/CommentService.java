package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.Comment;
import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.CommentRepository;
import com.voyageai.voyageaibackend.domain.repo.CommunityPostRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing comments on community posts.
 * Supports nested replies via the adjacency list pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommunityPostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * Gets all top-level comments for a post.
   * Replies are loaded separately per comment for controlled depth.
   */
  public List<Comment> getTopLevelComments(Long postId) {
    return commentRepository.findByPost_IdAndParentCommentIsNullOrderByCreatedAtAsc(postId);
  }

  /**
   * Gets replies to a specific comment.
   */
  public List<Comment> getReplies(Long parentCommentId) {
    return commentRepository.findByParentComment_IdOrderByCreatedAtAsc(parentCommentId);
  }

  /**
   * Creates a new comment (or reply) on a post.
   */
  @Transactional
  public Comment createComment(Long postId, Long userId, String content, Long parentCommentId) {
    CommunityPost post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));

    Comment parent = null;
    if (parentCommentId != null) {
      parent = commentRepository.findById(parentCommentId)
          .orElseThrow(() -> new ResourceNotFoundException("Comment", parentCommentId));
    }

    Comment comment = Comment.builder()
        .post(post)
        .user(user)
        .parentComment(parent)
        .content(content)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    Comment saved = commentRepository.save(comment);

    // Update denormalized counter
    postRepository.incrementCommentsCount(postId);

    log.info("User {} commented on post {} (parent={})", userId, postId, parentCommentId);
    return saved;
  }

  /**
   * Deletes a comment (only by the author).
   */
  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

    if (!comment.getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("Comment", commentId);
    }

    Long postId = comment.getPost().getId();
    commentRepository.delete(comment);
    postRepository.decrementCommentsCount(postId);

    log.info("User {} deleted comment {} on post {}", userId, commentId, postId);
  }
}
