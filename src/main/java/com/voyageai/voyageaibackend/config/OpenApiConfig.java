package com.voyageai.voyageaibackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Configures JWT Bearer authentication and Google OAuth2 for testing.
 */
@Configuration
public class OpenApiConfig {

    @Value("${google.client-id:}")
    private String googleClientId;

    /**
     * Configures OpenAPI documentation with security schemes.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenApi() {
        // Build security schemes
        Components components = new Components()
            .addSecuritySchemes("bearer-jwt", createBearerScheme());
        
        // Add Google OAuth2 if configured
        if (googleClientId != null && !googleClientId.isEmpty()) {
            components.addSecuritySchemes("google-oauth2", createGoogleOAuthScheme());
        }

        return new OpenAPI()
            .info(new Info()
                .title("VoyageAI Backend API")
                .version("1.0")
                .description("AI-powered travel planning platform with authentication and OAuth2 support")
                .contact(new Contact()
                    .name("VoyageAI Team")
                    .email("support@voyageai.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .components(components)
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    /**
     * Creates JWT Bearer token security scheme.
     *
     * @return SecurityScheme for JWT
     */
    private SecurityScheme createBearerScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization")
            .description("Enter JWT token obtained from /api/auth/login or /api/auth/register");
    }

    /**
     * Creates Google OAuth2 security scheme for testing OAuth flow in Swagger.
     *
     * @return SecurityScheme for Google OAuth2
     */
    private SecurityScheme createGoogleOAuthScheme() {
        Scopes scopes = new Scopes()
            .addString("openid", "OpenID Connect")
            .addString("profile", "User profile information")
            .addString("email", "User email address");

        OAuthFlow authorizationCodeFlow = new OAuthFlow()
            .authorizationUrl("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUrl("https://oauth2.googleapis.com/token")
            .scopes(scopes);

        return new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .flows(new OAuthFlows().authorizationCode(authorizationCodeFlow))
            .description("Google OAuth2 login (requires application-secrets.properties configuration)");
    }
}

