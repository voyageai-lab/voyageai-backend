package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator;
import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator.CollaboratorRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ProjectCollaborator entity.
 */
@Repository
@SuppressWarnings("checkstyle:MethodName")
public interface ProjectCollaboratorRepository extends JpaRepository<ProjectCollaborator, Long> {

  /** Find all collaborators for a project. */
  List<ProjectCollaborator> findByProjectIdOrderByCreatedAtAsc(String projectId);

  /** Find a specific collaborator record. */
  Optional<ProjectCollaborator> findByProjectIdAndUser_Id(String projectId, Long userId);

  /** Check if a user is a collaborator on a project. */
  boolean existsByProjectIdAndUser_Id(String projectId, Long userId);

  /** Find all projects a user collaborates on (not as owner). */
  List<ProjectCollaborator> findByUser_IdAndRoleNotOrderByCreatedAtDesc(
      Long userId, CollaboratorRole excludeRole);

  /** Find all projects shared with a user (any role). */
  List<ProjectCollaborator> findByUser_IdOrderByCreatedAtDesc(Long userId);

  /** Delete a specific collaborator. */
  void deleteByProjectIdAndUser_Id(String projectId, Long userId);
}
