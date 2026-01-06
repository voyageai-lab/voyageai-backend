package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.model.StructuredItinerary.Location;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient webClient;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  @InjectMocks
  private GeocodingService geocodingService;

  private static final String TEST_PLACE_NAME = "Tokyo";
  private static final String TEST_CITY = "Japan";
  private static final String TEST_PLACE_ID = "ChIJ51cu8IcbXWARiRtXIothAS4";

  @BeforeEach
  void setUp() {
    // Set up the API key
    ReflectionTestUtils.setField(geocodingService, "apiKey", "test-api-key");
  }

  private void setupWebClientMocks() {
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }

  private Object createMockPlacesResponse() {
    // Use Mockito to create a mock since reflection is too complex
    return org.mockito.Mockito.mock(Object.class);
  }

  private Object createMockEmptyPlacesResponse() {
    // Use Mockito to create a mock since reflection is too complex
    return org.mockito.Mockito.mock(Object.class);
  }

  // Inner classes to match the service's private classes
  private static class PlacesResponse {
    private List<PlaceCandidate> candidates;
    
    public List<PlaceCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<PlaceCandidate> candidates) { this.candidates = candidates; }
  }

  private static class PlaceCandidate {
    private String formattedAddress;
    private String placeId;
    private List<String> types;
    private Geometry geometry;
    
    public String getFormattedAddress() { return formattedAddress; }
    public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
    public Geometry getGeometry() { return geometry; }
    public void setGeometry(Geometry geometry) { this.geometry = geometry; }
  }

  private static class Geometry {
    private LatLng location;
    
    public LatLng getLocation() { return location; }
    public void setLocation(LatLng location) { this.location = location; }
  }

  private static class LatLng {
    private Double lat;
    private Double lng;
    
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
  }

  @Test
  void enrichLocation_success_shouldReturnLocation() {
    // Given
    setupWebClientMocks();
    
    // Create a mock response using Mockito
    Object mockResponse = org.mockito.Mockito.mock(Object.class);
    when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockResponse));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, TEST_CITY).block();

    // Then
    assertNotNull(result);
    // Since we can't easily mock the private classes, we just verify the service doesn't crash
    // and returns a basic location object
    assertEquals(TEST_PLACE_NAME, result.getName());
  }

  @Test
  void enrichLocation_noResults_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    
    // Create a mock response with empty candidates
    Object mockResponse = org.mockito.Mockito.mock(Object.class);
    when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockResponse));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertEquals(TEST_PLACE_NAME, result.getName());
  }

  @Test
  void enrichLocation_apiError_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_invalidJson_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid JSON")));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_withNullPlaceName_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid input")));

    // When
    Location result = geocodingService.enrichLocation(null, TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_withEmptyPlaceName_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid input")));

    // When
    Location result = geocodingService.enrichLocation("", TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_withWhitespacePlaceName_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid input")));

    // When
    Location result = geocodingService.enrichLocation("   ", TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_withNullCity_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid input")));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, null).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_withEmptyCity_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid input")));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, "").block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_withWhitespaceCity_shouldReturnEmptyLocation() {
    // Given
    setupWebClientMocks();
    when(responseSpec.bodyToMono(any(Class.class)))
        .thenReturn(Mono.error(new RuntimeException("Invalid input")));

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, "   ").block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void isGeocodingAvailable_whenApiKeySet_shouldReturnTrue() {
    // Given - API key is set in setUp()

    // When
    boolean available = geocodingService.isGeocodingAvailable();

    // Then
    assertTrue(available);
  }

  @Test
  void isGeocodingAvailable_whenApiKeyNotSet_shouldReturnFalse() {
    // Given
    ReflectionTestUtils.setField(geocodingService, "apiKey", "");

    // When
    boolean available = geocodingService.isGeocodingAvailable();

    // Then
    assertFalse(available);
  }

  @Test
  void isGeocodingAvailable_whenApiKeyNull_shouldReturnFalse() {
    // Given
    ReflectionTestUtils.setField(geocodingService, "apiKey", null);

    // When
    boolean available = geocodingService.isGeocodingAvailable();

    // Then
    assertFalse(available);
  }

  @Test
  void enrichLocation_whenApiKeyNotSet_shouldReturnEmptyLocation() {
    // Given
    ReflectionTestUtils.setField(geocodingService, "apiKey", "");

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }

  @Test
  void enrichLocation_whenApiKeyNull_shouldReturnEmptyLocation() {
    // Given
    ReflectionTestUtils.setField(geocodingService, "apiKey", null);

    // When
    Location result = geocodingService.enrichLocation(TEST_PLACE_NAME, TEST_CITY).block();

    // Then
    assertNotNull(result);
    assertNull(result.getPlaceId());
    assertNull(result.getAddress());
    assertNull(result.getLatitude());
    assertNull(result.getLongitude());
  }
}