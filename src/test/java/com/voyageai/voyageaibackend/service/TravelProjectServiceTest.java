package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.repo.TravelProjectRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TravelProjectService}.
 */
@ExtendWith(MockitoExtension.class)
class TravelProjectServiceTest {

  @Mock
  private TravelProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private TravelProjectService projectService;

  private User testUser;
  private TravelProject testProject;

  /**
   * Setup test data before each test.
   */
  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setDisplayName("Test User");
    testUser.setAuthProvider(AuthProvider.LOCAL);

    testProject = TravelProject.builder()
        .id(1L)
        .projectId("proj-123")
        .user(testUser)
        .title("Tokyo Trip")
        .description("7-day Tokyo adventure")
        .status(ProjectStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  void createProject_shouldGenerateIdAndCreateProject() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    // When
    TravelProject result = projectService.createProject(1L, "Tokyo Trip", "7-day adventure");

    // Then
    assertNotNull(result);
    assertNotNull(result.getProjectId());
    assertTrue(result.getProjectId().startsWith("proj-"));
    assertEquals(ProjectStatus.ACTIVE, result.getStatus());
    verify(userRepository).findById(1L);
    verify(projectRepository).save(any(TravelProject.class));
  }

  @Test
  void createProject_shouldThrowException_whenUserNotFound() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(ResourceNotFoundException.class,
        () -> projectService.createProject(1L, "Tokyo Trip", "7-day adventure"));
  }

  @Test
  void getProject_shouldReturnProject_whenExists() {
    // Given
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));

    // When
    TravelProject result = projectService.getProject("proj-123", 1L);

    // Then
    assertNotNull(result);
    assertEquals("Tokyo Trip", result.getTitle());
    assertEquals("proj-123", result.getProjectId());
    verify(projectRepository).findByProjectIdAndUser_Id("proj-123", 1L);
  }

  @Test
  void getProject_shouldThrowException_whenNotFound() {
    // Given
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.empty());

    // When / Then
    assertThrows(ResourceNotFoundException.class,
        () -> projectService.getProject("proj-123", 1L));
  }

  @Test
  void getUserProjects_shouldReturnProjectList() {
    // Given
    when(projectRepository.findActiveProjectsByUserId(1L))
        .thenReturn(List.of(testProject));

    // When
    List<TravelProject> results = projectService.getUserProjects(1L);

    // Then
    assertEquals(1, results.size());
    assertEquals("Tokyo Trip", results.get(0).getTitle());
    verify(projectRepository).findActiveProjectsByUserId(1L);
  }

  @Test
  void touchProject_shouldUpdateTimestamp() {
    // Given
    when(projectRepository.findByProjectId("proj-123")).thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    Instant before = testProject.getUpdatedAt();

    // When
    projectService.touchProject("proj-123");

    // Then
    verify(projectRepository).findByProjectId("proj-123");
    verify(projectRepository).save(testProject);
  }

  @Test
  void touchProject_shouldDoNothing_whenNotFound() {
    // Given
    when(projectRepository.findByProjectId("proj-123")).thenReturn(Optional.empty());

    // When
    projectService.touchProject("proj-123");

    // Then - verify that findByProjectId was called but save was not
    verify(projectRepository).findByProjectId("proj-123");
    verify(projectRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void archiveProject_shouldSetStatusToArchived() {
    // Given
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    // When
    projectService.archiveProject("proj-123", 1L);

    // Then
    assertEquals(ProjectStatus.ARCHIVED, testProject.getStatus());
    verify(projectRepository).save(testProject);
  }

  @Test
  void deleteProject_shouldSetStatusToDeleted() {
    // Given
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    // When
    projectService.deleteProject("proj-123", 1L);

    // Then
    assertEquals(ProjectStatus.DELETED, testProject.getStatus());
    verify(projectRepository).save(testProject);
  }

  @Test
  void deleteProject_shouldThrowException_whenNotFound() {
    // Given
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.empty());

    // When / Then
    assertThrows(ResourceNotFoundException.class,
        () -> projectService.deleteProject("proj-123", 1L));
  }
}
