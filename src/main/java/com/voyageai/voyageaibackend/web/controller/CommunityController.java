package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.Comment;
import com.voyageai.voyageaibackend.domain.entity.CommunityPost;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import com.voyageai.voyageaibackend.service.CommentService;
import com.voyageai.voyageaibackend.service.CommunityService;
import com.voyageai.voyageaibackend.service.LikeService;
import com.voyageai.voyageaibackend.web.dto.CommentResponse;
import com.voyageai.voyageaibackend.web.dto.CommunityPostResponse;
import com.voyageai.voyageaibackend.web.dto.CreateCommentRequest;
import com.voyageai.voyageaibackend.web.dto.CreatePostRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for community features: posts, comments, and likes.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community", description = "Community posts, comments, and likes")
public class CommunityController {

  private final CommunityService communityService;
  private final CommentService commentService;
  private final LikeService likeService;
  private final UserRepository userRepository;

  // ========== Posts ==========

  @GetMapping("/posts")
  @Operation(summary = "Get community feed",
      description = "Returns paginated community posts (cursor-based)")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Map<String, Object>> getFeed(
      @RequestParam(required = false) Long cursor,
      Principal principal
  ) {
    User user = extractUser(principal);
    List<CommunityPost> posts = communityService.getFeed(cursor);

    List<CommunityPostResponse> responseList = posts.stream()
        .map(p -> toPostResponse(p, user.getId()))
        .collect(Collectors.toList());

    Long nextCursor = posts.isEmpty() ? null : posts.get(posts.size() - 1).getId();

    Map<String, Object> response = new HashMap<>();
    response.put("posts", responseList);
    response.put("nextCursor", nextCursor);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/posts/{postId}")
  @Operation(summary = "Get post details")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<CommunityPostResponse> getPost(
      @PathVariable Long postId,
      Principal principal
  ) {
    User user = extractUser(principal);
    CommunityPost post = communityService.getPost(postId);
    return ResponseEntity.ok(toPostResponse(post, user.getId()));
  }

  @PostMapping("/posts")
  @Operation(summary = "Create community post",
      description = "Shares a project as a community post")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<CommunityPostResponse> createPost(
      @Valid @RequestBody CreatePostRequest request,
      Principal principal
  ) {
    User user = extractUser(principal);
    CommunityPost post = communityService.createPost(
        user.getId(), request.getProjectId(), request.getTitle(), request.getDescription());
    return ResponseEntity.ok(toPostResponse(post, user.getId()));
  }

  @DeleteMapping("/posts/{postId}")
  @Operation(summary = "Delete community post")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> deletePost(
      @PathVariable Long postId,
      Principal principal
  ) {
    User user = extractUser(principal);
    communityService.deletePost(postId, user.getId());
    return ResponseEntity.noContent().build();
  }

  // ========== Comments ==========

  @GetMapping("/posts/{postId}/comments")
  @Operation(summary = "Get comments for a post",
      description = "Returns top-level comments with nested replies")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
    List<Comment> topLevel = commentService.getTopLevelComments(postId);
    List<CommentResponse> response = topLevel.stream()
        .map(this::toCommentResponseWithReplies)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/posts/{postId}/comments")
  @Operation(summary = "Add a comment", description = "Creates a comment or reply on a post")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<CommentResponse> addComment(
      @PathVariable Long postId,
      @Valid @RequestBody CreateCommentRequest request,
      Principal principal
  ) {
    User user = extractUser(principal);
    Comment comment = commentService.createComment(
        postId, user.getId(), request.getContent(), request.getParentCommentId());
    return ResponseEntity.ok(toCommentResponse(comment));
  }

  @DeleteMapping("/comments/{commentId}")
  @Operation(summary = "Delete a comment")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> deleteComment(
      @PathVariable Long commentId,
      Principal principal
  ) {
    User user = extractUser(principal);
    commentService.deleteComment(commentId, user.getId());
    return ResponseEntity.noContent().build();
  }

  // ========== Likes ==========

  @PostMapping("/posts/{postId}/like")
  @Operation(summary = "Toggle like",
      description = "Likes or unlikes a post (toggle)")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Map<String, Object>> toggleLike(
      @PathVariable Long postId,
      Principal principal
  ) {
    User user = extractUser(principal);
    boolean liked = likeService.toggleLike(postId, user.getId());

    Map<String, Object> response = new HashMap<>();
    response.put("postId", postId);
    response.put("liked", liked);
    return ResponseEntity.ok(response);
  }

  // ========== Helpers ==========

  private CommunityPostResponse toPostResponse(CommunityPost post, Long currentUserId) {
    User author = post.getUser();
    return CommunityPostResponse.builder()
        .id(post.getId())
        .userId(author.getId())
        .planId(post.getPlanId())
        .projectId(post.getProjectId())
        .title(post.getTitle())
        .description(post.getDescription())
        .coverImageUrl(post.getCoverImageUrl())
        .likesCount(post.getLikesCount())
        .commentsCount(post.getCommentsCount())
        .viewsCount(post.getViewsCount())
        .authorDisplayName(author.getDisplayName())
        .authorAvatarUrl(author.getAvatarUrl())
        .likedByMe(communityService.isLikedByUser(post.getId(), currentUserId))
        .createdAt(post.getCreatedAt())
        .updatedAt(post.getUpdatedAt())
        .build();
  }

  private CommentResponse toCommentResponse(Comment comment) {
    User author = comment.getUser();
    return CommentResponse.builder()
        .id(comment.getId())
        .postId(comment.getPost().getId())
        .userId(author.getId())
        .parentCommentId(
            comment.getParentComment() != null ? comment.getParentComment().getId() : null)
        .content(comment.getContent())
        .authorDisplayName(author.getDisplayName())
        .authorAvatarUrl(author.getAvatarUrl())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }

  private CommentResponse toCommentResponseWithReplies(Comment comment) {
    CommentResponse response = toCommentResponse(comment);
    List<Comment> replies = commentService.getReplies(comment.getId());
    if (!replies.isEmpty()) {
      response.setReplies(
          replies.stream()
              .map(this::toCommentResponseWithReplies)
              .collect(Collectors.toList())
      );
    }
    return response;
  }

  private User extractUser(Principal principal) {
    if (principal instanceof org.springframework.security.core.Authentication) {
      Object principalObj = ((org.springframework.security.core.Authentication) principal)
          .getPrincipal();
      if (principalObj instanceof User) {
        return (User) principalObj;
      }
    }
    String email = principal.getName();
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
  }
}
