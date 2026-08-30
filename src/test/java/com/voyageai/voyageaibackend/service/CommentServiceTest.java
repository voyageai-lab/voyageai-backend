package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.Comment;
import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.repo.CommentRepository;
import com.voyageai.voyageaibackend.domain.repo.CommunityPostRepository;
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

/**
 * Unit tests for {@link CommentService}.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CommunityPostRepository postRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CommentService commentService;

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
        .commentsCount(3)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  void getTopLevelComments_shouldReturnComments() {
    Comment c1 = Comment.builder().id(1L).post(testPost).user(testUser)
        .content("Hello").createdAt(Instant.now()).updatedAt(Instant.now()).build();
    when(commentRepository.findByPost_IdAndParentCommentIsNullOrderByCreatedAtAsc(10L))
        .thenReturn(List.of(c1));

    List<Comment> result = commentService.getTopLevelComments(10L);

    assertEquals(1, result.size());
    assertEquals("Hello", result.get(0).getContent());
  }

  @Test
  void createComment_topLevel_shouldSaveAndIncrementCounter() {
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    Comment saved = Comment.builder().id(100L).post(testPost).user(testUser)
        .content("Nice trip!").createdAt(Instant.now()).updatedAt(Instant.now()).build();
    when(commentRepository.save(any(Comment.class))).thenReturn(saved);

    Comment result = commentService.createComment(10L, 1L, "Nice trip!", null);

    assertNotNull(result);
    assertEquals("Nice trip!", result.getContent());
    assertNull(result.getParentComment());
    verify(postRepository).incrementCommentsCount(10L);
  }

  @Test
  void createComment_reply_shouldSetParent() {
    Comment parent = Comment.builder().id(50L).post(testPost).user(testUser)
        .content("Parent").createdAt(Instant.now()).updatedAt(Instant.now()).build();
    when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));

    Comment saved = Comment.builder().id(101L).post(testPost).user(testUser)
        .parentComment(parent).content("Reply").createdAt(Instant.now())
        .updatedAt(Instant.now()).build();
    when(commentRepository.save(any(Comment.class))).thenReturn(saved);

    Comment result = commentService.createComment(10L, 1L, "Reply", 50L);

    assertNotNull(result);
    assertEquals(parent, result.getParentComment());
    verify(postRepository).incrementCommentsCount(10L);
  }

  @Test
  void createComment_postNotFound_shouldThrow() {
    when(postRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> commentService.createComment(999L, 1L, "Test", null));
  }

  @Test
  void deleteComment_asAuthor_shouldDeleteAndDecrementCounter() {
    Comment comment = Comment.builder().id(100L).post(testPost).user(testUser)
        .content("To delete").createdAt(Instant.now()).updatedAt(Instant.now()).build();
    when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

    commentService.deleteComment(100L, 1L);

    verify(commentRepository).delete(comment);
    verify(postRepository).decrementCommentsCount(10L);
  }

  @Test
  void deleteComment_notAuthor_shouldThrow() {
    User otherUser = new User();
    otherUser.setId(2L);
    Comment comment = Comment.builder().id(100L).post(testPost).user(otherUser)
        .content("Not mine").createdAt(Instant.now()).updatedAt(Instant.now()).build();
    when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

    assertThrows(ResourceNotFoundException.class,
        () -> commentService.deleteComment(100L, 1L));
  }
}
