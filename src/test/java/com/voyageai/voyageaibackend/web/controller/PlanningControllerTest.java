package com.voyageai.voyageaibackend.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.security.JwtAuthenticationFilter;
import com.voyageai.voyageaibackend.security.JwtUtil;
import com.voyageai.voyageaibackend.service.PlanningService;
import com.voyageai.voyageaibackend.service.RedisTaskService;
import com.voyageai.voyageaibackend.service.TravelProjectService;
import com.voyageai.voyageaibackend.web.dto.PlanningRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link PlanningController}.
 */
@WebMvcTest(PlanningController.class)
@Import(JwtAuthenticationFilter.class)
class PlanningControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private PlanningService planningService;

  @MockBean
  private RedisTaskService taskService;

  @MockBean
  private TravelProjectService projectService;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private com.voyageai.voyageaibackend.service.ConversationHistoryService conversationHistoryService;

  @MockBean
  private com.voyageai.voyageaibackend.service.GeocodingService geocodingService;

  @Test
  @WithMockUser(username = "user@example.com")
  void generatePlan_validRequest_shouldReturnAccepted() throws Exception {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements("Plan a 7-day trip to Tokyo");
    String taskId = "task-123";

    // Mock user lookup (since @WithMockUser only provides username)
    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setEmail("user@example.com");
    mockUser.setDisplayName("Test User");
    mockUser.setAuthProvider(AuthProvider.LOCAL);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));

    // Mock project creation
    TravelProject mockProject = TravelProject.builder()
        .id(1L)
        .projectId("proj-123")
        .user(mockUser)
        .title("Plan a 7-day trip to...")
        .status(TravelProject.ProjectStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    when(projectService.createProject(any(Long.class), anyString(), any())).thenReturn(mockProject);

    when(planningService.submitPlanningRequest(anyString(), anyString(), anyString())).thenReturn(taskId);

    // When / Then
    mockMvc.perform(post("/api/planning/generate")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(taskId))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @WithMockUser
  void generatePlan_emptyRequirements_shouldReturnBadRequest() throws Exception {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements("");

    // When / Then
    mockMvc.perform(post("/api/planning/generate")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void getTaskStatus_existingTask_shouldReturnTask() throws Exception {
    // Given
    String taskId = "task-123";
    PlanningTask task = PlanningTask.builder()
        .taskId(taskId)
        .userId("user-456")
        .status(TaskStatus.COMPLETED)
        .requirements("Requirements")
        .result("{\"plan\": \"data\"}")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .completedAt(Instant.now())
        .build();

    when(taskService.getTask(taskId)).thenReturn(Optional.of(task));

    // When / Then
    mockMvc.perform(get("/api/planning/status/" + taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(taskId))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.requirements").value("Requirements"))
        .andExpect(jsonPath("$.result").value("{\"plan\": \"data\"}"));
  }

  @Test
  @WithMockUser
  void getTaskStatus_nonExistingTask_shouldReturnNotFound() throws Exception {
    // Given
    String taskId = "non-existing-id";
    when(taskService.getTask(taskId)).thenReturn(Optional.empty());

    // When / Then
    mockMvc.perform(get("/api/planning/status/" + taskId))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void getTaskStatus_failedTask_shouldReturnErrorMessage() throws Exception {
    // Given
    String taskId = "task-123";
    PlanningTask task = PlanningTask.builder()
        .taskId(taskId)
        .userId("user-456")
        .status(TaskStatus.FAILED)
        .requirements("Requirements")
        .errorMessage("API call failed")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .completedAt(Instant.now())
        .build();

    when(taskService.getTask(taskId)).thenReturn(Optional.of(task));

    // When / Then
    mockMvc.perform(get("/api/planning/status/" + taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(taskId))
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.requirements").value("Requirements"))
        .andExpect(jsonPath("$.errorMessage").value("API call failed"));
  }

  @Test
  void generatePlan_withoutAuthentication_shouldRequireAuth() throws Exception {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements("Plan a 7-day trip to Tokyo");

    // When / Then - Spring Security redirects to login (302) or returns 401/403
    mockMvc.perform(post("/api/planning/generate")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void getTaskStatus_withoutAuthentication_shouldRequireAuth() throws Exception {
    // When / Then - Spring Security redirects to login (302) or returns 401/403
    mockMvc.perform(get("/api/planning/status/task-123"))
        .andExpect(status().is3xxRedirection());
  }
}
