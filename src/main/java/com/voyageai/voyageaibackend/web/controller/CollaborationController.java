package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator;
import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator.CollaboratorRole;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import com.voyageai.voyageaibackend.service.CollaborationService;
import com.voyageai.voyageaibackend.service.PresenceService;
import com.voyageai.voyageaibackend.web.dto.CollaboratorResponse;
import com.voyageai.voyageaibackend.web.dto.InviteRequest;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for project collaboration: invitations, collaborators, and presence.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collaboration", description = "Project collaboration and presence management")
public class CollaborationController {

  private final CollaborationService collaborationService;
  private final PresenceService presenceService;
  private final UserRepository userRepository;

  // ========== Collaborators ==========

  @GetMapping("/api/projects/{projectId}/collaborators")
  @Operation(summary = "List collaborators")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<List<CollaboratorResponse>> getCollaborators(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    // User must be a collaborator or owner to see the list
    if (!collaborationService.hasRole(projectId, user.getId(), CollaboratorRole.VIEWER)) {
      throw new ResourceNotFoundException("Project", projectId);
    }

    List<ProjectCollaborator> collabs = collaborationService.getCollaborators(projectId);
    List<CollaboratorResponse> response = collabs.stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/api/projects/{projectId}/collaborators")
  @Operation(summary = "Invite collaborator",
      description = "Invites a user by email to collaborate on a project")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<CollaboratorResponse> inviteCollaborator(
      @PathVariable String projectId,
      @Valid @RequestBody InviteRequest request,
      Principal principal
  ) {
    User user = extractUser(principal);
    CollaboratorRole role = CollaboratorRole.valueOf(request.getRole());
    ProjectCollaborator collab = collaborationService.inviteCollaborator(
        projectId, user.getId(), request.getEmail(), role);
    return ResponseEntity.ok(toResponse(collab));
  }

  @DeleteMapping("/api/projects/{projectId}/collaborators/{userId}")
  @Operation(summary = "Remove collaborator")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> removeCollaborator(
      @PathVariable String projectId,
      @PathVariable Long userId,
      Principal principal
  ) {
    User user = extractUser(principal);
    collaborationService.removeCollaborator(projectId, user.getId(), userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api/collaborations/invitations/{projectId}/accept")
  @Operation(summary = "Accept invitation")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Map<String, String>> acceptInvitation(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    collaborationService.acceptInvitation(projectId, user.getId());

    Map<String, String> response = new HashMap<>();
    response.put("projectId", projectId);
    response.put("status", "accepted");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api/projects/shared-with-me")
  @Operation(summary = "Get projects shared with me")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<List<CollaboratorResponse>> getSharedWithMe(Principal principal) {
    User user = extractUser(principal);
    List<ProjectCollaborator> collabs = collaborationService.getSharedWithMe(user.getId());
    List<CollaboratorResponse> response = collabs.stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api/projects/{projectId}/role")
  @Operation(summary = "Get my role on a project")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Map<String, String>> getMyRole(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    CollaboratorRole role = collaborationService.getUserRole(projectId, user.getId());

    Map<String, String> response = new HashMap<>();
    response.put("projectId", projectId);
    response.put("role", role != null ? role.name() : null);
    return ResponseEntity.ok(response);
  }

  // ========== Presence ==========

  @PostMapping("/api/projects/{projectId}/presence")
  @Operation(summary = "Send presence heartbeat")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> heartbeat(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    presenceService.heartbeat(projectId, user.getId(), user.getDisplayName(), user.getAvatarUrl());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/api/projects/{projectId}/presence")
  @Operation(summary = "Get online users for a project")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<List<Map<String, Object>>> getOnlineUsers(
      @PathVariable String projectId,
      Principal principal
  ) {
    // Verify user has access
    User user = extractUser(principal);
    if (!collaborationService.hasRole(projectId, user.getId(), CollaboratorRole.VIEWER)) {
      throw new ResourceNotFoundException("Project", projectId);
    }

    List<Map<String, Object>> users = presenceService.getOnlineUsers(projectId);
    return ResponseEntity.ok(users);
  }

  // ========== Helpers ==========

  private CollaboratorResponse toResponse(ProjectCollaborator collab) {
    User user = collab.getUser();
    return CollaboratorResponse.builder()
        .id(collab.getId())
        .projectId(collab.getProjectId())
        .userId(user.getId())
        .role(collab.getRole().name())
        .email(user.getEmail())
        .displayName(user.getDisplayName())
        .avatarUrl(user.getAvatarUrl())
        .acceptedAt(collab.getAcceptedAt())
        .createdAt(collab.getCreatedAt())
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
