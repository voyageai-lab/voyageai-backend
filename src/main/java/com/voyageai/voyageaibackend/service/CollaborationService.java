package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator;
import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator.CollaboratorRole;
import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.ProjectCollaboratorRepository;
import com.voyageai.voyageaibackend.domain.repo.TravelProjectRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.BusinessException;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing project collaboration: invitations, roles, and access control.
 *
 * <p>Implements resource-level RBAC with three roles:
 * OWNER (full control), EDITOR (can edit), VIEWER (read-only).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

  private final ProjectCollaboratorRepository collaboratorRepository;
  private final TravelProjectRepository projectRepository;
  private final UserRepository userRepository;

  /**
   * Invites a user to collaborate on a project.
   *
   * @param projectId project UUID
   * @param inviterId user ID of the person inviting (must be OWNER)
   * @param inviteeEmail email of the user to invite
   * @param role role to assign
   * @return the created collaborator record
   */
  @Transactional
  public ProjectCollaborator inviteCollaborator(
      String projectId, Long inviterId, String inviteeEmail, CollaboratorRole role) {

    // Verify project exists and inviter is the owner
    TravelProject project = projectRepository.findByProjectIdAndUser_Id(projectId, inviterId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project not found or you are not the owner: " + projectId));

    // Find the invitee by email
    User invitee = userRepository.findByEmail(inviteeEmail)
        .orElseThrow(() -> new ResourceNotFoundException(
            "User not found with email: " + inviteeEmail));

    // Cannot invite yourself
    if (invitee.getId().equals(inviterId)) {
      throw new BusinessException("Cannot invite yourself as a collaborator");
    }

    // Cannot assign OWNER role via invitation
    if (role == CollaboratorRole.OWNER) {
      throw new BusinessException("Cannot assign OWNER role via invitation");
    }

    // Check for existing invitation
    Optional<ProjectCollaborator> existing =
        collaboratorRepository.findByProjectIdAndUser_Id(projectId, invitee.getId());
    if (existing.isPresent()) {
      throw new BusinessException("User is already a collaborator on this project");
    }

    User inviter = userRepository.findById(inviterId)
        .orElseThrow(() -> new ResourceNotFoundException("User", inviterId));

    Instant now = Instant.now();
    ProjectCollaborator collaborator = ProjectCollaborator.builder()
        .projectId(projectId)
        .user(invitee)
        .role(role)
        .invitedBy(inviter)
        .acceptedAt(now)
        .createdAt(now)
        .build();

    ProjectCollaborator saved = collaboratorRepository.save(collaborator);
    log.info("User {} invited {} as {} on project {}",
        inviterId, inviteeEmail, role, projectId);
    return saved;
  }

  /**
   * Accepts a collaboration invitation.
   */
  @Transactional
  public ProjectCollaborator acceptInvitation(String projectId, Long userId) {
    ProjectCollaborator collab = collaboratorRepository
        .findByProjectIdAndUser_Id(projectId, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Invitation not found for project: " + projectId));

    if (collab.getAcceptedAt() != null) {
      return collab; // Already accepted
    }

    collab.setAcceptedAt(Instant.now());
    ProjectCollaborator saved = collaboratorRepository.save(collab);
    log.info("User {} accepted invitation for project {}", userId, projectId);
    return saved;
  }

  /**
   * Removes a collaborator from a project.
   *
   * @param projectId project UUID
   * @param requesterId user performing the removal (must be OWNER)
   * @param collaboratorUserId user to remove
   */
  @Transactional
  public void removeCollaborator(String projectId, Long requesterId, Long collaboratorUserId) {
    // Verify the requester is the project owner
    projectRepository.findByProjectIdAndUser_Id(projectId, requesterId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project not found or you are not the owner: " + projectId));

    collaboratorRepository.deleteByProjectIdAndUser_Id(projectId, collaboratorUserId);
    log.info("User {} removed collaborator {} from project {}",
        requesterId, collaboratorUserId, projectId);
  }

  /**
   * Gets all collaborators for a project.
   */
  public List<ProjectCollaborator> getCollaborators(String projectId) {
    return collaboratorRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
  }

  /**
   * Gets all projects shared with a user (where they are a collaborator).
   */
  public List<ProjectCollaborator> getSharedWithMe(Long userId) {
    return collaboratorRepository.findByUser_IdAndRoleNotOrderByCreatedAtDesc(
        userId, CollaboratorRole.OWNER);
  }

  /**
   * Gets the role of a user on a project.
   *
   * @return the role, or null if not a collaborator
   */
  public CollaboratorRole getUserRole(String projectId, Long userId) {
    // Check if user is the project owner
    Optional<TravelProject> ownedProject =
        projectRepository.findByProjectIdAndUser_Id(projectId, userId);
    if (ownedProject.isPresent()) {
      return CollaboratorRole.OWNER;
    }

    // Check collaborator role
    return collaboratorRepository.findByProjectIdAndUser_Id(projectId, userId)
        .map(ProjectCollaborator::getRole)
        .orElse(null);
  }

  /**
   * Checks if a user has at least the specified role on a project.
   */
  public boolean hasRole(String projectId, Long userId, CollaboratorRole minimumRole) {
    CollaboratorRole role = getUserRole(projectId, userId);
    if (role == null) return false;

    return roleLevel(role) >= roleLevel(minimumRole);
  }

  private int roleLevel(CollaboratorRole role) {
    return switch (role) {
      case OWNER -> 3;
      case EDITOR -> 2;
      case VIEWER -> 1;
    };
  }
}
