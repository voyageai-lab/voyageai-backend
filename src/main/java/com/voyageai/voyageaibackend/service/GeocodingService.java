package com.voyageai.voyageaibackend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary.Location;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for geocoding and location enrichment using Google Places API.
 * 
 * <p>This service provides:
 * <ul>
 *   <li>Location search by name and city</li>
 *   <li>Geographic coordinate lookup (latitude/longitude)</li>
 *   <li>Google Place ID retrieval</li>
 *   <li>Place type classification</li>
 * </ul>
 * 
 * <p>Use cases:
 * - Enrich AI-generated locations with precise coordinates
 * - Validate and correct location data
 * - Enable rich place details in frontend (photos, reviews, hours)
 * 
 * <p>API quota consideration:
 * - Google Places API has usage limits and costs
 * - Use sparingly: only enrich locations that need it
 * - Consider caching results for common places
 * - AI-generated coordinates are often accurate enough for initial display
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

  private final WebClient.Builder webClientBuilder;

  @Value("${google.maps.api.key:}")
  private String apiKey;

  private static final String PLACES_API_BASE_URL = 
      "https://maps.googleapis.com/maps/api/place";

  /**
   * Enriches a location with precise geographic data from Google Places API.
   * 
   * <p>This method:
   * <ol>
   *   <li>Searches for the place by name and city</li>
   *   <li>Retrieves latitude, longitude, place ID</li>
   *   <li>Returns enriched location or original if API fails</li>
   * </ol>
   * 
   * <p>Fallback strategy: If Google API fails or is unavailable,
   * returns the original location unchanged (AI-generated coordinates are still usable).
   *
   * @param placeName Place name (e.g., "Shinjuku Gyoen National Garden")
   * @param city City name (e.g., "Tokyo")
   * @return Mono of enriched Location
   */
  public Mono<Location> enrichLocation(String placeName, String city) {
    // If API key is not configured, return empty location
    if (apiKey == null || apiKey.isEmpty()) {
      log.debug("Google Maps API key not configured, skipping geocoding");
      return Mono.just(Location.builder()
          .name(placeName)
          .build());
    }

    String query = placeName + " " + city;
    
    return findPlaceFromText(query)
        .map(response -> {
          if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            PlaceCandidate candidate = response.getCandidates().get(0);
            
            return Location.builder()
                .name(placeName)
                .address(candidate.getFormattedAddress())
                .latitude(candidate.getGeometry().getLocation().getLat())
                .longitude(candidate.getGeometry().getLocation().getLng())
                .placeId(candidate.getPlaceId())
                .placeType(candidate.getTypes() != null && !candidate.getTypes().isEmpty() 
                    ? candidate.getTypes().get(0) 
                    : null)
                .build();
          } else {
            log.warn("No candidates found for place: {}", query);
            return Location.builder()
                .name(placeName)
                .build();
          }
        })
        .onErrorResume(error -> {
          log.error("Error enriching location '{}': {}", query, error.getMessage());
          // Fallback: return basic location without coordinates
          return Mono.just(Location.builder()
              .name(placeName)
              .build());
        });
  }

  /**
   * Searches for a place using Google Places API Find Place from Text.
   * 
   * <p>API endpoint: /place/findplacefromtext/json
   * 
   * <p>Fields requested:
   * - formatted_address: Full address
   * - geometry: Lat/lng coordinates
   * - place_id: Google Place ID
   * - types: Place type classification
   *
   * @param query Search query (place name + city)
   * @return Mono of PlacesResponse
   */
  private Mono<PlacesResponse> findPlaceFromText(String query) {
    WebClient webClient = webClientBuilder.baseUrl(PLACES_API_BASE_URL).build();

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/findplacefromtext/json")
            .queryParam("input", query)
            .queryParam("inputtype", "textquery")
            .queryParam("fields", "formatted_address,geometry,place_id,types")
            .queryParam("key", apiKey)
            .build())
        .retrieve()
        .bodyToMono(PlacesResponse.class)
        .doOnSuccess(response -> log.debug("Google Places API call successful for: {}", query))
        .doOnError(error -> log.error("Google Places API call failed for {}: {}", 
            query, error.getMessage()));
  }

  /**
   * Validates if geocoding is available (API key configured).
   *
   * @return true if geocoding is available
   */
  public boolean isGeocodingAvailable() {
    return apiKey != null && !apiKey.isEmpty();
  }

  // ==================== Google Places API Response DTOs ====================

  /**
   * Google Places API response.
   */
  @Data
  private static class PlacesResponse {
    private List<PlaceCandidate> candidates;
    private String status;
  }

  /**
   * Place candidate from Google Places API.
   */
  @Data
  private static class PlaceCandidate {
    @JsonProperty("formatted_address")
    private String formattedAddress;
    
    private Geometry geometry;
    
    @JsonProperty("place_id")
    private String placeId;
    
    private List<String> types;
  }

  /**
   * Geometry containing location coordinates.
   */
  @Data
  private static class Geometry {
    private LatLng location;
  }

  /**
   * Latitude and longitude coordinates.
   */
  @Data
  private static class LatLng {
    private Double lat;
    private Double lng;
  }
}

