package com.voyageai.voyageaibackend.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for replying to clarification questions.
 *
 * <p>Sent by the frontend when the user answers inline questions
 * from the agent's pre-flight analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClarificationReplyRequest {

  /**
   * User's answers to clarification questions.
   * Each entry should have "question" and "answer" keys.
   */
  @NotEmpty(message = "Answers cannot be empty")
  private List<Map<String, String>> answers;
}
