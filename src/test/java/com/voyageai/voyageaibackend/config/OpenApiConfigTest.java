package com.voyageai.voyageaibackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for OpenApiConfig.
 */
@SpringBootTest
class OpenApiConfigTest {

  @Autowired
  private OpenAPI openApi;

  @Test
  void customOpenApi_shouldHaveCorrectInfo() {
    assertNotNull(openApi);
    assertNotNull(openApi.getInfo());
    assertEquals("VoyageAI Backend API", openApi.getInfo().getTitle());
    assertEquals("1.0", openApi.getInfo().getVersion());
    assertTrue(openApi.getInfo().getDescription().contains("AI-powered"));
    assertNotNull(openApi.getInfo().getContact());
    assertEquals("VoyageAI Team", openApi.getInfo().getContact().getName());
    assertNotNull(openApi.getInfo().getLicense());
    assertEquals("Apache 2.0", openApi.getInfo().getLicense().getName());
  }

  @Test
  void customOpenApi_shouldHaveJwtSecurityScheme() {
    assertNotNull(openApi.getComponents());
    assertNotNull(openApi.getComponents().getSecuritySchemes());
    assertTrue(openApi.getComponents().getSecuritySchemes().containsKey("bearer-jwt"));

    SecurityScheme jwtScheme = openApi.getComponents().getSecuritySchemes().get("bearer-jwt");
    assertEquals(SecurityScheme.Type.HTTP, jwtScheme.getType());
    assertEquals("bearer", jwtScheme.getScheme());
    assertEquals("JWT", jwtScheme.getBearerFormat());
    assertEquals("Authorization", jwtScheme.getName());
    assertEquals(SecurityScheme.In.HEADER, jwtScheme.getIn());
    assertNotNull(jwtScheme.getDescription());
  }

  @Test
  void customOpenApi_shouldHaveSecurityRequirement() {
    assertNotNull(openApi.getSecurity());
    assertTrue(openApi.getSecurity().stream()
        .anyMatch(req -> req.containsKey("bearer-jwt")));
  }

  @Test
  void customOpenApi_shouldNotHaveGoogleOAuthScheme_whenNotConfigured() {
    // By default, google.client-id is empty, so OAuth2 should not be configured
    assertNotNull(openApi.getComponents());
    if (openApi.getComponents().getSecuritySchemes() != null) {
      assertFalse(openApi.getComponents().getSecuritySchemes()
          .containsKey("google-oauth2"));
    }
  }
}

