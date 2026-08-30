package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectVisibility;
import com.voyageai.voyageaibackend.domain.repo.TravelProjectRepository;
import com.voyageai.voyageaibackend.exception.BusinessException;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing project sharing and visibility.
 *
 * <p>Handles share-token generation, revocation, and public access
 * resolution. Tokens are cryptographically random 32-byte values
 * encoded as URL-safe Base64 (43 characters).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private final TravelProjectRepository projectRepository;

  /**
   * Generates a share link for a project.
   * Sets visibility to LINK_SHARED and creates a unique token.
   *
   * @param projectId project UUID
   * @param userId    owner user ID (for authorization)
   * @return the updated project with share token
   */
  @Transactional
  public TravelProject generateShareLink(String projectId, Long userId) {
    TravelProject project = projectRepository.findByProjectIdAndUser_Id(projectId, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project not found or access denied: " + projectId));

    if (project.getShareToken() != null) {
      log.info("Project {} already has a share token, returning existing", projectId);
      return project;
    }

    String token = generateToken();
    project.setShareToken(token);
    project.setShareTokenCreatedAt(Instant.now());
    if (project.getVisibility() == ProjectVisibility.PRIVATE) {
      project.setVisibility(ProjectVisibility.LINK_SHARED);
    }
    project.setUpdatedAt(Instant.now());

    TravelProject saved = projectRepository.save(project);
    log.info("Generated share token for project {}", projectId);
    return saved;
  }

  /**
   * Revokes the share link for a project.
   * Resets visibility to PRIVATE and clears the token.
   *
   * @param projectId project UUID
   * @param userId    owner user ID (for authorization)
   */
  @Transactional
  public void revokeShareLink(String projectId, Long userId) {
    TravelProject project = projectRepository.findByProjectIdAndUser_Id(projectId, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project not found or access denied: " + projectId));

    project.setShareToken(null);
    project.setShareTokenCreatedAt(null);
    project.setVisibility(ProjectVisibility.PRIVATE);
    project.setUpdatedAt(Instant.now());

    projectRepository.save(project);
    log.info("Revoked share token for project {}", projectId);
  }

  /**
   * Updates the visibility of a project.
   *
   * @param projectId  project UUID
   * @param userId     owner user ID
   * @param visibility new visibility level
   * @return updated project
   */
  @Transactional
  public TravelProject updateVisibility(String projectId, Long userId,
                                        ProjectVisibility visibility) {
    TravelProject project = projectRepository.findByProjectIdAndUser_Id(projectId, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project not found or access denied: " + projectId));

    if (visibility == ProjectVisibility.LINK_SHARED && project.getShareToken() == null) {
      project.setShareToken(generateToken());
      project.setShareTokenCreatedAt(Instant.now());
    }

    if (visibility == ProjectVisibility.PRIVATE) {
      project.setShareToken(null);
      project.setShareTokenCreatedAt(null);
    }

    project.setVisibility(visibility);
    project.setUpdatedAt(Instant.now());
    TravelProject saved = projectRepository.save(project);
    log.info("Updated visibility for project {} to {}", projectId, visibility);
    return saved;
  }

  /**
   * Resolves a share token to a project (public access, no auth required).
   *
   * @param token the share token
   * @return the project if token is valid and project is shared
   */
  public TravelProject resolveShareToken(String token) {
    TravelProject project = projectRepository.findByShareToken(token)
        .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired share link"));

    if (project.getStatus() != ProjectStatus.ACTIVE) {
      throw new BusinessException("This project is no longer available", "PROJECT_UNAVAILABLE");
    }

    if (project.getVisibility() == ProjectVisibility.PRIVATE) {
      throw new BusinessException("This project is private", "ACCESS_DENIED", 403);
    }

    return project;
  }

  /**
   * Generates a cryptographically secure URL-safe token.
   */
  private String generateToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
