package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator;
import com.voyageai.voyageaibackend.domain.entity.ProjectCollaborator.CollaboratorRole;
import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.TravelProject.ProjectStatus;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.repo.ProjectCollaboratorRepository;
import com.voyageai.voyageaibackend.domain.repo.TravelProjectRepository;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.BusinessException;
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
 * Unit tests for {@link CollaborationService}.
 */
@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

  @Mock
  private ProjectCollaboratorRepository collaboratorRepository;

  @Mock
  private TravelProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CollaborationService collaborationService;

  private User owner;
  private User invitee;
  private TravelProject testProject;

  @BeforeEach
  void setUp() {
    owner = new User();
    owner.setId(1L);
    owner.setEmail("owner@example.com");
    owner.setDisplayName("Owner");
    owner.setAuthProvider(AuthProvider.LOCAL);

    invitee = new User();
    invitee.setId(2L);
    invitee.setEmail("invitee@example.com");
    invitee.setDisplayName("Invitee");
    invitee.setAuthProvider(AuthProvider.LOCAL);

    testProject = TravelProject.builder()
        .id(1L)
        .projectId("proj-123")
        .user(owner)
        .title("Tokyo Trip")
        .status(ProjectStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  void inviteCollaborator_shouldCreateRecord() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(userRepository.findByEmail("invitee@example.com"))
        .thenReturn(Optional.of(invitee));
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());
    when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

    ProjectCollaborator saved = ProjectCollaborator.builder()
        .id(10L).projectId("proj-123").user(invitee)
        .role(CollaboratorRole.EDITOR).invitedBy(owner)
        .createdAt(Instant.now()).build();
    when(collaboratorRepository.save(any(ProjectCollaborator.class))).thenReturn(saved);

    ProjectCollaborator result = collaborationService.inviteCollaborator(
        "proj-123", 1L, "invitee@example.com", CollaboratorRole.EDITOR);

    assertNotNull(result);
    assertEquals(CollaboratorRole.EDITOR, result.getRole());
    verify(collaboratorRepository).save(any(ProjectCollaborator.class));
  }

  @Test
  void inviteCollaborator_notOwner_shouldThrow() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> collaborationService.inviteCollaborator(
            "proj-123", 2L, "other@example.com", CollaboratorRole.VIEWER));
  }

  @Test
  void inviteCollaborator_inviteSelf_shouldThrow() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(userRepository.findByEmail("owner@example.com"))
        .thenReturn(Optional.of(owner));

    assertThrows(BusinessException.class,
        () -> collaborationService.inviteCollaborator(
            "proj-123", 1L, "owner@example.com", CollaboratorRole.EDITOR));
  }

  @Test
  void inviteCollaborator_ownerRole_shouldThrow() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(userRepository.findByEmail("invitee@example.com"))
        .thenReturn(Optional.of(invitee));

    assertThrows(BusinessException.class,
        () -> collaborationService.inviteCollaborator(
            "proj-123", 1L, "invitee@example.com", CollaboratorRole.OWNER));
  }

  @Test
  void inviteCollaborator_alreadyCollaborator_shouldThrow() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));
    when(userRepository.findByEmail("invitee@example.com"))
        .thenReturn(Optional.of(invitee));
    ProjectCollaborator existing = ProjectCollaborator.builder()
        .id(10L).projectId("proj-123").user(invitee)
        .role(CollaboratorRole.VIEWER).invitedBy(owner).build();
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.of(existing));

    assertThrows(BusinessException.class,
        () -> collaborationService.inviteCollaborator(
            "proj-123", 1L, "invitee@example.com", CollaboratorRole.EDITOR));
  }

  @Test
  void acceptInvitation_shouldSetAcceptedAt() {
    ProjectCollaborator collab = ProjectCollaborator.builder()
        .id(10L).projectId("proj-123").user(invitee)
        .role(CollaboratorRole.EDITOR).invitedBy(owner)
        .createdAt(Instant.now()).build();
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.of(collab));
    when(collaboratorRepository.save(any(ProjectCollaborator.class))).thenReturn(collab);

    ProjectCollaborator result = collaborationService.acceptInvitation("proj-123", 2L);

    assertNotNull(result.getAcceptedAt());
    verify(collaboratorRepository).save(collab);
  }

  @Test
  void acceptInvitation_noInvitation_shouldThrow() {
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> collaborationService.acceptInvitation("proj-123", 2L));
  }

  @Test
  void getUserRole_owner_shouldReturnOwner() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));

    CollaboratorRole role = collaborationService.getUserRole("proj-123", 1L);

    assertEquals(CollaboratorRole.OWNER, role);
  }

  @Test
  void getUserRole_collaborator_shouldReturnRole() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());
    ProjectCollaborator collab = ProjectCollaborator.builder()
        .id(10L).projectId("proj-123").user(invitee)
        .role(CollaboratorRole.EDITOR).invitedBy(owner).build();
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.of(collab));

    CollaboratorRole role = collaborationService.getUserRole("proj-123", 2L);

    assertEquals(CollaboratorRole.EDITOR, role);
  }

  @Test
  void getUserRole_noAccess_shouldReturnNull() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 3L))
        .thenReturn(Optional.empty());
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 3L))
        .thenReturn(Optional.empty());

    CollaboratorRole role = collaborationService.getUserRole("proj-123", 3L);

    assertNull(role);
  }

  @Test
  void hasRole_ownerHasEditorAccess_shouldReturnTrue() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));

    assertTrue(collaborationService.hasRole("proj-123", 1L, CollaboratorRole.EDITOR));
  }

  @Test
  void hasRole_viewerLacksEditorAccess_shouldReturnFalse() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());
    ProjectCollaborator collab = ProjectCollaborator.builder()
        .id(10L).projectId("proj-123").user(invitee)
        .role(CollaboratorRole.VIEWER).invitedBy(owner).build();
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.of(collab));

    assertFalse(collaborationService.hasRole("proj-123", 2L, CollaboratorRole.EDITOR));
  }

  @Test
  void hasRole_noAccess_shouldReturnFalse() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 3L))
        .thenReturn(Optional.empty());
    when(collaboratorRepository.findByProjectIdAndUser_Id("proj-123", 3L))
        .thenReturn(Optional.empty());

    assertFalse(collaborationService.hasRole("proj-123", 3L, CollaboratorRole.VIEWER));
  }

  @Test
  void getCollaborators_shouldReturnList() {
    ProjectCollaborator collab = ProjectCollaborator.builder()
        .id(10L).projectId("proj-123").user(invitee)
        .role(CollaboratorRole.EDITOR).invitedBy(owner).build();
    when(collaboratorRepository.findByProjectIdOrderByCreatedAtAsc("proj-123"))
        .thenReturn(List.of(collab));

    List<ProjectCollaborator> result = collaborationService.getCollaborators("proj-123");

    assertEquals(1, result.size());
  }

  @Test
  void removeCollaborator_asOwner_shouldDelete() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 1L))
        .thenReturn(Optional.of(testProject));

    collaborationService.removeCollaborator("proj-123", 1L, 2L);

    verify(collaboratorRepository).deleteByProjectIdAndUser_Id("proj-123", 2L);
  }

  @Test
  void removeCollaborator_notOwner_shouldThrow() {
    when(projectRepository.findByProjectIdAndUser_Id("proj-123", 2L))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> collaborationService.removeCollaborator("proj-123", 2L, 3L));
  }
}
