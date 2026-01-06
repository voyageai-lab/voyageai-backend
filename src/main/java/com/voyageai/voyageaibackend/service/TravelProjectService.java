package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.TravelProjectRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing travel planning projects.
 * 
 * <p>A project represents a conversation/session where users can have
 * multiple iterations of planning tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TravelProjectService {

  private final TravelProjectRepository projectRepository;
  private final UserRepository userRepository;

  /**
   * Creates a new travel project for a user.
   *
   * @param userId User ID
   * @param title Project title
   * @param description Optional project description
   * @return Created project
   */
  @Transactional
  public TravelProject createProject(Long userId, String title, String description) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    String projectId = generateProjectId();
    Instant now = Instant.now();

    TravelProject project = TravelProject.builder()
        .projectId(projectId)
        .user(user)
        .title(title)
        .description(description)
        .status(ProjectStatus.ACTIVE)
        .createdAt(now)
        .updatedAt(now)
        .build();

    TravelProject savedProject = projectRepository.save(project);
    log.info("Created project {} for user {}: {}", projectId, userId, title);

    return savedProject;
  }

  /**
   * Gets a project by projectId.
   * Validates that the project belongs to the specified user.
   *
   * @param projectId Project ID
   * @param userId User ID (for security validation)
   * @return Project
   * @throws ResourceNotFoundException if project not found or doesn't belong to user
   */
  public TravelProject getProject(String projectId, Long userId) {
    return projectRepository.findByProjectIdAndUser_Id(projectId, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project not found or access denied: " + projectId));
  }

  /**
   * Gets all active projects for a user.
   *
   * @param userId User ID
   * @return List of active projects
   */
  public List<TravelProject> getUserProjects(Long userId) {
    return projectRepository.findActiveProjectsByUserId(userId);
  }

  /**
   * Updates a project's last updated timestamp.
   * Called when a new task is created in the project.
   *
   * @param projectId Project ID
   */
  @Transactional
  public void touchProject(String projectId) {
    projectRepository.findByProjectId(projectId).ifPresent(project -> {
      project.setUpdatedAt(Instant.now());
      projectRepository.save(project);
      log.debug("Updated timestamp for project {}", projectId);
    });
  }

  /**
   * Archives a project (makes it read-only).
   *
   * @param projectId Project ID
   * @param userId User ID (for security validation)
   */
  @Transactional
  public void archiveProject(String projectId, Long userId) {
    TravelProject project = getProject(projectId, userId);
    project.setStatus(ProjectStatus.ARCHIVED);
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);
    log.info("Archived project {}", projectId);
  }

  /**
   * Deletes a project (soft delete).
   *
   * @param projectId Project ID
   * @param userId User ID (for security validation)
   */
  @Transactional
  public void deleteProject(String projectId, Long userId) {
    TravelProject project = getProject(projectId, userId);
    project.setStatus(ProjectStatus.DELETED);
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);
    log.info("Deleted project {}", projectId);
  }

  /**
   * Generates a unique project ID.
   *
   * @return Unique project ID in format "proj-{uuid}"
   */
  private String generateProjectId() {
    return "proj-" + UUID.randomUUID().toString();
  }
}

