package com.voyageai.voyageaibackend.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDto {
  private String openaiApiKey;
  private boolean hasApiKey;
  private String geminiApiKey;
  private boolean hasGeminiKey;
  private String displayName;
  private String email;
  private String avatarUrl;
}
