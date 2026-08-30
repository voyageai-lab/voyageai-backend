package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectVisibility;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.repo.TravelProjectRepository;
import com.voyageai.voyageaibackend.exception.BusinessException;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ShareService}.
 */
@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

  @Mock
  private TravelProjectRepository projectRepository;

  @InjectMocks
  private ShareService shareService;

  private User testUser;
  private TravelProject testProject;

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
        .status(ProjectStatus.ACTIVE)
        .visibility(ProjectVisibility.PRIVATE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  void generateShareLink_shouldCreateTokenAndSetVisibility() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    TravelProject result = shareService.generateShareLink("proj-123", 1L);

    assertNotNull(result.getShareToken());
    assertTrue(result.getShareToken().length() > 20);
    assertEquals(ProjectVisibility.LINK_SHARED, result.getVisibility());
    assertNotNull(result.getShareTokenCreatedAt());
    verify(projectRepository).save(any(TravelProject.class));
  }

  @Test
  void generateShareLink_existingToken_shouldReturnExisting() {
    testProject.setShareToken("existing-token");
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));

    TravelProject result = shareService.generateShareLink("proj-123", 1L);

    assertEquals("existing-token", result.getShareToken());
  }

  @Test
  void generateShareLink_notOwner_shouldThrow() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> shareService.generateShareLink("proj-123", 2L));
  }

  @Test
  void revokeShareLink_shouldClearTokenAndSetPrivate() {
    testProject.setShareToken("some-token");
    testProject.setVisibility(ProjectVisibility.LINK_SHARED);
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    shareService.revokeShareLink("proj-123", 1L);

    assertNull(testProject.getShareToken());
    assertEquals(ProjectVisibility.PRIVATE, testProject.getVisibility());
    verify(projectRepository).save(testProject);
  }

  @Test
  void updateVisibility_toLinkShared_shouldGenerateToken() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    TravelProject result = shareService.updateVisibility(
        "proj-123", 1L, ProjectVisibility.LINK_SHARED);

    assertNotNull(result.getShareToken());
    assertEquals(ProjectVisibility.LINK_SHARED, result.getVisibility());
  }

  @Test
  void updateVisibility_toPrivate_shouldClearToken() {
    testProject.setShareToken("some-token");
    testProject.setVisibility(ProjectVisibility.LINK_SHARED);
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(TravelProject.class))).thenReturn(testProject);

    TravelProject result = shareService.updateVisibility(
        "proj-123", 1L, ProjectVisibility.PRIVATE);

    assertNull(result.getShareToken());
    assertEquals(ProjectVisibility.PRIVATE, result.getVisibility());
  }

  @Test
  void resolveShareToken_validToken_shouldReturnProject() {
    testProject.setShareToken("valid-token");
    testProject.setVisibility(ProjectVisibility.LINK_SHARED);
    when(projectRepository.findByShareToken("valid-token"))
        .thenReturn(Optional.of(testProject));

    TravelProject result = shareService.resolveShareToken("valid-token");

    assertNotNull(result);
    assertEquals("proj-123", result.getProjectId());
  }

  @Test
  void resolveShareToken_invalidToken_shouldThrow() {
    when(projectRepository.findByShareToken("bad-token"))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> shareService.resolveShareToken("bad-token"));
  }

  @Test
  void resolveShareToken_deletedProject_shouldThrow() {
    testProject.setShareToken("valid-token");
    testProject.setVisibility(ProjectVisibility.LINK_SHARED);
    testProject.setStatus(ProjectStatus.DELETED);
    when(projectRepository.findByShareToken("valid-token"))
        .thenReturn(Optional.of(testProject));

    assertThrows(BusinessException.class,
        () -> shareService.resolveShareToken("valid-token"));
  }

  @Test
  void resolveShareToken_privateProject_shouldThrow() {
    testProject.setShareToken("valid-token");
    testProject.setVisibility(ProjectVisibility.PRIVATE);
    when(projectRepository.findByShareToken("valid-token"))
        .thenReturn(Optional.of(testProject));

    assertThrows(BusinessException.class,
        () -> shareService.resolveShareToken("valid-token"));
  }
}
