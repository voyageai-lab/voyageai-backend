package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.service.GmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for Gmail trip detection.
 *
 * <p>Scans the authenticated user's Gmail for travel booking confirmations
 * (hotel reservations, flight bookings, event tickets) and returns structured
 * results that the frontend can present as trip suggestions.
 */
@Tag(name = "Gmail", description = "Gmail trip detection")
@RestController
@RequestMapping("/api/gmail")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class GmailController {

  private final GmailService gmailService;
  private final UserRepository userRepository;

  /**
   * Scan Gmail for travel bookings and return detected trips.
   *
   * <p>Requires the user to have logged in via Google OAuth2 with Gmail read scope.
   * The stored Gmail access token is forwarded to the Python service for scanning.
   *
   * @param principal the authenticated user
   * @return detected travel bookings
   */
  @Operation(summary = "Scan Gmail for travel bookings")
  @PostMapping("/scan-trips")
  public Mono<ResponseEntity<Map>> scanTrips(Principal principal) {
    User user = userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new IllegalStateException("User not found"));

    if (user.getGmailAccessToken() == null) {
      return Mono.just(ResponseEntity.badRequest().body(Map.of(
          "error", "Gmail access not authorized",
          "message", "Please log in with Google to enable Gmail trip detection"
      )));
    }

    return gmailService.scanTrips(user.getId())
        .map(ResponseEntity::ok)
        .onErrorResume(e -> {
          log.error("Gmail scan failed: {}", e.getMessage());
          return Mono.just(ResponseEntity.internalServerError().body(Map.of(
              "error", "Gmail scan failed",
              "message", e.getMessage()
          )));
        });
  }
}
