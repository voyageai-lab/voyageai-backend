package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectVisibility;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.model.ConversationMessage;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import com.voyageai.voyageaibackend.service.ConversationHistoryService;
import com.voyageai.voyageaibackend.service.ShareService;
import com.voyageai.voyageaibackend.web.dto.ConversationHistoryResponse;
import com.voyageai.voyageaibackend.web.dto.ShareResponse;
import com.voyageai.voyageaibackend.web.dto.SharedProjectResponse;
import com.voyageai.voyageaibackend.web.dto.VisibilityRequest;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for project sharing and public access.
 *
 * <p>Authenticated endpoints under /api/projects/{id}/share manage
 * share link lifecycle. The public endpoint /api/shared/{token}
 * resolves a share token without authentication.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sharing", description = "Project sharing and visibility management")
public class ShareController {

  private final ShareService shareService;
  private final ConversationHistoryService conversationHistoryService;
  private final UserRepository userRepository;

  /**
   * Generates a share link for a project.
   */
  @PostMapping("/api/projects/{projectId}/share")
  @Operation(summary = "Generate share link",
      description = "Creates a shareable link for the project")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<ShareResponse> generateShareLink(
      @PathVariable String projectId,
      Principal principal,
      HttpServletRequest request
  ) {
    User user = extractUser(principal);
    TravelProject project = shareService.generateShareLink(projectId, user.getId());
    return ResponseEntity.ok(toShareResponse(project, request));
  }

  /**
   * Revokes the share link for a project.
   */
  @DeleteMapping("/api/projects/{projectId}/share")
  @Operation(summary = "Revoke share link",
      description = "Revokes the shareable link and sets project to private")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> revokeShareLink(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    shareService.revokeShareLink(projectId, user.getId());
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates project visibility.
   */
  @PutMapping("/api/projects/{projectId}/visibility")
  @Operation(summary = "Update visibility",
      description = "Changes project visibility (PRIVATE, LINK_SHARED, PUBLIC)")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<ShareResponse> updateVisibility(
      @PathVariable String projectId,
      @Valid @RequestBody VisibilityRequest request,
      Principal principal,
      HttpServletRequest httpRequest
  ) {
    User user = extractUser(principal);
    ProjectVisibility visibility = ProjectVisibility.valueOf(request.getVisibility());
    TravelProject project = shareService.updateVisibility(
        projectId, user.getId(), visibility);
    return ResponseEntity.ok(toShareResponse(project, httpRequest));
  }

  /**
   * Public endpoint: resolves a share token to a project.
   * No authentication required.
   */
  @GetMapping("/api/shared/{token}")
  @Operation(summary = "View shared project",
      description = "Accesses a shared project by its token (no auth required)")
  public ResponseEntity<SharedProjectResponse> viewSharedProject(
      @PathVariable String token
  ) {
    TravelProject project = shareService.resolveShareToken(token);
    User owner = project.getUser();

    SharedProjectResponse response = SharedProjectResponse.builder()
        .projectId(project.getProjectId())
        .title(project.getTitle())
        .description(project.getDescription())
        .ownerDisplayName(owner.getDisplayName())
        .ownerAvatarUrl(owner.getAvatarUrl())
        .createdAt(project.getCreatedAt())
        .updatedAt(project.getUpdatedAt())
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Public endpoint: fetches conversation history for a shared project.
   * No authentication required — validates via share token.
   */
  @GetMapping("/api/shared/{token}/history")
  @Operation(summary = "Get shared project history",
      description = "Retrieves conversation history for a shared project (no auth required)")
  public ResponseEntity<ConversationHistoryResponse> getSharedHistory(
      @PathVariable String token
  ) {
    TravelProject project = shareService.resolveShareToken(token);

    List<ConversationMessage> messages =
        conversationHistoryService.getHistory(project.getProjectId(), 50);
    long totalCount = conversationHistoryService.getMessageCount(project.getProjectId());

    ConversationHistoryResponse response = ConversationHistoryResponse.builder()
        .projectId(project.getProjectId())
        .messages(messages)
        .totalCount(totalCount)
        .build();

    return ResponseEntity.ok(response);
  }

  private ShareResponse toShareResponse(TravelProject project, HttpServletRequest request) {
    String shareUrl = null;
    if (project.getShareToken() != null) {
      String baseUrl = request.getScheme() + "://" + request.getServerName();
      int port = request.getServerPort();
      if ((request.getScheme().equals("http") && port != 80)
          || (request.getScheme().equals("https") && port != 443)) {
        baseUrl += ":" + port;
      }
      shareUrl = baseUrl + "/shared/" + project.getShareToken();
    }

    return ShareResponse.builder()
        .projectId(project.getProjectId())
        .visibility(project.getVisibility().name())
        .shareToken(project.getShareToken())
        .shareUrl(shareUrl)
        .shareTokenCreatedAt(project.getShareTokenCreatedAt())
        .build();
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
