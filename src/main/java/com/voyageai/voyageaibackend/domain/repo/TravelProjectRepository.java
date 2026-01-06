package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for TravelProject entity.
 * Provides database operations for travel planning projects.
 */
@Repository
@SuppressWarnings("checkstyle:MethodName")
public interface TravelProjectRepository extends JpaRepository<TravelProject, Long> {

  /**
   * Find project by projectId (UUID).
   *
   * @param projectId project UUID
   * @return optional project
   */
  Optional<TravelProject> findByProjectId(String projectId);

  /**
   * Find all projects for a user by status.
   *
   * @param userId user ID
   * @param status project status
   * @return list of projects
   */
  List<TravelProject> findByUser_IdAndStatusOrderByUpdatedAtDesc(Long userId, ProjectStatus status);

  /**
   * Find all active projects for a user.
   *
   * @param userId user ID
   * @return list of active projects
   */
  default List<TravelProject> findActiveProjectsByUserId(Long userId) {
    return findByUser_IdAndStatusOrderByUpdatedAtDesc(userId, ProjectStatus.ACTIVE);
  }

  /**
   * Find project by projectId and userId (for security).
   *
   * @param projectId project UUID
   * @param userId user ID
   * @return optional project
   */
  Optional<TravelProject> findByProjectIdAndUser_Id(String projectId, Long userId);

  /**
   * Check if project exists for user.
   *
   * @param projectId project UUID
   * @param userId user ID
   * @return true if exists
   */
  boolean existsByProjectIdAndUser_Id(String projectId, Long userId);

  /**
   * Count active projects for a user.
   *
   * @param userId user ID
   * @param status project status
   * @return count
   */
  long countByUser_IdAndStatus(Long userId, ProjectStatus status);
}

