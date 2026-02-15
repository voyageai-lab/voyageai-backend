package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import com.voyageai.voyageaibackend.service.TravelProjectService;
import com.voyageai.voyageaibackend.web.dto.ProjectResponse;
import com.voyageai.voyageaibackend.web.dto.UpdateProjectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing travel projects.
 *
 * <p>Projects represent conversation threads for travel planning.
 * Each project contains a series of planning tasks and conversation messages.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Projects", description = "Travel project management endpoints")
public class ProjectController {

  private final TravelProjectService projectService;
  private final UserRepository userRepository;

  /**
   * Lists all active projects for the authenticated user.
   *
   * @param principal Authenticated user
   * @return List of projects ordered by last updated
   */
  @GetMapping
  @Operation(summary = "List user projects",
      description = "Returns all active travel projects for the current user")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<List<ProjectResponse>> listProjects(Principal principal) {
    User user = extractUser(principal);
    List<TravelProject> projects = projectService.getUserProjects(user.getId());

    List<ProjectResponse> response = projects.stream()
        .map(this::toResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  /**
   * Gets a single project by ID.
   *
   * @param projectId Project ID
   * @param principal Authenticated user
   * @return Project details
   */
  @GetMapping("/{projectId}")
  @Operation(summary = "Get project details",
      description = "Returns details of a specific travel project")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<ProjectResponse> getProject(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    TravelProject project = projectService.getProject(projectId, user.getId());
    return ResponseEntity.ok(toResponse(project));
  }

  /**
   * Updates a project (e.g., rename).
   *
   * @param projectId Project ID
   * @param request Update request
   * @param principal Authenticated user
   * @return Updated project
   */
  @PutMapping("/{projectId}")
  @Operation(summary = "Update project",
      description = "Updates project title or description")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<ProjectResponse> updateProject(
      @PathVariable String projectId,
      @Valid @RequestBody UpdateProjectRequest request,
      Principal principal
  ) {
    User user = extractUser(principal);
    TravelProject project = projectService.updateTitle(projectId, user.getId(), request.getTitle());
    return ResponseEntity.ok(toResponse(project));
  }

  /**
   * Archives (soft-deletes) a project.
   *
   * @param projectId Project ID
   * @param principal Authenticated user
   * @return 204 No Content
   */
  @DeleteMapping("/{projectId}")
  @Operation(summary = "Delete project",
      description = "Soft-deletes a travel project (can be restored)")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> deleteProject(
      @PathVariable String projectId,
      Principal principal
  ) {
    User user = extractUser(principal);
    projectService.deleteProject(projectId, user.getId());
    return ResponseEntity.noContent().build();
  }

  /**
   * Extracts the authenticated User entity from the Spring Security principal.
   */
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

  /**
   * Converts a TravelProject entity to a ProjectResponse DTO.
   */
  private ProjectResponse toResponse(TravelProject project) {
    return ProjectResponse.builder()
        .projectId(project.getProjectId())
        .title(project.getTitle())
        .description(project.getDescription())
        .status(project.getStatus().name())
        .createdAt(project.getCreatedAt())
        .updatedAt(project.getUpdatedAt())
        .build();
  }
}
