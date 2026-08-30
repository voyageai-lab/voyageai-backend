package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.PostLike;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.repo.CommunityPostRepository;
import com.voyageai.voyageaibackend.domain.repo.PostLikeRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link LikeService}.
 */
@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

  @Mock
  private PostLikeRepository likeRepository;

  @Mock
  private CommunityPostRepository postRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private LikeService likeService;

  private User testUser;
  private CommunityPost testPost;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setAuthProvider(AuthProvider.LOCAL);

    testPost = CommunityPost.builder()
        .id(10L)
        .user(testUser)
        .title("Test Post")
        .likesCount(5)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  void toggleLike_notLiked_shouldLikeAndReturnTrue() {
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(likeRepository.findByPost_IdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());
    when(likeRepository.save(any(PostLike.class))).thenReturn(new PostLike());

    boolean result = likeService.toggleLike(10L, 1L);

    assertTrue(result);
    verify(likeRepository).save(any(PostLike.class));
    verify(postRepository).incrementLikesCount(10L);
  }

  @Test
  void toggleLike_alreadyLiked_shouldUnlikeAndReturnFalse() {
    PostLike existingLike = PostLike.builder()
        .id(99L).post(testPost).user(testUser).build();
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(likeRepository.findByPost_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(existingLike));

    boolean result = likeService.toggleLike(10L, 1L);

    assertFalse(result);
    verify(likeRepository).delete(existingLike);
    verify(postRepository).decrementLikesCount(10L);
  }

  @Test
  void toggleLike_postNotFound_shouldThrow() {
    when(postRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> likeService.toggleLike(999L, 1L));
  }

  @Test
  void toggleLike_userNotFound_shouldThrow() {
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> likeService.toggleLike(10L, 999L));
  }
}
