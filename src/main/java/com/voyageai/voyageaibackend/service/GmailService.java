package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for scanning Gmail travel bookings via the Python AI service.
 *
 * <p>Retrieves the user's stored Gmail access token, calls the Python service's
 * /api/v1/gmail/scan-trips endpoint, and returns detected travel bookings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GmailService {

  private final UserRepository userRepository;
  private final WebClient.Builder webClientBuilder;

  @Value("${python.service.url:http://localhost:8000}")
  private String pythonServiceUrl;

  /**
   * Scan a user's Gmail for travel bookings.
   *
   * @param userId the authenticated user's ID
   * @return Mono containing the scan results from the Python service
   * @throws IllegalStateException if user has no Gmail token
   */
  public Mono<Map> scanTrips(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

    String gmailToken = user.getGmailAccessToken();
    if (gmailToken == null || gmailToken.isBlank()) {
      return Mono.error(
          new IllegalStateException("Gmail access not authorized. Please re-login with Google.")
      );
    }

    log.info("Scanning Gmail for travel bookings, userId={}", userId);

    WebClient client = webClientBuilder.baseUrl(pythonServiceUrl).build();

    return client.post()
        .uri("/api/v1/gmail/scan-trips")
        .bodyValue(Map.of(
            "gmail_access_token", gmailToken,
            "max_results", 15,
            "include_body", true
        ))
        .retrieve()
        .bodyToMono(Map.class)
        .doOnSuccess(result -> log.info("Gmail scan complete for userId={}", userId))
        .doOnError(error -> log.error("Gmail scan failed for userId={}: {}", userId, error.getMessage()));
  }
}
