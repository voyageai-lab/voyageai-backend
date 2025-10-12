package com.voyageai.voyageaibackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration.
 * Provides API documentation and testing interface.
 */
@Configuration
public class OpenApiConfig {

  /**
   * Configures OpenAPI documentation.
   *
   * @return the OpenAPI configuration
   */
  @Bean
  public OpenAPI customOpenApi() {
    final String securitySchemeName = "bearerAuth";
    
    return new OpenAPI()
        .info(new Info()
            .title("VoyageAI Backend API")
            .description("RESTful API for VoyageAI travel planning application")
            .version("1.0.0")
            .contact(new Contact()
                .name("VoyageAI Team")
                .email("support@voyageai.com")
            )
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")
            )
        )
        .addSecurityItem(new SecurityRequirement()
            .addList(securitySchemeName)
        )
        .components(new Components()
            .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token obtained from login/register endpoint")
            )
        );
  }
}

