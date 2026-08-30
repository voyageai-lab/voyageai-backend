package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.CommunityPostRepository;
import com.voyageai.voyageaibackend.domain.repo.PostLikeRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing community posts (shared travel plans).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

  private static final int PAGE_SIZE = 20;

  private final CommunityPostRepository postRepository;
  private final PostLikeRepository likeRepository;
  private final UserRepository userRepository;

  /**
   * Fetches a paginated feed of community posts.
   *
   * @param cursor post ID to paginate from (null for first page)
   * @return list of posts
   */
  public List<CommunityPost> getFeed(Long cursor) {
    PageRequest page = PageRequest.of(0, PAGE_SIZE);
    if (cursor != null) {
      return postRepository.findByCursorBefore(cursor, page);
    }
    return postRepository.findAllByOrderByCreatedAtDesc(page);
  }

  /**
   * Gets a single post by ID.
   */
  public CommunityPost getPost(Long postId) {
    return postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
  }

  /**
   * Creates a new community post from a project.
   */
  @Transactional
  public CommunityPost createPost(Long userId, String projectId, String title, String description) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));

    CommunityPost post = CommunityPost.builder()
        .user(user)
        .projectId(projectId)
        .title(title)
        .description(description)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    CommunityPost saved = postRepository.save(post);
    log.info("User {} created community post {} for project {}", userId, saved.getId(), projectId);
    return saved;
  }

  /**
   * Deletes a community post (only by the author).
   */
  @Transactional
  public void deletePost(Long postId, Long userId) {
    CommunityPost post = getPost(postId);
    if (!post.getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("Post", postId);
    }
    postRepository.delete(post);
    log.info("User {} deleted community post {}", userId, postId);
  }

  /**
   * Checks if a user has liked a specific post.
   */
  public boolean isLikedByUser(Long postId, Long userId) {
    return likeRepository.existsByPost_IdAndUser_Id(postId, userId);
  }
}
