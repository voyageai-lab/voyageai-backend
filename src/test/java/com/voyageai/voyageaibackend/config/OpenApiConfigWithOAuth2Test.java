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
 * Unit tests for OpenApiConfig with Google OAuth2 configured.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "google.client-id=test-client-id"
})
class OpenApiConfigWithOAuth2Test {

  @Autowired
  private OpenAPI openApi;

  @Test
  void customOpenApi_shouldHaveGoogleOAuthScheme_whenConfigured() {
    assertNotNull(openApi.getComponents());
    assertNotNull(openApi.getComponents().getSecuritySchemes());
    assertTrue(openApi.getComponents().getSecuritySchemes().containsKey("google-oauth2"));

    SecurityScheme oauthScheme = openApi.getComponents()
        .getSecuritySchemes().get("google-oauth2");
    assertEquals(SecurityScheme.Type.OAUTH2, oauthScheme.getType());
    assertNotNull(oauthScheme.getFlows());
    assertNotNull(oauthScheme.getFlows().getAuthorizationCode());
    assertNotNull(oauthScheme.getFlows().getAuthorizationCode().getAuthorizationUrl());
    assertNotNull(oauthScheme.getFlows().getAuthorizationCode().getTokenUrl());
    assertNotNull(oauthScheme.getFlows().getAuthorizationCode().getScopes());
    assertFalse(oauthScheme.getFlows().getAuthorizationCode().getScopes().isEmpty());
    assertNotNull(oauthScheme.getDescription());
  }

  @Test
  void customOpenApi_shouldHaveCorrectOAuthUrls() {
    SecurityScheme oauthScheme = openApi.getComponents()
        .getSecuritySchemes().get("google-oauth2");

    assertEquals("https://accounts.google.com/o/oauth2/v2/auth",
        oauthScheme.getFlows().getAuthorizationCode().getAuthorizationUrl());
    assertEquals("https://oauth2.googleapis.com/token",
        oauthScheme.getFlows().getAuthorizationCode().getTokenUrl());
  }

  @Test
  void customOpenApi_shouldHaveCorrectOAuthScopes() {
    SecurityScheme oauthScheme = openApi.getComponents()
        .getSecuritySchemes().get("google-oauth2");

    var scopes = oauthScheme.getFlows().getAuthorizationCode().getScopes();
    assertTrue(scopes.containsKey("openid"));
    assertTrue(scopes.containsKey("profile"));
    assertTrue(scopes.containsKey("email"));
    assertEquals("OpenID Connect", scopes.get("openid"));
    assertEquals("User profile information", scopes.get("profile"));
    assertEquals("User email address", scopes.get("email"));
  }
}

