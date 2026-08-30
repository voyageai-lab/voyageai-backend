package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.util.ApiKeyEncryptor;
import com.voyageai.voyageaibackend.web.dto.UserSettingsDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearer-jwt")
public class SettingsController {

  private final UserRepository userRepository;
  private final ApiKeyEncryptor encryptor;

  @GetMapping
  public ResponseEntity<UserSettingsDto> getSettings() {
    User user = getAuthenticatedUser();
    if (user == null) return ResponseEntity.status(401).build();

    boolean hasKey = user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isBlank();
    String maskedKey = null;
    if (hasKey) {
      try {
        maskedKey = ApiKeyEncryptor.mask(encryptor.decrypt(user.getOpenaiApiKey()));
      } catch (Exception e) {
        log.warn("Failed to decrypt OpenAI API key for user {}", user.getId());
        hasKey = false;
      }
    }

    boolean hasGeminiKey = user.getGeminiApiKey() != null && !user.getGeminiApiKey().isBlank();
    String maskedGeminiKey = null;
    if (hasGeminiKey) {
      try {
        maskedGeminiKey = ApiKeyEncryptor.mask(encryptor.decrypt(user.getGeminiApiKey()));
      } catch (Exception e) {
        log.warn("Failed to decrypt Gemini API key for user {}", user.getId());
        hasGeminiKey = false;
      }
    }

    return ResponseEntity.ok(UserSettingsDto.builder()
        .openaiApiKey(maskedKey)
        .hasApiKey(hasKey)
        .geminiApiKey(maskedGeminiKey)
        .hasGeminiKey(hasGeminiKey)
        .displayName(user.getDisplayName())
        .email(user.getEmail())
        .avatarUrl(user.getAvatarUrl())
        .build());
  }

  @PutMapping
  public ResponseEntity<UserSettingsDto> updateSettings(@RequestBody UserSettingsDto dto) {
    User user = getAuthenticatedUser();
    if (user == null) return ResponseEntity.status(401).build();

    if (dto.getOpenaiApiKey() != null && !dto.getOpenaiApiKey().isBlank()) {
      String encrypted = encryptor.encrypt(dto.getOpenaiApiKey().strip());
      user.setOpenaiApiKey(encrypted);
      log.info("User {} updated OpenAI API key", user.getId());
    }
    if (dto.getGeminiApiKey() != null && !dto.getGeminiApiKey().isBlank()) {
      String encrypted = encryptor.encrypt(dto.getGeminiApiKey().strip());
      user.setGeminiApiKey(encrypted);
      log.info("User {} updated Gemini API key", user.getId());
    }
    if (dto.getDisplayName() != null && !dto.getDisplayName().isBlank()) {
      user.setDisplayName(dto.getDisplayName());
    }
    userRepository.save(user);

    return getSettings();
  }

  @DeleteMapping("/api-key")
  public ResponseEntity<UserSettingsDto> deleteApiKey() {
    User user = getAuthenticatedUser();
    if (user == null) return ResponseEntity.status(401).build();

    user.setOpenaiApiKey(null);
    userRepository.save(user);
    log.info("User {} removed OpenAI API key", user.getId());

    return getSettings();
  }

  @DeleteMapping("/gemini-key")
  public ResponseEntity<UserSettingsDto> deleteGeminiKey() {
    User user = getAuthenticatedUser();
    if (user == null) return ResponseEntity.status(401).build();

    user.setGeminiApiKey(null);
    userRepository.save(user);
    log.info("User {} removed Gemini API key", user.getId());

    return getSettings();
  }

  private User getAuthenticatedUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    return (User) auth.getPrincipal();
  }
}
