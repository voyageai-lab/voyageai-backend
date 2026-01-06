package com.voyageai.voyageaibackend.domain.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for {@link TravelProjectRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class TravelProjectRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private TravelProjectRepository travelProjectRepository;

  private User testUser;
  private TravelProject testProject;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setDisplayName("Test User");
    testUser.setAuthProvider(AuthProvider.LOCAL);
    testUser.setPasswordHash("hashedPassword");
    testUser.setCreatedAt(Instant.now());
    testUser = entityManager.persistAndFlush(testUser);

    // Create test project
    testProject = TravelProject.builder()
        .projectId("proj-123")
        .title("Test Project")
        .description("Test Description")
        .status(ProjectStatus.ACTIVE)
        .user(testUser)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    testProject = entityManager.persistAndFlush(testProject);
  }

  @Test
  void findByProjectId_existingProject_shouldReturnProject() {
    // When
    Optional<TravelProject> result = travelProjectRepository.findByProjectId("proj-123");

    // Then
    assertTrue(result.isPresent());
    assertEquals(testProject.getId(), result.get().getId());
    assertEquals("proj-123", result.get().getProjectId());
    assertEquals("Test Project", result.get().getTitle());
  }

  @Test
  void findByProjectId_nonExistingProject_shouldReturnEmpty() {
    // When
    Optional<TravelProject> result = travelProjectRepository.findByProjectId("non-existent");

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void findByUser_IdAndStatusOrderByUpdatedAtDesc_shouldReturnProjectsForUser() {
    // Given
    TravelProject anotherProject = TravelProject.builder()
        .projectId("proj-456")
        .title("Another Project")
        .status(ProjectStatus.ACTIVE)
        .user(testUser)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    entityManager.persistAndFlush(anotherProject);

    // When
    List<TravelProject> result = travelProjectRepository
        .findByUser_IdAndStatusOrderByUpdatedAtDesc(testUser.getId(), ProjectStatus.ACTIVE);

    // Then
    assertEquals(2, result.size());
    // Should be ordered by updatedAt desc
    assertTrue(result.get(0).getUpdatedAt().isAfter(result.get(1).getUpdatedAt()) 
        || result.get(0).getUpdatedAt().equals(result.get(1).getUpdatedAt()));
  }

  @Test
  void findByUser_IdAndStatusOrderByUpdatedAtDesc_differentStatus_shouldReturnEmpty() {
    // When
    List<TravelProject> result = travelProjectRepository
        .findByUser_IdAndStatusOrderByUpdatedAtDesc(testUser.getId(), ProjectStatus.ARCHIVED);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void findActiveProjectsByUserId_shouldReturnActiveProjects() {
    // Given
    TravelProject archivedProject = TravelProject.builder()
        .projectId("proj-archived")
        .title("Archived Project")
        .status(ProjectStatus.ARCHIVED)
        .user(testUser)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    entityManager.persistAndFlush(archivedProject);

    // When
    List<TravelProject> result = travelProjectRepository.findActiveProjectsByUserId(testUser.getId());

    // Then
    assertEquals(1, result.size());
    assertEquals(ProjectStatus.ACTIVE, result.get(0).getStatus());
    assertEquals("proj-123", result.get(0).getProjectId());
  }

  @Test
  void findByProjectIdAndUser_Id_existingProject_shouldReturnProject() {
    // When
    Optional<TravelProject> result = travelProjectRepository
        .findByProjectIdAndUser_Id("proj-123", testUser.getId());

    // Then
    assertTrue(result.isPresent());
    assertEquals(testProject.getId(), result.get().getId());
  }

  @Test
  void findByProjectIdAndUser_Id_wrongUser_shouldReturnEmpty() {
    // Given
    User anotherUser = new User();
    anotherUser.setEmail("another@example.com");
    anotherUser.setDisplayName("Another User");
    anotherUser.setAuthProvider(AuthProvider.LOCAL);
    anotherUser.setPasswordHash("hashedPassword");
    anotherUser.setCreatedAt(Instant.now());
    anotherUser = entityManager.persistAndFlush(anotherUser);

    // When
    Optional<TravelProject> result = travelProjectRepository
        .findByProjectIdAndUser_Id("proj-123", anotherUser.getId());

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void existsByProjectIdAndUser_Id_existingProject_shouldReturnTrue() {
    // When
    boolean exists = travelProjectRepository.existsByProjectIdAndUser_Id("proj-123", testUser.getId());

    // Then
    assertTrue(exists);
  }

  @Test
  void existsByProjectIdAndUser_Id_nonExistingProject_shouldReturnFalse() {
    // When
    boolean exists = travelProjectRepository.existsByProjectIdAndUser_Id("non-existent", testUser.getId());

    // Then
    assertFalse(exists);
  }

  @Test
  void countByUser_IdAndStatus_shouldReturnCorrectCount() {
    // Given
    TravelProject anotherActiveProject = TravelProject.builder()
        .projectId("proj-active-2")
        .title("Another Active Project")
        .status(ProjectStatus.ACTIVE)
        .user(testUser)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    entityManager.persistAndFlush(anotherActiveProject);

    TravelProject archivedProject = TravelProject.builder()
        .projectId("proj-archived")
        .title("Archived Project")
        .status(ProjectStatus.ARCHIVED)
        .user(testUser)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    entityManager.persistAndFlush(archivedProject);

    // When
    long activeCount = travelProjectRepository.countByUser_IdAndStatus(testUser.getId(), ProjectStatus.ACTIVE);
    long archivedCount = travelProjectRepository.countByUser_IdAndStatus(testUser.getId(), ProjectStatus.ARCHIVED);

    // Then
    assertEquals(2L, activeCount);
    assertEquals(1L, archivedCount);
  }
}
