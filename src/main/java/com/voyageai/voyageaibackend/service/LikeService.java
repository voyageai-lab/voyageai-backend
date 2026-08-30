package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.PostLike;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.CommunityPostRepository;
import com.voyageai.voyageaibackend.domain.repo.PostLikeRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing post likes.
 * Implements idempotent toggle: like if not liked, unlike if already liked.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

  private final PostLikeRepository likeRepository;
  private final CommunityPostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * Toggles like state for a user on a post.
   *
   * @param postId the post to like/unlike
   * @param userId the user performing the action
   * @return true if the post is now liked, false if unliked
   */
  @Transactional
  public boolean toggleLike(Long postId, Long userId) {
    CommunityPost post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));

    Optional<PostLike> existing = likeRepository.findByPost_IdAndUser_Id(postId, userId);

    if (existing.isPresent()) {
      // Unlike
      likeRepository.delete(existing.get());
      postRepository.decrementLikesCount(postId);
      log.debug("User {} unliked post {}", userId, postId);
      return false;
    } else {
      // Like
      PostLike like = PostLike.builder()
          .post(post)
          .user(user)
          .createdAt(Instant.now())
          .build();
      likeRepository.save(like);
      postRepository.incrementLikesCount(postId);
      log.debug("User {} liked post {}", userId, postId);
      return true;
    }
  }
}
