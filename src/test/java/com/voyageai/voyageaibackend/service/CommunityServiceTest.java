package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.repo.CommunityPostRepository;
import com.voyageai.voyageaibackend.domain.repo.PostLikeRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for {@link CommunityService}.
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

  @Mock
  private CommunityPostRepository postRepository;

  @Mock
  private PostLikeRepository likeRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CommunityService communityService;

  private User testUser;
  private CommunityPost testPost;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setDisplayName("Test User");
    testUser.setAuthProvider(AuthProvider.LOCAL);

    testPost = CommunityPost.builder()
        .id(10L)
        .user(testUser)
        .projectId("proj-123")
        .title("Amazing Tokyo Trip")
        .description("A detailed itinerary")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  void getFeed_firstPage_shouldReturnPosts() {
    when(postRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
        .thenReturn(List.of(testPost));

    List<CommunityPost> result = communityService.getFeed(null);

    assertEquals(1, result.size());
    assertEquals("Amazing Tokyo Trip", result.get(0).getTitle());
  }

  @Test
  void getFeed_withCursor_shouldUseCursorQuery() {
    when(postRepository.findByCursorBefore(any(Long.class), any(PageRequest.class)))
        .thenReturn(List.of(testPost));

    List<CommunityPost> result = communityService.getFeed(100L);

    assertEquals(1, result.size());
    verify(postRepository).findByCursorBefore(any(Long.class), any(PageRequest.class));
  }

  @Test
  void getPost_existing_shouldReturn() {
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

    CommunityPost result = communityService.getPost(10L);

    assertNotNull(result);
    assertEquals("Amazing Tokyo Trip", result.getTitle());
  }

  @Test
  void getPost_nonExisting_shouldThrow() {
    when(postRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> communityService.getPost(999L));
  }

  @Test
  void createPost_shouldSaveAndReturn() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(postRepository.save(any(CommunityPost.class))).thenReturn(testPost);

    CommunityPost result = communityService.createPost(
        1L, "proj-123", "Amazing Tokyo Trip", "A detailed itinerary");

    assertNotNull(result);
    verify(postRepository).save(any(CommunityPost.class));
  }

  @Test
  void deletePost_asAuthor_shouldDelete() {
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

    communityService.deletePost(10L, 1L);

    verify(postRepository).delete(testPost);
  }

  @Test
  void deletePost_notAuthor_shouldThrow() {
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

    assertThrows(ResourceNotFoundException.class,
        () -> communityService.deletePost(10L, 2L));
  }

  @Test
  void isLikedByUser_shouldDelegateToRepository() {
    when(likeRepository.existsByPost_IdAndUser_Id(10L, 1L)).thenReturn(true);

    assertTrue(communityService.isLikedByUser(10L, 1L));
    verify(likeRepository).existsByPost_IdAndUser_Id(10L, 1L);
  }
}
